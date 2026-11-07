package core.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public enum DataType {
    INT {
        @Override
        public Object parse(String value) {
            return Integer.parseInt(value.trim());
        }
    },
    FLOAT { // double actually
        @Override
        public Object parse(String value) {
            return Double.parseDouble(value.trim());
        }
    },
    STRING {
        @Override
        public Object parse(String value) {
            if (value.startsWith("'") && value.endsWith("'")) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
    },
    DATE {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

        @Override
        public Object parse(String value) {
            String cleanValue = value.startsWith("'") && value.endsWith("'")
                    ? value.substring(1, value.length() - 1)
                    : value;
            return LocalDate.parse(cleanValue.trim(), formatter);
        }
    },
    BOOLEAN {
        @Override
        public Object parse(String value) {
            String v = value.trim().toLowerCase();
            if (v.equals("true") || v.equals("false")) {
                return Boolean.parseBoolean(v);
            }
            throw new IllegalArgumentException("Invalid logical value: " + value);
        }
    };
    public abstract Object parse(String value);
}