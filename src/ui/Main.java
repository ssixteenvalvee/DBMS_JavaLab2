package ui;

import core.logic.Database;
import core.service.DatabaseEngine;
import parser.QueryParser;
import storage.JsonStorageManager;
import storage.StorageManager;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // системный look & feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                JsonStorageManager storage = new JsonStorageManager("data");
                Database db = storage.load();
                QueryParser parser = new QueryParser(db);
                DatabaseEngine engine = new DatabaseEngine(db, storage);

                Frame gui = new Frame();
                Controller controller = new Controller(db, parser, engine, storage, gui);
                gui.setController(controller);
                gui.setVisible(true);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Не удалось запустить: " + e.getMessage(),
                        "Fatal", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}