package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Route;

import java.util.*;

/**
 * Manages route corridors (graph edges) and builds graph adjacency lists.
 */
public class RouteManager extends BaseManager<Route> {

    // Adjacency List mapping Location ID -> List of outgoing routes
    private final Map<String, List<Route>> adjacencyList = new HashMap<>();

    public RouteManager() {
        super(AppConfig.ROUTES_FILE);
    }

    public RouteManager(String customFilePath) {
        super(customFilePath);
    }

    @Override
    public synchronized void loadFromFile() {
        super.loadFromFile();
        rebuildAdjacencyList();
    }

    public synchronized void rebuildAdjacencyList() {
        adjacencyList.clear();
        for (Route route : list) {
            // Bidirectional connection in disaster relief transport
            adjacencyList.computeIfAbsent(route.getSourceLocationId(), k -> new ArrayList<>()).add(route);
            // Reverse edge
            Route reverse = new Route(route.getId() + "-REV", route.getDestLocationId(),
                    route.getSourceLocationId(), route.getDistanceKm(), route.getCreatedAt());
            adjacencyList.computeIfAbsent(route.getDestLocationId(), k -> new ArrayList<>()).add(reverse);
        }
    }

    @Override
    public synchronized void add(Route entity) {
        super.add(entity);
        rebuildAdjacencyList();
    }

    @Override
    public synchronized void update(Route entity) {
        super.update(entity);
        rebuildAdjacencyList();
    }

    @Override
    public synchronized boolean delete(String id) {
        boolean removed = super.delete(id);
        if (removed) {
            rebuildAdjacencyList();
        }
        return removed;
    }

    public synchronized Map<String, List<Route>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    public synchronized List<Route> getOutgoingRoutes(String locationId) {
        return adjacencyList.getOrDefault(locationId, Collections.emptyList());
    }

    /**
     * Builds and returns a live Graph instance populated with given vertices and all route corridors.
     */
    public synchronized org.rescuedrone.ds.Graph toGraph(List<String> vertexIds) {
        org.rescuedrone.ds.Graph graph = new org.rescuedrone.ds.Graph();
        if (vertexIds != null) {
            for (String v : vertexIds) {
                graph.addVertex(v);
            }
        }
        for (Route r : list) {
            graph.addEdge(r.getSourceLocationId(), r.getDestLocationId(), r.getDistanceKm(), r.getId(), true);
        }
        return graph;
    }

    @Override
    protected Route parseRecord(String[] tokens) {
        return Route.fromCsvTokens(tokens);
    }
}
