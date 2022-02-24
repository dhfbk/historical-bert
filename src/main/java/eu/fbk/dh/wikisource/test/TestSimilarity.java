package eu.fbk.dh.wikisource.test;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;

public class TestSimilarity {
    private static String readLineByLineJava8(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    public static void main(String[] args) {
//        String file1 = "/Users/alessio/Desktop/historical-bert/itwikisource-out/books/Pascoli_-_Poemi_italici'2C_1911.txt";
//        String file1 = "/Users/alessio/Desktop/historical-bert/itwikisource-out/data/Enrichetto_dal_ciuffo.txt";
        String file1 = "/Users/alessio/Desktop/historical-bert/itwikisource-out/data/Le_cento_novelle_antiche.txt";

//        String file2 = "/Users/alessio/Desktop/historical-bert/LiberLiber/1900/1997__Pascoli_Giovanni__Poemi_italici.txt";
//        String file2 = "/Users/alessio/Desktop/historical-bert/LiberLiber/1900/1961__Belli_Giuseppe_Gioachino__Le_lettere.txt";
        String file2 = "/Users/alessio/Desktop/historical-bert/LiberLiber/1900/1936__Borgese_Maria__La_contessa_Lara.txt";

        FuzzyScore score = new FuzzyScore(Locale.ITALIAN);
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        String s1 = readLineByLineJava8(file1);
        String s2 = readLineByLineJava8(file2);
        System.out.println(s1.length());
        System.out.println(s2.length());
        System.out.println();
        long time1;
        long time2;

        time1 = System.currentTimeMillis();
        System.out.println(FuzzySearch.tokenSetRatio(s1, s2));
        time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1));

        System.out.println();

        time1 = System.currentTimeMillis();
        System.out.println(score.fuzzyScore(s1, s2));
//        System.out.println(similarity.apply(s1, s2));
        time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1));
    }
}
