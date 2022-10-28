package eu.fbk.dh.historical_bert.old;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.text.similarity.FuzzyScore;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WikiDataExtractionItem {
    private final ExecutorService executor;

    Set<String> allowedTypes = new HashSet<>();
    Set<String> allowedSitelinks = new HashSet<>();
    Set<String> languages = new HashSet<>();

    public WikiDataExtractionItem(int numThreads, Set<String> allowedTypes, Set<String> allowedSitelinks, Set<String> languages) {
        executor = Executors.newFixedThreadPool(numThreads);
        this.allowedTypes = allowedTypes;
        this.allowedSitelinks = allowedSitelinks;
        this.languages = languages;
    }

    public Future<String> calculate(String title, String body) {
        return this.executor.submit(() -> {
            JsonParser parser = new JsonParser();
            JsonObject wikidataObj = parser.parse(body).getAsJsonObject();

            boolean isHuman = false;
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
                        if (allowedTypes.contains(value)) {
                            isHuman = true;
                        }
                    } catch (Exception e) {
                        // ignored
                    }
                }
            } catch (Exception e) {
                // ignored
            }

            if (!isHuman) {
                return "";
            }

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

            StringBuilder writer = new StringBuilder();
            writer.append(title);
            for (String label : labels) {
                writer.append("\t").append(label);
            }
            writer.append("\n");

            return writer.toString();
        });
    }

    public void close() {
        executor.shutdown();
    }
}
