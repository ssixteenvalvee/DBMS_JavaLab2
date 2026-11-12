package core.service;

import core.logic.Column;
import core.logic.Row;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class QResult {
    private final boolean success;
    private final String msg;

    // for select only
    private final ArrayList<Row> rows;
    private final LinkedHashMap<String, Column> scheme;

    public QResult(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
        //
        this.rows = null;
        this.scheme = null;
    }

    public QResult(boolean success, String msg, ArrayList<Row> rs, LinkedHashMap<String, Column> sch) {
        this.success = success;
        this.msg = msg;
        this.rows = rs;
        this.scheme = sch;
    }

    public boolean getSucc() {
        return this.success;
    }
    public String getMsg() {
        return this.msg;
    }
    public ArrayList<Row> getRowsQr() {
        return this.rows;
    }
    public LinkedHashMap<String, Column> getSchemeQr() {
        return this.scheme;
    }
}

