package eu.fbk.dh.wikisource.structures;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import info.bliki.api.creator.ImageData;
import info.bliki.api.creator.TopicData;
import info.bliki.api.creator.WikiDB;
import info.bliki.htmlcleaner.BaseToken;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.filter.Encoder;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.WikipediaParser;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import info.bliki.wiki.tags.WPATag;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class DumpWikiModel extends WikiModel {
    private Siteinfo fSiteinfo;
    private WikiDB fWikiDB;
    private final String fTemplateNamespace;
    private final File fImageDirectory;

    public DumpWikiModel(WikiDB wikiDB, Siteinfo siteinfo, String imageBaseURL, String linkBaseURL, @Nullable File imageDirectory) {
        this(wikiDB, siteinfo, Locale.ENGLISH, imageBaseURL, linkBaseURL, imageDirectory);
    }

    public DumpWikiModel(WikiDB wikiDB, Siteinfo siteinfo, Locale locale, String imageBaseURL, String linkBaseURL, @Nullable File imageDirectory) {
        super(new Configuration(), locale, siteinfo.getNamespace(), imageBaseURL, linkBaseURL);
        this.fWikiDB = wikiDB;
        this.fSiteinfo = siteinfo;
        this.fTemplateNamespace = this.fSiteinfo.getNamespace(NamespaceCode.TEMPLATE_NAMESPACE_KEY.code);

        assert imageDirectory == null || imageDirectory.exists() || imageDirectory.mkdirs();

        this.fImageDirectory = imageDirectory;
    }

    public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> templateParameters) throws WikiModelContentException {
        String result = super.getRawWikiContent(parsedPagename, templateParameters);
        if (result != null) {
            return result;
        } else if (!parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)) {
            return null;
        } else {
            String name = parsedPagename.pagename;
            if (this.fSiteinfo.getCharacterCase().equals("first-letter")) {
                name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }

            String content = null;

            try {
                synchronized (fWikiDB) {
                    TopicData topicData = this.fWikiDB.selectTopic(this.fTemplateNamespace + ":" + name);
                    if (topicData != null) {
                        content = topicData.getContent();
                        content = this.getRedirectedWikiContent(content, templateParameters);
                        if (content != null) {
                            return content.length() == 0 ? null : content;
                        } else {
                            return null;
                        }
                    } else {
                        return content;
                    }
                }
            } catch (Exception var8) {
                String temp = var8.getMessage();
                if (temp != null) {
                    throw new WikiModelContentException("<span class=\"error\">Exception: " + temp + "</span>", var8);
                } else {
                    throw new WikiModelContentException("<span class=\"error\">Exception: " + var8.getClass().getSimpleName() + "</span>", var8);
                }
            }
        }
    }

    public String getRedirectedWikiContent(String rawWikitext, Map<String, String> templateParameters) {
        if (rawWikitext.length() < 9) {
            return rawWikitext;
        } else {
            String redirectedLink = WikipediaParser.parseRedirect(rawWikitext, this);
            if (redirectedLink != null) {
                ParsedPageName redirParsedPage = ParsedPageName.parsePageName(this, redirectedLink, this.fNamespace.getTemplate(), true, true);
                return WikipediaParser.getRedirectedRawContent(this, redirParsedPage, templateParameters);
            } else {
                return rawWikitext;
            }
        }
    }

    public void appendInternalImageLink(String hrefImageLink, String srcImageLink, ImageFormat imageFormat) {
        try {
            String imageName = imageFormat.getFilename();
            synchronized (fWikiDB) {
                ImageData imageData = this.fWikiDB.selectImage(imageName);
                if (imageData != null) {
                    File file = imageData.getFile();
                    if (file.exists()) {
                        super.appendInternalImageLink(hrefImageLink, file.toURI().toString(), imageFormat);
                        return;
                    }
                }
            }

            String imageNameURL = Encoder.encodeTitleLocalUrl(imageName);
            super.appendInternalImageLink(hrefImageLink, "file:///" + this.fImageDirectory + imageNameURL, imageFormat);
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public void appendInternalLink(String topic, String hashSection, String topicDescription, String cssClass, boolean parseRecursive) {
        WPATag aTagNode = new WPATag();
        aTagNode.addAttribute("id", "w", true);
        String titleURL = Encoder.encodeTitleLocalUrl(topic);
        String href = titleURL + ".html";
        if (hashSection != null) {
            href = href + '#' + hashSection;
        }

        aTagNode.addAttribute("href", href, true);
        if (cssClass != null) {
            aTagNode.addAttribute("class", cssClass, true);
        }

        aTagNode.addObjectAttribute("wikilink", topic);
        this.pushNode(aTagNode);
        WikipediaParser.parseRecursive(topicDescription.trim(), this, false, true);
        this.popNode();
    }

    public void parseInternalImageLink(String imageNamespace, String rawImageLink) {
        String imageSrc = this.getImageBaseURL();
        if (imageSrc != null) {
            String imageHref = this.getWikiBaseURL();
            ImageFormat imageFormat = ImageFormat.getImageFormat(rawImageLink, imageNamespace);
            String imageName = imageFormat.getFilename();
            imageName = Encoder.encodeUrl(imageName);
            if (this.replaceColon()) {
                imageHref = imageHref.replace("${title}", imageNamespace + '/' + imageName);
                imageSrc = imageSrc.replace("${image}", imageName);
            } else {
                imageHref = imageHref.replace("${title}", imageNamespace + ':' + imageName);
                imageSrc = imageSrc.replace("${image}", imageName);
            }

            this.appendInternalImageLink(imageHref, imageSrc, imageFormat);
        }

    }

    static {
        TagNode.addAllowedAttribute("style");
    }
}
