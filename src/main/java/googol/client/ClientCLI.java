package googol.client;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Scanner;

import googol.common.GatewayService;
import googol.common.dto.SearchResult;

public class ClientCLI {
    private static String hostGateway;
    private static int portGateway;
    private static GatewayService gateway;
    private static int connectionRetries;
    private static int retryDelay;

    private static void reconnectGateway() throws Exception {
        int retries = connectionRetries;
        Exception lastException = null;
        while (retries-- > 0) {
            try {
                gateway = (GatewayService) Naming.lookup("rmi://" + hostGateway + ":" + portGateway + "/Gateway");
                System.out.println("[Client] Reconnected to Gateway at " + hostGateway + ":" + portGateway);
                return;
            } catch (Exception e) {
                lastException = e;
                if (retries > 0) {
                    System.out.println("[Client] Gateway not ready, retrying... (" + retries + " left) - " + e.getMessage());
                    Thread.sleep(retryDelay);
                }
            }
        }
        throw new Exception("Failed to connect to Gateway after multiple retries", lastException);
    }

    public static void main(String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            System.out.println("Looking up Gateway...");

            Properties cfg = new Properties();
            try (FileInputStream fis = new FileInputStream("client.properties")) {
                cfg.load(fis);
                } catch (Exception e) {
                    System.err.println("error loading client properties: " + e.getMessage());
                }
            hostGateway = System.getenv().getOrDefault("HOST_GATEWAY", cfg.getProperty("gateway.host"));
            portGateway = Integer.parseInt(System.getenv().getOrDefault("PORT_GATEWAY", cfg.getProperty("gateway.port")));

            connectionRetries = Integer.parseInt(cfg.getProperty("connection.retries", "3"));
            retryDelay = Integer.parseInt(cfg.getProperty("connection.retryDelay", "2000"));

            reconnectGateway();
            System.out.println("Connected. Commands: search <terms> | incoming <url> | putNew <url> | exit");

            while (true) {
                System.out.print("Commands: search <terms> | incoming <url> | putNew <url> | exit");
                System.out.print("\n> ");
                String line = in.nextLine().trim();
                if (line.equalsIgnoreCase("exit")) break;
                if (line.isBlank()) continue;

                try {
                    if (line.toLowerCase().startsWith("search ")) {
                    String[] terms = line.substring(7).trim().split("\\s+");
                    int page = 1;
                    while (true) {
                        SearchResult[] results = gateway.searchWord(terms, page);
                        if (results.length == 0) {
                            System.out.println("(no results on this page)");
                        } else {
                            int totalResults = results[0].totalResults;                   
                            int pageSize = 10;
                            int totalPages = (int) Math.ceil((double) totalResults / pageSize);
                            System.out.printf("--- Results page %d of %d --- %n", page, totalPages);
                            for (int i = 0; i < results.length; i++) {
                                SearchResult r = results[i];
                                System.out.printf("[%d] %s%n", i + 1, (r.title == null || r.title.isBlank()) ? r.url : r.title);
                                System.out.println("    " + r.url);
                                System.out.println("    (" + r.incomingLinksCount + " incomingLinks)");
                                System.out.println("    " + (r.text == null ? "" : r.text));
                            }
                        }
                        System.out.print("(n)ext, (p)rev, (q)uit: ");
                        String cmd = in.nextLine().trim().toLowerCase();
                        if (cmd.equals("n")) page++;
                        else if (cmd.equals("p") && page > 1) page--;
                        else if (cmd.equals("q")) break;
                        else System.out.println("Unknown command.");
                    }

                } else if (line.toLowerCase().startsWith("incoming ")) {
                    String url = line.substring(9).trim();
                    String[] inc = gateway.getIncomingLinks(url);
                    System.out.println("incomingLinks to " + url + " (" + inc.length + "):");
                    for (String s : inc) {
                        System.out.println("  " + s);
                    }

                } else if (line.toLowerCase().startsWith("putnew ")) {
                    String url = line.substring(6).trim();
                    gateway.putNewURL(url);
                    System.out.println("dei queue: " + url);

                } else {
                    System.out.println("Unknown command.");
                }
                } catch (RemoteException e) {
                    System.err.println("[Client] Connection error: " + e.getMessage());
                    System.err.println("[Client] Attempting to reconnect to Gateway...");
                    try {
                        reconnectGateway();
                        System.err.println("[Client] Reconnected successfully. Please retry your command.");
                    } catch (Exception reconnectEx) {
                        System.err.println("[Client] Failed to reconnect: " + reconnectEx.getMessage());
                        System.err.println("[Client] Client continues running...");
                    }
                } catch (Exception e) {
                    System.err.println("Error processing command: " + e.getMessage());
                    System.err.println("Client continues running...");
                }
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
