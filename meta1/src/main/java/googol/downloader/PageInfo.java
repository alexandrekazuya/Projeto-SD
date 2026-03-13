package googol.downloader;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PageInfo implements Serializable {
    public final String url;
    public String title;
    public String text;
    public final Set<String> outgoingLinks = new HashSet<>();

    public PageInfo(String url) { this.url = url; }
}
