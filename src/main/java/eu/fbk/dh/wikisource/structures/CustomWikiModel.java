package eu.fbk.dh.wikisource.structures;

import info.bliki.wiki.model.WikiModel;

import java.io.IOException;
import java.util.Map;

public class CustomWikiModel extends WikiModel {
    public CustomWikiModel(String imageBaseURL, String linkBaseURL) {
        super(imageBaseURL, linkBaseURL);
    }



    @Override
    public void substituteTemplateCall(String templateName, Map<String, String> parameterMap, Appendable writer) throws IOException {
        System.out.println(templateName);
//        super.substituteTemplateCall(templateName, parameterMap, writer);
    }
}
