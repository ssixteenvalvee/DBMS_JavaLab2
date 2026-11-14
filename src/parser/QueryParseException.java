package parser;

public class QueryParseException extends IllegalArgumentException {
    private final int position;

    public QueryParseException(String message, int position) {
        super(message + " (позиция " + position + ")");
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
