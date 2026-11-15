package org.rescuedrone.service;

import org.rescuedrone.model.BaseEntity;
import org.rescuedrone.persistence.FileManager;

import java.util.*;

/**
 * Generic base manager encapsulating ArrayList (sequential collections)
 * and HashMap (O(1) key-based lookups).
 * Demonstrates Abstraction, Generics, and Data Structure integration.
 */
public abstract class BaseManager<T extends BaseEntity> {
    protected final List<T> list = new ArrayList<>();
    protected final Map<String, T> map = new HashMap<>();
    protected final String filePath;

    public BaseManager(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void loadFromFile() {
        list.clear();
        map.clear();
        List<String> lines = FileManager.readLines(filePath);

        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            try {
                String[] tokens = FileManager.parseCsvLine(line);
                T entity = parseRecord(tokens);
                if (entity != null) {
                    list.add(entity);
                    map.put(entity.getId(), entity);
                }
            } catch (Exception e) {
                System.err.println("[BaseManager] Warning: Skipped corrupted record at " + filePath + ":" + lineNum + " (" + e.getMessage() + ")");
            }
        }
    }

    public synchronized void saveToFile() {
        List<String> lines = new ArrayList<>();
        for (T entity : list) {
            lines.add(entity.toCsvRecord());
        }
        FileManager.writeLines(filePath, lines);
    }

    public synchronized void add(T entity) {
        if (entity == null) throw new IllegalArgumentException("Cannot add null entity");
        entity.validate();
        if (map.containsKey(entity.getId())) {
            throw new IllegalArgumentException("Entity with ID " + entity.getId() + " already exists");
        }
        list.add(entity);
        map.put(entity.getId(), entity);
        saveToFile();
    }

    public synchronized void update(T entity) {
        if (entity == null) throw new IllegalArgumentException("Cannot update null entity");
        entity.validate();
        if (!map.containsKey(entity.getId())) {
            throw new IllegalArgumentException("Entity with ID " + entity.getId() + " not found");
        }
        map.put(entity.getId(), entity);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                list.set(i, entity);
                break;
            }
        }
        saveToFile();
    }

    public synchronized boolean delete(String id) {
        if (id == null) return false;
        T removed = map.remove(id);
        if (removed != null) {
            list.removeIf(item -> item.getId().equals(id));
            saveToFile();
            return true;
        }
        return false;
    }

    public synchronized T getById(String id) {
        if (id == null) return null;
        return map.get(id); // O(1) HashMap lookup
    }

    public synchronized List<T> getAll() {
        return new ArrayList<>(list); // Return defensive copy of ArrayList
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized boolean exists(String id) {
        return map.containsKey(id);
    }

    /**
     * Factory parsing method to convert tokenized CSV into entity instance.
     */
    protected abstract T parseRecord(String[] tokens);
}
