package parser.cmd;

import core.logic.Row;
import java.util.ArrayList;

public class InsertCmd implements Command {
    private final String tName;
    private final ArrayList<String> columns;
    private final Row values;

    InsertCmd(String n, ArrayList<String> cols, Row vals) {
        this.tName = n;
        this.columns = cols;
        this.values = vals;
    }

    public ArrayList<String> getColumns() {
        return this.columns;
    }
    public Row getValues() {
        return this.values;
    }

    @Override
    public String table() {
        return this.tName;
    }
}
