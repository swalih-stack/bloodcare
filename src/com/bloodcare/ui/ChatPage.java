package com.bloodcare.ui;

import com.bloodcare.db.ChatDao;
import com.bloodcare.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * ChatPage - Conversations. Left: the conversations the signed-in user takes part in (as the requester or
 * as an accepted donor). Right: the messages of the selected one, and a box to reply. Contact details are
 * never shown, people talk through the platform. The open conversation reloads every 8 seconds.
 */
public class ChatPage extends BasePage {
    private final User user;
    private final Runnable afterChange;
    private final JTable table = Ui.table("ID", "Request", "With", "Patient", "Group", "Status", "Unread");
    private final JLabel title = new JLabel("Select a conversation");
    private final JTextArea history = new JTextArea();
    private final JTextField input = Ui.field();
    private final JButton send = Ui.button("Send", e -> sendMessage());
    private boolean loading;

    public ChatPage(User user, Runnable afterChange) {
        super(new BorderLayout(18, 0));
        this.user = user;
        this.afterChange = afterChange;

        RoundPanel leftCard = Ui.card(Ui.section("My conversations", Ui.scroll(table)));
        leftCard.setPreferredSize(new Dimension(480, 0));

        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        history.setFont(Ui.BODY);
        history.setMargin(new Insets(10, 12, 10, 12));
        input.addActionListener(e -> sendMessage());
        JPanel box = new JPanel(new BorderLayout(8, 0));
        box.setOpaque(false);
        box.add(input, BorderLayout.CENTER);
        box.add(send, BorderLayout.EAST);
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.add(title, BorderLayout.NORTH);
        JScrollPane historyScroll = new JScrollPane(history);
        historyScroll.setBorder(BorderFactory.createLineBorder(Ui.CARD_BORDER));
        right.add(historyScroll, BorderLayout.CENTER);
        right.add(box, BorderLayout.SOUTH);

        add(leftCard, BorderLayout.WEST);
        add(Ui.card(right), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loading) {
                loadHistory();
                afterChange.run();
            }
        });
        new Timer(8000, e -> {
            if (isShowing()) refresh();
        }).start();
        showConversation(false);
    }

    @Override
    public void refresh() {
        int keep = Ui.selectedId(table);
        loading = true;
        try {
            List<Object[]> rows = ChatDao.conversations(user.getId());
            Ui.setRows(table, rows);
            for (int i = 0; i < rows.size(); i++) {
                if ((Integer) rows.get(i)[0] == keep) table.setRowSelectionInterval(i, i);
            }
            // nothing selected yet: open the first conversation so the chat is not an empty pane
            if (table.getSelectedRow() < 0 && !rows.isEmpty()) table.setRowSelectionInterval(0, 0);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        } finally {
            loading = false;
        }
        loadHistory();
        afterChange.run();
    }

    private void showConversation(boolean open) {
        input.setEnabled(open);
        send.setEnabled(open);
        if (!open) {
            title.setText("Select a conversation");
            history.setText("");
        }
    }

    private void loadHistory() {
        int id = Ui.selectedId(table);
        if (id < 0) {
            showConversation(false);
            return;
        }
        showConversation(true);
        int row = table.getSelectedRow();
        title.setText("Request #" + table.getValueAt(row, 1) + "  ·  " + table.getValueAt(row, 3)
                + " (" + table.getValueAt(row, 4) + ")  ·  with " + table.getValueAt(row, 2));
        try {
            StringBuilder sb = new StringBuilder();
            for (Object[] m : ChatDao.history(id, user.getId())) {
                sb.append((Boolean) m[1] ? "You" : m[0]).append("   ").append(m[3]).append('\n')
                        .append(m[2]).append("\n\n");
            }
            if (sb.length() == 0) sb.append("No messages yet. Say hello!");
            history.setText(sb.toString());
            history.setCaretPosition(history.getDocument().getLength());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void sendMessage() {
        int id = Ui.selectedId(table);
        if (id < 0) return;
        try {
            String problem = ChatDao.send(id, user.getId(), input.getText().trim());
            if (problem != null) {
                Ui.error(this, problem);
                return;
            }
            input.setText("");
            loadHistory();
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
