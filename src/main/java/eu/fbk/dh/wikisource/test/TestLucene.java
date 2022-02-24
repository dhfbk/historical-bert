package eu.fbk.dh.wikisource.test;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.File;
import java.io.IOException;

public class TestLucene {
    public static void main(String[] args) {
        try {
            Directory myStore = FSDirectory.open(new File("/Users/alessio/Desktop/historical-bert/wikidatawiki-out").toPath());
            IndexReader indexReader = DirectoryReader.open(myStore);
            IndexSearcher searcher = new IndexSearcher(indexReader);

            for (int i = 0; i < indexReader.numDocs(); i++) {
                Document document = indexReader.document(i);
                System.out.println(document.get("title"));
                System.out.println(document.getBinaryValue("body").utf8ToString());
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
