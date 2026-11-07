package core.index;
import core.logic.Row;

public interface Index {
    void put(Object key, Row row);
    void remove(Object key, Row toRemove);
    Row get(Object key, Row toGet);
}
