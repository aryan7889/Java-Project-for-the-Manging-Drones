package org.rescuedrone.ds;

import java.util.*;

/**
 * Weighted Graph represented via Adjacency Lists.
 * Vertices are location IDs; Edges contain destination, weight (km), and route ID.
 */
public class Graph {

    public static class Edge {
        private final String destinationId;
        private final double distanceKm;
        private final String routeId;

        public Edge(String destinationId, double distanceKm, String routeId) {
            this.destinationId = destinationId;
            this.distanceKm = distanceKm;
            this.routeId = routeId;
        }

        public String getDestinationId() {
            return destinationId;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public String getRouteId() {
            return routeId;
        }

        @Override
        public String toString() {
            return "->" + destinationId + " (" + distanceKm + "km)";
        }
    }

    private final Map<String, List<Edge>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public synchronized void addVertex(String vertexId) {
        if (vertexId == null) return;
        adjacencyList.putIfAbsent(vertexId, new ArrayList<>());
    }

    public synchronized void removeVertex(String vertexId) {
        if (vertexId == null) return;
        adjacencyList.remove(vertexId);
        // Remove incoming edges to this vertex
        for (List<Edge> edges : adjacencyList.values()) {
            edges.removeIf(e -> e.getDestinationId().equalsIgnoreCase(vertexId));
        }
    }

    public synchronized void addEdge(String source, String destination, double distance, String routeId, boolean bidirectional) {
        if (source == null || destination == null || distance <= 0) return;

        addVertex(source);
        addVertex(destination);

        // Remove any prior edge between these two vertices
        adjacencyList.get(source).removeIf(e -> e.getDestinationId().equalsIgnoreCase(destination));
        adjacencyList.get(source).add(new Edge(destination, distance, routeId));

        if (bidirectional) {
            adjacencyList.get(destination).removeIf(e -> e.getDestinationId().equalsIgnoreCase(source));
            adjacencyList.get(destination).add(new Edge(source, distance, routeId));
        }
    }

    public synchronized List<Edge> getNeighbors(String vertexId) {
        return adjacencyList.getOrDefault(vertexId, Collections.emptyList());
    }

    public synchronized Set<String> getAllVertices() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    public synchronized Map<String, List<Edge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    public synchronized boolean containsVertex(String vertexId) {
        return adjacencyList.containsKey(vertexId);
    }

    public synchronized int getVertexCount() {
        return adjacencyList.size();
    }

    public synchronized int getEdgeCount() {
        int total = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            total += edges.size();
        }
        return total;
    }
}
