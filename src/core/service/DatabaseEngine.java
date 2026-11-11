package core.service;

import core.logic.Column;
import core.logic.Database;
import core.logic.Row;
import core.logic.Table;
import parser.cmd.Command;
import parser.cmd.DeleteCmd;
import parser.cmd.InsertCmd;
import storage.StorageManeger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;

public class DatabaseEngine {
    private Database db;
    private StorageManeger storage;

    DatabaseEngine(Database database, StorageManeger st) {
        this.db = database;
        this.storage = st;
    }

    public void update() {

    }

    public void insert(InsertCmd cmd) {
        String tableName = cmd.table();
        ArrayList<String> columns = cmd.getColumns();
        Row values = cmd.getValues();

        Table t = this.db.getTable(tableName);
        LinkedHashMap<String, Column> scheme = t.getScheme();
        ArrayList<String> cols = new ArrayList<>(scheme.keySet());

        for (String colStr : cols) {
            if (!scheme.containsKey(colStr)) {
                throw new NoSuchElementException("");
            }
        }
        if (values.getRow().size() != cols.size()) {
            throw new IllegalArgumentException("Row and col sizes are not mathcing");
        }

        Row r = new Row();
        for (String colName : cols) {
            Column col = scheme.get(colName);
            Object raw = values.get(colName);
            if (raw == null) {
                r.getRow().put(colName, null);
                continue;
            }
            Object typeAccepted;
            if (col.accepts(raw)) {
                typeAccepted = raw;
            } else {
                typeAccepted = col.convertValidate(raw.toString());
            }
            r.getRow().put(colName, typeAccepted);
        }
        t.addRow(r);;
        storage.save(this.db);
        System.out.println("insert ok");
    }

    public void delete(DeleteCmd cmd) {
        try {
            boolean noWhere = (cmd.getWhereCol() == null);
            boolean noOp    = (cmd.getWhereOp() == null);
            boolean noVal   = (cmd.getWhereVal() == null);
            if (noWhere != noOp || noWhere != noVal) {
                throw new IllegalArgumentException("All null or all set");
            }

            Table t = db.getTable(cmd.getTableName());
            ArrayList<Row> targets = resolveTargets(t, cmd.getWhereCol(), cmd.getWhereOp(), cmd.getWhereVal());
            ArrayList<Row> toDelete = new ArrayList<>(targets);
            int deleted = 0;
            for (Row r : toDelete) {
                if (t.removeRow(r)) {
                    deleted++;
                }
            }
            storage.save(db);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Delete failed: " + e.getMessage());
        }
    }


    public Object select() {

        return null;
    }

    public void createTable() {

    }

    public void dropTable() {

    }

    private ArrayList<Row> resolveTargets(Table t, String whereCol, String whereOp, Object whereVal) {
        if (whereCol == null) {
            return new ArrayList<>(t.getRows());
        }
        if ("=".equals(whereOp) && t.hasIndex(whereCol)) { // o log n yay :P
            return t.getIndex(whereCol).get(whereVal);
        }
        ArrayList<Row> out = new ArrayList<>();
        for (Row r : t.getRows()) {
            if (matches(t, r, whereCol, whereOp, whereVal)) {
                out.add(r);
            }
        }
        return out;
    }

    private boolean matches(Table t, Row r, String col, String op, Object val) {
        Object v = r.getRow().get(col);
        if (v == null || val == null) return v == val;
        Column c = t.getScheme().get(col);
        int cmp = c.getType().getComparator().compare(v, val);
        switch (op) {
            case "=":  return cmp == 0;
            case "!=": return cmp != 0;
            case ">":  return cmp > 0;
            case "<":  return cmp < 0;
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
            default: throw new IllegalArgumentException("Unknown operation: " + op);
        }
    }
}

