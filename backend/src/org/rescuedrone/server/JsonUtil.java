package org.rescuedrone.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Lightweight, robust JSON serializer and parser in pure Java.
 * Eliminates external dependency issues while preserving full control.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Enum<?>) {
            return "\"" + ((Enum<?>) obj).name() + "\"";
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeString(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : arr) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }

        // Generic object serialization via reflection
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Class<?> currentClass = obj.getClass();
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object val = field.get(obj);
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("\"").append(escapeString(field.getName())).append("\":");
                    sb.append(toJson(val));
                } catch (IllegalAccessException ignored) {
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Parses a flat or simple nested JSON object string into a Map.
     */
    public static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return map;

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) return map;

        int i = 0;
        int n = content.length();
        while (i < n) {
            while (i < n && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ',')) i++;
            if (i >= n) break;

            if (content.charAt(i) != '"') break;
            i++; // skip open quote
            int keyStart = i;
            while (i < n && content.charAt(i) != '"') {
                if (content.charAt(i) == '\\') i++;
                i++;
            }
            String key = content.substring(keyStart, i);
            if (i < n && content.charAt(i) == '"') i++; // skip close quote

            while (i < n && Character.isWhitespace(content.charAt(i))) i++;
            if (i < n && content.charAt(i) == ':') i++; // skip colon
            while (i < n && Character.isWhitespace(content.charAt(i))) i++;

            // Parse value
            if (i >= n) break;
            char firstValChar = content.charAt(i);

            if (firstValChar == '"') {
                i++; // skip open quote
                StringBuilder valSb = new StringBuilder();
                while (i < n && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                        char esc = content.charAt(i);
                        if (esc == 'n') valSb.append('\n');
                        else if (esc == 't') valSb.append('\t');
                        else if (esc == 'r') valSb.append('\r');
                        else valSb.append(esc);
                    } else {
                        valSb.append(content.charAt(i));
                    }
                    i++;
                }
                if (i < n && content.charAt(i) == '"') i++;
                map.put(key, valSb.toString());
            } else if (firstValChar == '{') {
                int braceDepth = 1;
                int start = i;
                i++;
                while (i < n && braceDepth > 0) {
                    if (content.charAt(i) == '{') braceDepth++;
                    else if (content.charAt(i) == '}') braceDepth--;
                    i++;
                }
                String subJson = content.substring(start, i);
                map.put(key, parseJsonObject(subJson));
            } else if (firstValChar == '[') {
                int bracketDepth = 1;
                int start = i;
                i++;
                while (i < n && bracketDepth > 0) {
                    if (content.charAt(i) == '[') bracketDepth++;
                    else if (content.charAt(i) == ']') bracketDepth--;
                    i++;
                }
                String subArr = content.substring(start, i);
                map.put(key, parseJsonArray(subArr));
            } else {
                // Number or boolean or null
                int start = i;
                while (i < n && content.charAt(i) != ',' && content.charAt(i) != '}') {
                    i++;
                }
                String rawVal = content.substring(start, i).trim();
                if ("true".equalsIgnoreCase(rawVal)) {
                    map.put(key, Boolean.TRUE);
                } else if ("false".equalsIgnoreCase(rawVal)) {
                    map.put(key, Boolean.FALSE);
                } else if ("null".equalsIgnoreCase(rawVal)) {
                    map.put(key, null);
                } else {
                    try {
                        if (rawVal.contains(".")) {
                            map.put(key, Double.parseDouble(rawVal));
                        } else {
                            map.put(key, Long.parseLong(rawVal));
                        }
                    } catch (NumberFormatException e) {
                        map.put(key, rawVal);
                    }
                }
            }
        }
        return map;
    }

    /**
     * Parses a simple JSON array of strings or numbers into a List.
     */
    public static List<Object> parseJsonArray(String json) {
        List<Object> list = new ArrayList<>();
        if (json == null) return list;
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return list;

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) return list;

        String[] parts = content.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.startsWith("\"") && p.endsWith("\"") && p.length() >= 2) {
                list.add(p.substring(1, p.length() - 1));
            } else if (!p.isEmpty()) {
                list.add(p);
            }
        }
        return list;
    }
}
