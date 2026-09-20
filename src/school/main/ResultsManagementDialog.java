package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ResultsManagementDialog extends JDialog {

    private JComboBox<String> enrollmentBox = new JComboBox<>();
    private JTextField scoreField = new JTextField();

    private DefaultTableModel tableModel;
    private JTable resultTable;

    public ResultsManagementDialog(JFrame owner) {

        super(owner, "Student Results Management", true);

        setSize(1050, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));

        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));

        form.add(new JLabel("Student Enrollment"));
        form.add(new JLabel("Examination Score"));

        form.add(enrollmentBox);
        form.add(scoreField);

        String[] columns = {
            "Result ID",
            "Student ID",
            "Student Name",
            "Course",
            "Score",
            "Grade"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(tableModel);

        resultTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JPanel buttons = new JPanel(new FlowLayout());

        JButton add = new JButton("Record Result");
        JButton update = new JButton("Update Score");
        JButton delete = new JButton("Delete Result");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");

        add.addActionListener(e -> addResult());
        update.addActionListener(e -> updateResult());
        delete.addActionListener(e -> deleteResult());
        refresh.addActionListener(e -> refreshAll());
        close.addActionListener(e -> dispose());

        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);

        main.add(form, BorderLayout.NORTH);

        main.add(
            new JScrollPane(resultTable),
            BorderLayout.CENTER
        );

        main.add(buttons, BorderLayout.SOUTH);

        add(main);

        refreshAll();

        setVisible(true);
    }

    private String calculateGrade(double score) {

        if (score >= 70) return "A";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        if (score >= 45) return "D";
        if (score >= 40) return "E";

        return "F";
    }

    private double readScore(String text) {

        double score = Double.parseDouble(text.trim());

        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalArgumentException(
                "Score must be between 0 and 100."
            );
        }

        return score;
    }

    private void refreshAll() {

        refreshEnrollments();
        refreshResults();
    }

    private void refreshEnrollments() {

        String sql = """
            SELECT student_id, course_id
            FROM enrollments
            ORDER BY student_id, course_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            enrollmentBox.removeAllItems();

            while (result.next()) {

                enrollmentBox.addItem(
                    result.getString("student_id") + " | " +
                    result.getString("course_id")
                );
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addResult() {

        String enrollment =
            (String) enrollmentBox.getSelectedItem();

        if (enrollment == null) {

            JOptionPane.showMessageDialog(
                this,
                "Please enroll a student in a course first."
            );

            return;
        }

        try {

            double score = readScore(scoreField.getText());

            String[] parts = enrollment.split(" \\| ");

            String studentId = parts[0];
            String courseId = parts[1];

            try (Connection connection = StudentDatabase.connect()) {

                String check = """
                    SELECT COUNT(*)
                    FROM results
                    WHERE student_id = ?
                    AND course_id = ?
                    """;

                try (PreparedStatement statement =
                         connection.prepareStatement(check)) {

                    statement.setString(1, studentId);
                    statement.setString(2, courseId);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next() && result.getInt(1) > 0) {

                            JOptionPane.showMessageDialog(
                                this,
                                "A result already exists. Select it and update the score."
                            );

                            return;
                        }
                    }
                }

                String sql = """
                    INSERT INTO results
                    (student_id, course_id, score, grade)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                    statement.setString(1, studentId);
                    statement.setString(2, courseId);
                    statement.setDouble(3, score);
                    statement.setString(4, calculateGrade(score));

                    statement.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Result recorded successfully."
            );

            scoreField.setText("");

            refreshResults();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void refreshResults() {

        String sql = """
            SELECT r.result_id,
                   r.student_id,
                   s.first_name,
                   s.last_name,
                   r.course_id,
                   r.score,
                   r.grade
            FROM results r
            JOIN students s
            ON r.student_id = s.student_id
            ORDER BY r.result_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            tableModel.setRowCount(0);

            while (result.next()) {

                String name =
                    result.getString("first_name") + " " +
                    result.getString("last_name");

                tableModel.addRow(new Object[]{
                    result.getInt("result_id"),
                    result.getString("student_id"),
                    name,
                    result.getString("course_id"),
                    result.getDouble("score"),
                    result.getString("grade")
                });
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private Integer selectedResult() {

        int row = resultTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a result first."
            );

            return null;
        }

        int modelRow =
            resultTable.convertRowIndexToModel(row);

        return (Integer) tableModel.getValueAt(modelRow, 0);
    }

    private void updateResult() {

        Integer resultId = selectedResult();

        if (resultId == null) return;

        String input = JOptionPane.showInputDialog(
            this,
            "Enter new examination score:"
        );

        if (input == null) return;

        try {

            double score = readScore(input);

            String sql = """
                UPDATE results
                SET score = ?, grade = ?
                WHERE result_id = ?
                """;

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setDouble(1, score);
                statement.setString(2, calculateGrade(score));
                statement.setInt(3, resultId);

                int rows = statement.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Result not found.");
                }
            }

            refreshResults();

            JOptionPane.showMessageDialog(
                this,
                "Result updated successfully."
            );

        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteResult() {

        Integer resultId = selectedResult();

        if (resultId == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this examination result?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = """
            DELETE FROM results
            WHERE result_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setInt(1, resultId);

            statement.executeUpdate();

            refreshResults();

            JOptionPane.showMessageDialog(
                this,
                "Result deleted successfully."
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
} 