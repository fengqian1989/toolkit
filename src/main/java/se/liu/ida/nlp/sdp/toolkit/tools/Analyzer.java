/*
 * See the file "LICENSE" for the full license governing this code.
 */
package se.liu.ida.nlp.sdp.toolkit.tools;

import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import se.liu.ida.nlp.sdp.toolkit.graph.Edge;
import se.liu.ida.nlp.sdp.toolkit.graph.Graph;
import se.liu.ida.nlp.sdp.toolkit.graph.InspectedGraph;
import se.liu.ida.nlp.sdp.toolkit.graph.Node;
import se.liu.ida.nlp.sdp.toolkit.io.GraphReader;
import se.liu.ida.nlp.sdp.toolkit.io.GraphReader2015;

/**
 * Print statistics about a collection of graphs.
 *
 * @author Marco Kuhlmann
 */
public class Analyzer {

    // The number of graphs read.
    private int nGraphs;

    // The number of non-wall nodes seen.
    private int nNonWallNodes;

    // The number of edges seen.
    private int nEdges;

    // Set of labels seen.
    private final Set<String> labels = new HashSet<String>();

    // The number of singleton nodes.
    private int nSingletons;

    // Number of cyclic graphs.
    private int nCyclic;

    // Number of forests.
    private int nForests;

    // Number of trees.
    private int nTrees;

    // Number of graphs that are semi-connected.
    private int nFragmented;

    // Number of nodes that have more than one incoming edge.
    private int nReentrantNodes;

    // Number of topless graphs.
    private int nToplessGraphs;

    // Number of top nodes.
    private int nTopNodes;

    // Number of special nodes.
    private int nSpecialNodes;

    // Number of noncrossing graphs.
    private int nNoncrossingGraphs;

    // Number of projective graphs.
    private int nProjectiveGraphs;

    // Set of senses seen.
    private final Set<String> senses = new HashSet<String>();

    // Number of (non-singleton) nodes with senses.
    private int nScorablePredicates;

    /**
     * Updates the statistics with the specified graph.
     *
     * @param graph a semantic dependency graph
     */
    public void update(Graph graph) {
        InspectedGraph inspectedGraph = new InspectedGraph(graph);

        // number of graphs
        nGraphs++;

        // number of non-wall nodes
        nNonWallNodes += inspectedGraph.getNNonWallNodes();

        // number of edges
        nEdges += graph.getNEdges();

        // distinct labels
        for (Edge edge : graph.getEdges()) {
            labels.add(edge.label);
        }

        // number of singleton nodes
        nSingletons += inspectedGraph.getNSingletons();

        // number of cyclic graphs
        nCyclic += inspectedGraph.isCyclic() ? 1 : 0;

        // number of forests
        nForests += inspectedGraph.isForest() ? 1 : 0;

        // number of trees
        nTrees += inspectedGraph.isTree() ? 1 : 0;

        // number of graphs that are fragmented
        nFragmented += inspectedGraph.getNComponents() - 1 - inspectedGraph.getNSingletons() == 1 ? 0 : 1;

        // number of reentrant nodes
        for (Node node : graph.getNodes()) {
            nReentrantNodes += node.getNIncomingEdges() > 1 ? 1 : 0;
        }

        // number of topless graphs
        boolean isTopless = true;
        for (Node node : graph.getNodes()) {
            isTopless = isTopless && !node.isTop;
        }
        nToplessGraphs += isTopless ? 1 : 0;

        // number of top nodes
        for (Node node : graph.getNodes()) {
            nTopNodes += node.isTop ? 1 : 0;
        }

        // number of special nodes
        for (Node node : graph.getNodes()) {
            nSpecialNodes += node.id != 0 && !inspectedGraph.isSingleton(node.id) && !node.hasIncomingEdges() && !node.isTop ? 1 : 0;
        }

        // number of noncrossing graphs
        nNoncrossingGraphs += inspectedGraph.isNoncrossing() ? 1 : 0;

        // number of projective graphs
        nProjectiveGraphs += inspectedGraph.isProjective() ? 1 : 0;

        // number of senses
        for (Node node : graph.getNodes()) {
            if (node.id != 0 && !inspectedGraph.isSingleton(node.id) && node.isPred && node.pos.startsWith("V") && !node.sense.equals("_")) {
                senses.add(node.sense);
                nScorablePredicates += 1;
            }
        }
    }

    /**
     * Prints statistics about a set of graphs.
     *
     * @param args names of files from which to read graphs
     * @throws Exception if an I/O exception occurs
     */
    public static void main(String[] args) throws Exception {
        Analyzer analyzer = new Analyzer();
        GraphReader reader = new GraphReader2015(new InputStreamReader(System.in));
        Graph graph;
        while ((graph = reader.readGraph()) != null) {
            analyzer.update(graph);
        }
        reader.close();
        System.err.format("number of labels:\t%d%n", analyzer.labels.size());
        System.err.format("percentage of singletons:\t%s%n", percentage(analyzer.nSingletons, analyzer.nNonWallNodes));
        System.err.format("edge density:\t%s%n", fraction(analyzer.nEdges, analyzer.nNonWallNodes - analyzer.nSingletons, 2));
        System.err.format("percentage of graphs that are trees:\t%s%n", percentage(analyzer.nTrees, analyzer.nGraphs));
        System.err.format("percentage of graphs that are projective:\t%s%n", percentage(analyzer.nProjectiveGraphs, analyzer.nGraphs));
        System.err.format("percentage of graphs that are fragmented:\t%s%n", percentage(analyzer.nFragmented, analyzer.nGraphs));
        System.err.format("percentage of nodes that have reentrancies:\t%s%n", percentage(analyzer.nReentrantNodes, analyzer.nNonWallNodes - analyzer.nSingletons));
        System.err.format("percentage of graphs that are topless:\t%s%n", percentage(analyzer.nToplessGraphs, analyzer.nGraphs));
        System.err.format("number of top nodes per graph:\t%s%n", fraction(analyzer.nTopNodes, analyzer.nGraphs));
        System.err.format("percentage of nodes that are non-top roots:\t%s%n", percentage(analyzer.nSpecialNodes, analyzer.nNonWallNodes - analyzer.nSingletons));
        System.err.format("number of senses:\t%d%n", analyzer.senses.size());
        System.err.format("percentage of predicates with senses:\t%s%n", percentage(analyzer.nScorablePredicates, analyzer.nNonWallNodes - analyzer.nSingletons));
    }

    public static String fraction(int a, int b, int digits) {
        return String.format(String.format("%%.%df", digits), (double) a / (double) b);
    }

    public static String fraction(int a, int b) {
        return fraction(a, b, 4);
    }

    public static String percentage(int enumerator, int denominator) {
        return String.format("%.2f", (double) enumerator / (double) denominator * 100);
    }
}
