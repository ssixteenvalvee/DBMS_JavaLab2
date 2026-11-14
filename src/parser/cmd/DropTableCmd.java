package parser.cmd;

public class DropTableCmd implements Command {
    private final String tableName;

    public DropTableCmd(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String table() {
        return tableName;
    }
}
