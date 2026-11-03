package com.bloodcare.ui;

import com.bloodcare.db.UserDao;
import com.bloodcare.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Profile & password: shows the signed-in user's details and saves changes (the User object validates the
 * new values), and lets them change their own password. Used by both the donor and the admin dashboard.
 */
public class ProfilePage extends BasePage {
    private final User user;
    private final JTextField name = Ui.field();
    private final JTextField email = Ui.field();
    private final JTextField phone = Ui.field();
    private final JComboBox<String> group = Ui.combo(Ui.GROUPS);
    private final JComboBox<String> district = Ui.districtCombo();
    private final JPasswordField oldPassword = Ui.password();
    private final JPasswordField newPassword = Ui.password();
    private final JPasswordField confirm = Ui.password();

    public ProfilePage(User user) {
        super(new BorderLayout());
        this.user = user;
        email.setEditable(false);

        Ui.Form details = new Ui.Form();
        details.row("Full name", name);
        details.row("Email (cannot change)", email);
        details.row("Phone", phone);
        details.row("Blood group", group);
        details.row("District (Kerala)", district);

        Ui.Form password = new Ui.Form();
        password.row("Current password", oldPassword);
        password.row("New password (min 6)", newPassword);
        password.row("Confirm new password", confirm);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(Ui.section("Your details", details));
        body.add(Box.createVerticalStrut(8));
        body.add(Ui.buttonRow(Ui.button("Save Changes", e -> saveProfile())));
        body.add(Box.createVerticalStrut(22));
        body.add(Ui.section("Change password", password));
        body.add(Box.createVerticalStrut(8));
        body.add(Ui.buttonRow(Ui.button("Change Password", e -> changePassword())));
        leftOnlyLayout(body, 540);
    }

    @Override
    public void refresh() {
        name.setText(user.getName());
        email.setText(user.getEmail());
        phone.setText(user.getPhone());
        group.setSelectedItem(user.getBloodGroup());
        Ui.setDistrict(district, user.getDistrict());
    }

    private void saveProfile() {
        try {
            user.updateProfile(name.getText().trim(), phone.getText().trim(),
                    (String) group.getSelectedItem(), Ui.districtValue(district)); // validates, or throws
            UserDao.updateProfile(user);
            Ui.info(this, "Profile updated.");
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void changePassword() {
        String newPw = new String(newPassword.getPassword());
        if (newPw.length() < 6) {
            Ui.error(this, "New password must be at least 6 characters.");
            return;
        }
        if (!newPw.equals(new String(confirm.getPassword()))) {
            Ui.error(this, "New passwords do not match.");
            return;
        }
        try {
            if (UserDao.changePassword(user.getId(), new String(oldPassword.getPassword()), newPw)) {
                oldPassword.setText("");
                newPassword.setText("");
                confirm.setText("");
                Ui.info(this, "Password changed.");
            } else {
                Ui.error(this, "Current password is wrong.");
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
