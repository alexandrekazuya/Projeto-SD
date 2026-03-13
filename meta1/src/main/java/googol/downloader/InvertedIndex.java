package googol.downloader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvertedIndex {
    private final Map<String, Set<String>> indexedItems = new HashMap<>();
    private final Map<String, PageInfo> pages = new HashMap<>();

    public final Set<String> outgoingLinks = new HashSet<>();
    private final Map<String, Set<String>> incomingLinks = new HashMap<>();

    public int totalPages() { 
        return pages.size();
    }
    public int totalWords() {
        return indexedItems.size();
    }
    public int totalIncomingEdges() {
        int sum = 0;
        for (java.util.Set<String> s : incomingLinks.values()){
            sum += s.size();
        }
        return sum;
    }
    
    public void addToIndex(String url, String title, String text, Set<String> outgoingLinks) {
        //dar load à data
        PageInfo p = pages.computeIfAbsent(url, PageInfo::new);//se ja tinhamos info deste url, da return a essa info
                                                               //senao cria uma nova PageInfo para esse url
        p.title = (title == null) ? "" : title;
        p.text = buildTexto(text);
        p.outgoingLinks.clear();
        if (outgoingLinks != null) p.outgoingLinks.addAll(outgoingLinks);

        //meter no inv index { "cão": [page1, page2], "gato": [page3] }
        String[] tokens = text.toLowerCase().split("\\W+");
        for (String word : tokens) {
            if (word.isBlank()) continue;
            indexedItems.computeIfAbsent(word, k -> new HashSet<>()).add(url);
        }
        incomingLinks.computeIfAbsent(url, k -> new HashSet<>());

        if(outgoingLinks !=null){
            for (String link: outgoingLinks){
                incomingLinks.computeIfAbsent(link, k -> new HashSet<>()).add(url);
                //System.out.println("[InvertedIndex] Added incoming link: " + url + " -> " + link);
            }
        }
    }

    // search for pages containing all words
        public Set<String> searchWord(String... words) {
            Set<String> result = new HashSet<>();
            boolean first = true;

            for (String w : words) {
                Set<String> pagesWithWord = indexedItems.get(w.toLowerCase()); //retorna as paginas q teem esta palavra
                if (pagesWithWord == null) return Collections.emptySet();
                if (first) {
                    result.addAll(pagesWithWord);//põe paginas q têm a palavra
                    first = false;
                } else {
                    result.retainAll(pagesWithWord);//descarta aquelas q não contêm a nova palavra
                }
            }
            return result;
        }

    public PageInfo getPage(String url) {
        return pages.get(url);
    }
    public int incomingLinksCount(String url) {
        return incomingLinks.getOrDefault(url, Set.of()).size();
    }
    public Set<String> getIncomingLinks(String url) {
        System.out.println("[InvertedIndex] Looking for incoming links to: " + url);
        System.out.println("[InvertedIndex] Total URLs with incoming links: " + incomingLinks.size());
        Set<String> result = incomingLinks.getOrDefault(url, Set.of());
        System.out.println("[InvertedIndex] Found " + result.size() + " incoming links for this URL");
        if (result.isEmpty() && incomingLinks.containsKey(url)) {
            System.out.println("[InvertedIndex] URL exists in map but has no incoming links");
        } else if (!incomingLinks.containsKey(url)) {
            System.out.println("[InvertedIndex] URL not found in incoming links map");
        }
        return new HashSet<>(result);
    }

    private static String buildTexto(String text) {
        if (text == null) return "";
        String t = text.strip();
        if (t.length() <= 100) return t;
        return t.substring(0, 100) + "...";
    }

    //save data
    public synchronized void saveToDisk(String filePath) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(pages);
            out.writeObject(indexedItems);
            out.writeObject(incomingLinks);
        }
    }

    //load data
    @SuppressWarnings("unchecked") //para tirar os avisos amarelos
    public synchronized void loadFromDisk(String filePath) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            pages.clear();
            indexedItems.clear();
            incomingLinks.clear();
            
            pages.putAll((Map<String, PageInfo>) in.readObject());
            indexedItems.putAll((Map<String, Set<String>>) in.readObject());
            incomingLinks.putAll((Map<String, Set<String>>) in.readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load index: " + e.getMessage(), e);
        }
    }
}
