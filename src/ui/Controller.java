package ui;

import core.logic.Database;
import core.service.DatabaseEngine;
import core.service.QResult;
import parser.QueryParseException;
import parser.QueryParser;
import parser.cmd.*;
import storage.JsonStorageManager;
import storage.StorageManager;

import java.io.IOException;
import java.util.ArrayList;

public class Controller {
    private final Database db;
    private final QueryParser parser;
    private final DatabaseEngine engine;
    private final JsonStorageManager storage;
    private final Frame gui;

    public Controller(Database db, QueryParser parser, DatabaseEngine engine, JsonStorageManager storage, Frame view) {
        this.db = db;
        this.parser = parser;
        this.engine = engine;
        this.storage = storage;
        this.gui = view;
    }

    public QResult dispatch(Command cmd) {
        if (cmd instanceof InsertCmd)      return engine.insert((InsertCmd) cmd);
        if (cmd instanceof SelectCmd)      return engine.select((SelectCmd) cmd);
        if (cmd instanceof UpdateCmd)      return engine.update((UpdateCmd) cmd);
        if (cmd instanceof DeleteCmd)      return engine.delete((DeleteCmd) cmd);
        if (cmd instanceof CreateTableCmd) return engine.createTable((CreateTableCmd) cmd);
        if (cmd instanceof DropTableCmd)   return engine.dropTable((DropTableCmd) cmd);
        if (cmd instanceof CreateIndexCmd) return engine.createIndex((CreateIndexCmd) cmd);
        throw new IllegalArgumentException("Unknown command: " + cmd.getClass());
    }

    public void submit(String sql) {
        if (sql.trim().isEmpty()) {
            gui.showMsg("Запрос пуст.");
        }
        try {
            ArrayList<Command> cmds = parser.parse(sql);
            QResult qr = null;
            for (Command cmd : cmds) {
                qr = dispatch(cmd);
            }

            storage.save(db); // !!!
            if (qr != null) {
                if (qr.getSucc()) {
                    if (qr.getRowsQr() != null) {
                        gui.showTable(qr);
                    } else {
                        gui.showMsg(qr.getMsg());
                    }
                } else {
                    gui.showError(qr.getMsg());
                }
            }
        } catch (IOException e) {
            gui.showError(e.getMessage());
            System.err.println("[CONTROLLER] Error: " + e.getMessage());
        } catch (QueryParseException e) {
            gui.showError(e.getMessage());
        } catch (IllegalArgumentException e) {
            gui.showError("while executing: " + e.getMessage());
        }
    }
}
