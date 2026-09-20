package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Locale;

public class ReportsManagementDialog extends JDialog {

    private JTextArea reportArea = new JTextArea();

    public ReportsManagementDialog(JFrame owner) {

        super(owner, "School Reports", true);

        setSize(850, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));

        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel title = new JLabel(
            "SCHOOL MANAGEMENT REPORTS",
            SwingConstants.CENTER
        );

        title.setFont(
            new Font("Segoe UI", Font.BOLD, 24)
        );

        reportArea.setEditable(false);
        reportArea.setFont(
            new Font("Consolas", Font.PLAIN, 15)
        );

        reportArea.setMargin(new Insets(15, 15, 15, 15));

        JPanel buttons = new JPanel(new FlowLayout());

        JButton refresh = new JButton("Refresh Reports");
        JButton export = new JButton("Export Report");
        JButton close = new JButton("Close");

        refresh.addActionListener(e -> generateReport());
        export.addActionListener(e -> exportReport());
        close.addActionListener(e -> dispose());

        buttons.add(refresh);
        buttons.add(export);
        buttons.add(close);

        main.add(title, BorderLayout.NORTH);

        main.add(
            new JScrollPane(reportArea),
            BorderLayout.CENTER
        );

        main.add(buttons, BorderLayout.SOUTH);

        add(main);

        generateReport();

        setVisible(true);
    }

    private int getCount(Connection connection, String sql)
            throws SQLException {

        try (Statement statement =
                 connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void generateReport() {

        StringBuilder report = new StringBuilder();

        try (Connection connection = StudentDatabase.connect()) {

            int students = getCount(
                connection,
                "SELECT COUNT(*) FROM students"
            );

            int teachers = getCount(
                connection,
                "SELECT COUNT(*) FROM teachers"
            );

            int courses = getCount(
                connection,
                "SELECT COUNT(*) FROM courses"
            );

            int enrollments = getCount(
                connection,
                "SELECT COUNT(*) FROM enrollments"
            );

            report.append("SCHOOL MANAGEMENT SYSTEM\n");
            report.append("========================================\n\n");

            report.append("GENERAL STATISTICS\n");
            report.append("----------------------------------------\n");

            report.append("Total Students: ").append(students).append("\n");
            report.append("Total Teachers: ").append(teachers).append("\n");
            report.append("Total Courses: ").append(courses).append("\n");
            report.append("Total Enrollments: ").append(enrollments).append("\n\n");

            report.append("STUDENTS BY DEPARTMENT\n");
            report.append("----------------------------------------\n");

            String departmentSql = """
                SELECT d.department_name,
                       COUNT(s.student_id) AS total
                FROM departments d
                LEFT JOIN students s
                ON d.department_id = s.department_id
                GROUP BY d.department_id, d.department_name
                ORDER BY d.department_name
                """;

            try (Statement statement =
                     connection.createStatement();
                 ResultSet result =
                     statement.executeQuery(departmentSql)) {

                while (result.next()) {

                    report.append(
                        result.getString("department_name")
                    );

                    report.append(": ");

                    report.append(result.getInt("total"));

                    report.append("\n");
                }
            }

            report.append("\nEXAMINATION STATISTICS\n");
            report.append("----------------------------------------\n");

            String examSql = """
                SELECT COUNT(*) AS total,
                       AVG(score) AS average,
                       MAX(score) AS highest,
                       MIN(score) AS lowest
                FROM results
                """;

            try (Statement statement =
                     connection.createStatement();
                 ResultSet result =
                     statement.executeQuery(examSql)) {

                if (result.next()) {

                    int total = result.getInt("total");

                    report.append("Total Results: ")
                          .append(total).append("\n");

                    if (total > 0) {

                        report.append(
                            String.format(
                                Locale.US,
                                "Average Score: %.2f%n",
                                result.getDouble("average")
                            )
                        );

                        report.append("Highest Score: ")
                              .append(result.getDouble("highest"))
                              .append("\n");

                        report.append("Lowest Score: ")
                              .append(result.getDouble("lowest"))
                              .append("\n");
                    }
                }
            }

            report.append("\nGRADE DISTRIBUTION\n");
            report.append("----------------------------------------\n");

            String gradeSql = """
                SELECT grade, COUNT(*) AS total
                FROM results
                GROUP BY grade
                ORDER BY grade
                """;

            try (Statement statement =
                     connection.createStatement();
                 ResultSet result =
                     statement.executeQuery(gradeSql)) {

                while (result.next()) {

                    report.append("Grade ")
                          .append(result.getString("grade"))
                          .append(": ")
                          .append(result.getInt("total"))
                          .append("\n");
                }
            }

            report.append("\nATTENDANCE REPORT\n");
            report.append("----------------------------------------\n");

            String attendanceSql = """
                SELECT status, COUNT(*) AS total
                FROM attendance
                GROUP BY status
                ORDER BY status
                """;

            try (Statement statement =
                     connection.createStatement();
                 ResultSet result =
                     statement.executeQuery(attendanceSql)) {

                while (result.next()) {

                    report.append(result.getString("status"))
                          .append(": ")
                          .append(result.getInt("total"))
                          .append("\n");
                }
            }

            report.append("\nPAYMENT REPORT\n");
            report.append("----------------------------------------\n");

            String paymentSql = """
                SELECT COUNT(*) AS transactions,
                       COALESCE(SUM(amount), 0) AS total
                FROM payments
                """;

            try (Statement statement =
                     connection.createStatement();
                 ResultSet result =
                     statement.executeQuery(paymentSql)) {

                if (result.next()) {

                    report.append("Transactions: ")
                          .append(result.getInt("transactions"))
                          .append("\n");

                    report.append(
                        String.format(
                            Locale.US,
                            "Total Payments: NGN %,.2f%n",
                            result.getDouble("total")
                        )
                    );
                }
            }

            report.append("\n========================================\n");
            report.append("Report generated successfully.\n");

            reportArea.setText(report.toString());
            reportArea.setCaretPosition(0);

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Report Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void exportReport() {

        JFileChooser chooser = new JFileChooser();

        chooser.setSelectedFile(
            new java.io.File("school-report.txt")
        );

        int option = chooser.showSaveDialog(this);

        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();

        if (Files.exists(file)) {

            int confirm = JOptionPane.showConfirmDialog(
                this,
                "The file already exists. Replace it?",
                "Confirm Export",
                JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {

            Files.writeString(
                file,
                reportArea.getText(),
                StandardCharsets.UTF_8
            );

            JOptionPane.showMessageDialog(
                this,
                "Report exported successfully."
            );

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
} 