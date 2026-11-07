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

    public String getPrimaryKeyColumnName() { // perhaps it is better to make it with Column c : scheme.getValues()
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
        // add edge-cases here later
        if (scheme.size() != toAdd.size()) {
            throw new IllegalArgumentException("[TABLE] Non-matching row");
        }
        for (Map.Entry<String, Column> entry : scheme.entrySet()) {
            String columName = entry.getKey();
            Column col = entry.getValue();
            Object value = toAdd.get(columName); // .get is written as a method for Row

            if (!col.validate(value.toString())) {
                throw new IllegalArgumentException();
            }
        }

        String pkColumnName = getPrimaryKeyColumnName(); // метод ищет колонку с isPrimaryKey=true
        if (pkColumnName != null) {
            Object newKeyValue = toAdd.get(pkColumnName);
            for (Row existingRow : rows) {
                if (newKeyValue != null && newKeyValue.equals(existingRow.get(pkColumnName))) {
                    throw new IllegalArgumentException("[TABLE] non-unique Primary Key: " + newKeyValue);
                }
            }
        } else {
            throw new IllegalArgumentException("column name is null");
        }

        rows.add(toAdd);
        for (Map.Entry<String, Index> entry : indexes.entrySet()) {
            String columnName = entry.getKey(); // gives Col's name. e.g. "price"
            Index tree = entry.getValue(); // gives TreeMap currently
            Object valueForIndex = toAdd.get(columnName); // Object valueForIndex = row.get("price") == 25000

            tree.put(valueForIndex, toAdd); // 25000 -> Row, where price is 25000
        }
    }
}
