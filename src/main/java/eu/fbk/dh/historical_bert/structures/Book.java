package eu.fbk.dh.historical_bert.structures;

import com.google.common.collect.HashMultimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book implements Serializable {
    HashMultimap<String, String> rawValues;
    String fileName;
    String link = null;
    String title = null;
    String author = null;
    Integer year = null;
    List<Integer> yearGuesses = new ArrayList<>();
    String content = null;
    String label;

    Boolean isEmpty = false;

    public boolean isGood() {
        if (isEmpty) {
            return false;
        }
        if (content == null) {
            return false;
        }
        if (title == null) {
            return false;
        }

        return true;
    }

    public void save(File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    static public Book load(File file) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Book book = (Book) objectInputStream.readObject();
        objectInputStream.close();
        return book;
    }

    public String getChecksum(@Nullable String prefix) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String s = this.fileName;
        if (prefix != null) {
            s = prefix + "_" + s;
        }
        md.update(s.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }

    public static void saveBook(Book book, Set<String> md5s, File outputPathFile, int min) throws NoSuchAlgorithmException, IOException {
        if (book.getContent().length() < min) {
            return;
        }
        String checksum = book.getChecksum(book.label);
        if (md5s.contains(checksum)) {
            System.out.printf("[ERR] Checksum already exists: %s%n", book.getFileName());
        }
        book.save(getOutputFile(outputPathFile, checksum));
        md5s.add(checksum);
    }

    public static String savePlainBook(Book book, Set<String> md5s, File outputPathFile, int min) throws NoSuchAlgorithmException, IOException {
        if (book.getContent().length() < min) {
            return null;
        }
        String checksum = book.getChecksum(book.label);
        if (md5s.contains(checksum)) {
            System.out.printf("[ERR] Checksum already exists: %s%n", book.getFileName());
        }
        File outputFile = getOutputFile(outputPathFile, checksum);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.append(book.getContent());
        writer.close();
        md5s.add(checksum);
        return checksum;
    }

    public static File getOutputFile(File outputPath, String md5) {
        if (md5.length() < 3) {
            return null;
        }
        String absolutePath = outputPath.getAbsolutePath();
        String folder = absolutePath + File.separator + md5.substring(0, 2);
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        return new File(folder + File.separator + md5);
    }

}
