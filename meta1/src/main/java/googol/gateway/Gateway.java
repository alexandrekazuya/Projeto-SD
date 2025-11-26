package googol.gateway;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import googol.common.BarrelService;
import googol.common.GatewayService;
import googol.common.SimpleClientCallback;
import googol.common.dto.SearchResult;

public class Gateway extends UnicastRemoteObject implements GatewayService {

    private final List<BarrelService> barrels = new ArrayList<>();
    private final List<String> barrelNames = new ArrayList<>();
    private final List<String> barrelHosts = new ArrayList<>();
    private final List<Integer> barrelPorts = new ArrayList<>();

    private int maxQueueSize;
    private int maxRetries;
    private final Queue<String> urlsToIndex = new ConcurrentLinkedQueue<>();

    // Round-robin counter
    private int nextBarrelIndex = 0;

    // stats tracking
    private final List<SimpleClientCallback> registeredClients = new CopyOnWriteArrayList<>();
    private final Map<String, Long> searchCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> barrelTotalTime = new ConcurrentHashMap<>();
    private final Map<String, Long> barrelSearchCount = new ConcurrentHashMap<>();
    private String[][] currentTop10 = new String[0][0];

    public Gateway(Properties cfg) throws Exception {
        try {
            maxQueueSize = Integer.parseInt(cfg.getProperty("queue.maxSize"));
            maxRetries = Integer.parseInt(cfg.getProperty("max.retries", "3"));

            int i = 1;
            int added = 0;
            while (true) {
                String prefix = "barrel" + i + ".";
                String name = cfg.getProperty(prefix + "name");
                String host = cfg.getProperty(prefix + "host");
                String portStr = cfg.getProperty(prefix + "port");
                if (name == null || host == null || portStr == null)
                    break;

                int port = Integer.parseInt(portStr);
                try {
                    Registry reg = LocateRegistry.getRegistry(host, port);
                    BarrelService barrelProxy = (BarrelService) reg.lookup(name);
                    barrels.add(barrelProxy);
                    barrelNames.add(name);
                    barrelHosts.add(host);
                    barrelPorts.add(port);
                    System.out.println("Connected to " + name + ":" + port);
                    added++;
                } catch (Exception e) {
                    System.err.println("Failed to connect to " + name + ": " + e.getMessage());
                }
                i++;
            }
            if (added == 0)
                System.err.println("No barrels configured or connected.");
        } catch (Exception e) {
            System.err.println("Gateway init issue: " + e.getMessage());
        }
    }

    // client -> gateway: search; gateway -> barrel: search
    @Override
    public SearchResult[] searchWord(String[] terms, int page) throws RemoteException {
        if (terms == null || terms.length == 0)
            return new SearchResult[0];

        // limpar o texto
        List<String> temp = new ArrayList<>();
        for (String s : terms) {
            if (s != null && !s.isBlank()) {
                temp.add(s);
            }
        }
        String[] clean = temp.toArray(String[]::new);
        if (clean.length == 0)
            return new SearchResult[0];
        if (page < 1)
            page = 1;

        // Track search query
        String query = String.join(" ", clean);
        searchCounters.merge(query, 1L, Long::sum);

        int n = barrels.size();
        if (n == 0)
            throw new RemoteException("No barrels available");

        boolean searched = false;
        int attempts = 0;
        int maxAttempts = n * maxRetries;
        int startIndex = nextBarrelIndex;

        while (!searched && attempts < maxAttempts) {
            int i = (startIndex + attempts) % n;
            BarrelService barrelProxy = barrels.get(i);
            String bName = barrelNames.get(i);

            long startTime = System.nanoTime();
            try {
                SearchResult[] results = barrelProxy.searchWord(clean, page);
                long elapsed = System.nanoTime() - startTime;

                // Track stats and update top 10
                trackBarrelStats(bName, elapsed);

                // Update round-robin counter for next request
                nextBarrelIndex = (i + 1) % n;

                return results;
            } catch (RemoteException e) {
                System.err.println("search failed on " + bName + ": " + e.getMessage());

                try {
                    Registry reg = LocateRegistry.getRegistry(barrelHosts.get(i), barrelPorts.get(i));
                    BarrelService newBarrelProxy = (BarrelService) reg.lookup(bName);
                    barrels.set(i, newBarrelProxy);
                    System.out.println("[Gateway] Reconnected to " + bName);

                    startTime = System.nanoTime();
                    SearchResult[] results = newBarrelProxy.searchWord(clean, page);
                    long elapsed = System.nanoTime() - startTime;

                    trackBarrelStats(bName, elapsed);

                    // Update round-robin counter for next request
                    nextBarrelIndex = (i + 1) % n;

                    return results;
                } catch (Exception e2) {
                    System.out.println("Reconnect failed for " + bName + ": " + e2.getMessage());
                }
            }
            attempts++;
        }
        throw new RemoteException("All barrels failed for searchWord()");
    }

    // client ->gateway: incoming links; gateway ->barrel: incoming links
    @Override
    public String[] getIncomingLinks(String url) throws RemoteException {
        if (url == null || url.isBlank())
            return new String[0];

        int n = barrels.size();
        if (n == 0)
            throw new RemoteException("No barrels available");

        boolean searched = false;
        int attempts = 0;
        int maxAttempts = n * maxRetries;

        while (!searched && attempts < maxAttempts) {
            int i = attempts % n;
            BarrelService barrelProxy = barrels.get(i);
            String bName = barrelNames.get(i);

            try {
                return barrelProxy.getIncomingLinks(url);
            } catch (RemoteException e) {
                System.err.println("incoming-links failed on " + bName + ": " + e.getMessage());
                try {
                    Registry reg = LocateRegistry.getRegistry(barrelHosts.get(i), barrelPorts.get(i));
                    BarrelService newBarrelProxy = (BarrelService) reg.lookup(bName);
                    barrels.set(i, newBarrelProxy);
                    System.out.println("[Gateway] Reconnected to " + bName);
                    return newBarrelProxy.getIncomingLinks(url);
                } catch (Exception e1) {
                    System.out.println("Reconnect failed for " + bName + ": " + e1.getMessage());
                }
            }
            attempts++;
        }
        throw new RemoteException("All barrels failed for getIncomingLinks()");
    }

    // client-> gateway: putNewURL
    @Override
    public void putNewURL(String url) throws RemoteException {
        if (url == null || url.isBlank())
            return;
        String u = url.trim();
        /*
         * if (urlsToIndex.size() >= maxQueueSize) {
         * System.out.println("[Gateway] queue full (" + maxQueueSize + "), ignoring: "
         * + u);
         * return;
         * }
         */
        urlsToIndex.add(u);
    }

    // downloader-> gateway: takeNext
    @Override
    public String takeNext() throws RemoteException {
        return urlsToIndex.poll();
    }

    @Override
    public void registerClient(SimpleClientCallback client) throws RemoteException {
        if (!registeredClients.contains(client)) {
            registeredClients.add(client);
            System.out.println(
                    "[Gateway] Client registered for stats updates (total clients: " + registeredClients.size() + ")");

            // Send initial stats
            try {
                String stats = buildPlainStatsString(currentTop10, getBarrelStatus(), getAverageResponseTimes());
                client.updateStatsString(stats);
            } catch (RemoteException e) {
                System.err.println("[Gateway] Failed to send initial stats: " + e.getMessage());
                registeredClients.remove(client);
                throw e;
            }
        }
    }

    @Override
    public void unregisterClient(SimpleClientCallback client) throws RemoteException {
        registeredClients.remove(client);
        System.out.println("[Gateway] Client unregistered from stats updates");
    }

    @Override
    public String[][] getTop10Searches() throws RemoteException {
        return currentTop10;
    }

    @Override
    public String getPlainStatsString() throws RemoteException {
        return buildPlainStatsString(currentTop10, getBarrelStatus(), getAverageResponseTimes());
    }

    // update às stats no barrel
    private void trackBarrelStats(String barrelName, long elapsed) {
        barrelTotalTime.put(barrelName, barrelTotalTime.getOrDefault(barrelName, 0L) + elapsed);
        barrelSearchCount.put(barrelName, barrelSearchCount.getOrDefault(barrelName, 0L) + 1);
        updateTop10IfChanged();
    }

    // ir buscar um map: barrel name -> index size
    public Map<String, Integer> getBarrelStatus() {
        Map<String, Integer> status = new HashMap<>();
        for (int i = 0; i < barrels.size(); i++) {
            try {
                int indexSize = barrels.get(i).getIndexSize();
                status.put(barrelNames.get(i), indexSize);
            } catch (RemoteException e) {
                System.err.println("[Gateway] Failed to get status for " + barrelNames.get(i));
            }
        }
        return status;
    }

    // fazer o average tempo de search
    public Map<String, Double> getAverageResponseTimes() {
        Map<String, Double> avgTimes = new HashMap<>();
        for (String barrelName : barrelSearchCount.keySet()) {
            Long totalTime = barrelTotalTime.get(barrelName);
            Long count = barrelSearchCount.get(barrelName);

            if (totalTime != null && count != null && count > 0) {
                double avgNanos = (double) totalTime / count;
                double deciseconds = avgNanos / 100_000_000.0; // nanos to deciseconds
                avgTimes.put(barrelName, Math.round(deciseconds * 10.0) / 10.0);
            }
        }
        return avgTimes;
    }

    private void updateTop10IfChanged() {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(searchCounters.entrySet());
        entries.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

        // sacar top 10
        int limit = Math.min(10, entries.size());// se houver menos de 10, pega no q ha
        String[][] newTop10 = new String[limit][2];
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            newTop10[i][0] = entry.getKey(); // getKey é a search term
            newTop10[i][1] = String.valueOf(entry.getValue());// value é o count
        }

        String oldStr = java.util.Arrays.deepToString(currentTop10);
        String newStr = java.util.Arrays.deepToString(newTop10);

        if (!oldStr.equals(newStr)) {
            currentTop10 = newTop10;
            Map<String, Integer> barrelStatus = getBarrelStatus();
            Map<String, Double> responseTimes = getAverageResponseTimes();

            String statsString = buildPlainStatsString(newTop10, barrelStatus, responseTimes);

            notifyClients(client -> client.updateStatsString(statsString));
        }
    }

    private String buildPlainStatsString(String[][] top10, Map<String, Integer> barrelStatus,
            Map<String, Double> responseTimes) {
        StringBuilder sb = new StringBuilder();

        if (top10 != null && top10.length > 0) {
            for (int i = 0; i < top10.length; i++) {
                String term = (top10[i] != null && top10[i].length > 0) ? top10[i][0] : "";
                String count = (top10[i] != null && top10[i].length > 1) ? top10[i][1] : "0";
                sb.append(term).append(" (").append(count).append(")\n");
            }
        } else {
            sb.append("No searches yet\n");
        }

        sb.append("\nLINHA SEPARACAO\n");

        java.util.Set<String> barrelNames = new java.util.LinkedHashSet<>();
        barrelNames.addAll(barrelStatus.keySet());
        barrelNames.addAll(responseTimes.keySet());

        if (barrelNames.isEmpty()) {
            sb.append("No barrels available\n");
        } else {
            for (String name : barrelNames) {
                Integer idx = barrelStatus.getOrDefault(name, 0);
                Double avg = responseTimes.get(name);
                sb.append(name).append(" - ").append(idx).append(" entries");
                if (avg != null)
                    sb.append(" - avg ").append(avg).append(" ds");
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private void notifyClients(SimpleClientNotification notification) {
        List<SimpleClientCallback> failedClients = new ArrayList<>();
        for (SimpleClientCallback client : registeredClients) {
            try {
                notification.notify(client);
            } catch (RemoteException e) {
                System.err.println("[Gateway] Failed to notify client, removing: " + e.getMessage());
                failedClients.add(client);
            }
        }
        registeredClients.removeAll(failedClients);
    }

    @FunctionalInterface
    private interface SimpleClientNotification {
        void notify(SimpleClientCallback client) throws RemoteException;
    }

    public static void main(String[] args) {
        try {
            Properties cfg = new Properties();
            try (FileInputStream fis = new FileInputStream("gateway.properties")) {
                cfg.load(fis);
            } catch (Exception e) {
                System.err.println("error loading gateway properties: " + e.getMessage());
            }

            String bindHost = cfg.getProperty("gateway.host");
            int bindPort = Integer.parseInt(cfg.getProperty("gateway.port"));

            Gateway gw = new Gateway(cfg);

            System.setProperty("java.rmi.server.hostname", bindHost);
            try {
                LocateRegistry.createRegistry(bindPort);
            } catch (RemoteException e1) {
                System.out.println("[Gateway] RMI registry already running on port " + bindPort + ", reusing.");
            }

            Naming.rebind("rmi://" + bindHost + ":" + bindPort + "/Gateway", gw);
            System.out.println("Gateway bound as rmi://" + bindHost + ":" + bindPort + "/Gateway - ready.");

        } catch (Exception e) {
            System.err.println("Gateway error:" + e.getMessage());
        }
    }
}
