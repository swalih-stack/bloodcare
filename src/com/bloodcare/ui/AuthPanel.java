package com.bloodcare.ui;

import com.bloodcare.db.UserDao;
import com.bloodcare.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * AuthPanel - Sign In and Create Account on one screen.
 * One sign-in form serves everyone: after the password check, the role
 * stored in the database decides whether the admin page or the user
 * dashboard opens. New accounts are always created with role USER.
 */
public class AuthPanel extends JPanel {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final MainFrame frame;
    private final CardLayout forms = new CardLayout();
    private final JPanel formArea = new JPanel(forms);

    // sign-in fields
    private final JTextField loginEmail = Ui.field();
    private final JPasswordField loginPassword = Ui.password();

    // registration fields
    private final JTextField regName = Ui.field();
    private final JTextField regEmail = Ui.field();
    private final JTextField regPhone = Ui.field();
    private final JComboBox<String> regGroup = Ui.combo(Ui.GROUPS);
    private final JComboBox<String> regCity = Ui.districtCombo();
    private final JPasswordField regPassword = Ui.password();
    private final JPasswordField regConfirm = Ui.password();

    public AuthPanel(MainFrame frame) {
        super(new GridLayout(1, 2));
        this.frame = frame;
        add(brandSide());
        formArea.setBackground(Color.WHITE);
        formArea.add(signInForm(), "signin");
        formArea.add(registerForm(), "register");
        add(formArea);
    }

    private JPanel brandSide() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Ui.RED);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("BloodCare", Icons.dropIcon(40, Color.WHITE), SwingConstants.LEFT);
        logo.setIconTextGap(10);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 38));
        logo.setForeground(Color.WHITE);
        JLabel tag = new JLabel("Every drop counts.");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        tag.setForeground(Color.WHITE);
        logo.setAlignmentX(0);
        tag.setAlignmentX(0);
        box.add(logo);
        box.add(Box.createVerticalStrut(8));
        box.add(tag);
        p.add(box);
        return p;
    }

    private JPanel wrap(String title, JComponent body) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Color.WHITE);
        JPanel box = new JPanel(new BorderLayout(0, 14));
        box.setOpaque(false);
        box.add(Ui.heading(title), BorderLayout.NORTH);
        box.add(body, BorderLayout.CENTER);
        outer.add(box);
        return outer;
    }

    private JPanel signInForm() {
        Ui.Form f = new Ui.Form();
        f.row("Email", loginEmail);
        f.row("Password", loginPassword);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(Ui.button("Sign In", e -> doLogin()));
        actions.add(Ui.link("Create account", Ui.RED, e -> forms.show(formArea, "register")));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(f, BorderLayout.CENTER);
        body.add(actions, BorderLayout.SOUTH);
        loginPassword.addActionListener(e -> doLogin());
        return wrap("Sign In", body);
    }

    private JPanel registerForm() {
        Ui.Form f = new Ui.Form();
        f.row("Full name", regName);
        f.row("Email", regEmail);
        f.row("Phone (10 digits)", regPhone);
        f.row("Blood group", regGroup);
        f.row("District (Kerala)", regCity);
        f.row("Password (min 6)", regPassword);
        f.row("Confirm password", regConfirm);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(Ui.button("Create Account", e -> doRegister()));
        actions.add(Ui.link("I already have an account", Ui.RED, e -> forms.show(formArea, "signin")));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(f, BorderLayout.CENTER);
        body.add(actions, BorderLayout.SOUTH);
        return wrap("Create Account", body);
    }

    private void doLogin() {
        String email = loginEmail.getText().trim();
        String password = new String(loginPassword.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            Ui.error(this, "Enter your email and password.");
            return;
        }
        try {
            User user = UserDao.login(email, password);
            if (user == null) {
                Ui.error(this, "Wrong email or password.");
                return;
            }
            loginPassword.setText("");
            frame.openDashboard(user);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void doRegister() {
        String name = regName.getText().trim();
        String email = regEmail.getText().trim();
        String phone = regPhone.getText().trim();
        String city = Ui.districtValue(regCity); // null until a district is chosen
        String pw = new String(regPassword.getPassword());
        String confirm = new String(regConfirm.getPassword());

        // profile rules live in the User class; only the e-mail and password checks are specific to sign-up
        String problem = User.profileProblem(name, phone, (String) regGroup.getSelectedItem(), city);
        if (problem == null) {
            if (!EMAIL.matcher(email).matches()) problem = "Enter a valid email address.";
            else if (pw.length() < 6) problem = "Password must be at least 6 characters.";
            else if (!pw.equals(confirm)) problem = "Passwords do not match.";
        }
        if (problem != null) {
            Ui.error(this, problem);
            return;
        }
        try {
            if (UserDao.emailExists(email)) {
                Ui.error(this, "An account with this email already exists.");
                return;
            }
            UserDao.create(name, email, phone, (String) regGroup.getSelectedItem(), city, pw, "USER");
            for (JTextField t : new JTextField[]{regName, regEmail, regPhone, regPassword, regConfirm}) {
                t.setText("");
            }
            regCity.setSelectedIndex(0);
            loginEmail.setText(email);
            Ui.info(this, "Account created. Please sign in.");
            forms.show(formArea, "signin");
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
