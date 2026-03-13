package googol.downloader;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import googol.common.BarrelService;
import googol.common.GatewayService;
import googol.common.dto.PageDTO;

public class Downloader implements Runnable {

    private volatile GatewayService gateway;
    private volatile BarrelService barrel1;
    private volatile BarrelService barrel2;

    // Connection info para poder dar relookup
    private final String hostGateway;
    private final int portGateway;
    private final String hostBarrel1;
    private final int portBarrel1;
    private final String hostBarrel2;
    private final int portBarrel2;
    
    // Retry settings
    private final int connectionRetries;
    private final int retryDelay;

    public Downloader(String hostGateway, int portGateway,
                      String hostBarrel1, int portBarrel1,
                      String hostBarrel2, int portBarrel2,
                      int connectionRetries, int retryDelay) throws Exception {
        this.hostGateway = hostGateway;
        this.portGateway = portGateway;
        this.hostBarrel1 = hostBarrel1;
        this.portBarrel1 = portBarrel1;
        this.hostBarrel2 = hostBarrel2;
        this.portBarrel2 = portBarrel2;
        this.connectionRetries = connectionRetries;
        this.retryDelay = retryDelay;

        refreshGateway();
        refreshBarrel1();
        refreshBarrel2();
    }

    private void refreshGateway() throws Exception {
        int retries = connectionRetries;
        while (retries-- > 0) {
            try {
                this.gateway = (GatewayService) Naming.lookup("rmi://" + hostGateway + ":" + portGateway + "/Gateway");
                System.out.println("[Downloader] Connected to Gateway at " + hostGateway + ":" + portGateway);
                return;
            } catch (Exception e) {
                System.out.println("[Downloader] Gateway not ready, retrying... (" + retries + " left)");
                Thread.sleep(retryDelay);
            }
        }
        throw new Exception("Failed to connect to Gateway after multiple retries");
    }

    private void refreshBarrel1() throws Exception {
        int retries = connectionRetries;
        while (retries-- > 0) {
            try {
                this.barrel1 = (BarrelService) Naming.lookup("rmi://" + hostBarrel1 + ":" + portBarrel1 + "/Barrel1");
                System.out.println("[Downloader] Connected to Barrel 1 at " + hostBarrel1 + ":" + portBarrel1);
                return;
            } catch (Exception e) {
                System.out.println("[Downloader] Barrel 1 not ready, retrying... (" + retries + " left)");
                Thread.sleep(retryDelay);
            }
        }
        throw new Exception("Failed to connect to Barrel 1 after multiple retries");
    }

    private void refreshBarrel2() throws Exception {
        int retries = connectionRetries;
        while (retries-- > 0) {
            try {
                this.barrel2 = (BarrelService) Naming.lookup("rmi://" + hostBarrel2 + ":" + portBarrel2 + "/Barrel2");
                System.out.println("[Downloader] Connected to Barrel 2 at " + hostBarrel2 + ":" + portBarrel2);
                return;
            } catch (Exception e) {
                System.out.println("[Downloader] Barrel 2 not ready, retrying... (" + retries + " left)");
                Thread.sleep(retryDelay);
            }
        }
        throw new Exception("Failed to connect to Barrel 2 after multiple retries");
    }

    @Override
    public void run() {
        while (true) {
            try {
                String url;
                try {
                    url = gateway.takeNext();
                } catch (Exception ge) {
                    System.err.println("[Downloader] Gateway call failed (takeNext): " + ge.getMessage());
                    try { 
                        refreshGateway();
                    } catch (Exception e) {
                        System.err.println("[Downloader] Failed to refresh Gateway: " + e.getMessage());
                    }
                    Thread.sleep(500);
                    continue;
                }
                if (url == null) {
                    Thread.sleep(500); //poupar o pc
                    continue;
                }

                Document doc = Jsoup.connect(url).get();

                //extract title e text
                String title = (doc.title() != null) ? doc.title() : "";
                String text = (doc.body() != null) ? doc.body().text() : "";

                Set<String> outgoing = collectOutgoingURLs(doc);

                //build pagedto
                PageDTO page = new PageDTO(url, title, text, outgoing);

                //send to barrels, se falhar tenta o outro, se falhar os 2 tenta outra vez
                boolean delivered1 = false;
                boolean delivered2 = false;

                while (!(delivered1 || delivered2)) {
                    if (!delivered1) {
                        try {
                            barrel1.sendPage(page);
                            delivered1 = true; 
                            System.out.println("[Downloader] Sent page to Barrel 1");
                        } catch (Exception e1) {
                            System.err.println("[Downloader] Failed to send page to Barrel 1: " + e1.getMessage());
                            try {
                                refreshBarrel1();
                            } catch (Exception e) {
                                System.err.println("[Downloader] Failed to refresh Barrel 1: " + e.getMessage());
                            }
                        }
                    }
                    if (!delivered2) {
                        try {
                            barrel2.sendPage(page);
                            delivered2 = true;
                            System.out.println("[Downloader] Sent page to Barrel 2");
                        } catch (Exception e2) {
                            System.err.println("[Downloader] Failed to send page to Barrel 2: " + e2.getMessage());
                            try {
                                refreshBarrel2();
                            } catch (Exception e) {
                                System.err.println("[Downloader] Failed to refresh Barrel 2: " + e.getMessage());
                            }
                        }
                    }
                    if (!(delivered1 || delivered2)) {
                        System.out.println("[Downloader] Both barrels unavailable, waiting to retry...");
                        Thread.sleep(2000);
                    }
                }
                
                // Try to send to the other barrel for redundancy
                if(delivered1 && !delivered2){
                    try {
                        barrel2.sendPage(page);
                        System.out.println("[Downloader] Also sent to Barrel 2");
                    } catch (Exception e) {
                        try {
                            refreshBarrel2();
                        } catch (Exception e2) {
                            System.err.println("[Downloader] Failed to refresh Barrel 2: " + e.getMessage());
                        }
                    }
                }
                if(delivered2 && !delivered1){
                    try {
                        barrel1.sendPage(page);
                        System.out.println("[Downloader] Also sent to Barrel 1");
                    } catch (Exception e1) {
                        try { 
                            refreshBarrel1();
                        } catch (Exception e2) {
                            System.err.println("[Downloader] Failed to refresh Barrel 1: " + e2.getMessage());
                        }
                    }
                }

                System.out.println("[Downloader] Indexed url "+url+ " (outgoing=" + outgoing.size() + ")");

                //da queue aos links descobertos
                if (delivered1 || delivered2) {
                    for (String outurl : outgoing) {
                        try { 
                            gateway.putNewURL(outurl); 
                        } catch (Exception e) {
                            refreshGateway();
                            System.err.println("[Downloader] Failed to queue outgoing URL: " + e.getMessage());
                        }
                    }
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        }
    }

    private static Set<String> collectOutgoingURLs(Document doc) {
        Set<String> outgoing = new LinkedHashSet<>();
        for (Element a : doc.select("a[href]")) {
            String abs = a.attr("abs:href");
            if (abs == null || abs.isBlank()) continue;
            if (abs.startsWith("http://") || abs.startsWith("https://")) {
                outgoing.add(abs);
            }
            if (outgoing.size() >= 100) break;//limite
        }
        return outgoing;
    }

    public static void main(String[] args) throws Exception {
    Properties cfg = new Properties();
        try (FileInputStream fis = new FileInputStream("downloader.properties")) { cfg.load(fis);} catch (Exception ignored) {}

        //priority: env vars > properties file
        String hostGateway = System.getenv().getOrDefault("HOST_GATEWAY", cfg.getProperty("gateway.host"));
        int portGateway = Integer.parseInt(System.getenv().getOrDefault("PORT_GATEWAY", cfg.getProperty("gateway.port")));

        String hostBarrel1 = System.getenv().getOrDefault("HOST_BARREL1", cfg.getProperty("barrel1.host"));
        int portBarrel1 = Integer.parseInt(System.getenv().getOrDefault("PORT_BARREL1", cfg.getProperty("barrel1.port")));

        String hostBarrel2 = System.getenv().getOrDefault("HOST_BARREL2", cfg.getProperty("barrel2.host"));
        int portBarrel2 = Integer.parseInt(System.getenv().getOrDefault("PORT_BARREL2", cfg.getProperty("barrel2.port")));

        // Read retry settings
        int connectionRetries = Integer.parseInt(cfg.getProperty("connection.retries"));
        int retryDelay = Integer.parseInt(cfg.getProperty("connection.retryDelay"));

        Downloader d = new Downloader(hostGateway, portGateway, hostBarrel1, portBarrel1, hostBarrel2, portBarrel2, connectionRetries, retryDelay);
        new Thread(d, "downloader").start();//auto call ao run()
        System.out.println("Downloader started.");
    }
}
