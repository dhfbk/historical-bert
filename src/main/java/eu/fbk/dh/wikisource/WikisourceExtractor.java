package eu.fbk.dh.wikisource;

import com.google.common.collect.HashMultimap;
import eu.fbk.dh.wikisource.structures.DumpWikiModel;
import eu.fbk.dh.wikisource.structures.PlainTextLinksConverter;
import eu.fbk.dh.wikisource.structures.WikiPage;
import eu.fbk.utils.core.CommandLine;
import info.bliki.api.creator.TopicData;
import info.bliki.api.creator.WikiDB;
import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.Encoder;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Create static HTML files from a given Mediawiki dump
 */
public class WikisourceExtractor {

    public static Pattern paginaPattern = Pattern.compile("^(Pagina|Page):(.*)\\.djvu/([0-9]+)");
    public static Pattern indexPattern = Pattern.compile("^(Indice|Index):(.*)");
    public static Pattern langPattern = Pattern.compile("^[a-z]{1,10}:");
    public static Pattern pagesPattern = Pattern.compile("<pages[^>]*index=\"([^\"]*)\"[^>]*/>", Pattern.MULTILINE);

    public static Set<String> removeTemplates = new HashSet<>();
    public static Set<String> removeStrings = new HashSet<>();
    public static Set<String> removePrefixes = new HashSet<>();
    public static Set<String> removeTags = new HashSet<>();
    public static Set<String> removeTemplateCategories = new HashSet<>();

    //    final public static String ONLY_ONE = "'22Style_is_a_greater_Social_Asset_than_Beauty'22";
//    final public static String ONLY_ONE = "Auteurswet";
    final public static String ONLY_ONE = null;
    final public static Integer LIMIT = null;

    final public static Integer DEFAULT_NUM_THREADS = 8;
//    final public static boolean USE_COMMUNITIES = false;

    static {
        // Same case as Wikisource
        removeTemplates.add("R");
        removeTemplates.add("IncludiIntestazione");
        removeTemplates.add("Immagine");
        removeTemplates.add("Qualità");
//        removeTemplates.add("Intestazione");
        removeTemplates.add("Disambigua");
        removeTemplates.add("AltraVersione");
        removeTemplates.add("Other_versions");

        removeTemplateCategories.add("Specific navigation templates");
        removeTemplateCategories.add("Tanakh navigation templates");
        removeTemplateCategories.add("Scanned volume navigation templates");
        removeTemplateCategories.add("Navigation templates");
        removeTemplateCategories.add("TOC templates");

        removeTemplates.add("License");
        removeTemplates.add("Translation license");
        removeTemplates.add("TextQuality");
        removeTemplates.add("Featured");

        // Lowercase
        removeStrings.add("altri progetti");
        removeStrings.add("defaultsort");

        removePrefixes.add("Autore");
        removePrefixes.add("Author");
        removePrefixes.add("Portal");
        removePrefixes.add("Image");
        removePrefixes.add("Template");

        removeTags.add("pagequality");
        removeTags.add("templatestyles");
    }

    public WikisourceExtractor() {
        super();
    }

    static class RunMe implements Runnable {

        private WikiArticle page;
        private Siteinfo siteinfo;
        private WikiDB wikiDB;
        private String imageDirectory;
        private String htmlDirectory;
        private Map<String, Map<Integer, String>> books;
        private AtomicInteger myCounter;
        private Map<String, List<String>> links;
        private Map<String, List<String>> djvuLinks;
        private Map<String, String> headers;

        public RunMe(WikiArticle page, Siteinfo siteinfo, WikiDB wikiDB, String imageDirectory, String htmlDirectory, Map<String, Map<Integer, String>> books, AtomicInteger myCounter, Map<String, List<String>> links, Map<String, List<String>> djvuLinks, Map<String, String> headers) {
            this.page = page;
            this.siteinfo = siteinfo;
            this.wikiDB = wikiDB;
            this.imageDirectory = imageDirectory;
            this.htmlDirectory = htmlDirectory;
            this.books = books;
            this.myCounter = myCounter;
            this.links = links;
            this.djvuLinks = djvuLinks;
            this.headers = headers;
        }

        private WikiPage getFinalText(String text, Siteinfo siteinfo, boolean keepLinks) throws IOException {
            List<String> lines = new BufferedReader(new StringReader(text))
                    .lines()
                    .collect(Collectors.toList());
            List<String> okLines = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                String otherTitlesLine = line.toLowerCase(Locale.ROOT).replace('=', ' ');
                otherTitlesLine = otherTitlesLine.trim();
                if (!line.equals(otherTitlesLine) && removeStrings.contains(otherTitlesLine)) {
                    break;
                }

                okLines.add(line);
            }

            StringBuilder builder = new StringBuilder();
            for (String line : okLines) {
                builder.append(line).append("\n");
            }
            text = builder.toString();

            DumpWikiModel wikiModel = new DumpWikiModel(wikiDB, siteinfo, "${image}", "${title}", new File(imageDirectory));
            wikiModel.setUp();
            builder = new StringBuilder();
            PlainTextLinksConverter plainTextLinksConverter = new PlainTextLinksConverter(keepLinks);
            wikiModel.render(plainTextLinksConverter, text, builder, true, true);
            lines = new BufferedReader(new StringReader(builder.toString()))
                    .lines()
                    .collect(Collectors.toList());
            boolean somethingGood = false;
            okLines = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).trim();
                if (removeStrings.contains(line.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (!somethingGood) {
                    if (line.length() == 0) {
                        continue;
                    }
                    Matcher m = langPattern.matcher(line);
                    if (!m.find()) {
                        somethingGood = true;
                    } else {
                        continue;
                    }
                }
                okLines.add(line);
            }

            builder = new StringBuilder();
            for (int i = okLines.size() - 1; i >= 0; i--) {
                String line = okLines.get(i);
                line = line.replaceAll("</?poem>", "");
                builder.append(line).append("\n");
            }
            String finalString = builder.toString();

            List<String> links = plainTextLinksConverter.getLinks();
            List<String> djvuLinks = new ArrayList<>();

            for (String tag : removeTags) {
                finalString = finalString.replaceAll("<" + tag + "[^>]*/>", "");
            }
            Matcher matcher = pagesPattern.matcher(finalString);
            if (matcher.find()) {
//                String djvuLink = Encoder.encodeTitleLocalUrl(matcher.group(1));
                String djvuLink = matcher.group(1);
                djvuLink = djvuLink.replace(".djvu", "");
                djvuLinks.add(djvuLink);
            }
            finalString = finalString.replaceAll("<pages[^>]*/>", "");

            return new WikiPage(finalString, links, djvuLinks);
        }

        @Override
        public void run() {
            if (LIMIT != null && myCounter.get() > LIMIT) {
                return;
            }
            int counter = myCounter.incrementAndGet();

            if (counter % 10 == 0) {
                System.out.print('.');
            }
            if (counter % 1000 == 0) {
                System.out.println(" " + String.format("%7d", counter));
            }

            String title = page.getTitle();
            String titleURL = Encoder.encodeTitleLocalUrl(title);
            if (ONLY_ONE != null && !titleURL.equals(ONLY_ONE)) {
                return;
            }

            for (String prefix : removePrefixes) {
                if (title.startsWith(prefix + ":")) {
                    return;
                }
            }

            Matcher matcher;
            matcher = paginaPattern.matcher(title);

            // DJVU
            if (matcher.find()) {
                String bookTitle = matcher.group(2);
                Integer bookPage = Integer.parseInt(matcher.group(3));
                synchronized (books) {
                    books.putIfAbsent(bookTitle, new TreeMap<>());
                }

                try {
                    WikiPage wikiPage = getFinalText(page.getText(), siteinfo, false);
                    links.put(title, wikiPage.getLinks());
                    djvuLinks.put(title, wikiPage.getDjvuLinks());
                    synchronized (books) {
                        books.get(bookTitle).put(bookPage, wikiPage.getText());
                    }
                } catch (IOException e) {
                    // continue
                }

                return;
            }

            matcher = indexPattern.matcher(title);
            if (matcher.find()) {
                String text = page.getText();
                text = text.replace(":MediaWiki:Proofreadpage_index_template", "Proofreadpage_index_template");
                try {
                    WikiPage wikiPage = getFinalText(text, siteinfo, false);
                    String headerTitle = matcher.group(2).replace(".djvu", "");
                    headers.put(headerTitle, wikiPage.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (page.isMain()) {
                String generatedHTMLFilename = htmlDirectory + titleURL + ".txt";

                File file = new File(generatedHTMLFilename);
                File dir = file.getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try {
                    String text = page.getText();
                    if (text.startsWith("#REDIRECT")) {
                        return;
                    }
                    WikiPage wikiPage = getFinalText(text, siteinfo, false);
                    text = wikiPage.getText();
                    links.put(title, wikiPage.getLinks());
                    djvuLinks.put(title, wikiPage.getDjvuLinks());

                    // Put here something to avoid creating the file

                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.append(text);
                    writer.close();

                } catch (Exception e) {
                    // continue
                }
            }
        }

    }

    static class DemoArticleFilter implements IArticleFilter {
        WikiDB wikiDB;
        AtomicInteger myCounter;
        private final String htmlDirectory;
        private final String imageDirectory;
        private ExecutorService executorService;

        private Map<String, Map<Integer, String>> books = new HashMap<>();
        private Map<String, List<String>> links = new HashMap<>();
        private Map<String, List<String>> djvuLinks = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();

        public Map<String, List<String>> getDjvuLinks() {
            return djvuLinks;
        }

        public Map<String, List<String>> getLinks() {
            return links;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Map<String, Map<Integer, String>> getBooks() {
            return books;
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }

        public DemoArticleFilter(WikiDB db, String htmlDirectory, String imageDirectory, int nThreads) {
            this.myCounter = new AtomicInteger();
            this.wikiDB = db;
            if (htmlDirectory.charAt(htmlDirectory.length() - 1) != '/') {
                htmlDirectory = htmlDirectory + "/";
            }
            this.htmlDirectory = htmlDirectory;
            this.imageDirectory = imageDirectory;
            this.executorService = Executors.newFixedThreadPool(nThreads);
        }

        public void process(WikiArticle page, Siteinfo siteinfo) {
            RunMe runnable = new RunMe(page, siteinfo, wikiDB, imageDirectory, htmlDirectory, books, myCounter, links, djvuLinks, headers);
            this.executorService.execute(runnable);
        }

    }

    static class DemoTemplateArticleFilter implements IArticleFilter {
        WikiDB wikiDB;
        int counter;

        public DemoTemplateArticleFilter(WikiDB wikiDB) {
            this.wikiDB = wikiDB;
            this.counter = 0;
        }

        public void process(WikiArticle page, Siteinfo siteinfo) {
            if (page.isTemplate()) {
                String title = page.getTitle();
                String text = page.getText();

                // Add rules for template filtering
                for (String templateCategory : removeTemplateCategories) {
                    if (text.contains("Category:" + templateCategory)) {
                        text = "<includeonly></includeonly>";
                        break;
                    }
                }

                TopicData topicData = new TopicData(title, text);
                try {
                    wikiDB.insertTopic(topicData);
                    ++counter;
                    if (counter % 10 == 0) {
                        System.out.print('.');
                    }
                    if (counter % 1000 == 0) {
                        System.out.println(" " + String.format("%7d", counter));
                    }
                } catch (Exception e) {
                    String mess = e.getMessage();
                    e.printStackTrace();
                }
            }
        }
    }

    public static WikiDB prepareDB(String mainDirectory) {
        WikiDB db = null;

        try {
            db = new WikiDB(new File(mainDirectory));
            return db;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

//        String logConfig = System.getProperty("log-config");
//        if (logConfig == null) {
//            logConfig = "./log-config.txt";
//        }
//        PropertyConfigurator.configure(logConfig);

        final CommandLine cmd = CommandLine
                .parser()
                .withName("extract-wikisource")
                .withHeader("Extract text from Wikisource.")
                .withOption("d", "dump-file", "Dump file (can be in bzip2 format)", "FILE",
                        CommandLine.Type.FILE_EXISTING, true, false, true)
                .withOption("o", "output-folder", "Base output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("t", "threads", "Number of threads", "NUM",
                        CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

        File bz2Filename = cmd.getOptionValue("dump-file", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);
        Integer numThreads = cmd.getOptionValue("threads", Integer.class, DEFAULT_NUM_THREADS);

        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        WikiDB db = null;

        try {
            String mainDirectory = outputFolder.getAbsolutePath() + "/db/";
            String htmlDirectory = outputFolder.getAbsolutePath() + "/data/";
            String booksDirectory = outputFolder.getAbsolutePath() + "/books/";
//            String graphFile = outputFolder.getAbsolutePath() + "/graph.txt";

            // the following directory must exist for image references
            String imageDirectory = outputFolder.getAbsolutePath() + "/images/";

            WikiXMLParser wxp;

            File mdFile = new File(mainDirectory);
            if (!mdFile.exists()) {
                System.out.println("Prepare wiki database");
                db = prepareDB(mainDirectory);
                System.out.println("First pass - write templates to database:");
                DemoTemplateArticleFilter handler = new DemoTemplateArticleFilter(db);
                wxp = new WikiXMLParser(bz2Filename, handler);
                wxp.parse();
                System.out.println(' ');
            } else {
                System.out.println("Prepare wiki database");
                db = prepareDB(mainDirectory);
            }

            for (String template : removeTemplates) {
                String templateName = "Template:" + template;
                String content = "<includeonly></includeonly>";
                db.updateTopic(new TopicData(templateName, content));
            }

            db.updateTopic(new TopicData("Template:Header", "" +
                    "--\n-- HEADER\n" +
                    "TITLE: {{{title}}}\n" +
                    "AUTHOR: {{{author}}}\n" +
                    "YEAR: {{{year|none}}}\n" +
                    "SECTION: {{{section}}}\n" +
                    "PREVIOUS: {{{previous}}}\n" +
                    "NEXT: {{{next}}}\n" +
                    "NOTES: {{{notes}}}\n" +
                    "\n-- END OF HEADER --\n\n" +
                    ""));

            db.updateTopic(new TopicData("Template:Intestazione", "" +
                    "--\n-- HEADER\n" +
                    "TITLE: {{{Titolo}}}\n" +
                    "AUTHOR: {{{Nome e cognome dell'autore}}}\n" +
                    "YEAR: {{{Anno di pubblicazione|none}}}\n" +
                    "LANGUAGE: {{{Lingua originale del testo|italiano}}}\n" +
                    "TRANSLATOR: {{{Nome e cognome del traduttore|none}}}\n" +
                    "TRANSLATION_YEAR: {{{Anno di traduzione|none}}}\n" +
                    "GENRE: {{{Argomento}}}\n" +
                    "\n-- END OF HEADER --\n\n" +
                    ""));

            db.updateTopic(new TopicData("Sjabloon:Infobox_document", "" +
                    "--\n-- HEADER\n" +
                    "TITLE: {{{naam}}}\n" +
                    "AUTHOR: {{{auteur}}}\n" +
                    "GENRE: {{{genre}}}\n" +
                    "YEAR: {{{datum|none}}}\n" +
                    "\n-- END OF HEADER --\n\n" +
                    ""));

            db.updateTopic(new TopicData("Template:Proofreadpage_index_template", "" +
                    "--\n-- HEADER\n" +
                    "TITLE: {{{Title|{{{Titolo|none}}} }}}\n" +
                    "AUTHOR: {{{Author|{{{Autore|none}}} }}}\n" +
                    "YEAR: {{{Year|{{{Anno|none}}} }}}\n" +
                    "\n-- END OF HEADER --\n\n" +
                    ""));

            System.out.println("Second pass - write TXT files to directory:");
            delete(htmlDirectory);
            DemoArticleFilter articleFilter = new DemoArticleFilter(db, htmlDirectory, imageDirectory, numThreads);
            wxp = new WikiXMLParser(bz2Filename, articleFilter);
            wxp.parse();

            articleFilter.getExecutorService().shutdown();
            while (!articleFilter.getExecutorService().isTerminated()) {
                // nothing here
            }
            System.out.println();
            System.out.println("Done!");

            Map<String, String> headers = articleFilter.getHeaders();

            HashMultimap<String, String> djvuLinksMM = HashMultimap.create();
            for (String from : articleFilter.getDjvuLinks().keySet()) {
                for (String to : articleFilter.getDjvuLinks().get(from)) {
                    djvuLinksMM.put(to, from);
                }
            }

//            for (String key : djvuLinksMM.keys()) {
//                if (articleFilter.getBooks().containsKey(key)) {
//                    System.out.println(key + " --> " + djvuLinksMM.get(key));
//                }
//            }

            System.out.println("Third pass - write graph data:");
//            if (USE_COMMUNITIES) {
//                Set<String> vertices = new HashSet<>();
//                FrequencyHashSet<String> frequencyHashSet = new FrequencyHashSet();
//                for (String from : articleFilter.getLinks().keySet()) {
//                    for (String to : articleFilter.getLinks().get(from)) {
//                        if (to.startsWith(".")) {
//                            continue;
//                        }
//                        if (to.startsWith("/")) {
//                            to = from + to;
//                        }
//
//                        boolean skip = false;
//                        for (String prefix : removePrefixes) {
//                            if (from.startsWith(prefix) || to.startsWith(prefix)) {
//                                skip = true;
//                            }
//                        }
//
//                        if (skip) {
//                            continue;
//                        }
//
//                        String key = from + "\t" + to;
//                        frequencyHashSet.add(key);
//                        vertices.add(from);
//                        vertices.add(to);
//                    }
//                }
//                System.out.println("Vertices: " + vertices.size());
//                GraphBuilder graphBuilder = new GraphBuilder().setSize(vertices.size());
//                Map<String, Integer> vertexToIndex = new HashMap<>();
//                Map<Integer, String> indexToVertex = new HashMap<>();
//                int i = 0;
//                for (String vertex : vertices) {
//                    vertexToIndex.put(vertex, i);
//                    indexToVertex.put(i, vertex);
//                    i++;
//                }
//
//                BufferedWriter gWriter = new BufferedWriter(new FileWriter(graphFile));
//                for (String key : frequencyHashSet.keySet()) {
//                    String[] parts = key.split("\t");
//                    try {
//                        graphBuilder.addEdge(vertexToIndex.get(parts[0]), vertexToIndex.get(parts[1]), frequencyHashSet.get(key));
//                    } catch (Exception e) {
//                        // continue
//                    }
//                    gWriter.append(key).append("\t").append(frequencyHashSet.get(key).toString()).append("\n");
//                }
//                gWriter.close();
//                Graph g = graphBuilder.build();
//                final LouvainDetector ld = new LouvainDetector(g);
//                ld.cluster();
//                int[] community = ld.getCommunities().get(ld.getCommunities().size() - 1);
//                List<Integer> communityList = Arrays.asList(ArrayUtils.toObject(community));
//                System.out.println(Collections.max(communityList));
//                System.out.println("Done!");
//            }

            System.out.println("Fourth pass - write books:");
            delete(booksDirectory);
            Map<String, Map<Integer, String>> books = articleFilter.getBooks();
            File bookFile = new File(booksDirectory);
            if (!bookFile.exists()) {
                bookFile.mkdirs();
            }
            int counter = 0;
            for (String title : books.keySet()) {
                ++counter;
                System.out.print('.');
                if (counter % 100 == 0) {
                    System.out.println(" " + String.format("%7d", counter));
                }
                String titleURL = Encoder.encodeTitleLocalUrl(title);

                StringBuilder header = new StringBuilder();

                if (djvuLinksMM.containsKey(title)) {
                    //todo: use majority
                    Optional<String> first = djvuLinksMM.get(title).stream().findFirst();
                    String randomPage = first.get();
                    randomPage = Encoder.encodeTitleLocalUrl(randomPage);
                    String[] parts = randomPage.split("/");
                    String candidateFileName = htmlDirectory + parts[0] + ".txt";
                    File candidateFile = new File(candidateFileName);
                    boolean isHeader = false;
                    boolean somethingIsAdded = false;
                    if (candidateFile.exists()) {
                        BufferedReader reader = new BufferedReader(new FileReader(candidateFile));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("-- HEADER")) {
                                isHeader = true;
                                somethingIsAdded = true;
                            }
                            if (line.startsWith("-- END OF HEADER --")) {
                                header.append(line);
                                isHeader = false;
                            }

                            if (isHeader) {
                                header.append(line).append("\n");
                            }
                        }
                        reader.close();
                    }
                    if (somethingIsAdded) {
                        header.append("\n\n");
                    }
                }

                if (headers.containsKey(title)) {
                    header.append(headers.get(title));
                    header.append("\n\n");
                }

                String bookFileName = booksDirectory + titleURL + ".txt";
                BufferedWriter writer = new BufferedWriter(new FileWriter(bookFileName));
                writer.append(header.toString());

                for (Integer page : books.get(title).keySet()) {
                    writer.append(books.get(title).get(page));
                    writer.append("\n\n");
                }
                writer.close();
            }
            System.out.println();
            System.out.println("Done!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) {
                try {
                    db.tearDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void delete(String dir) throws IOException {
        File dFile = new File(dir);
        if (dFile.exists()) {
            FileUtils.deleteDirectory(dFile);
        }
    }
}
