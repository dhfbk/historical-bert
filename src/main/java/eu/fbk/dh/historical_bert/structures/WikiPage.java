package eu.fbk.dh.historical_bert.structures;

import java.util.List;

public class WikiPage {
    private String text;
    private List<String> links;
    private List<String> djvuLinks;

    public WikiPage(String text, List<String> links, List<String> djvuLinks) {
        this.text = text;
        this.links = links;
        this.djvuLinks = djvuLinks;
    }

    public String getText() {
        return text;
    }

    public List<String> getDjvuLinks() {
        return djvuLinks;
    }

    public List<String> getLinks() {
        return links;
    }
}
