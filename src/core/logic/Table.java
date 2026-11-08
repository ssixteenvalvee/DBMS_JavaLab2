package core.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import core.index.*;

public class Table {
    public final String tableName; // cars, vegetables etc.
    private final LinkedHashMap<String, Column> scheme;
    private final ArrayList<Row> rows;
    private final LinkedHashMap<String, Index> indexes; // <Column.getColName, TreeIndex>
    private int nextId;

    public Table(String name, LinkedHashMap<String, Column> scheme) {
        this.tableName = name;
        this.scheme = scheme;
        this.rows = new ArrayList<>();
        this.indexes = new LinkedHashMap<>();
        this.nextId = 1;
    }

    public Table(String name, LinkedHashMap<String, Column> hm, ArrayList<Row> rows, LinkedHashMap<String, Index> indecies, int nextId) {
        this.tableName = name;
        this.scheme = hm;
        this.rows = rows;
        this.indexes = indecies;
        this.nextId = nextId;
    }

    public String getPrimaryKeyColumnName() {
        for (Map.Entry<String, Column> entry : scheme.entrySet()) {
            if (entry.getValue().isPk()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addCol(String key, Column col) {
        this.scheme.put(key, col);
    }

    public void removeCol(String key) {
        this.scheme.remove(key);
    }

    public LinkedHashMap<String, Column> getScheme() {
        return scheme;
    }

    public void addRow(Row toAdd) {
        String pk = getPrimaryKeyColumnName();
        if (pk == null)
            throw new IllegalStateException("Table has no PK column");
        if (scheme.get(pk).getType() != DataType.INT)
            throw new IllegalStateException("Autoincrement requires INT PK: " + pk);

        // data — mutable map inside Row
        LinkedHashMap<String, Object> data = toAdd.getRow();

        // extra columns check
        for (String key : data.keySet()) {
            if (!scheme.containsKey(key))
                throw new IllegalArgumentException("Unknown column: " + key);
        }

        // if some columns are absent -> make them null
        for (String col : scheme.keySet()) {
            data.putIfAbsent(col, null);
        }

        // type check; null == "not given"
        for (Map.Entry<String, Column> e : scheme.entrySet()) {
            Object v = data.get(e.getKey());
            if (v == null) continue;
            if (!e.getValue().accepts(v))
                throw new IllegalArgumentException("Unacceptable argument is given: " + e.getValue());
            if (!e.getClass().isInstance(v))
                throw new IllegalArgumentException(
                        "Bad type in column '" + e.getKey() + "': expected "
                                + e.getValue().getType() + ", got " + v.getClass().getSimpleName());
        }

        // id++ or unique check. User can both give and not give their own PK.
        Object newKey = data.get(pk);
        if (newKey == null) { // if user didn't give
            while (pkInUse(nextId)) nextId++;
            newKey = nextId;
            data.put(pk, newKey); // update in row
            nextId++;
        } else {
            int asInt = (Integer) newKey;
            if (asInt < 1)
                throw new IllegalArgumentException("PK must be >= 1, got: " + asInt);
            if (pkInUse(asInt))
                throw new IllegalArgumentException("PK duplicate: " + asInt);
            if (asInt >= nextId) nextId = asInt + 1;
        }
        rows.add(toAdd);

        // update idx only for not-null values
        // (TreeMap w/ natural ordering throws NPE on null-key)
        for (Map.Entry<String, Index> entry : indexes.entrySet()) {
            Object valueForIndex = data.get(entry.getKey());
            if (valueForIndex != null)
                entry.getValue().put(valueForIndex, toAdd);
        }
    }

    private boolean pkInUse(Object key) {
        String pk = getPrimaryKeyColumnName();
        for (Row r : rows) {
            if (key.equals(r.getRow().get(pk))) return true;
        }
        return false;
    }
}
