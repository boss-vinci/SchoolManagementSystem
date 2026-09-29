package school.main;

import school.service.RoleAuthService;
import school.service.StudentDatabase;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Locale;

public class RoleHomeDialog extends JDialog {

    private final JTextArea area = new JTextArea();
    private final RoleAuthService.Session session;

    public RoleHomeDialog(JFrame owner, RoleAuthService.Session session) {
        super(owner, session == null ? "Dashboard" : session.getRole() + " Dashboard", true);
        if (session == null || session.isAdmin()) {
            throw new SecurityException("Non-administrator session required.");
        }
        this.session = session;
        setSize(820, 610);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setMargin(new Insets(20, 20, 20, 20));
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        if ("TEACHER".equals(session.getRole())) {
            JButton workspace = new JButton("My Students, Results & Attendance");
            workspace.addActionListener(e -> {
                new TeacherWorkspaceDialog(owner, session);
                refresh();
            });
            buttons.add(workspace);
        } else if ("ACCOUNTANT".equals(session.getRole())) {
            JButton payments = new JButton("Manage Payments");
            payments.addActionListener(e -> {
                new PaymentManagementDialog(owner);
                refresh();
            });
            buttons.add(payments);
        }
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> dispose());
        buttons.add(refresh);
        buttons.add(logout);
        add(buttons, BorderLayout.SOUTH);
        refresh();
        setVisible(true);
    }

    private void refresh() {
        StringBuilder text = new StringBuilder();
        text.append("Welcome, ").append(session.getUsername())
            .append("\nRole: ").append(session.getRole()).append("\n\n");
        try (Connection connection = StudentDatabase.connect()) {
            switch (session.getRole()) {
                case "STUDENT" -> studentView(connection, text);
                case "TEACHER" -> teacherView(connection, text);
                case "ACCOUNTANT" -> accountantView(connection, text);
                default -> text.append("Access denied.\n");
            }
        } catch (SQLException e) {
            text.append("Database error: ").append(e.getMessage());
        }
        area.setText(text.toString());
        area.setCaretPosition(0);
    }

    private void studentView(Connection connection, StringBuilder text) throws SQLException {
        String id = session.getProfileId();
        if (id == null) throw new SecurityException("No linked student record.");
        String profile = """
            SELECT s.first_name, s.last_name, s.level, d.department_name
            FROM students s LEFT JOIN departments d ON d.department_id=s.department_id
            WHERE s.student_id=?
            """;
        try (PreparedStatement statement = connection.prepareStatement(profile)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    text.append("MY PROFILE\n-----------------------------\n")
                        .append("Name: ").append(result.getString("first_name")).append(" ")
                        .append(result.getString("last_name")).append("\nLevel: ")
                        .append(result.getInt("level")).append("\nDepartment: ")
                        .append(result.getString("department_name")).append("\n\n");
                }
            }
        }
        String courses = """
            SELECT c.course_id, c.course_name FROM enrollments e
            JOIN courses c ON c.course_id=e.course_id
            WHERE e.student_id=? ORDER BY c.course_id
            """;
        text.append("MY COURSES\n-----------------------------\n");
        try (PreparedStatement statement = connection.prepareStatement(courses)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    text.append(result.getString("course_id")).append(" | ")
                        .append(result.getString("course_name")).append("\n");
                }
            }
        }
        String results = "SELECT course_id, score, grade FROM results WHERE student_id=? ORDER BY course_id";
        text.append("\nMY RESULTS\n-----------------------------\n");
        try (PreparedStatement statement = connection.prepareStatement(results)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    text.append(result.getString("course_id")).append(" | ")
                        .append(result.getDouble("score")).append(" | ")
                        .append(result.getString("grade")).append("\n");
                }
            }
        }
        String attendance = """
            SELECT course_id, attendance_date, status FROM attendance
            WHERE student_id=? ORDER BY attendance_date DESC, course_id LIMIT 30
            """;
        text.append("\nMY ATTENDANCE (LAST 30 RECORDS)\n-----------------------------\n");
        try (PreparedStatement statement = connection.prepareStatement(attendance)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    text.append(result.getString("course_id")).append(" | ")
                        .append(result.getString("attendance_date")).append(" | ")
                        .append(result.getString("status")).append("\n");
                }
            }
        }
        String payments = """
            SELECT amount, payment_date, payment_method FROM payments
            WHERE student_id=? ORDER BY payment_date DESC, payment_id DESC
            """;
        text.append("\nMY PAYMENTS\n-----------------------------\n");
        double paid = 0;
        try (PreparedStatement statement = connection.prepareStatement(payments)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double amount = result.getDouble("amount");
                    paid += amount;
                    text.append(result.getString("payment_date")).append(" | NGN ")
                        .append(String.format(Locale.US, "%,.2f", amount)).append(" | ")
                        .append(result.getString("payment_method")).append("\n");
                }
            }
        }
        text.append(String.format(Locale.US, "Total paid: NGN %,.2f%n", paid));
        text.append("Outstanding fees unavailable: no fee schedule has been configured.\n");
    }

    private void teacherView(Connection connection, StringBuilder text) throws SQLException {
        if (session.getProfileId() == null) throw new SecurityException("No linked teacher record.");
        String sql = "SELECT course_id, course_name FROM courses WHERE teacher_id=? ORDER BY course_id";
        text.append("MY ASSIGNED COURSES\n-----------------------------\n");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, session.getProfileId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    text.append(result.getString("course_id")).append(" | ")
                        .append(result.getString("course_name")).append("\n");
                }
            }
        }
        text.append("\nUse the teacher workspace to view enrolled students, ")
            .append("record results and mark attendance for assigned courses only.\n");
    }

    private void accountantView(Connection connection, StringBuilder text) throws SQLException {
        String sql = "SELECT COUNT(*) AS transactions, COALESCE(SUM(amount), 0) AS total FROM payments";
        text.append("FINANCIAL SUMMARY\n-----------------------------\n");
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                text.append("Transactions: ").append(result.getInt("transactions")).append("\n")
                    .append(String.format(Locale.US, "Total payments: NGN %,.2f%n",
                            result.getDouble("total")));
            }
        }
        text.append("\nUse Manage Payments to record payments and view payment history.\n")
            .append("Outstanding fees require a fee schedule (not configured yet).\n");
    }
}
