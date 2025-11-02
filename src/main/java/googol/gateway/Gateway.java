package googol.gateway;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import googol.common.BarrelService;
import googol.common.GatewayService;
import googol.common.dto.SearchResult;


public class Gateway extends UnicastRemoteObject implements GatewayService {

    private final List<BarrelService> barrels = new ArrayList<>();
    private final List<String> barrelNames = new ArrayList<>();
    private final List<String> barrelHosts = new ArrayList<>();
    private final List<Integer> barrelPorts = new ArrayList<>();
    private final java.util.concurrent.atomic.AtomicBoolean flip = new java.util.concurrent.atomic.AtomicBoolean();

    private int maxQueueSize;
    private final Queue<String> urlsToIndex = new ConcurrentLinkedQueue<>();

    public Gateway(Properties cfg) throws Exception {
        try {
            maxQueueSize = Integer.parseInt(cfg.getProperty("queue.maxSize"));

            int i = 1; int added = 0;
            while (true) {
                String prefix = "barrel" + i + ".";
                String name = cfg.getProperty(prefix + "name");
                String host = cfg.getProperty(prefix + "host");
                String portStr = cfg.getProperty(prefix + "port");
                if (name == null || host == null || portStr == null) break;

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
            if (added == 0) System.err.println("No barrels configured or connected.");
        } catch (Exception e) {
            System.err.println("Gateway init issue: " + e.getMessage());
        }
    }    

    //client -> gateway: search;  gateway -> barrel: search
    @Override
    public SearchResult[] searchWord(String[] terms, int page) throws RemoteException {
        if (terms == null || terms.length == 0) return new SearchResult[0];

        //limpar o texto
        List<String> temp = new ArrayList<>();
        for (String s : terms) {
            if (s != null && !s.isBlank()) {
                temp.add(s);
            }
        }
        String[] clean = temp.toArray(String[]::new);
        if (clean.length == 0) return new SearchResult[0];
        if (page < 1) page = 1;

        //balance ao load
        int n= barrels.size();
        if(n==0) throw new RemoteException("No barrels available");
        int start = flip.getAndSet(!flip.get()) ? 1 : 0;

        for (int off = 0; off < n; off++) {
            int i = (start + off) % n;
            BarrelService barrelProxy = barrels.get(i);
            String bName = barrelNames.get(i);
            try {
                return barrelProxy.searchWord(clean, page);
            } catch (RemoteException e) {
                System.err.println("search failed on " + bName + ": " + e.getMessage());
                
                try {
                    Registry reg = LocateRegistry.getRegistry(barrelHosts.get(i), barrelPorts.get(i));
                    BarrelService newBarrelProxy = (BarrelService) reg.lookup(bName);
                    barrels.set(i, newBarrelProxy);
                    
                    System.out.println("[Gateway] Reconnected to " + bName + " at " + barrelHosts.get(i) + ":" + barrelPorts.get(i));
                    return newBarrelProxy.searchWord(clean, page);
                } catch (Exception e2) {
                    System.out.println("Reconnection attempt to " + bName + " failed: " + e2.getMessage());
                }
            }
        }
        throw new RemoteException("All barrels failed for searchWord()");
    }

    //client ->gateway: incoming links; gateway ->barrel: incoming links
    @Override
    public String[] getIncomingLinks(String url) throws RemoteException {
        if (url == null || url.isBlank()) return new String[0];
        for (int i = 0; i < barrels.size(); i++) {
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
                    System.out.println("[Gateway] Reconnected to " + bName + " at " + barrelHosts.get(i) + ":" + barrelPorts.get(i));
                    return newBarrelProxy.getIncomingLinks(url);
                } catch (Exception e1) {
                    System.out.println("Reconnection attempt to " + bName + " failed: " + e1.getMessage());
                }
            }
        }
        throw new RemoteException("All barrels failed for getIncomingLinks()");
    }
    //client-> gateway: putNewURL
    @Override
    public void putNewURL(String url) throws RemoteException {
        if (url == null || url.isBlank()) return;
        String u = url.trim();
        /*if (urlsToIndex.size() >= maxQueueSize) {
            System.out.println("[Gateway] queue full (" + maxQueueSize + "), ignoring: " + u);
            return;
        }*/
        urlsToIndex.add(u);
    }

    //downloader-> gateway: takeNext
    @Override
    public String takeNext() throws RemoteException {
        String u = urlsToIndex.poll();
        return u;
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
