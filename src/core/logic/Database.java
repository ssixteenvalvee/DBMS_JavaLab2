package core.logic;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

public class Database {
    private final LinkedHashMap<String, Table> tables = new LinkedHashMap<>();

    public void CreateTable(Table t) {
        this.tables.put(t.tableName, t);
        System.out.println("[DB] CREATE TABLE OK.");
    }

    public Table getTable(String tName) {
        if (!hasTable(tName)) {
            throw new NoSuchElementException("No such table with this name");
        } else {
            return this.tables.get(tName);
        }
    }

    public boolean hasTable(String tName) {
        return this.tables.containsKey(tName);
    }

    public boolean dropTable(String tName) {
        if (hasTable(tName)) {
            this.tables.remove(tName);
            System.out.println("[DB] Drop OK.");
            return true;
        }
        System.out.println("[DB] Drop FALSE.");
        return false;
    }

    public Collection<Table> getAllTables() {
        return this.tables.values();
    }
}
