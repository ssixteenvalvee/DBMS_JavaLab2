package core.logic;

import java.util.LinkedHashMap;
import java.util.Map;

public class Row {
    private LinkedHashMap<String, Object> rowValues = new LinkedHashMap<>();

    // constructor
    public Row() {
        this.rowValues = new LinkedHashMap<>();
    }

    public Row(Map<String, Object> values) { // for loading from JSON
        this.rowValues = new LinkedHashMap<>(values);
    }

    // -- getter & setter
    public LinkedHashMap<String, Object> getRowValues() {
        return rowValues;
    }

    public void setRowValues(LinkedHashMap<String, Object> rowValues) {
        this.rowValues = rowValues;
    }
    // --

    @Override
    public String toString() {
        return rowValues.toString();
    }
}
