package eu.fbk.dh.historical_bert.old;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.fbk.twm.utils.ExtractorParameters;
import eu.fbk.twm.wiki.xmldump.AbstractWikipediaExtractor;
import eu.fbk.utils.core.CommandLine;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
//import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiDataLangExtractor extends AbstractWikipediaExtractor {

    Logger logger = Logger.getLogger(WikiDataLangExtractor.class.getName());
    Pattern q = Pattern.compile("^Q([0-9]+)$");
    IndexWriter writer;
    Set<String> languagesToKeep;

    AtomicInteger count = new AtomicInteger();
    AtomicInteger presentCount = new AtomicInteger();
//    private String outputDir, clSchema;
//    IndexWriter clSchemaWriter = null;
//    HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
//    HashSet<String> langs = null;

    public WikiDataLangExtractor(int numThreads, int numPages, Locale locale, String configurationFolder) {
        super(numThreads, numPages, locale, configurationFolder);
        logger.info(String.format("Starting with %d threads", numThreads));
    }

    public void start(String fileName, String outputDir, Set<String> languagesToKeep) {
        this.languagesToKeep = languagesToKeep;
        try {
            logger.info("Starting index");
            Directory myStore = FSDirectory.open(new File(outputDir).toPath());
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(myStore, indexWriterConfig);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        startProcess(fileName);
    }

    @Override
    public void start(ExtractorParameters extractorParameters) {

    }

    @Override
    public void disambiguationPage(String s, String s1, int i) {

    }

    @Override
    public void categoryPage(String s, String s1, int i) {

    }

    @Override
    public void templatePage(String s, String s1, int i) {

    }

    @Override
    public void redirectPage(String s, String s1, int i) {

    }

    @Override
    public void contentPage(String s, String s1, int i) {
        Matcher m = q.matcher(s1);
        if (!m.find()) {
            return;
        }
        String id = m.group(1);

        boolean present = false;

        JsonParser parser = new JsonParser();
        JsonObject wikidataObj = parser.parse(s).getAsJsonObject();
        try {
            JsonObject sitelinks = wikidataObj.get("sitelinks").getAsJsonObject();
            for (String lang : languagesToKeep) {
                if (sitelinks.has(lang)) {
                    present = true;
                }
            }
        } catch (IllegalStateException e) {
            return;
        }

        count.incrementAndGet();
        if (!present) {
            return;
        }
        presentCount.incrementAndGet();

        Document document = new Document();

        document.add(new TextField("title", id, Field.Store.YES));
        document.add(new StoredField("body", s.getBytes(StandardCharsets.UTF_8)));

        try {
            writer.updateDocument(new Term("title", id), document);
//            writer.addDocument(document);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

//        System.out.println("ID: " + id);
    }

    @Override
    public void portalPage(String s, String s1, int i) {

    }

    @Override
    public void projectPage(String s, String s1, int i) {

    }

    @Override
    public void filePage(String s, String s1, int i) {

    }

    @Override
    public void endProcess() {
        super.endProcess();
        try {
            logger.info("Closing index");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Present: " + presentCount.get());
        logger.info("Total: " + count.get());
    }

    public static void main(String[] args) {

        final CommandLine cmd = CommandLine
                .parser()
                .withName("extract-wikidata")
                .withHeader("Extract languages from WikiData.")
                .withOption("d", "dump-file", "Dump file (can be in bzip2 format)", "FILE",
                        CommandLine.Type.FILE_EXISTING, true, false, true)
                .withOption("c", "configuration-folder", "Configuration folder", "FILE",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-folder", "Base output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("t", "threads", "Number of threads", "NUM",
                        CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                .parse(args);

        File bz2Filename = cmd.getOptionValue("dump-file", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);
        Integer numThreads = cmd.getOptionValue("threads", Integer.class, 8);
        String configurationFolder = cmd.getOptionValue("configuration-folder", String.class);

        Set<String> languages = new HashSet<>();
        languages.add("itwiki");
        languages.add("itwikisource");

        Properties defaultProps = new Properties();
        defaultProps.setProperty("log4j.rootLogger", "info,stdout");
        defaultProps.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        defaultProps.setProperty("log4j.appender.stdout.layout.ConversionPattern", "[%t] %-5p (%F:%L) - %m %n");
        defaultProps.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");

        PropertyConfigurator.configure(defaultProps);

        WikiDataLangExtractor wikiDataLangExtractor = new WikiDataLangExtractor(numThreads, Integer.MAX_VALUE, Locale.ITALIAN, configurationFolder);
        wikiDataLangExtractor.start(bz2Filename.getAbsolutePath(), outputFolder.getAbsolutePath(), languages);

    }
}
