package storage;

import core.index.Index;
import core.index.TreeIndex;
import core.logic.Column;
import core.logic.DataType;
import core.logic.Database;
import core.logic.Row;
import core.logic.Table;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonStorageManager implements StorageManager {

    private final Path storageDir;

    public JsonStorageManager() {
        this("data");
    }

    public JsonStorageManager(String dirPath) {
        this.storageDir = Paths.get(dirPath);
        initStorageDirectory();
    }

    private void initStorageDirectory() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("[STORAGE] Cannot create directory: " + storageDir, e);
        }
    }

    @Override
    public void saveDatabase(Database db) throws IOException {
        if (db == null) {
            return;
        }
        for (Table table : db.getAllTables()) {
            saveTable(table);
        }
    }

    @Override
    public Database loadDatabase() throws IOException {
        Database db = new Database();
        if (!Files.exists(storageDir)) {
            return db;
        }

        recoverTempFiles();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "*.json")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String tableName = fileName.substring(0, fileName.length() - ".json".length());
                Table table = loadTable(tableName);
                if (table != null) {
                    db.CreateTable(table);
                }
            }
        }
        return db;
    }

    @Override
    public void saveTable(Table table) throws IOException {
        if (table == null) {
            return;
        }
        Path targetFile = storageDir.resolve(table.getName() + ".json");
        String jsonContent = serializeTable(table);
        writeAtomically(targetFile, jsonContent);
    }

    @Override
    public Table loadTable(String tableName) throws IOException {
        Path file = storageDir.resolve(tableName + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return deserializeTable(json);
    }

    @Override
    public void deleteTableFile(String tableName) throws IOException {
        Files.deleteIfExists(storageDir.resolve(tableName + ".json"));
        Files.deleteIfExists(storageDir.resolve(tableName + ".json.tmp"));
    }

    private void writeAtomically(Path target, String content) throws IOException {
        Path tmpFile = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
        try {
            Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void recoverTempFiles() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "*.tmp")) {
            for (Path tmp : stream) {
                String name = tmp.getFileName().toString();
                Path original = storageDir.resolve(name.substring(0, name.length() - ".tmp".length()));
                if (!Files.exists(original) && isValidTableJson(tmp)) {
                    Files.move(tmp, original, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(tmp);
                }
            }
        }
    }

    private boolean isValidTableJson(Path tmpFile) {
        try {
            if (Files.size(tmpFile) == 0) {
                return false;
            }
            String content = new String(Files.readAllBytes(tmpFile), StandardCharsets.UTF_8);
            Table table = deserializeTable(content);
            return table != null && table.getName() != null && !table.getName().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String serializeTable(Table table) {
        StringBuilder sb = new StringBuilder();
        int nextId = extractNextId(table);
        LinkedHashMap<String, Column> scheme = table.getScheme();
        ArrayList<Row> rows = table.getRows();
        LinkedHashMap<String, Index> indexes = extractIndexes(table);

        sb.append("{\n");
        sb.append("  \"tableName\": \"").append(escape(table.getName())).append("\",\n");
        sb.append("  \"nextId\": ").append(nextId).append(",\n");

        sb.append("  \"scheme\": [\n");
        int colIdx = 0;
        for (Column col : scheme.values()) {
            sb.append("    {")
                    .append("\"colName\": \"").append(escape(col.getColName())).append("\", ")
                    .append("\"type\": \"").append(col.getType().name()).append("\", ")
                    .append("\"isPK\": ").append(col.isPk())
                    .append("}");
            if (++colIdx < scheme.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"rows\": [\n");
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            LinkedHashMap<String, Object> rowData = r.getRow();
            sb.append("    {");
            int cellIdx = 0;
            for (Map.Entry<String, Column> colEntry : scheme.entrySet()) {
                String colName = colEntry.getKey();
                DataType type = colEntry.getValue().getType();
                Object val = rowData.get(colName);

                sb.append("\"").append(escape(colName)).append("\": ")
                        .append(formatJsonValue(val, type));
                if (++cellIdx < scheme.size()) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            if (i < rows.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"indexes\": [\n");
        int idxCounter = 0;
        for (Map.Entry<String, Index> idxEntry : indexes.entrySet()) {
            String indexedCol = idxEntry.getKey();
            Index indexObj = idxEntry.getValue();

            sb.append("    {\n");
            sb.append("      \"colName\": \"").append(escape(indexedCol)).append("\",\n");
            sb.append("      \"entries\": [\n");

            if (indexObj instanceof TreeIndex) {
                TreeIndex treeIndex = (TreeIndex) indexObj;
                int eIdx = 0;
                for (Map.Entry<Object, ArrayList<Row>> treeEntry : treeIndex.tree.entrySet()) {
                    Object key = treeEntry.getKey();
                    ArrayList<Row> indexedRows = treeEntry.getValue();

                    sb.append("        {\"key\": \"").append(escape(String.valueOf(key))).append("\", \"rowPositions\": [");
                    boolean firstPos = true;
                    for (int k = 0; k < indexedRows.size(); k++) {
                        int pos = findRowPosition(rows, indexedRows.get(k));
                        if (pos >= 0) {
                            if (!firstPos) {
                                sb.append(", ");
                            }
                            sb.append(pos);
                            firstPos = false;
                        }
                    }
                    sb.append("]}");
                    if (++eIdx < treeIndex.tree.size()) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
            }

            sb.append("      ]\n");
            sb.append("    }");
            if (++idxCounter < indexes.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private int findRowPosition(ArrayList<Row> rows, Row target) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == target) {
                return i;
            }
        }
        return rows.indexOf(target);
    }

    @SuppressWarnings("unchecked")
    private Table deserializeTable(String json) {
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("[STORAGE] Root of table JSON must be an object");
        }
        Map<String, Object> root = (Map<String, Object>) parsed;
        if (!root.containsKey("tableName")) {
            throw new IllegalArgumentException("[STORAGE] Missing tableName in JSON");
        }

        String tableName = (String) root.get("tableName");
        Number nextIdNum = (Number) root.get("nextId");
        int nextId = (nextIdNum != null) ? nextIdNum.intValue() : 1;

        LinkedHashMap<String, Column> scheme = new LinkedHashMap<>();
        List<Object> rawScheme = (List<Object>) root.get("scheme");
        if (rawScheme != null) {
            for (Object item : rawScheme) {
                Map<String, Object> colMap = (Map<String, Object>) item;
                String colName = (String) colMap.get("colName");
                DataType type = DataType.valueOf((String) colMap.get("type"));
                boolean isPK = Boolean.TRUE.equals(colMap.get("isPK"));

                Column col = new Column(colName, type, isPK);
                scheme.put(colName, col);
            }
        }

        ArrayList<Row> rows = new ArrayList<>();
        List<Object> rawRows = (List<Object>) root.get("rows");
        if (rawRows != null) {
            for (Object item : rawRows) {
                Map<String, Object> rawRowMap = (Map<String, Object>) item;
                LinkedHashMap<String, Object> typedValues = new LinkedHashMap<>();

                for (Map.Entry<String, Column> colEntry : scheme.entrySet()) {
                    String colName = colEntry.getKey();
                    Column col = colEntry.getValue();
                    Object rawVal = rawRowMap.get(colName);
                    typedValues.put(colName, convertValue(rawVal, col.getType()));
                }
                rows.add(new Row(typedValues));
            }
        }

        LinkedHashMap<String, Index> indexes = new LinkedHashMap<>();
        List<Object> rawIndexes = (List<Object>) root.get("indexes");
        if (rawIndexes != null) {
            for (Object item : rawIndexes) {
                Map<String, Object> idxMap = (Map<String, Object>) item;
                String colName = (String) idxMap.get("colName");
                Column col = scheme.get(colName);
                if (col == null) {
                    continue;
                }

                TreeIndex treeIndex = new TreeIndex(naturalComparator());
                List<Object> entries = (List<Object>) idxMap.get("entries");

                if (entries != null && !entries.isEmpty()) {
                    for (Object entryObj : entries) {
                        Map<String, Object> entryMap = (Map<String, Object>) entryObj;
                        String keyStr = (String) entryMap.get("key");
                        Object typedKey = convertValue(keyStr, col.getType());

                        List<Object> positions = (List<Object>) entryMap.get("rowPositions");
                        if (positions != null && typedKey != null) {
                            for (Object posObj : positions) {
                                int pos = ((Number) posObj).intValue();
                                if (pos >= 0 && pos < rows.size()) {
                                    treeIndex.put(typedKey, rows.get(pos));
                                }
                            }
                        }
                    }
                } else {
                    for (Row r : rows) {
                        Object val = r.getRow().get(colName);
                        if (val != null) {
                            treeIndex.put(val, r);
                        }
                    }
                }
                indexes.put(colName, treeIndex);
            }
        }

        return new Table(tableName, scheme, rows, indexes, nextId);
    }

    private Object convertValue(Object rawVal, DataType type) {
        if (rawVal == null) {
            return null;
        }
        if (type == DataType.STRING) {
            return rawVal.toString();
        }
        if (type == DataType.INT && rawVal instanceof Integer) {
            return rawVal;
        }
        if (type == DataType.FLOAT && rawVal instanceof Number) {
            return ((Number) rawVal).doubleValue();
        }
        if (type == DataType.BOOLEAN && rawVal instanceof Boolean) {
            return rawVal;
        }
        return type.parse(rawVal.toString());
    }

    private String formatJsonValue(Object val, DataType type) {
        if (val == null) {
            return "null";
        }
        switch (type) {
            case INT:
            case FLOAT:
            case BOOLEAN:
                return val.toString();
            case DATE:
            case STRING:
            default:
                return "\"" + escape(val.toString()) + "\"";
        }
    }

    @SuppressWarnings("unchecked")
    private Comparator<Object> naturalComparator() {
        return (a, b) -> ((Comparable<Object>) a).compareTo(b);
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Index> extractIndexes(Table table) {
        try {
            Field f = Table.class.getDeclaredField("indexes");
            f.setAccessible(true);
            return (LinkedHashMap<String, Index>) f.get(table);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("[STORAGE] Failed to access indexes field in Table", e);
        }
    }

    private int extractNextId(Table table) {
        try {
            Field f = Table.class.getDeclaredField("nextId");
            f.setAccessible(true);
            return f.getInt(table);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("[STORAGE] Failed to access nextId field in Table", e);
        }
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static class JsonParser {
        private final String src;
        private int pos = 0;

        private JsonParser(String src) {
            this.src = src;
        }

        static Object parse(String json) {
            if (json == null) {
                throw new IllegalArgumentException("JSON string is null");
            }
            JsonParser parser = new JsonParser(json);
            Object result = parser.parseValue();
            parser.skipWhitespace();
            if (parser.pos != parser.src.length()) {
                throw new IllegalArgumentException("Unexpected trailing characters in JSON at position " + parser.pos);
            }
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if ((c >= '0' && c <= '9') || c == '-') return parseNumber();
            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos >= src.length() || src.charAt(pos) != ':') {
                    throw new IllegalArgumentException("Expected ':' at position " + pos);
                }
                pos++;
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    return map;
                } else {
                    throw new IllegalArgumentException("Malformed JSON object at position " + pos);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON object");
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    return list;
                } else {
                    throw new IllegalArgumentException("Malformed JSON array at position " + pos);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON array");
        }

        private String parseString() {
            if (pos >= src.length() || src.charAt(pos) != '"') {
                throw new IllegalArgumentException("Expected '\"' at position " + pos);
            }
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) {
                        throw new IllegalArgumentException("Unterminated escape sequence");
                    }
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > src.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape");
                            }
                            String hex = src.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default:
                            sb.append(esc);
                            break;
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at position " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null at position " + pos);
        }

        private Number parseNumber() {
            int start = pos;
            boolean isFloat = false;

            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true;
                }
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }

            String num = src.substring(start, pos);
            if (isFloat) {
                return Double.valueOf(num);
            }
            return Integer.valueOf(num);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}