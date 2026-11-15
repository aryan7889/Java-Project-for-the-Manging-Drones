package org.rescuedrone.algorithm;

import org.rescuedrone.ds.Graph;
import org.rescuedrone.ds.Graph.Edge;

import java.util.*;

/**
 * Manual implementation of Dijkstra's Shortest Path Algorithm
 * using a PriorityQueue (Binary Min-Heap) for vertex relaxation.
 * Mandatory academic requirement for B.Tech CS Java Lab.
 */
public class DijkstraOptimizer {

    /**
     * Node wrapper used inside the PriorityQueue for shortest path relaxation.
     */
    public static class PathNode implements Comparable<PathNode> {
        private final String vertexId;
        private final double distance;

        public PathNode(String vertexId, double distance) {
            this.vertexId = vertexId;
            this.distance = distance;
        }

        public String getVertexId() {
            return vertexId;
        }

        public double getDistance() {
            return distance;
        }

        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Data Transfer Object holding the shortest path outcome.
     */
    public static class PathResult {
        private final boolean found;
        private final String sourceId;
        private final String destId;
        private final double totalDistanceKm;
        private final List<String> pathNodeIds;
        private final int hopCount;
        private final String message;

        public PathResult(boolean found, String sourceId, String destId, double totalDistanceKm,
                          List<String> pathNodeIds, int hopCount, String message) {
            this.found = found;
            this.sourceId = sourceId;
            this.destId = destId;
            this.totalDistanceKm = totalDistanceKm;
            this.pathNodeIds = pathNodeIds != null ? pathNodeIds : new ArrayList<>();
            this.hopCount = hopCount;
            this.message = message;
        }

        public boolean isFound() {
            return found;
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getDestId() {
            return destId;
        }

        public double getTotalDistanceKm() {
            return totalDistanceKm;
        }

        public List<String> getPathNodeIds() {
            return pathNodeIds;
        }

        public int getHopCount() {
            return hopCount;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            if (!found) return "Path not found between " + sourceId + " and " + destId + ": " + message;
            return String.format("Shortest Path [%s]: %s (Total Dist: %.2f km, Hops: %d)",
                    sourceId + " -> " + destId, String.join(" -> ", pathNodeIds), totalDistanceKm, hopCount);
        }
    }

    /**
     * Calculates the shortest path between source and destination over the given graph.
     */
    public static PathResult findShortestPath(Graph graph, String sourceId, String destId) {
        if (graph == null) {
            return new PathResult(false, sourceId, destId, 0.0, null, 0, "Graph is null");
        }
        if (sourceId == null || !graph.containsVertex(sourceId)) {
            return new PathResult(false, sourceId, destId, 0.0, null, 0, "Source vertex " + sourceId + " not found in graph");
        }
        if (destId == null || !graph.containsVertex(destId)) {
            return new PathResult(false, sourceId, destId, 0.0, null, 0, "Destination vertex " + destId + " not found in graph");
        }

        // Trivial case: source is destination
        if (sourceId.equalsIgnoreCase(destId)) {
            return new PathResult(true, sourceId, destId, 0.0, List.of(sourceId), 0, "Source is destination");
        }

        // Working data structures
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<PathNode> minHeap = new PriorityQueue<>();

        // Initialize all vertex distances to infinity
        for (String vertex : graph.getAllVertices()) {
            distances.put(vertex, Double.POSITIVE_INFINITY);
        }

        distances.put(sourceId, 0.0);
        minHeap.add(new PathNode(sourceId, 0.0));

        while (!minHeap.isEmpty()) {
            PathNode current = minHeap.poll();
            String u = current.getVertexId();
            double d = current.getDistance();

            // Early exit if optimal path to destination reached
            if (u.equalsIgnoreCase(destId)) {
                break;
            }

            if (visited.contains(u)) continue;
            visited.add(u);

            // Relax outgoing edges
            for (Edge edge : graph.getNeighbors(u)) {
                String v = edge.getDestinationId();
                if (visited.contains(v)) continue;

                double weight = edge.getDistanceKm();
                double tentativeDistance = d + weight;

                if (tentativeDistance < distances.get(v)) {
                    distances.put(v, tentativeDistance);
                    predecessors.put(v, u);
                    minHeap.add(new PathNode(v, tentativeDistance));
                }
            }
        }

        double shortestDistance = distances.get(destId);
        if (Double.isInfinite(shortestDistance)) {
            return new PathResult(false, sourceId, destId, 0.0, null, 0,
                    "Destination " + destId + " is completely unreachable from " + sourceId + " (No connecting corridor)");
        }

        // Reconstruct path backward from destination to source using predecessors map
        LinkedList<String> path = new LinkedList<>();
        String step = destId;
        path.addFirst(step);

        while (predecessors.containsKey(step)) {
            step = predecessors.get(step);
            path.addFirst(step);
            if (step.equalsIgnoreCase(sourceId)) break;
        }

        int hops = path.size() - 1;
        String msg = String.format("Optimal route found: %d hops, %.2f km total distance", hops, shortestDistance);

        return new PathResult(true, sourceId, destId, shortestDistance, path, hops, msg);
    }
}
