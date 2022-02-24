package eu.fbk.dh.wikisource.old;

import com.google.common.collect.HashMultimap;
import eu.fbk.utils.core.CommandLine;

import java.io.File;
import java.util.*;

public class FilterWikisource {

    public static void main(String[] args) {
//        String inputPath = "/Users/alessio/Desktop/historical-bert/itwikisource-out";
//        String outputPath = "/Users/alessio/Desktop/historical-bert/itwikisource-out-ok";

        final CommandLine cmd = CommandLine
                .parser()
                .withName("filter-wikisource")
                .withHeader("Filter Wikisource.")
                .withOption("i", "input-folder", "Input folder", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-folder", "output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .parse(args);

        File inputFolder = cmd.getOptionValue("input-folder", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);

//        File outputPathFile = new File(outputPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        Set<String> md5s = new HashSet<>();

//        String booksPath = inputFolder.getAbsolutePath() + File.separator + "books";
//        try {
//            Set<String> files = Files.list(Paths.get(booksPath))
//                    .filter(file -> !Files.isDirectory(file))
//                    .map(Path::toString)
//                    .collect(Collectors.toSet());
//            for (String file : files) {
//                Book book = cleanBook(file, "ws_books");
//                saveBook(book, md5s, outputFolder);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        String dataPath = inputFolder.getAbsolutePath() + File.separator + "data";
//        try {
//            Set<String> files = Files.list(Paths.get(dataPath))
//                    .filter(file -> !Files.isDirectory(file))
//                    .map(Path::toString)
//                    .collect(Collectors.toSet());
//            for (String file : files) {
//                if (!file.endsWith(".txt")) {
//                    continue;
//                }
//                File thisDir = new File(file.substring(0, file.length() - 4));
//                Book book = cleanBook(file, "ws_data");
//
//                if (book.getIsEmpty()) {
//                    continue;
//                }
//                if (book.getYear() == null) {
//                    continue;
//                }
//
//                if (thisDir.exists()) {
//                    // todo: check order of files
//                    Set<String> subFiles = Files.find(thisDir.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
//                            .map(Path::toString)
//                            .filter(n -> n.endsWith(".txt"))
//                            .collect(Collectors.toSet());
//
//                    StringBuilder builder = new StringBuilder();
//                    for (String subFile : subFiles) {
//                        Book tmpBook = cleanBook(subFile, "ws_sub");
//                        builder.append(tmpBook.getContent()).append("\n");
//                    }
//
//                    book.setContent(builder.toString());
//                    saveBook(book, md5s, outputFolder);
//                } else if (book.isGood()) {
//                    saveBook(book, md5s, outputFolder);
//                }
//
////                System.out.println(thisFile.getName().replace(".txt"));
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static int romanToInteger(String roman) {
        Map<Character, Integer> numbersMap = new HashMap<>();
        numbersMap.put('I', 1);
        numbersMap.put('V', 5);
        numbersMap.put('X', 10);
        numbersMap.put('L', 50);
        numbersMap.put('C', 100);
        numbersMap.put('D', 500);
        numbersMap.put('M', 1000);

        int result = 0;

        for (int i = 0; i < roman.length(); i++) {
            char ch = roman.charAt(i);

            if (i > 0 && numbersMap.get(ch) > numbersMap.get(roman.charAt(i - 1))) {
                result += numbersMap.get(ch) - 2 * numbersMap.get(roman.charAt(i - 1));
            } else {
                result += numbersMap.get(ch);
            }
        }

        return result;
    }


}
