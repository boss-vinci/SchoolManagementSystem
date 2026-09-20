package school.main;

import school.service.RoleAuthService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class AccountManagementDialog extends JDialog {

    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JPasswordField confirmField = new JPasswordField();

    private JComboBox<String> roleBox = new JComboBox<>(
        new String[]{"STUDENT", "TEACHER", "ACCOUNTANT"}
    );

    private JTextField profileField = new JTextField();

    private RoleAuthService.Session session;

    public AccountManagementDialog(
            JFrame owner,
            RoleAuthService.Session session) {

        super(owner, "Account Management", true);

        if (session == null || !session.isAdmin()) {
            throw new SecurityException("Administrator access required.");
        }

        this.session = session;

        setSize(550, 450);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15));

        form.setBorder(
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        );

        form.add(new JLabel("Username"));
        form.add(usernameField);

        form.add(new JLabel("Password"));
        form.add(passwordField);

        form.add(new JLabel("Confirm Password"));
        form.add(confirmField);

        form.add(new JLabel("Role"));
        form.add(roleBox);

        form.add(new JLabel("Student / Teacher ID"));
        form.add(profileField);

        JButton create = new JButton("Create Account");
        JButton close = new JButton("Close");

        create.addActionListener(e -> createAccount());
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout());

        buttons.add(create);
        buttons.add(close);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        roleBox.addActionListener(e -> {

            boolean needsProfile =
                !roleBox.getSelectedItem().equals("ACCOUNTANT");

            profileField.setEnabled(needsProfile);

            if (!needsProfile) {
                profileField.setText("");
            }
        });

        setVisible(true);
    }

    private void createAccount() {

        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        try {

            if (!Arrays.equals(password, confirm)) {
                throw new IllegalArgumentException(
                    "Passwords do not match."
                );
            }

            RoleAuthService.createAccount(
                session,
                usernameField.getText().trim(),
                password,
                (String) roleBox.getSelectedItem(),
                profileField.getText().trim()
            );

            JOptionPane.showMessageDialog(
                this,
                "Account created successfully."
            );

            usernameField.setText("");
            passwordField.setText("");
            confirmField.setText("");
            profileField.setText("");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Account Error",
                JOptionPane.ERROR_MESSAGE
            );

        } finally {

            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }
} 