package eu.fbk.dh.wikisource;

import com.google.common.collect.HashMultimap;
import eu.fbk.dh.wikisource.structures.Book;
import eu.fbk.dh.wikisource.structures.RichGraph;
import eu.fbk.utils.core.CommandLine;
import me.tongfei.progressbar.ProgressBar;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FindDuplicatesOnTitles {

    private static String UNKNOWN_LABEL = "UNKNOWN";

    public static boolean isBad(String author) {
        if (author == null) {
            return true;
        }
        if (author.length() < 5) {
            return true;
        }
        if (author.charAt(0) == '{') {
            return true;
        }
        String[] parts = author.trim().split("\\s+");
        if (parts.length <= 1) {
            return true;
        }
        return false;
    }

    public static RichGraph getGraph(DirectedGraph<String, DefaultEdge> myGraph, Map<File, String> list1, Map<File, String> list2, String label) {
        ProgressBar pb = new ProgressBar(label, list1.size());
        pb.start();

        HashMultimap<String, File> myMap1 = HashMultimap.create();
        HashMultimap<String, File> myMap2 = HashMultimap.create();
        for (Map.Entry<File, String> entry1 : list1.entrySet()) {
            pb.step();
            String s1 = entry1.getValue();
            if (isBad(s1)) {
                myGraph.addVertex(UNKNOWN_LABEL);
                myMap1.put(UNKNOWN_LABEL, entry1.getKey());
                continue;
            }
            s1 = s1.trim();
            for (Map.Entry<File, String> entry2 : list2.entrySet()) {
                String s2 = entry2.getValue();
                if (isBad(s2)) {
                    myGraph.addVertex(UNKNOWN_LABEL);
                    myMap2.put(UNKNOWN_LABEL, entry2.getKey());
                    continue;
                }
                s2 = s2.trim();
                String[] parts1 = s1.split("[;/]");
                String[] parts2 = s2.split("[;/]");
                boolean e1added = false;
                boolean e2added = false;
                for (String p1 : parts1) {
                    if (isBad(p1)) {
                        continue;
                    }
                    p1 = p1.trim();
                    for (String p2 : parts2) {
                        if (isBad(p2)) {
                            continue;
                        }
                        p2 = p2.trim();
                        e1added = true;
                        e2added = true;
                        myMap1.put(p1, entry1.getKey());
                        myMap2.put(p2, entry2.getKey());
                        myGraph.addVertex(p1);
                        myGraph.addVertex(p2);
                        int value = FuzzySearch.tokenSetRatio(p1, p2);
                        if (value > 90) {
                            myGraph.addEdge(p1, p2);
                        }
                    }
                }
                if (!e1added) {
                    myGraph.addVertex(UNKNOWN_LABEL);
                    myMap1.put(UNKNOWN_LABEL, entry1.getKey());
                }
                if (!e2added) {
                    myGraph.addVertex(UNKNOWN_LABEL);
                    myMap2.put(UNKNOWN_LABEL, entry2.getKey());
                }
            }
        }
        pb.stop();

        return new RichGraph(myGraph, myMap1, myMap2);
    }

    public static void main(String[] args) {

        /*
         * Documentation: https://github.com/xdrop/fuzzywuzzy
         * Python: https://chairnerd.seatgeek.com/fuzzywuzzy-fuzzy-string-matching-in-python/
         * Blogpost: https://towardsdatascience.com/string-matching-with-fuzzywuzzy-e982c61f8a84
         *
         * */

        // todo: Fare il tutti-contro-tutti nel medesimo cluster e filtrare da subito
        //  (così da fare meno controlli)

        final CommandLine cmd = CommandLine
                .parser()
                .withName("compare-datasets")
                .withHeader("Compare datasets.")
                .withOption("f1", "folder1", "Input folder 1", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("f2", "folder2", "Input folder 2", "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
//                .withOption("t", "threads", "Number of threads", "NUM",
//                        CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                .withOption("o", "output-file", "output file", "FILE",
                        CommandLine.Type.FILE, true, false, true)
                .parse(args);

        File folder1 = cmd.getOptionValue("folder1", File.class);
        File folder2 = cmd.getOptionValue("folder2", File.class);
        File outputFile = cmd.getOptionValue("output-file", File.class);
//        Integer numThreads = cmd.getOptionValue("threads", Integer.class, 8);

        ProgressBar pb;

        try {
            Map<File, String> authorList1 = new HashMap<>();
            Map<File, String> authorList2 = new HashMap<>();
            Map<File, String> titleList1 = new HashMap<>();
            Map<File, String> titleList2 = new HashMap<>();

//            System.out.println("[INFO] Loading list 1");
            Set<File> subFiles1 = Files.find(folder1.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            pb = new ProgressBar("First list", subFiles1.size());
            pb.start();
            for (File subFile : subFiles1) {
                pb.step();
                Book book = Book.load(subFile);
                authorList1.put(subFile, book.getAuthor());
                titleList1.put(subFile, book.getTitle());
            }
            pb.stop();
//            System.out.printf("%d items loaded%n", authorList1.size());
//            System.out.println("[INFO] Loading list 2");
            Set<File> subFiles2 = Files.find(folder2.toPath(), 5, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(n -> n.getName().length() == 32)
                    .collect(Collectors.toSet());
            pb = new ProgressBar("Second list", subFiles2.size());
            pb.start();
            for (File subFile : subFiles2) {
                pb.step();
                Book book = Book.load(subFile);
                authorList2.put(subFile, book.getAuthor());
                titleList2.put(subFile, book.getTitle());
            }
            pb.stop();
//            System.out.printf("%d items loaded%n", authorList2.size());

//            System.out.println("[INFO] Starting author comparison");
            DirectedGraph<String, DefaultEdge> authorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
            authorGraph.addVertex(UNKNOWN_LABEL);
            authorGraph.addVertex("Autori vari");
            authorGraph.addVertex("AA. VV.");
            authorGraph.addEdge("AA. VV.", "Autori vari");
            RichGraph authorRichGraph = getGraph(authorGraph, authorList1, authorList2, "Author comparison");

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            ConnectivityInspector<String, DefaultEdge> connectivityInspector;
            connectivityInspector = new ConnectivityInspector<>(authorRichGraph.getGraph());
            for (Set<String> connectedSet : connectivityInspector.connectedSets()) {
                Set<File> set1 = new HashSet<>();
                Set<File> set2 = new HashSet<>();
                for (String author : connectedSet) {
                    set1.addAll(authorRichGraph.getMyMap1().get(author));
                    set2.addAll(authorRichGraph.getMyMap2().get(author));
                }
                int count = set1.size() * set2.size();
                if (count > 1000) {
                    System.err.println("Expanding set: " + connectedSet);
                    Set<File> globalSet = new HashSet<>();
                    globalSet.addAll(set1);
                    globalSet.addAll(set2);
                    Map<File, String> tmpTitleList1 = new HashMap<>(titleList1);
                    Map<File, String> tmpTitleList2 = new HashMap<>(titleList2);
                    tmpTitleList1.keySet().retainAll(globalSet);
                    tmpTitleList2.keySet().retainAll(globalSet);
                    DirectedGraph<String, DefaultEdge> titleGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
                    authorGraph.addVertex(UNKNOWN_LABEL);
                    RichGraph graphWithTitles = getGraph(titleGraph, tmpTitleList1, tmpTitleList2, "Title comparison");
                    connectivityInspector = new ConnectivityInspector<>(graphWithTitles.getGraph());
                    for (Set<String> connectedSet2 : connectivityInspector.connectedSets()) {
                        Set<File> setA = new HashSet<>();
                        Set<File> setB = new HashSet<>();
                        for (String title : connectedSet2) {
                            setA.addAll(graphWithTitles.getMyMap1().get(title));
                            setB.addAll(graphWithTitles.getMyMap2().get(title));
                        }
                        System.err.println("Comparing title set: " + connectedSet2);
                        compareSets(setA, setB, writer);
                    }
                } else {
                    System.err.println("Comparing author set: " + connectedSet);
                    compareSets(set1, set2, writer);
                }
            }
            writer.close();

//            System.exit(1);
//
//            System.out.println(authorRichGraph.getMyMap1().get(UNKNOWN_LABEL).size());
//            System.out.println(authorRichGraph.getMyMap2().get(UNKNOWN_LABEL).size());
//
//            Set<File> toKeep = new HashSet<>();
//            toKeep.addAll(authorRichGraph.getMyMap1().get(UNKNOWN_LABEL));
//            toKeep.addAll(authorRichGraph.getMyMap2().get(UNKNOWN_LABEL));
//
//            titleList1.keySet().retainAll(toKeep);
//            titleList2.keySet().retainAll(toKeep);
//            DirectedGraph<String, DefaultEdge> titleGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
//            authorGraph.addVertex(UNKNOWN_LABEL);
//            RichGraph graphWithTitles = getGraph(titleGraph, titleList1, titleList2, "Title comparison");
//            connectivityInspector = new ConnectivityInspector<>(graphWithTitles.getGraph());
//            for (Set<String> connectedSet : connectivityInspector.connectedSets()) {
//                if (connectedSet.size() > 1) {
//                    System.out.println(connectedSet);
//                }
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void compareSets(Set<File> set1, Set<File> set2, BufferedWriter writer) throws IOException, ClassNotFoundException {
        int comparisons = set1.size() * set2.size();
        if (comparisons == 0) {
            return;
        }

        ProgressBar pb = new ProgressBar(String.format("Comparison (%d)", comparisons), set1.size());
        pb.start();
        for (File file1 : set1) {
            pb.step();
            Book book1 = Book.load(file1);
            for (File file2 : set2) {
                Book book2 = Book.load(file2);

                String content1 = book1.getContent();
                String content2 = book2.getContent();

                int value;
                if (content1.length() > content2.length()) {
                    String textToFind = content2.substring(content2.length() - Math.min(10000, content2.length()));
                    value = FuzzySearch.tokenSetRatio(content1, textToFind);
                } else {
                    String textToFind = content1.substring(content1.length() - Math.min(10000, content1.length()));
                    value = FuzzySearch.tokenSetRatio(content2, textToFind);
                }

//                int value = FuzzySearch.tokenSetRatio(content1, book2.getContent());
                if (value > 90) {
                    writer.append(file1.getAbsolutePath());
                    writer.append("\t");
                    writer.append(file2.getAbsolutePath());
                    writer.append("\t");
                    writer.append(Integer.toString(value));
                    writer.append("\n");
                    writer.flush();
                }
            }
        }
        pb.stop();
    }
}
