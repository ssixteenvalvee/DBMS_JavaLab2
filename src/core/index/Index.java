package core.index;
import core.logic.Row;

import java.util.ArrayList;

public interface Index {
    void put(Object key, Row row);
    void remove(Object key, Row toRemove);
    ArrayList<Row> get(Object key, Row toGet);
}
