package org.rescuedrone.persistence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable, thread-safe CSV File Manager.
 * Handles file reading, atomic writing, missing file creation, and CSV quote parsing.
 */
public class FileManager {

    /**
     * Reads all non-empty lines from a file.
     * If the file does not exist, creates it and returns an empty list.
     */
    public static synchronized List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            ensureParentDirectoryExists(file);
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("[FileManager] Warning: Could not create file " + filePath + ": " + e.getMessage());
            }
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[FileManager] Error reading file " + filePath + ": " + e.getMessage());
        }

        return lines;
    }

    /**
     * Atomically writes a list of records to a CSV file.
     */
    public static synchronized boolean writeLines(String filePath, List<String> lines) {
        File file = new File(filePath);
        ensureParentDirectoryExists(file);

        File tempFile = new File(filePath + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[FileManager] Error writing temporary file " + tempFile.getPath() + ": " + e.getMessage());
            return false;
        }

        // Atomic replace
        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("[FileManager] Warning: Could not delete existing file for atomic swap: " + filePath);
            }
        }

        boolean renamed = tempFile.renameTo(file);
        if (!renamed) {
            System.err.println("[FileManager] Error renaming temp file to " + filePath);
            return false;
        }

        return true;
    }

    /**
     * Appends a single record line to a file.
     */
    public static synchronized boolean appendLine(String filePath, String line) {
        File file = new File(filePath);
        ensureParentDirectoryExists(file);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("[FileManager] Error appending to " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Splits a CSV line into tokens, respecting double-quoted values containing commas.
     */
    public static String[] parseCsvLine(String line) {
        if (line == null) return new String[0];
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());

        return tokens.toArray(new String[0]);
    }

    private static void ensureParentDirectoryExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
