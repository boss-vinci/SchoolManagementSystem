package school.main;

import school.service.AuthService;
import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Arrays;

public class SchoolApp extends JFrame {

    private CardLayout cards = new CardLayout();
    private JPanel mainPanel = new JPanel(cards);

    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();

    private JTextField idField = new JTextField();
    private JTextField firstNameField = new JTextField();
    private JTextField lastNameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JTextField departmentField = new JTextField();
    private JTextField levelField = new JTextField();

    private DefaultTableModel tableModel;
    private JTable studentTable;

    private boolean authenticated = false;

    public SchoolApp() {

        setTitle("School Management System");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createDashboard(), "DASHBOARD");
        mainPanel.add(createStudentPanel(), "STUDENTS");

        add(mainPanel);

        cards.show(mainPanel, "LOGIN");

        setVisible(true);

        try {
            if (!AuthService.hasAdmin()) {
                setupAdmin();
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void setupAdmin() {

        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        panel.add(new JLabel("Create administrator password:"));
        panel.add(password);
        panel.add(new JLabel("Confirm password:"));
        panel.add(confirm);

        int option = JOptionPane.showConfirmDialog(
            this,
            panel,
            "First-Time Administrator Setup",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        char[] first = password.getPassword();
        char[] second = confirm.getPassword();

        try {

            if (!Arrays.equals(first, second)) {
                throw new IllegalArgumentException(
                    "Passwords do not match."
                );
            }

            AuthService.createAdmin(first);

            JOptionPane.showMessageDialog(
                this,
                "Administrator created.\nUsername: admin"
            );

        } catch (Exception e) {
            showError(e);
            setupAdmin();

        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }

    private JPanel createLoginPanel() {

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBackground(new Color(240, 244, 248));

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10));

        form.setPreferredSize(new Dimension(350, 300));

        form.setBorder(
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        );

        JLabel title = new JLabel(
            "SCHOOL MANAGEMENT SYSTEM",
            SwingConstants.CENTER
        );

        title.setFont(new Font("Segoe UI", Font.BOLD, 17));

        JButton loginButton = new JButton("LOGIN");

        form.add(title);
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(loginButton);

        loginButton.addActionListener(e -> login());

        passwordField.addActionListener(e -> login());

        panel.add(form);

        return panel;
    }

    private void login() {

        char[] password = passwordField.getPassword();

        try {

            boolean valid = AuthService.login(
                usernameField.getText().trim(),
                password
            );

            if (valid) {

                authenticated = true;

                passwordField.setText("");

                cards.show(mainPanel, "DASHBOARD");

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Invalid administrator credentials."
                );
            }

        } catch (Exception e) {
            showError(e);

        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private JPanel createDashboard() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));

        panel.setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel(
            "Administrator Dashboard",
            SwingConstants.CENTER
        );

        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JPanel buttons = new JPanel(new GridLayout(2, 1, 15, 15));

        JButton students = new JButton("Manage Students");
        JButton logout = new JButton("Logout");

        students.addActionListener(e -> {

            if (authenticated) {
                refreshStudents();
                cards.show(mainPanel, "STUDENTS");
            }
        });

        logout.addActionListener(e -> {

            authenticated = false;

            usernameField.setText("");
            passwordField.setText("");

            cards.show(mainPanel, "LOGIN");
        });

        buttons.add(students);
        buttons.add(logout);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStudentPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 6, 8, 8));

        form.add(new JLabel("Student ID"));
        form.add(new JLabel("First Name"));
        form.add(new JLabel("Last Name"));
        form.add(new JLabel("Email"));
        form.add(new JLabel("Department"));
        form.add(new JLabel("Level"));

        form.add(idField);
        form.add(firstNameField);
        form.add(lastNameField);
        form.add(emailField);
        form.add(departmentField);
        form.add(levelField);

        String[] columns = {
            "Student ID", "First Name", "Last Name",
            "Email", "Department", "Level"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        studentTable = new JTable(tableModel);

        studentTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JPanel buttons = new JPanel(new FlowLayout());

        JButton add = new JButton("Register Student");
        JButton update = new JButton("Update Level");
        JButton delete = new JButton("Delete Student");
        JButton refresh = new JButton("Refresh");
        JButton back = new JButton("Back");

        add.addActionListener(e -> registerStudent());
        update.addActionListener(e -> updateStudent());
        delete.addActionListener(e -> deleteStudent());
        refresh.addActionListener(e -> refreshStudents());

        back.addActionListener(e ->
            cards.show(mainPanel, "DASHBOARD")
        );

        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(back);

        panel.add(form, BorderLayout.NORTH);

        panel.add(
            new JScrollPane(studentTable),
            BorderLayout.CENTER
        );

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private int selectedRow() {

        int row = studentTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a student first."
            );
        }

        return row;
    }

    private void registerStudent() {

        if (!authenticated) return;

        String id = idField.getText().trim();
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String department = departmentField.getText().trim();

        try {

            if (id.isEmpty() || first.isEmpty() ||
                last.isEmpty() || department.isEmpty()) {

                throw new IllegalArgumentException(
                    "Complete all required student fields."
                );
            }

            int level = Integer.parseInt(
                levelField.getText().trim()
            );

            if (level < 100 || level > 500 || level % 100 != 0) {
                throw new IllegalArgumentException(
                    "Level must be 100, 200, 300, 400 or 500."
                );
            }

            int departmentId =
                StudentDatabase.addDepartment(department);

            String sql = """
                INSERT INTO students
                (student_id, first_name, last_name,
                 email, department_id, level)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, id);
                statement.setString(2, first);
                statement.setString(3, last);

                if (email.isEmpty()) {
                    statement.setNull(4, Types.VARCHAR);
                } else {
                    statement.setString(4, email);
                }

                statement.setInt(5, departmentId);
                statement.setInt(6, level);

                statement.executeUpdate();
            }

            JOptionPane.showMessageDialog(
                this,
                "Student registered successfully."
            );

            refreshStudents();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void refreshStudents() {

        if (!authenticated) return;

        String sql = """
            SELECT s.student_id, s.first_name, s.last_name,
                   s.email, d.department_name, s.level
            FROM students s
            LEFT JOIN departments d
            ON s.department_id = d.department_id
            ORDER BY s.student_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            tableModel.setRowCount(0);

            while (result.next()) {

                tableModel.addRow(new Object[] {

                    result.getString("student_id"),
                    result.getString("first_name"),
                    result.getString("last_name"),
                    result.getString("email"),
                    result.getString("department_name"),
                    result.getInt("level")
                });
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateStudent() {

        if (!authenticated) return;

        int row = selectedRow();

        if (row == -1) return;

        String id = tableModel.getValueAt(
            row, 0
        ).toString();

        String input = JOptionPane.showInputDialog(
            this,
            "Enter new student level:"
        );

        if (input == null) return;

        try {

            int level = Integer.parseInt(input.trim());

            if (level < 100 || level > 500 || level % 100 != 0) {
                throw new IllegalArgumentException(
                    "Invalid student level."
                );
            }

            String sql = """
                UPDATE students
                SET level = ?
                WHERE student_id = ?
                """;

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setInt(1, level);
                statement.setString(2, id);

                statement.executeUpdate();
            }

            refreshStudents();

            JOptionPane.showMessageDialog(
                this,
                "Student updated successfully."
            );

        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteStudent() {

        if (!authenticated) return;

        int row = selectedRow();

        if (row == -1) return;

        String id = tableModel.getValueAt(
            row, 0
        ).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete student " + id + "?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {

            String sql =
                "DELETE FROM students WHERE student_id = ?";

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, id);

                statement.executeUpdate();
            }

            refreshStudents();

            JOptionPane.showMessageDialog(
                this,
                "Student deleted successfully."
            );

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void showError(Exception e) {

        JOptionPane.showMessageDialog(
            this,
            e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(SchoolApp::new);
    }
} 