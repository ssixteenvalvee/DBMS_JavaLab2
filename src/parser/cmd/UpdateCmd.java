package parser.cmd;

import java.util.LinkedHashMap;

public class UpdateCmd implements Command {
    private final String tName;
    private final LinkedHashMap<String, Object> setValues;
    private final String whereCol;
    private final String whereOp;
    private final Object whereVal;

    public UpdateCmd(String tableName, LinkedHashMap<String, Object> setValues, String whereCol, String whereOp, Object whereVal) {
        this.tName = tableName;
        this.setValues = setValues;
        this.whereCol = whereCol;
        this.whereOp = whereOp;
        this.whereVal = whereVal;
    }

    @Override
    public String table() {
        return tName;
    }
    public LinkedHashMap<String, Object> getSetValues() { return setValues; }
    public String getWhereCol() { return whereCol; }
    public String getWhereOp() { return whereOp; }
    public Object getWhereVal() { return whereVal; }
}

