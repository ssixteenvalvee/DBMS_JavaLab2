package parser.cmd;

import core.logic.Column;

import java.util.LinkedHashMap;

public class CreateTableCmd implements Command {
    private final String tableName;
    private final LinkedHashMap<String, Column> scheme;

    public CreateTableCmd(String tableName, LinkedHashMap<String, Column> scheme) {
        this.tableName = tableName;
        this.scheme = scheme;
    }

    @Override
    public String table() {
        return tableName;
    }

    public LinkedHashMap<String, Column> getScheme() {
        return scheme;
    }
}
