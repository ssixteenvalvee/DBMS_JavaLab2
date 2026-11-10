package core.logic;

import core.index.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Table {
    public final String tableName; // cars, vegetables etc.
    private final LinkedHashMap<String, Column> scheme;
    private final ArrayList<Row> rows;
    private final LinkedHashMap<String, Index> indexes; // <Column.getColName, TreeIndex>
    private int nextId;

    public static final String PK_COL = "id";

    public Table(String name, LinkedHashMap<String, Column> scheme) {
        this.tableName = name;
        this.scheme = scheme;
        validatePk();
        this.rows = new ArrayList<>();
        this.indexes = new LinkedHashMap<>();
        this.nextId = 1;
    }

    public Table(String name, LinkedHashMap<String, Column> hm, ArrayList<Row> rows, LinkedHashMap<String, Index> indecies, int nextId) {
        this.tableName = name;
        this.scheme = hm;
        validatePk();
        this.rows = rows;
        this.indexes = indecies;
        int maxId = rows.stream().mapToInt(r -> (Integer) r.getRow().get(PK_COL)).max().orElse(0);
        this.nextId = Math.max(nextId, maxId + 1);
    }

    private void validatePk() {
        Column idCol = scheme.get(PK_COL);
        if (idCol == null)
            throw new IllegalArgumentException("Scheme must contain '" + PK_COL + "'");
        if (idCol.getType() != DataType.INT)
            throw new IllegalArgumentException("'" + PK_COL + "' must be INT");
        if (!idCol.isPk())
            throw new IllegalArgumentException("'" + PK_COL + "' must be marked PRIMARY KEY");
    }

    public String getPrimaryKeyColumnName() {
        return PK_COL;
    }

    public void addCol(String key, Column col) {
        this.scheme.put(key, col);
    }

    public void removeCol(String key) {
        this.scheme.remove(key);
    }

    public LinkedHashMap<String, Column> getScheme() {
        return this.scheme;
    }

    public ArrayList<Row> getRows() {
        return this.rows;
    }

    public String getName() {
        return this.tableName;
    }

    public void createIndex(String colName) {
        Column col = scheme.get(colName);
        if (col == null) {
            throw new IllegalArgumentException("No such column: " + colName);
        }
        if (indexes.containsKey(colName)) {
            throw new IllegalArgumentException("Index already exists: " + colName);
        }
        
        Index tree = new TreeIndex(col.getType().getComparator());
        for (Row r : rows) {
            Object v = r.getRow().get(colName);
            if (v != null) tree.put(v, r);
        }
        indexes.put(colName, tree);
    }

    public void addRow(Row toAdd) {
        String pk = PK_COL;
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
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        for (String col : scheme.keySet()) {
            normalized.put(col, data.getOrDefault(col, null));
        }
        toAdd.setrow(normalized);
        data = normalized;

        // type check; null == "not given"
        for (Map.Entry<String, Column> e : scheme.entrySet()) {
            Object v = data.get(e.getKey());
            if (v == null) continue;
            if (!e.getValue().accepts(v))
                throw new IllegalArgumentException("Unacceptable argument is given: " + e.getValue());
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
        String pk = PK_COL;
        for (Row r : rows) {
            if (key.equals(r.getRow().get(pk))) return true;
        }
        return false;
    }

    /*
    deletes certain row from the table and updates the idx
    toRemove is got by the link. The only responsibility is to remove the certain row and update info
    return true if deleted (for safety)
     */
    public boolean removeRow(Row toRemove) {
        int id = -1;
        for (int i = 0; i < this.rows.size(); i++) {
            if (this.rows.get(i) == toRemove) {
                id = i;
                break;
            }
        }
        if (id < 0) {
            id = rows.indexOf(toRemove);
        }
        if (id < 0) {
            return false;
        }

        Row toDelete = rows.remove(id); // no longer in the rows, but still in indexes

        for (Map.Entry<String, Index> entry : indexes.entrySet()) { // Index == TreeMap rn
            /*
            toDelete is a row -> we get the row -> the row is LinkedHashMap<String, Object>
            String is the name if FIELD/COL (id, carName, price etc,)
            We GET the value of COLUMN with GET(COLUMN NAME), where entry.getKey() is the COLUMN NAME.

            e.g.
            Row = {id:3, brand:Audi, price:25000};
            e.getKey() = 'price'
            toDelete.get(e.getKey()) = 25000;
            ( toDelete.getRow().get(e,getKey()) )
             */
            Object key = toDelete.get(entry.getKey());
            if (key == null) continue; // null ignored
            try {
                entry.getValue().remove(key, toDelete);
            } catch (NoSuchElementException | IllegalArgumentException ex) {
                System.err.println("[TABLE] deletion problem with TreeMap: " + entry.getKey() + " key= " + key + ": " + ex.getMessage());
            }
        }
        return true;
    }

    public void updateRow(Row target, LinkedHashMap<String, Object> newValues) {
        // validate the newVal
        if (!rows.contains(target)) {
            throw new IllegalArgumentException("Target does not exist in this table");
        }
        if (newValues == null) {
            throw new IllegalArgumentException("Did not get the values to update");
        }
        if (newValues.isEmpty()) {
            return;
        }

        for (String col : newValues.keySet()) {
            if (!scheme.containsKey(col)) {
                throw new IllegalArgumentException("Unknown column: " + col);
            }
        }

        for (Map.Entry<String, Object> entry : newValues.entrySet()) { // key - FIELD, value - real value
            Object val = entry.getValue();
            if (val == null) {
                continue;
            }
            Column col = scheme.get(entry.getKey());
            if (!col.accepts(val)) {
                throw new IllegalArgumentException("Wrong type in column " + entry.getKey());
            }
        }

        if (newValues.containsKey(PK_COL) && newValues.get(PK_COL) != null) {
            throw new IllegalArgumentException("ID is immutable");
        }

        // update
        LinkedHashMap<String, Object> oldIndexedValues = new LinkedHashMap<>();
        for (String col : newValues.keySet()) {
            if (indexes.containsKey(col)) {
                oldIndexedValues.put(col, target.getRow().get(col));
            }
        }
        for (Map.Entry<String, Object> entry : oldIndexedValues.entrySet()) {
            if (entry.getValue() != null) {
                indexes.get(entry.getKey()).remove(entry.getValue(), target);
            }
        }
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            if (entry.getValue() != null) {
                target.getRow().put(entry.getKey(), entry.getValue());
            }
        }
        for (String col : oldIndexedValues.keySet()) {
            Object newVal = target.getRow().get(col);
            if (newVal != null) {
                indexes.get(col).put(newVal, target);
            }
        }
    }
}
