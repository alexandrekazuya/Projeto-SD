package googol.common.dto;

import java.io.Serializable;

//o q o barrel manda ao gateway
public class SearchResult implements Serializable {
    public String url;
    public String title;
    public String text;
    public int incomingLinksCount;

    public int totalResults;//para o cli client

    public SearchResult() {}
    public SearchResult(String url, String title, String text, int incomingLinksCount) {
        this.url = url;
        this.title = title;
        this.text = text;
        this.incomingLinksCount = incomingLinksCount;
    }
}