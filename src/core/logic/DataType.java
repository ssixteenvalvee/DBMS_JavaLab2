package core.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public enum DataType {
    INT(Integer.class, new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return Integer.compare((Integer) a, (Integer) b);
        }
    }) {
        @Override
        public Object parse(String value) {
            return Integer.parseInt(value.trim());
        }
    },
    FLOAT(Double.class, new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return Double.compare((Double) a, (Double) b);
        }
    }) { // double actually
        @Override
        public Object parse(String value) {
            return Double.parseDouble(value.trim());
        }
    },
    STRING(String.class, new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return ((String) a).compareTo((String) b);
        }
    }) {
        @Override
        public Object parse(String value) {
            if (value.startsWith("'") && value.endsWith("'")) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
    },
    DATE(LocalDate.class, new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return ((LocalDate) a).compareTo((LocalDate) b);
        }
    }) {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

        @Override
        public Object parse(String value) {
            String cleanValue = value.startsWith("'") && value.endsWith("'")
                    ? value.substring(1, value.length() - 1)
                    : value;
            return LocalDate.parse(cleanValue.trim(), formatter);
        }
    },
    BOOLEAN(Boolean.class, new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return ((Boolean) a).compareTo((Boolean) b);
        }
    }) {
        @Override
        public Object parse(String value) {
            String v = value.trim().toLowerCase();
            if (v.equals("true") || v.equals("false")) {
                return Boolean.parseBoolean(v);
            }
            throw new IllegalArgumentException("Invalid logical value: " + value);
        }
    };

    private final Class<?> javaClass;
    private final Comparator<Object> comparator;

    DataType(Class<?> javaClass, Comparator<Object> comparator) {
        this.javaClass = javaClass;
        this.comparator = comparator;
    }

    public Class<?> getJavaClass() {
        return javaClass;
    }
    public Comparator<Object> getComparator() {
        return this.comparator;
    }

    public abstract Object parse(String value);
}