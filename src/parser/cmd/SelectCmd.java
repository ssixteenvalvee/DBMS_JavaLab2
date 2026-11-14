package parser.cmd;

import java.util.ArrayList;

public class SelectCmd implements Command{
    private final String tableName;
    private final ArrayList<String> columns;
    private final String whereCol;
    private final String whereOp;
    private final Object whereVal;

    public SelectCmd(String tableName, ArrayList<String> columns, String whereCol, String whereOp, Object whereVal) {
        this.tableName = tableName;
        this.columns = columns;
        this.whereCol = whereCol;
        this.whereOp = whereOp;
        this.whereVal = whereVal;
    }
    @Override
    public String table() {
        return tableName;
    }
    public ArrayList<String> getColumns() { return columns; }
    public String getWhereCol() { return whereCol; }
    public String getWhereOp() { return whereOp; }
    public Object getWhereVal() { return whereVal; }
}
