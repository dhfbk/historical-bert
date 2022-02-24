package eu.fbk.dh.wikisource.structures;

import com.google.common.collect.HashMultimap;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikisourceFilter extends Filter {

    public static Pattern fieldPattern = Pattern.compile("([A-Z_-]+):\\s+(.*)");
    public static Pattern centPattern = Pattern.compile("([IVXLCDM]+) secolo");
    public static Pattern doubleCentPattern = Pattern.compile("([IVXLCDM]+) secolo\\s*/\\s*([IVXLCDM]+) secolo");
    public static Pattern multiPattern = Pattern.compile("([0-9]+)(\\s*[-–/]\\s*([0-9]+))+");
    public static Pattern dayPattern = Pattern.compile("([0-9]+)\\s+(gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre)\\s+([0-9]+)");

    public static Integer ANTITCHITA = -1000;

    @Override
    public Book cleanBook(String fileName, String label) throws IOException {
        File thisFile = new File(fileName);

        StringBuilder builder = new StringBuilder();

        Book book = new Book();
        book.setLabel(label);
        book.setFileName(thisFile.getName());

        Scanner scanner = new Scanner(thisFile);
        boolean inHeader = false;
        HashMultimap<String, String> values = HashMultimap.create();
        boolean firstLine = true;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.trim();
            if (firstLine) {
                if (line.startsWith("RINVIA")) {
                    book.setIsEmpty(true);
                }
                if (line.length() > 0) {
                    firstLine = false;
                }
            }
            if (line.startsWith("<section")) {
                if (line.toLowerCase(Locale.ROOT).contains("url della versione cartacea a fronte")) {
                    book.setIsEmpty(true);
                }
                continue;
            }
            if (line.startsWith("--")) {
                if (line.startsWith("-- HEADER")) {
                    inHeader = true;
                }
                if (line.startsWith("-- END OF HEADER")) {
                    inHeader = false;
                }
                continue;
            }

            if (inHeader) {
                Matcher matcher = fieldPattern.matcher(line);
                if (matcher.matches()) {
                    String content = matcher.group(2);
                    content = content.trim();
                    if (content.length() > 0) {
                        values.put(matcher.group(1), content);
                    }
                }
                continue;
            }

            builder.append(line).append("\n");
        }

        String content = builder.toString().replaceAll("\n{2,}", "\n\n");
        content = content.trim();

        book.setContent(content);
        if (values.containsKey("AUTHOR")) {
            Optional<String> longestString = values.get("AUTHOR").stream().max(Comparator.comparingInt(String::length));
            longestString.ifPresent(book::setAuthor);
        }
        if (values.containsKey("TITLE")) {
            Optional<String> longestString = values.get("TITLE").stream().max(Comparator.comparingInt(String::length));
            longestString.ifPresent(book::setTitle);
        }
        Integer year = null;
        if (values.containsKey("YEAR")) {
            for (String yearStr : values.get("YEAR")) {
                boolean ac = false;
                Integer realYear = null;

                yearStr = yearStr.replace("a. C.", "a.C.");
                if (yearStr.contains("a.C.")) {
                    ac = true;
                    yearStr = yearStr.replace("a.C.", "").trim();
                }
                if (yearStr.contains("d.C.")) {
                    yearStr = yearStr.replace("d.C.", "").trim();
                }
                try {
                    realYear = Integer.parseInt(yearStr);
                } catch (NumberFormatException nfe) {
                    Matcher centMatcher = centPattern.matcher(yearStr);
                    Matcher multiMatcher = multiPattern.matcher(yearStr);
                    Matcher doublecentMatcher = doubleCentPattern.matcher(yearStr);
                    Matcher dayMatcher = dayPattern.matcher(yearStr);
                    if (centMatcher.matches()) {
                        realYear = (100 * romanToInteger(centMatcher.group(1))) - 50;
                    } else if (multiMatcher.matches()) {
                        realYear = (Integer.parseInt(multiMatcher.group(1)) + Integer.parseInt(multiMatcher.group(3))) / 2;
                    } else if (doublecentMatcher.matches()) {
                        realYear = (((100 * romanToInteger(doublecentMatcher.group(1))) + (100 * romanToInteger(doublecentMatcher.group(2)))) / 2) - 50;
                    } else if (dayMatcher.matches()) {
                        realYear = Integer.parseInt(dayMatcher.group(3));
                    } else if (yearStr.toLowerCase(Locale.ROOT).equals("antichità")) {
                        realYear = ANTITCHITA;
                    } else {
                        System.out.printf("[WARN] Year: %s (%s)%n", yearStr, fileName);
                    }
                }

                if (realYear == null) {
                    continue;
                }

                if (ac) {
                    realYear = -realYear;
                }

                if (year == null || year > realYear) {
                    year = realYear;
                }
            }
        }

        book.setYear(year);
        return book;
    }

}
