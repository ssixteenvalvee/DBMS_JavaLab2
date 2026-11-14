package ui;

import core.logic.Column;
import core.logic.Row;
import core.service.QResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Frame extends JFrame {

    private final JTextArea sqlInput;
    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private final JButton runButton;
    private final JButton clearButton;

    private Controller controller;

    public Frame() {
        super("Mini DBMS — AutoSalon");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        sqlInput = new JTextArea(6, 80);
        sqlInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sqlInput.setText("SELECT * FROM car_models;");
        JScrollPane sqlScroll = new JScrollPane(sqlInput);

        runButton = new JButton("Run (Ctrl+Enter)");
        clearButton = new JButton("Clear");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(runButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlScroll, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane tableScroll = new JScrollPane(resultTable);

        statusLabel = new JLabel(" Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        runButton.addActionListener(e -> run());
        clearButton.addActionListener(e -> {
            sqlInput.setText("");
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            statusLabel.setText(" Ready");
        });

        sqlInput.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
                "run");
        sqlInput.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { run(); }
        });
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    private void run() {
        if (controller == null) return;
        statusLabel.setText(" Выполняется...");
        controller.submit(sqlInput.getText());
    }


    public void showTable(QResult result) {
        LinkedHashMap<String, Column> scheme = result.getSchemeQr();
        ArrayList<Row> rows = result.getRowsQr();

        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        if (scheme == null || scheme.isEmpty()) {
            statusLabel.setText(" (нет колонок)");
            return;
        }

        String[] headers = scheme.keySet().toArray(new String[0]);
        for (String h : headers) tableModel.addColumn(h);

        if (rows != null) {
            for (Row r : rows) {
                Object[] cells = new Object[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    Object v = r.getRow().get(headers[i]);
                    cells[i] = (v == null) ? "NULL" : v.toString();
                }
                tableModel.addRow(cells);
            }
            statusLabel.setText(" " + rows.size() + " row(s)");
        } else {
            statusLabel.setText(" 0 rows");
        }
    }

    public void showMsg(String msg) {
        statusLabel.setText(" " + msg);
    }

    public void showError(String msg) {
        statusLabel.setText(" ERROR: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}