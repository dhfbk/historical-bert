package eu.fbk.dh.historical_bert.old;

import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FilterLiberLiber {

    private static final Logger logger = LoggerFactory.getLogger(FilterLiberLiber.class);

    public static void main(String[] args) {
//        String inputFolder = "/Users/alessio/Desktop/historical-bert/LiberLiber";
//        File inputFile = new File(inputFolder);
//        String outputPath = "/Users/alessio/Desktop/historical-bert/LiberLiber-out-ok";
//        File outputPathFile = new File(outputPath);

        final CommandLine cmd = CommandLine
                .parser()
                .withName("filter-liberliber")
                .withHeader("Filter LiberLiber.")
                .withOption("i", "input-folder", "Input folder", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-folder", "output folder", "FOLDER",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withLogger(LoggerFactory.getLogger("eu.fbk"))
                .parse(args);

        File inputFolder = cmd.getOptionValue("input-folder", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);

        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

//        Set<String> md5s = new HashSet<>();
//
//        try {
//            Set<String> files = Files.find(inputFolder.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
//                    .map(Path::toString)
//                    .filter(n -> n.endsWith(".txt"))
//                    .collect(Collectors.toSet());
//
//            for (String file : files) {
//                Book book = cleanBook(file, "liberliber");
//                saveBook(book, md5s, outputFolder);
//            }
//
////            System.out.println(subFiles);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }


}
