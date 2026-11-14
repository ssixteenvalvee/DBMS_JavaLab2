package storage;

import core.logic.Database;
import core.logic.Table;

import java.io.IOException;

public interface StorageManager {

    void saveDatabase(Database db) throws IOException;

    Database loadDatabase() throws IOException;

    void saveTable(Table table) throws IOException;

    Table loadTable(String tableName) throws IOException;

    void deleteTableFile(String tableName) throws IOException;

    default void save(Database db) throws IOException {
        saveDatabase(db);
    }

    default Database load() throws IOException {
        return loadDatabase();
    }
}
