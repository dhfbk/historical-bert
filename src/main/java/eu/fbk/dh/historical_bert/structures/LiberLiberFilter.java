package eu.fbk.dh.historical_bert.structures;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiberLiberFilter extends Filter {
    static Set<String> blockWords = new HashSet<>();
    static Pattern yearPattern = Pattern.compile("[0-9]{4}");

    static {
        blockWords.add("REVISIONE:");
        blockWords.add("IMPAGINAZIONE:");
        blockWords.add("PUBBLICAZIONE:");
    }

    @Override
    public Book cleanBook(String fileName, String label) throws IOException {
        File thisFile = new File(fileName);

        StringBuilder builder = new StringBuilder();

        Book book = new Book();
        book.setLabel(label);
        book.setFileName(thisFile.getName());

        Scanner scanner = new Scanner(thisFile);
        boolean triggeredBlockWord = false;
        StringBuilder title = new StringBuilder();
        String author = "";
        List<Integer> years = new ArrayList<>();
        boolean inTitle = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.length() > 0 && inTitle && line.charAt(0) == ' ') {
                title.append(" ").append(line.trim());
            } else {
                inTitle = false;
            }
            line = line.trim();

            if (line.startsWith("TRATTO DA:")) {
                Matcher matcher = yearPattern.matcher(line);
                while (matcher.find()) {
                    years.add(Integer.parseInt(matcher.group()));
                }
            }
            if (line.startsWith("TITOLO:")) {
                inTitle = true;
                title.append(line.substring("TITOLO:".length()).trim());
            }
            if (line.startsWith("AUTORE:") || line.startsWith("AUTORI:")) {
                author = line.substring("AUTORX:".length()).trim();
            }
            String rawLine = line.replaceAll("[\\s/:.]", "");
            if (rawLine.toLowerCase(Locale.ROOT).contains("liberliber")) {
                triggeredBlockWord = false;
                builder = new StringBuilder();
                continue;
            }
            if (blockWords.contains(line)) {
                triggeredBlockWord = true;
            }
            if (triggeredBlockWord && line.length() == 0) {
                triggeredBlockWord = false;
                builder = new StringBuilder();
                continue;
            }

            builder.append(line).append("\n");
        }

        String content = builder.toString().replaceAll("\n{2,}", "\n\n");
        content = content.trim();

        author = author.replaceAll("\\(.*\\)", "");
        author = author.replaceAll("\\[.*\\]", "");
        author = author.replaceAll("<.*>", "");
        author = author.replace(":", "");
        author = author.replaceAll("\\s+", " ");
        author = author.trim();

        List<String> newAuthors = new ArrayList<>();
        String[] authors = author.split("(;| e )");
        for (String s : authors) {
            List<String> newList = new ArrayList<>();
            String[] parts = s.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                newList.add(parts[i].trim());
            }
            String a = String.join(" ", newList);
            newAuthors.add(a);
        }
        author = String.join("; ", newAuthors);

        // /Users/alessio/Desktop/historical-bert/LiberLiber/1900/1906__Previati_Gaetano__I_principii_scientifici_del_divisionismo.txt
        // /Users/alessio/Desktop/historical-bert/LiberLiber/1900/1900__De_Cesare_Raffaele__La_fine_di_un_regno_Napoli_e_Sicilia_Parte_I_Regno_di_Ferdinando_II.txt
        // /Users/alessio/Desktop/historical-bert/LiberLiber/1900/1917__Romagnoli_Ettore__Minerva_e_lo_scimmione.txt

        book.setContent(content);
        book.setYearGuesses(years);
        book.setTitle(title.toString());
        book.setAuthor(author);

        return book;
    }
}
