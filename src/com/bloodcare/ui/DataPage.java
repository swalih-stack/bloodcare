package com.bloodcare.ui;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * DataPage - a reusable "searchable table + action buttons" page.
 * The loader runs a DAO query (any method that returns the rows); every action button reloads the table
 * afterwards and calls afterChange so the bell and sidebar badges update too.
 */
public class DataPage extends BasePage {
    private final JTable table;
    private final Callable<List<Object[]>> loader;
    private final Runnable afterChange;
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public DataPage(Callable<List<Object[]>> loader, Runnable afterChange, String... columns) {
        super(new BorderLayout());
        this.loader = loader;
        this.afterChange = afterChange;
        table = Ui.table(columns);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JTextField search = Ui.field();
        search.setColumns(18);
        search.addCaretListener(e -> { // runs whenever the text changes
            String text = search.getText().trim();
            sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        });
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        searchBox.add(Ui.label("Search:"));
        searchBox.add(search);
        actions.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        top.add(searchBox, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(top, BorderLayout.NORTH);
        inner.add(Ui.scroll(table), BorderLayout.CENTER);
        add(Ui.card(inner), BorderLayout.CENTER);
    }

    /** Adds a button. When clicked: run the action, reload the table, refresh badges. */
    public void addAction(String label, Runnable action) {
        actions.add(Ui.button(label, e -> {
            action.run();
            refresh();
            afterChange.run();
        }));
    }

    /** Id (first column) of the selected row, or -1. */
    public int selectedId() {
        return Ui.selectedId(table);
    }

    @Override
    public void refresh() {
        try {
            Ui.setRows(table, loader.call());
        } catch (Exception e) {
            Ui.dbError(this, e);
        }
    }
}
