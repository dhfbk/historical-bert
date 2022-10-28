package eu.fbk.dh.historical_bert.mt;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.text.similarity.FuzzyScore;

import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DiffCalculator {
    private final ExecutorService executor;

    public DiffCalculator(int numThreads) {
        executor = Executors.newFixedThreadPool(numThreads);

    }

    public Future<Map.Entry<String, Integer>> calculate(String fileName, String content1, String content2) {
        return this.executor.submit(() -> {
            FuzzyScore score = new FuzzyScore(Locale.ITALIAN);
            return new AbstractMap.SimpleEntry<>(fileName, score.fuzzyScore(content1, content2));
//            return new AbstractMap.SimpleEntry<>(fileName, FuzzySearch.tokenSetRatio(content1, content2));
        });
    }

    public void close() {
        executor.shutdown();
    }
}
