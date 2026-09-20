package school.main;

import school.service.RoleAuthService;
import school.service.StudentDatabase;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RoleHomeDialog extends JDialog {

    private JTextArea area = new JTextArea();

    public RoleHomeDialog(
            JFrame owner,
            RoleAuthService.Session session) {

        super(owner, session.getRole() + " Dashboard", true);

        setSize(750, 550);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 15));
        area.setMargin(new Insets(20, 20, 20, 20));

        JButton logout = new JButton("Logout");

        logout.addActionListener(e -> dispose());

        add(new JScrollPane(area), BorderLayout.CENTER);
        add(logout, BorderLayout.SOUTH);

        StringBuilder text = new StringBuilder();

        text.append("Welcome, ")
            .append(session.getUsername())
            .append("\n");

        text.append("Role: ")
            .append(session.getRole())
            .append("\n\n");

        try (Connection connection = StudentDatabase.connect()) {

            switch (session.getRole()) {

                case "STUDENT":

                    String studentSql = """
                        SELECT s.first_name, s.last_name,
                               s.level, d.department_name
                        FROM students s
                        LEFT JOIN departments d
                        ON s.department_id = d.department_id
                        WHERE s.student_id = ?
                        """;

                    try (PreparedStatement statement =
                             connection.prepareStatement(studentSql)) {

                        statement.setString(1, session.getProfileId());

                        try (ResultSet result = statement.executeQuery()) {

                            if (result.next()) {

                                text.append("Name: ")
                                    .append(result.getString("first_name"))
                                    .append(" ")
                                    .append(result.getString("last_name"))
                                    .append("\n");

                                text.append("Level: ")
                                    .append(result.getInt("level"))
                                    .append("\n");

                                text.append("Department: ")
                                    .append(result.getString("department_name"))
                                    .append("\n\n");
                            }
                        }
                    }

                    String resultsSql = """
                        SELECT course_id, score, grade
                        FROM results
                        WHERE student_id = ?
                        ORDER BY course_id
                        """;

                    text.append("MY RESULTS\n");
                    text.append("-------------------------\n");

                    try (PreparedStatement statement =
                             connection.prepareStatement(resultsSql)) {

                        statement.setString(1, session.getProfileId());

                        try (ResultSet result = statement.executeQuery()) {

                            while (result.next()) {

                                text.append(result.getString("course_id"))
                                    .append(" | ")
                                    .append(result.getDouble("score"))
                                    .append(" | ")
                                    .append(result.getString("grade"))
                                    .append("\n");
                            }
                        }
                    }

                    String coursesSql = """
                        SELECT c.course_id, c.course_name
                        FROM enrollments e
                        JOIN courses c ON e.course_id = c.course_id
                        WHERE e.student_id = ?
                        ORDER BY c.course_id
                        """;

                    text.append("\nMY COURSES\n");
                    text.append("-------------------------\n");

                    try (PreparedStatement statement =
                             connection.prepareStatement(coursesSql)) {

                        statement.setString(1, session.getProfileId());

                        try (ResultSet result = statement.executeQuery()) {

                            while (result.next()) {

                                text.append(result.getString("course_id"))
                                    .append(" | ")
                                    .append(result.getString("course_name"))
                                    .append("\n");
                            }
                        }
                    }

                    break;

                case "TEACHER":

                    text.append("MY ASSIGNED COURSES\n");
                    text.append("-------------------------\n");

                    String teacherSql = """
                        SELECT course_id, course_name
                        FROM courses
                        WHERE teacher_id = ?
                        ORDER BY course_id
                        """;

                    try (PreparedStatement statement =
                             connection.prepareStatement(teacherSql)) {

                        statement.setString(1, session.getProfileId());

                        try (ResultSet result = statement.executeQuery()) {

                            while (result.next()) {

                                text.append(result.getString("course_id"))
                                    .append(" | ")
                                    .append(result.getString("course_name"))
                                    .append("\n");
                            }
                        }
                    }

                    break;

                case "ACCOUNTANT":

                    text.append("FINANCIAL SUMMARY\n");
                    text.append("-------------------------\n");

                    String paymentSql = """
                        SELECT COUNT(*) AS transactions,
                               COALESCE(SUM(amount), 0) AS total
                        FROM payments
                        """;

                    try (PreparedStatement statement =
                             connection.prepareStatement(paymentSql);
                         ResultSet result = statement.executeQuery()) {

                        if (result.next()) {

                            text.append("Transactions: ")
                                .append(result.getInt("transactions"))
                                .append("\n");

                            text.append("Total Payments: NGN ")
                                .append(result.getDouble("total"))
                                .append("\n");
                        }
                    }

                    break;

                default:
                    text.append("Access denied.");
            }

        } catch (SQLException e) {

            text.append("\nDatabase error: ")
                .append(e.getMessage());
        }

        area.setText(text.toString());

        setVisible(true);
    }
} 