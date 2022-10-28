package eu.fbk.dh.historical_bert.structures;

import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.PlainTextConvertable;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.tags.WPATag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static info.bliki.wiki.model.Configuration.RENDERER_RECURSION_LIMIT;

/**
 * A converter which renders the internal tree node representation as plain text
 * without HTML tags and images
 */
public class PlainTextLinksConverter implements ITextConverter {

    public List<String> getLinks() {
        return links;
    }

    private List<String> links = new ArrayList<>();
    private boolean keepLinks;

    public PlainTextLinksConverter(boolean keepLinks) {
        this.keepLinks = keepLinks;
    }

    @Override
    public void nodesToText(List<?> nodes, Appendable resultBuffer, IWikiModel model) throws IOException {
        assert (model != null);
        if (nodes != null && !nodes.isEmpty()) {
            try {
                int level = model.incrementRecursionLevel();

                if (level > RENDERER_RECURSION_LIMIT) {
                    resultBuffer.append("Error - recursion limit exceeded rendering tags in PlainTextConverter#nodesToText().");
                    return;
                }
                for (Object item : nodes) {
                    if (item instanceof List<?>) {
                        nodesToText((List<?>) item, resultBuffer, model);
                    } else if (item instanceof PlainTextConvertable) {
                        ((PlainTextConvertable) item).renderPlainText(this, resultBuffer, model);
                        if (keepLinks && item instanceof WPATag) {
                            String link = ((WPATag) item).getLink();
                            resultBuffer.append("###LINK{" + link + "}");
                            links.add(link);
                        }
                    }
                }
            } finally {
                model.decrementRecursionLevel();
            }
        }
    }

    @Override
    public void imageNodeToText(TagNode imageTagNode, ImageFormat imageFormat, Appendable resultBuffer, IWikiModel model) throws IOException {

    }

    @Override
    public boolean renderLinks() {
        return true;
    }
}