package googol.barrel;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import googol.common.BarrelService;
import googol.common.dto.PageDTO;
import googol.common.dto.SearchResult;
import googol.downloader.InvertedIndex;
import googol.downloader.PageInfo;

public class Barrel extends UnicastRemoteObject implements BarrelService {

    private static final int PAGE_SIZE = 10;
    private final InvertedIndex indexedItems = new InvertedIndex();
    private String barrelName;
    private String dataFile;

    public Barrel() throws RemoteException {
        super(); 
    }

    public Barrel(int exportPort) throws RemoteException {
        super(exportPort);
    }

    //downloader ->barrel: index page
    @Override
    public void sendPage(PageDTO page) throws RemoteException {
        if (page == null || page.url == null) return;
        Set<String> outgoing = (page.outgoing == null) ? Collections.emptySet() : page.outgoing;
        indexedItems.addToIndex(page.url, page.title, page.text, outgoing);
    }

    //gateway -> barrel: search com ranking + paginacao
    @Override
    public SearchResult[] searchWord(String[] terms, int page) throws RemoteException {
        if (terms == null || terms.length == 0) return new SearchResult[0];

        //search
        System.out.println("[Barrel] Searching for terms: " + String.join(", ", terms));
        Set<String> hits = indexedItems.searchWord(terms);
        System.out.println("[Barrel] Found " + hits.size() + " hits");
        if (hits.isEmpty()) return new SearchResult[0];
        int totalHits=hits.size();

        //rank
        List<String> ranked = new ArrayList<>(hits);
        ranked.sort(Comparator.comparingInt(indexedItems::incomingLinksCount).reversed());

        //paginacao
        int from = Math.max(0, (page - 1) * PAGE_SIZE);
        int to = Math.min(ranked.size(), from + PAGE_SIZE);
        if (from >= ranked.size()) return new SearchResult[0];

        //fazer um PageDTO
        List<SearchResult> DTO = new ArrayList<>();
        List<String> sub = ranked.subList(from, to);

        for (String u : sub) {
            PageInfo p = indexedItems.getPage(u);
            SearchResult sr = new SearchResult();
            sr.url = u;
            sr.title = (p != null && p.title != null && !p.title.isBlank()) ? p.title : u;
            sr.text = (p != null && p.text != null) ? p.text : "";
            sr.incomingLinksCount = indexedItems.incomingLinksCount(u);
            sr.totalResults = totalHits;
            DTO.add(sr);
        }
        return DTO.toArray(SearchResult[]::new);
    }

    //gateway ->barrel: incoming links
    @Override
    public String[] getIncomingLinks(String url) throws RemoteException {
        if (url == null || url.isBlank()) return new String[0];
        System.out.println("[Barrel] Getting incoming links for: " + url);
        Set<String> incoming = indexedItems.getIncomingLinks(url);
        System.out.println("[Barrel] Found " + incoming.size() + " incoming links");
        return incoming.toArray(String[]::new);
    }
    
    //gateway -> barrel: get index size for stats
    @Override
    public int getIndexSize() throws RemoteException {
        return indexedItems.totalPages();
    }

    public static void main(String[] args) {
        try {
            Properties cfg = new Properties();
            String cfgPath = "barrel.properties";
            try (FileInputStream fis = new FileInputStream(cfgPath)) {
                cfg.load(fis);
            } catch (Exception e) {
                System.err.println("error loading barrel properties: " + e.getMessage());
            }

            String name = cfg.getProperty("barrel.name");
            int registryPort =  Integer.parseInt(cfg.getProperty("registry.port"));

            //hostname para o RMI
            String advertised = System.getenv().getOrDefault("RMI_HOSTNAME", cfg.getProperty("rmi.hostname", "localhost"));
            if (advertised != null && !advertised.isBlank()) {
                System.setProperty("java.rmi.server.hostname", advertised);
            }

            int exportPort = Integer.parseInt(cfg.getProperty("object.port", "0"));

            try {
                LocateRegistry.createRegistry(registryPort);
                System.out.println("RMI registry ready on port " + registryPort);
            } catch (java.rmi.server.ExportException ee) {
                    System.out.println("RMI registry already running on port " + registryPort);
            }

            Barrel barrel = new Barrel(exportPort);
            barrel.barrelName = name;
            barrel.dataFile = name + "_index.txt";
            //load a data se houver
            try {
                barrel.indexedItems.loadFromDisk(barrel.dataFile);
                System.out.println("[Barrel] Loaded " + barrel.indexedItems.totalPages());
            } catch (Exception e) {
                System.out.println("Não consigo ler o ficheiro, ye");
            }

            //hook para dar save ao index quando levar shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("[Barrel] Shutdown triggered, saving to '" + barrel.dataFile + "' ...");
                    barrel.indexedItems.saveToDisk(barrel.dataFile);
                    System.out.println("[Barrel] Saved index on shutdown to '" + barrel.dataFile + "' (" + barrel.indexedItems.totalPages() + " pages)");
                } catch (Exception e) {
                    System.err.println("[Barrel] Error saving on shutdown: " + e.getMessage());
                }
            }));

            String url = "rmi://localhost:" + registryPort + "/" + name;
            Naming.rebind(url, barrel);

            System.out.println("Barrel bound as " + url + " (exportPort=" + exportPort + ", hostname=" + System.getProperty("java.rmi.server.hostname") + ")");
            System.out.println("[Barrel] file: " + barrel.dataFile);
        } catch (Exception e) {
            System.err.println("Error getting file: "+ e.getMessage());
        }
    }

}
