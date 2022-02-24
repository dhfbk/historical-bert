package eu.fbk.dh.wikisource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.fbk.utils.core.CommandLine;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StaxWikiDataExtractor {

    static Logger logger = Logger.getLogger(StaxWikiDataExtractor.class.getName());

    public static class AnalyzePage implements Runnable {

        IndexWriter writer;
        String title;
        String content;
        Set<String> languages;
        AtomicInteger includedCount;

        public AnalyzePage(IndexWriter writer, String title, String content, Set<String> languages, AtomicInteger includedCount) {
            this.writer = writer;
            this.title = title;
            this.content = content;
            this.languages = languages;
            this.includedCount = includedCount;
        }

        @Override
        public void run() {
            boolean present = false;
            JsonParser parser = new JsonParser();

            try {
                JsonObject wikidataObj = parser.parse(content).getAsJsonObject();
                JsonObject sitelinks = wikidataObj.get("sitelinks").getAsJsonObject();
                for (String lang : languages) {
                    if (sitelinks.has(lang)) {
                        present = true;
                    }
                }

                if (present) {
                    includedCount.incrementAndGet();

                    Document document = new Document();

                    document.add(new TextField("title", title, Field.Store.YES));
                    document.add(new StoredField("body", content.getBytes(StandardCharsets.UTF_8)));

                    writer.updateDocument(new Term("title", title), document);
                }
            } catch (Exception e) {
                // ignored
            }        }
    }
    public static void main(String[] args) {
//        String fileName = "/Users/alessio/Desktop/historical-bert/wikidatawiki-20220120-pages-articles2.xml-p441398p1114931";
//        String outputDir = "/Users/alessio/Desktop/historical-bert/wikidatawiki-out-new";

        Properties defaultProps = new Properties();
        defaultProps.setProperty("log4j.rootLogger", "info,stdout");
        defaultProps.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        defaultProps.setProperty("log4j.appender.stdout.layout.ConversionPattern", "[%t] %-5p (%F:%L) - %m %n");
        defaultProps.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");

        PropertyConfigurator.configure(defaultProps);

        final CommandLine cmd = CommandLine
                .parser()
                .withName("extract-wikidata")
                .withHeader("Extract languages from WikiData.")
                .withOption("d", "dump-file", "Dump file (can be in bzip2 format)", "FILE",
                        CommandLine.Type.FILE_EXISTING, true, false, true)
                .withOption("o", "output-folder", "Base output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("t", "threads", "Number of threads", "NUM",
                        CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                .parse(args);

        File file = cmd.getOptionValue("dump-file", File.class);
        File outputDir = cmd.getOptionValue("output-folder", File.class);
        Integer numThreads = cmd.getOptionValue("threads", Integer.class, 8);

        String fileName = file.getAbsolutePath();

        IndexWriter writer;
        Set<String> languages = new HashSet<>();
        languages.add("itwiki");
        languages.add("itwikisource");

        try {
            logger.info("Starting index");
            Directory myStore = FSDirectory.open(outputDir.toPath());
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(myStore, indexWriterConfig);

            logger.info("Creating the thread executor (" + numThreads + ")");
            int blockQueueSize = 10000;
            BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(blockQueueSize);
            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
            ExecutorService myExecutor = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.MINUTES, blockingQueue, rejectedExecutionHandler);

            InputStream stream;
            if (fileName.endsWith(".bz2")) {
                logger.info("Reading bz2 file");
                FileInputStream in = new FileInputStream(fileName);
                stream = new BZip2CompressorInputStream(in);
            } else {
                stream = new FileInputStream(fileName);
            }

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(stream);

            int totalCount = 0;
            AtomicInteger includedCount = new AtomicInteger();

            boolean inPage = false;
            String content = null;
            String title = null;
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();

                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();

                    if (endElement.getName().getLocalPart().equals("page")) {
                        if (inPage && title != null && content != null) {
                            myExecutor.execute(new AnalyzePage(writer, title, content, languages, includedCount));
                        }
                        inPage = false;
                        title = null;
                        content = null;

                        totalCount++;
                        if (totalCount % 10000 == 0) {
                            logger.info(String.format("Files: %d/%d", includedCount.get(), totalCount));
                        }
                    }

                }
                if (nextEvent.isStartElement()) {
                    StartElement startElement = nextEvent.asStartElement();

                    if (startElement.getName().getLocalPart().equals("page")) {
                        inPage = true;
                    }
                    if (startElement.getName().getLocalPart().equals("title")) {
                        title = getContent(reader, "title");
                    }
                    if (startElement.getName().getLocalPart().equals("text")) {
                        content = getContent(reader, "text");
                    }
                }
            }

            myExecutor.shutdown();
            logger.debug("Waiting to end");
            myExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            logger.info("Total: " + totalCount);
            logger.info("Included: " + includedCount);
            logger.info("Closing index");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getContent(XMLEventReader reader, String tagName) throws XMLStreamException {
        StringBuilder content = new StringBuilder();
        XMLEvent followingEvent = null;
        do {
            followingEvent = reader.nextEvent();
            if (followingEvent.isCharacters()) {
                content.append(followingEvent.asCharacters().getData());
            }
        } while (!followingEvent.isEndElement() || !followingEvent.asEndElement().getName().getLocalPart().equals(tagName));
        return content.toString();
    }
}
