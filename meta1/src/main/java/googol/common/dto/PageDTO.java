package googol.common.dto;

import java.io.Serializable;
import java.util.Set;

//o q o downloader vai mandar ao barrel
public class PageDTO implements Serializable {
    public String url;
    public String title;
    public String text;
    public Set<String> outgoing;

    public PageDTO() {}
    public PageDTO(String url, String title, String text, Set<String> outgoing) {
        this.url = url;
        this.title = title;
        this.text = text;
        this.outgoing = outgoing;
    }
}
