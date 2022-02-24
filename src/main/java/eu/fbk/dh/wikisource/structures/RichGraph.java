package eu.fbk.dh.wikisource.structures;

import com.google.common.collect.HashMultimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.File;

@Data
@AllArgsConstructor
public class RichGraph {
    DirectedGraph<String, DefaultEdge> graph;
    HashMultimap<String, File> myMap1;
    HashMultimap<String, File> myMap2;
}
