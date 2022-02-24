package eu.fbk.dh.wikisource;

import eu.fbk.dh.wikisource.structures.Book;
import me.tongfei.progressbar.ProgressBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SaveTexts {
    public static void main(String[] args) {
        String inputFolder = "/Users/alessio/Desktop/historical-bert/md5-all";
        String outputFolder = "/Users/alessio/Desktop/historical-bert/txt-out";
        File inputFile = new File(inputFolder);
        File outputFile = new File(outputFolder);

        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }

        TreeMap<Integer, String> myMap = new TreeMap<>();
        myMap.put(1499, "before-1500");
        myMap.put(1699, "from-1500-to-1700");
        myMap.put(1899, "from-1700-to-1900");
        myMap.put(2099, "after-1900");

        for (Map.Entry<Integer, String> entry : myMap.entrySet()) {
            File thisFolderFile = new File(outputFolder + File.separator + entry.getValue());
            if (!thisFolderFile.exists()) {
                thisFolderFile.mkdirs();
            }
        }


        try {
            Set<File> files = Files.find(inputFile.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            ProgressBar pb = new ProgressBar("Save", files.size());
            pb.start();
            int skipped = 0;
            for (File file : files) {
                Book book = Book.load(file);
                if (book.getYear() == null) {
                    skipped++;
                    continue;
                }
                for (Map.Entry<Integer, String> entry : myMap.entrySet()) {
                    if (book.getYear() < entry.getKey()) {
                        File thisFile = new File(outputFolder + File.separator + entry.getValue() + File.separator + file.getName() + ".txt");
                        String str = book.getContent();
                        str += "\n"; // Needed to handle last line correctly
                        str = str.replaceAll("(.{1,200})\\s+", "$1\n");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(thisFile));
                        writer.write(str);
                        writer.close();
                        break;
                    }
                }
                pb.step();
            }
            pb.stop();
            System.out.println("Skipped: " + skipped);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
