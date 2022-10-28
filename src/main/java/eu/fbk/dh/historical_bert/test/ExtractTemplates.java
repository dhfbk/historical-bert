package eu.fbk.dh.historical_bert.test;

import eu.fbk.twm.utils.ExtractorParameters;
import eu.fbk.twm.wiki.xmldump.WikipediaTemplateContentExtractor;
import org.apache.log4j.PropertyConfigurator;

public class ExtractTemplates {
    public static void main(String[] args) {
        String logConfig = System.getProperty("log-config");
        if (logConfig == null) {
            logConfig = "configuration/log-config.txt";
        }
        PropertyConfigurator.configure(logConfig);

        String xin = "/Users/alessio/Desktop/wikisource/itwikisource-20211001-pages-articles.xml";
        String xout = "/Users/alessio/Desktop/wikisource/templates";

        ExtractorParameters e = new ExtractorParameters(xin, xout);
        WikipediaTemplateContentExtractor extractor = new WikipediaTemplateContentExtractor(8, Integer.MAX_VALUE, e.getLocale());
        extractor.start(e);
    }
}
