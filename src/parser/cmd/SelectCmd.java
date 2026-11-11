package parser.cmd;

public class SelectCmd implements Command{
    private final String tName;

    public SelectCmd(String tName) {
        this.tName = tName;
    }

    @Override
    public String table() {
        return this.tName;
    }
}
