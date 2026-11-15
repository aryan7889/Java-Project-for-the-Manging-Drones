package org.rescuedrone.test;

import org.rescuedrone.algorithm.DijkstraOptimizer;
import org.rescuedrone.algorithm.DijkstraOptimizer.PathResult;
import org.rescuedrone.ds.Graph;
import org.rescuedrone.service.ReliefSystem;

import java.util.List;

/**
 * Verification test suite for Phase 7: Graph & Dijkstra Route Optimization.
 * Tests manual Dijkstra implementation, PriorityQueue relaxation, path reconstruction,
 * and unreachable node handling.
 */
public class Phase7Test {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean condition, String message) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
            throw new AssertionError("Test failed: " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RescueDrone Phase 7 Graph & Dijkstra Tests     ");
        System.out.println("=================================================");

        testGraphAdjacencyListConstruction();
        testDijkstraTriangleShortestPath();
        testPathReconstructionOrder();
        testUnreachableLocationHandling();
        testSameSourceAndDestination();
        testLiveDisasterReliefGraphRouting();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testGraphAdjacencyListConstruction() {
        System.out.println("\n-- Testing Graph Adjacency List Construction --");
        Graph g = new Graph();

        g.addVertex("V1");
        g.addVertex("V2");
        g.addEdge("V1", "V2", 7.5, "R1", true);

        assertTrue(g.containsVertex("V1"), "Graph contains vertex V1");
        assertTrue(g.containsVertex("V2"), "Graph contains vertex V2");
        assertTrue(g.getVertexCount() == 2, "Vertex count is 2");
        assertTrue(g.getNeighbors("V1").size() == 1, "V1 has 1 outgoing neighbor");
        assertTrue(g.getNeighbors("V2").size() == 1, "V2 has 1 reverse neighbor (bidirectional)");
        assertTrue(g.getNeighbors("V1").get(0).getDistanceKm() == 7.5, "Edge weight is 7.5 km");
    }

    private static void testDijkstraTriangleShortestPath() {
        System.out.println("\n-- Testing Dijkstra Shortest Path on Classic Triangle Network --");
        // Graph topology:
        // A -> B direct: 10.0 km
        // A -> C: 3.0 km
        // C -> B: 4.0 km
        // Shortest path A -> B should be A -> C -> B (7.0 km) rather than direct (10.0 km)

        Graph g = new Graph();
        g.addEdge("A", "B", 10.0, "R_AB", true);
        g.addEdge("A", "C", 3.0, "R_AC", true);
        g.addEdge("C", "B", 4.0, "R_CB", true);

        PathResult res = DijkstraOptimizer.findShortestPath(g, "A", "B");

        assertTrue(res.isFound(), "Shortest path was successfully found");
        assertTrue(res.getTotalDistanceKm() == 7.0, "Calculated shortest distance is 7.0 km (relaxing 10.0 km edge)");
        assertTrue(res.getHopCount() == 2, "Path consists of exactly 2 hops");
    }

    private static void testPathReconstructionOrder() {
        System.out.println("\n-- Testing Path Node Sequence Reconstruction --");
        // Topology: S -> N1 (2.0) -> N2 (3.0) -> D (1.0) = 6.0 km
        Graph g = new Graph();
        g.addEdge("S", "N1", 2.0, "R1", true);
        g.addEdge("N1", "N2", 3.0, "R2", true);
        g.addEdge("N2", "D", 1.0, "R3", true);

        PathResult res = DijkstraOptimizer.findShortestPath(g, "S", "D");

        assertTrue(res.isFound(), "Path found");
        List<String> path = res.getPathNodeIds();
        assertTrue(path.size() == 4, "Path contains 4 nodes in sequence");
        assertTrue(path.get(0).equals("S"), "Path starts at source S");
        assertTrue(path.get(1).equals("N1"), "Intermediate node 1 is N1");
        assertTrue(path.get(2).equals("N2"), "Intermediate node 2 is N2");
        assertTrue(path.get(3).equals("D"), "Path ends at destination D");
        assertTrue(res.getTotalDistanceKm() == 6.0, "Total distance is 6.0 km");
    }

    private static void testUnreachableLocationHandling() {
        System.out.println("\n-- Testing Unreachable Location & Disconnected Component Handling --");
        Graph g = new Graph();
        g.addEdge("LOC-A", "LOC-B", 5.0, "R1", true);
        g.addVertex("LOC-ISOLATED"); // Vertex with 0 edges

        PathResult res = DijkstraOptimizer.findShortestPath(g, "LOC-A", "LOC-ISOLATED");

        assertTrue(!res.isFound(), "Unreachable vertex correctly marked as not found");
        assertTrue(res.getMessage().contains("unreachable"), "Descriptive unreachable diagnosis returned");
        assertTrue(res.getPathNodeIds().isEmpty(), "Path list is empty for unreachable destination");
    }

    private static void testSameSourceAndDestination() {
        System.out.println("\n-- Testing Trivial Source Equals Destination --");
        Graph g = new Graph();
        g.addVertex("LOC-SELF");

        PathResult res = DijkstraOptimizer.findShortestPath(g, "LOC-SELF", "LOC-SELF");

        assertTrue(res.isFound(), "Path found when source is destination");
        assertTrue(res.getTotalDistanceKm() == 0.0, "Distance is 0.0 km");
        assertTrue(res.getHopCount() == 0, "Hop count is 0");
        assertTrue(res.getPathNodeIds().size() == 1, "Path contains single node");
    }

    private static void testLiveDisasterReliefGraphRouting() {
        System.out.println("\n-- Testing Dijkstra Routing on Live Application Relief Graph --");
        ReliefSystem system = ReliefSystem.getInstance();
        system.initialize();

        List<String> locIds = new java.util.ArrayList<>();
        for (var loc : system.getLocationManager().getAll()) {
            locIds.add(loc.getId());
        }
        Graph liveGraph = system.getRouteManager().toGraph(locIds);

        // Routing from Base Hub LOC-001 to East Ridge Village LOC-003
        // Possible paths:
        // LOC-001 -> LOC-004 (6.0) -> LOC-003 (7.8) = 13.8 km
        // LOC-001 -> LOC-002 (8.5) -> LOC-003 (5.7) = 14.2 km
        PathResult res = DijkstraOptimizer.findShortestPath(liveGraph, "LOC-001", "LOC-003");

        assertTrue(res.isFound(), "Shortest path found on live disaster transportation network");
        assertTrue(res.getTotalDistanceKm() == 13.8, "Dijkstra chose shortest path via LOC-004 (13.8 km < 14.2 km)");
        assertTrue(res.getPathNodeIds().get(0).equals("LOC-001"), "Path originates at LOC-001 (Base Hub)");
        assertTrue(res.getPathNodeIds().get(1).equals("LOC-004"), "Intermediate vertex is LOC-004");
        assertTrue(res.getPathNodeIds().get(2).equals("LOC-003"), "Path terminates at LOC-003 (East Ridge Village)");
    }
}
