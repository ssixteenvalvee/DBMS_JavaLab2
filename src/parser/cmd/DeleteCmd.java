package parser.cmd;

import core.logic.Row;

import java.util.concurrent.locks.Condition;

public class DeleteCmd implements Command {
    private final String tName;
    private final String whereCol;
    private final String whereOp;
    private final Object whereVal;

    public DeleteCmd(String tableName, String whereCol, String whereOp, Object whereVal) {
        this.tName = tableName;
        this.whereCol = whereCol;
        this.whereOp = whereOp;
        this.whereVal = whereVal;
    }

    @Override
    public String table() {
        return tName;
    }
    public String getWhereCol() { return whereCol; }
    public String getWhereOp() { return whereOp; }
    public Object getWhereVal() { return whereVal; }

    public boolean hasWhere() {
        return whereCol != null;
    }
}
