package com.bloodcare.ui;

import com.bloodcare.db.MessageDao;
import com.bloodcare.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Contact Admin: a single conversation thread with the administrator. */
public class ContactPage extends BasePage {
    private final User user;
    private final JTextArea history = new JTextArea();
    private final JTextField input = Ui.field();
    private final JButton send = Ui.button("Send", e -> send());

    public ContactPage(User user) {
        super(new BorderLayout(0, 10));
        this.user = user;
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        history.setFont(Ui.BODY);
        history.setMargin(new Insets(10, 12, 10, 12));
        input.addActionListener(e -> send());

        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        composer.add(input, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);

        JPanel thread = new JPanel(new BorderLayout(0, 10));
        thread.setOpaque(false);
        thread.add(Ui.label("Conversation with Administrator"), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(history);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.CARD_BORDER));
        thread.add(scroll, BorderLayout.CENTER);
        thread.add(composer, BorderLayout.SOUTH);
        add(Ui.card(thread), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            StringBuilder text = new StringBuilder();
            for (Object[] message : MessageDao.forUser(user.getId())) {
                text.append(message[1]).append("   ").append(message[3]).append('\n')
                        .append(message[2]).append("\n\n");
            }
            history.setText(text.length() == 0 ? "No messages yet. Write to the administrator below." : text.toString());
            history.setCaretPosition(history.getDocument().getLength());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void send() {
        String body = input.getText().trim();
        if (body.isEmpty() || body.length() > MessageDao.MAX_LENGTH) {
            Ui.error(this, "Message must be between 1 and " + MessageDao.MAX_LENGTH + " characters.");
            return;
        }
        try {
            MessageDao.send(user.getId(), body);
            input.setText("");
            refresh();
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
