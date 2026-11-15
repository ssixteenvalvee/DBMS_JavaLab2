package core.index;

import core.logic.Row;

import java.util.*;

/*
TreeMap это дерево, а поиск по дереву(если оно балансируется, а тут балансируется) O(log n) -> все круто
 */

public class TreeIndex implements Index {
    public TreeMap<Object, ArrayList<Row>> tree;
    private final HashMap<Object, ArrayList<Row>> lookup;

    public TreeIndex(Comparator<Object> comparator) {
        this.tree = new TreeMap<>(comparator);
        this.lookup = new HashMap<>();
    }

    /**
     */
    @Override
    public void put(Object key, Row row) {
        ArrayList<Row> rows = lookup.get(key);
        if (rows == null) {
            rows = new ArrayList<>();
            tree.put(key, rows);
            lookup.put(key, rows);
        }
        rows.add(row);
    }

    /**
     */
    @Override
    public void remove(Object key, Row toRemove) {
        ArrayList<Row> result = lookup.get(key);
        if (result == null) {
            throw new IllegalArgumentException("[TREEIDX] Exception: No such key: " + key);
        }
        if (!result.remove(toRemove)) {
            throw new NoSuchElementException("[TREEIDX] Exception: Nothing to remove in the row with: " + toRemove);
        }
        if (result.isEmpty()) {
            tree.remove(key);
            lookup.remove(key);
        }
    }

    /**
     *
     */
    @Override
    public ArrayList<Row> get(Object key) {
        ArrayList<Row> rows = lookup.get(key);
        if (rows == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(rows);
    }

    @Override
    public ArrayList<Row> getRange(Object key, String operator) {
        NavigableMap<Object, ArrayList<Row>> range;
        switch (operator) {
            case "<":  range = tree.headMap(key, false); break;
            case "<=": range = tree.headMap(key, true); break;
            case ">":  range = tree.tailMap(key, false); break;
            case ">=": range = tree.tailMap(key, true); break;
            default: throw new IllegalArgumentException("Unknown range operator: " + operator);
        }
        ArrayList<Row> result = new ArrayList<>();
        for (ArrayList<Row> rows : range.values()) {
            result.addAll(rows);
        }
        return result;
    }
}
