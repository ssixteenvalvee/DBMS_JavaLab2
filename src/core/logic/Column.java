package core.logic;

// immutable for some reason.

public class Column {
    private final String colName; // carName, horsepower, available adn etc
    private final DataType type; // INT, FLOAT and etc.
    private final boolean isPrimaryKey;

    public Column(String colName, DataType type, boolean isPK) {
        this.colName = colName;
        this.type = type; // defines which data type can lay in this column
        this.isPrimaryKey = isPK;
    }
    // -- getters --
    public String getColName() {
        return this.colName;
    }

    public DataType getType() {
        return this.type;
    }

    public boolean isPk() {
        return this.isPrimaryKey;
    }
    // ---

    public Object convertValidate(String rawValue) {
        try {
            switch (type) {
                case INT:
                    return DataType.INT.parse(rawValue);
                case FLOAT:
                    return DataType.FLOAT.parse(rawValue);
                case STRING:
                    return DataType.STRING.parse(rawValue);
                case BOOLEAN:
                    return DataType.BOOLEAN.parse(rawValue);
                case DATE:
                    return DataType.DATE.parse(rawValue);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Can not convert %s into %s", rawValue, this.type));
        }
        return null;
    }
}
