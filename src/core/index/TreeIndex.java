package core.index;

import core.logic.Row;

import java.util.*;

/*
TreeMap это дерево, а поиск по дереву(если оно балансируется, а тут балансируется) O(log n) -> все круто
 */

public class TreeIndex implements Index {
    public TreeMap<Object, ArrayList<Row>> tree = new TreeMap<>();

    public TreeIndex(Comparator<Object> comparator) {
        this.tree = new TreeMap<>(comparator);
    }

    /**
     */
    @Override
    public void put(Object key, Row row) {
        if (!this.tree.containsKey(key)) {
            ArrayList<Row> rows = new ArrayList<>();
            rows.add(row);
            this.tree.put(key, rows);
            return;
        }
        this.tree.get(key).add(row);
    }

    /**
     */
    @Override
    public void remove(Object key, Row toRemove) {
        ArrayList<Row> result = tree.getOrDefault(key, null);
        if (result == null) {
            throw new IllegalArgumentException("[TREEIDX] Exception: No such key: " + key);
        }
        if (!result.remove(toRemove)) {
            throw new NoSuchElementException("[TREEIDX] Exception: Nothing to remove in the row with: " + toRemove);
        }
        if (result.isEmpty()) {
            tree.remove(key);
        }
    }

    /**
     */
    @Override
    public Row get(Object key, Row toGet) {
        ArrayList<Row> result = tree.getOrDefault(key, null);
        if (result == null) {
            throw new IllegalArgumentException("[TREEIDX] Exception: No such key: " + key);
        }

        int index = result.indexOf(toGet);

        if (index == -1) {
            throw new NoSuchElementException("Nothing to get in the row with: " + toGet);
        }

        return result.get(index);
    }

}
