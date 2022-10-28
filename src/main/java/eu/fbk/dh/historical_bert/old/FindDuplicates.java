package eu.fbk.dh.historical_bert.old;

import eu.fbk.dh.historical_bert.mt.DiffCalculator;
import eu.fbk.dh.historical_bert.structures.Book;
import eu.fbk.utils.core.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FindDuplicates {

    public static void main(String[] args) {

        /*
         * Documentation: https://github.com/xdrop/fuzzywuzzy
         * Python: https://chairnerd.seatgeek.com/fuzzywuzzy-fuzzy-string-matching-in-python/
         * Blogpost: https://towardsdatascience.com/string-matching-with-fuzzywuzzy-e982c61f8a84
         *
         * */

//        File folder1 = new File("/Users/alessio/Desktop/historical-bert/itwikisource-out-ok");
//        File folder2 = new File("/Users/alessio/Desktop/historical-bert/LiberLiber-out-ok");

        final CommandLine cmd = CommandLine
                .parser()
                .withName("compare-datasets")
                .withHeader("Compare datasets.")
                .withOption("f1", "folder1", "Input folder 1", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("f2", "folder2", "Input folder 2", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("t", "threads", "Number of threads", "NUM",
                        CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                .withOption("o", "output-file", "output file", "FILE",
                        CommandLine.Type.FILE, true, false, true)
                .parse(args);

        File folder1 = cmd.getOptionValue("folder1", File.class);
        File folder2 = cmd.getOptionValue("folder2", File.class);
        File outputFile = cmd.getOptionValue("output-file", File.class);
        Integer numThreads = cmd.getOptionValue("threads", Integer.class, 8);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            List<Book> list1 = new ArrayList<>();
            List<Book> list2 = new ArrayList<>();

            System.out.println("[INFO] Loading list 1");
            Set<File> files1 = Files.find(folder1.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            for (File subFile : files1) {
                Book book = Book.load(subFile);
                list1.add(book);
            }
            System.out.printf("%d items loaded%n", list1.size());
            System.out.println("[INFO] Loading list 2");
            Set<File> files2 = Files.find(folder2.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            for (File subFile : files2) {
                Book book = Book.load(subFile);
                list2.add(book);
            }
            System.out.printf("%d items loaded%n", list2.size());

            System.out.println("[INFO] Starting comparison 1");
            for (Book book : list2) {
                System.out.printf("%s - %s%n", book.getAuthor(), book.getTitle());
                Map<String, Integer> valueMaps = new HashMap<>();
                long time1 = System.currentTimeMillis();
                int i = 0;
                DiffCalculator diffCalculator = new DiffCalculator(numThreads);
                List<Future<Map.Entry<String, Integer>>> futures = new ArrayList<>();
                for (Book bookToCheck : list1) {
                    i++;
                    String content1 = book.getContent();
                    String content2 = bookToCheck.getContent();

//                    if (content1.length() > content2.length()) {
//                        String textToFind = content2.substring(content2.length() - Math.min(1000, content2.length()));
//                        Future<Map.Entry<String, Integer>> value = diffCalculator.calculate(bookToCheck.getFileName(), content1, textToFind);
//                        futures.add(value);
//                    } else {
//                        String textToFind = content1.substring(content1.length() - Math.min(1000, content1.length()));
//                        Future<Map.Entry<String, Integer>> value = diffCalculator.calculate(bookToCheck.getFileName(), textToFind, content2);
//                        futures.add(value);
//                    }

                    Future<Map.Entry<String, Integer>> value = diffCalculator.calculate(bookToCheck.getFileName(), content1, content2);
                    futures.add(value);
                }

                for (Future<Map.Entry<String, Integer>> entry : futures) {
                    Map.Entry<String, Integer> e = entry.get();
                    valueMaps.put(e.getKey(), e.getValue());
                    writer.append(book.getFileName());
                    writer.append("\t");
                    writer.append(e.getKey());
                    writer.append("\t");
                    writer.append(e.getValue().toString());
                    writer.append("\n");
                }

                diffCalculator.close();

                long time2 = System.currentTimeMillis();
                System.out.printf("Done %d iterations in %f seconds%n", i, (time2 - time1) / 1000.0);
//                Map.Entry<String, Integer> entry = valueMaps.entrySet().stream()
//                        .max(Map.Entry.comparingByValue()).get();
//                System.out.println(entry);

//                writer.append(book.getFileName());
//                writer.append("\t");
//                writer.append(entry.getKey());
//                writer.append("\n");
                writer.flush();
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
