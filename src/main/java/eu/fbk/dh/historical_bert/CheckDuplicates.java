package eu.fbk.dh.historical_bert;

import eu.fbk.dh.historical_bert.structures.Book;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckDuplicates {
    public static void main(String[] args) {
        String duplicateFileName = "/Users/alessio/Desktop/historical-bert/similarities.tsv";
        String folder1Name = "/Users/alessio/Desktop/historical-bert/md5-ws";
        String folder2Name = "/Users/alessio/Desktop/historical-bert/md5-ll";
        String wikidataName = "/Users/alessio/Desktop/historical-bert/wikidata_it.tsv";
        String outputFolderName = "/Users/alessio/Desktop/historical-bert/md5-all-complete";
        float delta = 0.9f;
        boolean keepFirstDefault = true;
        boolean transferYear = true;
        boolean keepDuplicates = true;

        File duplicateFile = new File(duplicateFileName);
        File folder1 = new File(folder1Name);
        File folder2 = new File(folder2Name);
        File outputFolder = new File(outputFolderName);
        File wikidata = new File(wikidataName);
        Set<File> toBeRemoved = new HashSet<>();
        Map<File, Integer> yearsToUpdate = new HashMap<>();

        try {
            System.err.println("Loading WikiData");
            Map<String, Map<String, Integer>> authors = new HashMap<>();
            Map<String, Map<String, Integer>> works = new HashMap<>();
            Scanner scanner;
            scanner = new Scanner(wikidata);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = line.trim();
                String[] parts = line.split("\t");
                String id = parts[0];
                String type = parts[1];
                String[] labels = Arrays.copyOfRange(parts, 5, parts.length);
                Integer year = null;
                if (type.equals("h")) {
                    Integer bd = null;
                    Integer dd = null;
                    try {
                        bd = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ignored) {

                    }
                    try {
                        dd = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException ignored) {

                    }
                    if (dd != null) {
                        year = dd - 5;
                    } else if (bd != null) {
                        year = bd + 20;
                    }
                } else { // w
                    try {
                        year = Integer.parseInt(parts[4]);
                    } catch (NumberFormatException ignored) {

                    }
                }
                if (year == null) {
                    continue;
                }
                for (String label : labels) {
                    label = normalize(label);
                    if (type.equals("h")) {
                        authors.putIfAbsent(label, new HashMap<>());
                        authors.get(label).put(id, year);
                    } else { // w
                        works.putIfAbsent(label, new HashMap<>());
                        works.get(label).put(id, year);
                    }
                }
            }

            System.err.println("Loading duplicates");
            scanner = new Scanner(duplicateFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = line.trim();
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                File file1 = new File(parts[0]);
                File file2 = new File(parts[1]);
                Book book1 = Book.load(file1);
                Book book2 = Book.load(file2);
//                Integer value = Integer.parseInt(parts[2]);

//                System.out.printf("%s - %s --- %s - %s --- %d%n", book1.getAuthor(), book1.getTitle(), book2.getAuthor(), book2.getTitle(), value);

                int size1 = book1.getContent().length();
                int size2 = book2.getContent().length();
                boolean keepFirst;
                if (keepFirstDefault) {
                    keepFirst = size1 > size2 * delta;
                } else {
                    keepFirst = !(size2 > size1 * delta);
                }

//                System.out.printf("%d - %d%n", book1.getYear(), book2.getYear());
//                System.out.printf("%d - %d - %s%n", size1, size2, keepFirst);
                if (transferYear) {
                    yearsToUpdate.put(file2, book1.getYear());
                }
                if (keepFirst) {
                    toBeRemoved.add(file2);
                } else {
                    toBeRemoved.add(file1);
                }
            }

            Set<String> md5s = new HashSet<>();
            Set<File> files;
            Stream<Path> stream1 = Files.find(folder1.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile());
            Stream<Path> stream2 = Files.find(folder2.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile());

            Stream<Path> stream = Stream.concat(stream1, stream2);
            files = stream
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            ProgressBar pb = new ProgressBar("Save", files.size());
            pb.start();
            int skippedCount = 0;
            for (File file : files) {
                pb.step();
                if (toBeRemoved.contains(file) && !keepDuplicates) {
                    continue;
                }
                Book book = Book.load(file);
                if (yearsToUpdate.containsKey(file)) {
                    book.setYear(yearsToUpdate.get(file));
                } else if (book.getYear() == null) {
                    String authorString = book.getAuthor();
                    if (authorString != null && authorString.trim().length() > 0) {
                        String[] authorsArray = authorString.split("[;/]");
                        List<Map<String, Integer>> found = new ArrayList<>();
                        for (String auth : authorsArray) {
                            String a = normalize(auth);
                            if (authors.containsKey(a)) {
                                found.add(authors.get(a));
                            }
                        }

                        if (found.size() > 0) {
                            Integer foundYear = found.get(0).values().stream().findFirst().get();
                            int bestCandidate = foundYear;
                            // todo: use birth/death date
                            int difference = 20;
                            List<Integer> yearGuesses = book.getYearGuesses();
                            for (Integer yearGuess : yearGuesses) {
                                if (Math.abs(yearGuess - foundYear) < difference) {
                                    difference = Math.abs(yearGuess - foundYear);
                                    bestCandidate = yearGuess;
                                }
                            }
                            book.setYear(bestCandidate);
                        } else {
                            List<Integer> yearGuesses = book.getYearGuesses();
                            int[] realisticYears = yearGuesses.stream().filter(i -> i < 2020).mapToInt(i -> i).toArray();
                            if (realisticYears.length == 0) {
                                // No year
                                skippedCount++;
                                continue;
                            }

                            // Get the maximum number
                            int max = Arrays.stream(realisticYears).max().getAsInt();
                            book.setYear(max);
                        }
                    }
                }

                Book.saveBook(book, md5s, outputFolder, 0);
            }
            pb.stop();

//            System.err.println("Searching residual");
//            for (String s : toSearch.keySet()) {
//                System.out.println(s + " - " + toSearch.get(s).size());
//            }
            System.err.printf("Skipped: %d%n", skippedCount);

//            System.out.println("SEARCHING");
//            long begin = System.currentTimeMillis();
//            ExtractedResult extractedResult = FuzzySearch.extractOne(a, authors.keySet());
//            long end = System.currentTimeMillis();
//            System.out.println(end - begin);
//            System.out.println(extractedResult);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalize(String a) {
        a = a.replace('’', '\'');
        a = StringUtils.stripAccents(a);
        a = a.toLowerCase(Locale.ROOT);
        return a;
    }
}
