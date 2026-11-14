package parser.cmd;

public class CreateIndexCmd implements Command {
    private final String indexName;
    private final String tableName;
    private final String column;

    public CreateIndexCmd(String indexName, String tableName, String column) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.column = column;
    }

    @Override
    public String table() {
        return tableName;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getColumn() {
        return column;
    }
}
