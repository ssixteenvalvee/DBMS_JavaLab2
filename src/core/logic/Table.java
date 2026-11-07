package core.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;

import core.index.*;

public class Table {
    public final String tableName; // cars, vegetables etc.
    private final LinkedHashMap<String, Column> scheme;
    private final ArrayList<Row> rows;
    private final LinkedHashMap<String, Index> idxs; // <Column.getColName, TreeIndex>
    private int nextId;

    public Table(String name) {
        this.tableName = name;
        this.scheme = new LinkedHashMap<>();
        this.rows = new ArrayList<>();
        this.idxs = new LinkedHashMap<>();
        this.nextId = 1;
    }

    public Table(String name, LinkedHashMap<String, Column> hm, ArrayList<Row> rows, LinkedHashMap<String, Index> indecies, int nextId) {
        this.tableName = name;
        this.scheme = hm;
        this.rows = rows;
        this.idxs = indecies;
        this.nextId = nextId;
    }
}
