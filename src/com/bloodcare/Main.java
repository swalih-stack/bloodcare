package com.bloodcare;

import com.bloodcare.db.Db;
import com.bloodcare.ui.MainFrame;

import javax.swing.*;
import java.sql.SQLException;

/** Entry point: prepares the database, then opens the window. */
public class Main {
    public static void main(String[] args) {
        try {
            Db.init();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to MySQL.\n\n" + e.getMessage()
                            + "\n\nCheck that MySQL is running and db.properties has the right user/password."
                    );
            return;
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
