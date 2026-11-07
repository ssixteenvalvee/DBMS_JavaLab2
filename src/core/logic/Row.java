package core.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Row {
    private LinkedHashMap<String, Object> row = new LinkedHashMap<>();

    // constructor
    public Row() {
        this.row = new LinkedHashMap<>();
    }

    public Row(Map<String, Object> values) { // for loading from JSON
        this.row = new LinkedHashMap<>(values);
    }

    // -- getter & setter
    public LinkedHashMap<String, Object> getRow() {
        return row;
    }

    public void setrow(LinkedHashMap<String, Object> row) {
        this.row = row;
    }
    // --

    public Object get(String name) {
        if (this.row.containsKey(name)) {
            return row.get(name);
        } else {
            throw new NoSuchElementException("No such key in the Row");
        }
    }

    @Override
    public String toString() {
        return row.toString();
    }

    public int size() {
        return this.row.size();
    }

    @Override
    public boolean equals(Object r) {
        if (this == r) return true;
        if (r == null || getClass() != r.getClass()) return false;
        return Objects.equals(this.getRow(), ((Row) r).getRow());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.row);
    }
}
