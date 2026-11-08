package core.logic;

// immutable for some reason.

import java.time.LocalDate;

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
            return this.type.parse(rawValue);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Can not convert %s into %s", rawValue, this.type));
        }
    }

    public boolean validate(String rawValue) {
        if (rawValue == null) return false;
        try {
            this.type.parse(rawValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean accepts(Object value) { // validator
        if (value == null) return true;
        switch (this.type) {
            case INT:     return value instanceof Integer;
            case FLOAT:   return value instanceof Double;
            case STRING:  return value instanceof String;
            case DATE:    return value instanceof LocalDate;
            case BOOLEAN: return value instanceof Boolean;
            default: throw new IllegalStateException("Unknown type: " + this.type);
        }
    }
}
