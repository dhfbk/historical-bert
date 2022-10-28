package eu.fbk.dh.historical_bert;

import eu.fbk.dh.historical_bert.structures.Book;
import eu.fbk.dh.historical_bert.structures.Filter;
import eu.fbk.dh.historical_bert.structures.LiberLiberFilter;
import eu.fbk.dh.historical_bert.structures.WikisourceFilter;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.fbk.dh.historical_bert.structures.Book.saveBook;

public class MainFilter {
    private static final Logger logger = LoggerFactory.getLogger(MainFilter.class);

    public static void main(String[] args) {
        Map<String, Class<? extends Filter>> classes = new HashMap<>();
        classes.put("ws", WikisourceFilter.class);
        classes.put("ll", LiberLiberFilter.class);

        String typeList = String.join(", ", classes.keySet());
        final CommandLine cmd = CommandLine
                .parser()
                .withName("mainfilter")
                .withHeader("Filter books.")
                .withOption("i", "input-folder", "Input folder", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-folder", "Output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("t", "type", String.format("Type (%s)", typeList), "TYPE",
                        CommandLine.Type.STRING, true, false, true)
                .withOption("m", "min", String.format("Minimum number of chars (default %d)", Filter.DEFAULT_MIN), "NUM",
                        CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk"))
                .parse(args);

        File inputFolder = cmd.getOptionValue("input-folder", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);
        String type = cmd.getOptionValue("type", String.class);
        Integer min = cmd.getOptionValue("min", Integer.class, Filter.DEFAULT_MIN);

        if (!classes.containsKey(type)) {
            logger.error("Invalid type");
            System.exit(1);
        }

        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        Set<String> md5s = new HashSet<>();

        try {
            Filter f = classes.get(type).newInstance();
            Set<String> files;

            switch (type) {
                case "ws":
                    String booksPath = inputFolder.getAbsolutePath() + File.separator + "books";
                    String dataPath = inputFolder.getAbsolutePath() + File.separator + "data";

                    files = Files.list(Paths.get(booksPath))
                            .filter(file -> !Files.isDirectory(file))
                            .map(Path::toString)
                            .collect(Collectors.toSet());
                    for (String file : files) {
                        Book book = f.cleanBook(file, "ws_books");
                        saveBook(book, md5s, outputFolder, min);
                    }

                    files = Files.list(Paths.get(dataPath))
                            .filter(file -> !Files.isDirectory(file))
                            .map(Path::toString)
                            .collect(Collectors.toSet());
                    for (String file : files) {
                        if (!file.endsWith(".txt")) {
                            continue;
                        }
                        File thisDir = new File(file.substring(0, file.length() - 4));
                        Book book = f.cleanBook(file, "ws_data");

                        if (book.getIsEmpty()) {
                            continue;
                        }
                        if (book.getYear() == null) {
                            continue;
                        }

                        if (thisDir.exists()) {
                            // todo: check order of files
                            Set<String> subFiles = Files.find(thisDir.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                                    .map(Path::toString)
                                    .filter(n -> n.endsWith(".txt"))
                                    .collect(Collectors.toSet());

                            StringBuilder builder = new StringBuilder();
                            for (String subFile : subFiles) {
                                Book tmpBook = f.cleanBook(subFile, "ws_sub");
                                builder.append(tmpBook.getContent()).append("\n");
                            }

                            book.setContent(builder.toString());
                            saveBook(book, md5s, outputFolder, min);
                        } else if (book.isGood()) {
                            saveBook(book, md5s, outputFolder, min);
                        }

//                System.out.println(thisFile.getName().replace(".txt"));
                    }

                    break;
                case "ll":
                    files = Files.find(inputFolder.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                            .map(Path::toString)
                            .filter(n -> n.endsWith(".txt"))
                            .collect(Collectors.toSet());

                    for (String file : files) {
                        Book book = f.cleanBook(file, "liberliber");
                        saveBook(book, md5s, outputFolder, min);
                    }
                    break;
            }

//            System.out.println(subFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
