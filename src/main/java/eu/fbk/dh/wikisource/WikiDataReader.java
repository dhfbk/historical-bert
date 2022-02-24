package eu.fbk.dh.wikisource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WikiDataReader {

    static Map<String, String> allowedTypes = new HashMap<>();
    static Set<String> allowedSitelinks = new HashSet<>();
    static Set<String> languages = new HashSet<>();

    static {

        allowedTypes.put("Q5", "h");
        allowedTypes.put("Q15632617", "h");
        allowedTypes.put("Q386724", "w");
        allowedSitelinks.add("wiki");
        allowedSitelinks.add("wikisource");
        allowedSitelinks.add("wikiquote");
        languages.add("it");
        languages.add("en");
        languages.add("fr");
    }

    public static class WriteLine implements Runnable {

        BufferedWriter writer;
        String title;
        String body;

        public WriteLine(BufferedWriter writer, String title, String body) {
            this.writer = writer;
            this.title = title;
            this.body = body;
        }

        private static List<Integer> dateList(JsonObject wikidataObj, String property) {
            List<Integer> ret = new ArrayList<>();
            try {
                for (JsonElement element : wikidataObj
                        .get("claims").getAsJsonObject()
                        .get(property).getAsJsonArray()) {
                    try {
                        String value = element.getAsJsonObject()
                                .get("mainsnak").getAsJsonObject()
                                .get("datavalue").getAsJsonObject()
                                .get("value").getAsJsonObject()
                                .get("time").getAsString();
                        ret.add(Integer.parseInt(value.substring(0, 5)));
                    } catch (Exception e) {
                        // ignored
                    }
                }
            } catch (Exception e) {
                // ignored
            }
            return ret;
        }

        @SneakyThrows
        @Override
        public void run() {
            JsonParser parser = new JsonParser();
            JsonObject wikidataObj = parser.parse(body).getAsJsonObject();

            String type = null;
            try {
                for (JsonElement element : wikidataObj
                        .get("claims").getAsJsonObject()
                        .get("P31").getAsJsonArray()) {
                    try {
                        String value = element.getAsJsonObject()
                                .get("mainsnak").getAsJsonObject()
                                .get("datavalue").getAsJsonObject()
                                .get("value").getAsJsonObject()
                                .get("id").getAsString();
                        if (allowedTypes.containsKey(value)) {
                            type = allowedTypes.get(value);
                        }
                    } catch (Exception e) {
                        // ignored
                    }
                }
            } catch (Exception e) {
                // ignored
            }

            if (type == null) {
                return;
            }

            List<Integer> birthDates = dateList(wikidataObj, "P569");
            List<Integer> deathDates = dateList(wikidataObj, "P570");
            List<Integer> workDates = dateList(wikidataObj, "P571");
            // P569
            // P570

            Set<String> labels = new HashSet<>();

            for (String language : languages) {
                try {
                    for (JsonElement element : wikidataObj.get("aliases").getAsJsonObject().get(language).getAsJsonArray()) {
                        labels.add(element.getAsJsonObject().get("value").getAsString());
                    }
                } catch (Exception e) {
                    // ignored
                }
                try {
                    labels.add(wikidataObj.get("labels").getAsJsonObject().get(language).getAsJsonObject().get("value").getAsString());
                } catch (Exception e) {
                    // ignored
                }
                for (String allowedSitelink : allowedSitelinks) {
                    try {
                        labels.add(wikidataObj.get("sitelinks").getAsJsonObject().get(language + allowedSitelink).getAsJsonObject().get("title").getAsString());
                    } catch (Exception e) {
                        // ignored
                    }
                }
            }

            synchronized (writer) {
                writer.append(title);
                writer.append("\t");
                writer.append(type);
                writer.append("\t");
                writer.append(birthDates.size() == 0 ? "null" : birthDates.get(0).toString());
                writer.append("\t");
                writer.append(deathDates.size() == 0 ? "null" : deathDates.get(0).toString());
                writer.append("\t");
                writer.append(workDates.size() == 0 ? "null" : workDates.get(0).toString());
                for (String label : labels) {
                    writer.append("\t").append(label);
                }
                writer.append("\n");
                writer.flush();
            }
        }
    }

    public static void main(String[] args) {
        String folder = "/Users/alessio/Desktop/historical-bert/out-giga";
        String outputFile = "/Users/alessio/Desktop/historical-bert/wikidata_it.tsv";
        int numThreads = 8;

        try {

            ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Directory myStore = FSDirectory.open(new File(folder).toPath());
            IndexReader reader = DirectoryReader.open(myStore);

            for (int i = 0; i < reader.maxDoc(); i++) {
                Document d = reader.document(i);

                if ((i + 1) % 100 == 0) {
                    System.out.print(".");
                }
                if ((i + 1) % 10000 == 0) {
                    System.out.println(" " + (i + 1));
                }

                String title = d.get("title");
                String body = new String(d.getBinaryValue("body").bytes, Charset.defaultCharset());

                executorService.submit(new WriteLine(writer, title, body));
            }

            System.out.println();

            executorService.shutdown();
            writer.close();

//            Query q = new TermQuery(new Term("title", "Q15"));
//            IndexSearcher searcher = new IndexSearcher(reader);
//
//            TopDocs search = searcher.search(q, 10);
//            System.out.println(search.totalHits);
//            System.out.println(reader.document(0));

//
//            TopDocs docs = searcher.search(new TermQuery(new Term("title", "Q42")), 10);
//
//            for (ScoreDoc scoreDoc : docs.scoreDocs) {
//                Document document = reader.document(scoreDoc.doc);
//                System.out.println(document);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
