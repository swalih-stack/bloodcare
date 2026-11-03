package com.bloodcare.ui;

import com.bloodcare.db.MessageDao;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Administrator inbox and conversation screen. Selecting an inbox row opens that user's complete
 * Contact Admin thread on the right; there is no separate Reply action or message preview column.
 */
public class AdminConversationPage extends BasePage {
    private final Runnable afterChange;
    private final JTable conversations = Ui.table("ID", "From", "Email");
    private final JLabel title = new JLabel("Select a conversation");
    private final JTextArea history = new JTextArea();
    private final JTextField input = Ui.field();
    private final JButton send = Ui.button("Send", e -> sendMessage());
    private boolean loading;

    public AdminConversationPage(Runnable afterChange) {
        super(new BorderLayout(18, 0));
        this.afterChange = afterChange;

        RoundPanel inbox = Ui.card(Ui.section("Conversations", Ui.scroll(conversations)));
        inbox.setPreferredSize(new Dimension(420, 0));

        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        history.setFont(Ui.BODY);
        history.setMargin(new Insets(10, 12, 10, 12));
        input.addActionListener(e -> sendMessage());

        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        composer.add(input, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);

        JPanel thread = new JPanel(new BorderLayout(0, 10));
        thread.setOpaque(false);
        thread.add(title, BorderLayout.NORTH);
        JScrollPane threadScroll = new JScrollPane(history);
        threadScroll.setBorder(BorderFactory.createLineBorder(Ui.CARD_BORDER));
        thread.add(threadScroll, BorderLayout.CENTER);
        thread.add(composer, BorderLayout.SOUTH);

        add(inbox, BorderLayout.WEST);
        add(Ui.card(thread), BorderLayout.CENTER);

        conversations.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loading) loadHistory();
        });
        new Timer(8000, e -> { if (isShowing()) refresh(); }).start();
        showConversation(false);
    }

    @Override
    public void refresh() {
        int keep = Ui.selectedId(conversations);
        loading = true;
        try {
            List<Object[]> rows = MessageDao.adminConversations();
            Ui.setRows(conversations, rows);
            for (int i = 0; i < rows.size(); i++) {
                if ((Integer) rows.get(i)[0] == keep) conversations.setRowSelectionInterval(i, i);
            }
            if (conversations.getSelectedRow() < 0 && !rows.isEmpty()) conversations.setRowSelectionInterval(0, 0);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        } finally {
            loading = false;
        }
        loadHistory();
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
        int userId = Ui.selectedId(conversations);
        if (userId < 0) {
            showConversation(false);
            return;
        }
        showConversation(true);
        int row = conversations.getSelectedRow();
        title.setText("Conversation with " + conversations.getValueAt(row, 1)
                + "  ·  " + conversations.getValueAt(row, 2));
        try {
            MessageDao.markConversationRead(userId);
            StringBuilder text = new StringBuilder();
            for (Object[] message : MessageDao.conversationForAdmin(userId)) {
                text.append(message[0]).append("   ").append(message[2]).append('\n')
                        .append(message[1]).append("\n\n");
            }
            history.setText(text.length() == 0 ? "No messages yet." : text.toString());
            history.setCaretPosition(history.getDocument().getLength());
            afterChange.run();
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void sendMessage() {
        int userId = Ui.selectedId(conversations);
        if (userId < 0) return;
        try {
            String problem = MessageDao.sendAdminReply(userId, input.getText());
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
