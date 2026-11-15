package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.ReliefRequest;
import org.rescuedrone.model.enums.RequestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Manages relief requests using ArrayList, HashMap, and Java PriorityQueue.
 * Specifically demonstrates Java PriorityQueue heap mechanics for academic rigor.
 */
public class ReliefRequestManager extends BaseManager<ReliefRequest> {

    // Dedicated Java PriorityQueue heap for active pending requests
    private final PriorityQueue<ReliefRequest> priorityQueue = new PriorityQueue<>();

    public ReliefRequestManager() {
        super(AppConfig.REQUESTS_FILE);
    }

    public ReliefRequestManager(String customFilePath) {
        super(customFilePath);
    }

    @Override
    public synchronized void loadFromFile() {
        super.loadFromFile();
        rebuildPriorityQueue();
    }

    /**
     * Rebuilds the internal PriorityQueue with all requests having PENDING status.
     */
    public synchronized void rebuildPriorityQueue() {
        priorityQueue.clear();
        for (ReliefRequest req : list) {
            if (req.getStatus() == RequestStatus.PENDING) {
                priorityQueue.add(req);
            }
        }
    }

    @Override
    public synchronized void add(ReliefRequest entity) {
        super.add(entity);
        if (entity.getStatus() == RequestStatus.PENDING) {
            priorityQueue.add(entity);
        }
    }

    @Override
    public synchronized void update(ReliefRequest entity) {
        super.update(entity);
        rebuildPriorityQueue();
    }

    @Override
    public synchronized boolean delete(String id) {
        boolean removed = super.delete(id);
        if (removed) {
            rebuildPriorityQueue();
        }
        return removed;
    }

    /**
     * Inspects the highest-priority pending request without removing it.
     */
    public synchronized ReliefRequest peekNextPriorityRequest() {
        return priorityQueue.peek();
    }

    /**
     * Polls the highest-priority pending request from the heap.
     */
    public synchronized ReliefRequest pollNextPriorityRequest() {
        ReliefRequest top = priorityQueue.poll();
        if (top != null) {
            top.setStatus(RequestStatus.ASSIGNED);
            update(top);
        }
        return top;
    }

    /**
     * Returns an ordered snapshot of pending requests as extracted from the PriorityQueue.
     * Preserves PriorityQueue contents.
     */
    public synchronized List<ReliefRequest> getPriorityOrderedPendingRequests() {
        PriorityQueue<ReliefRequest> copy = new PriorityQueue<>(priorityQueue);
        List<ReliefRequest> ordered = new ArrayList<>();
        while (!copy.isEmpty()) {
            ordered.add(copy.poll());
        }
        return ordered;
    }

    public synchronized int getPendingCount() {
        return priorityQueue.size();
    }

    @Override
    protected ReliefRequest parseRecord(String[] tokens) {
        return ReliefRequest.fromCsvTokens(tokens);
    }
}
