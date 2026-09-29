package school.main;

import school.service.StudentDatabase;
import school.util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class ExaminationManagementDialog extends JDialog {

    private final JTextField idField = new JTextField();
    private final JTextField courseField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField dateField = new JTextField();

    private final DefaultTableModel model = new DefaultTableModel(
        new String[] {"Exam ID", "Course ID", "Exam Name", "Exam Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(model);

    public ExaminationManagementDialog(Frame owner) {
        super(owner, "Examination Management", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("EXAMINATION MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        form.add(new JLabel("Exam ID"));
        form.add(new JLabel("Course ID"));
        form.add(new JLabel("Exam Name"));
        form.add(new JLabel("Exam Date (YYYY-MM-DD)"));
        form.add(idField);
        form.add(courseField);
        form.add(nameField);
        form.add(dateField);
        center.add(form, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> loadSelection());
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("Add Examination");
        JButton update = new JButton("Update Examination");
        JButton delete = new JButton("Delete Examination");
        JButton refresh = new JButton("Refresh");
        JButton clear = new JButton("Clear");
        JButton close = new JButton("Close");

        add.addActionListener(e -> addExam());
        update.addActionListener(e -> updateExam());
        delete.addActionListener(e -> deleteExam());
        refresh.addActionListener(e -> refresh());
        clear.addActionListener(e -> clearFields());
        close.addActionListener(e -> dispose());

        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(clear);
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);

        ensureTable();
        refresh();
        setVisible(true);
    }

    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS examinations (
                exam_id VARCHAR(50) PRIMARY KEY,
                course_id VARCHAR(50) NOT NULL,
                exam_name VARCHAR(150) NOT NULL,
                exam_date VARCHAR(20) NOT NULL,
                FOREIGN KEY (course_id) REFERENCES courses(course_id)
            )
            """;
        try (Connection connection = StudentDatabase.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addExam() {
        try {
            String id = required(idField.getText(), "Exam ID");
            String course = required(courseField.getText(), "Course ID");
            String name = required(nameField.getText(), "Exam name");
            String date = validDate();

            String sql = "INSERT INTO examinations (exam_id, course_id, exam_name, exam_date) VALUES (?, ?, ?, ?)";
            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.setString(2, course);
                statement.setString(3, name);
                statement.setString(4, date);
                statement.executeUpdate();
            }
            AppLogger.info("EXAM_CREATED", "Examination created: " + id + " for " + course);
            clearFields();
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateExam() {
        try {
            String id = required(idField.getText(), "Exam ID");
            String course = required(courseField.getText(), "Course ID");
            String name = required(nameField.getText(), "Exam name");
            String date = validDate();

            String sql = "UPDATE examinations SET course_id = ?, exam_name = ?, exam_date = ? WHERE exam_id = ?";
            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, course);
                statement.setString(2, name);
                statement.setString(3, date);
                statement.setString(4, id);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Examination not found: " + id);
                }
            }
            AppLogger.info("EXAM_UPDATED", "Examination updated: " + id);
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteExam() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select an examination first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete examination " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM examinations WHERE exam_id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
            AppLogger.info("EXAM_DELETED", "Examination deleted: " + id);
            clearFields();
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void refresh() {
        model.setRowCount(0);
        String sql = "SELECT exam_id, course_id, exam_name, exam_date FROM examinations ORDER BY exam_date, exam_id";
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[] {
                    result.getString("exam_id"),
                    result.getString("course_id"),
                    result.getString("exam_name"),
                    result.getString("exam_date")
                });
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        idField.setText(model.getValueAt(row, 0).toString());
        courseField.setText(model.getValueAt(row, 1).toString());
        nameField.setText(model.getValueAt(row, 2).toString());
        dateField.setText(model.getValueAt(row, 3).toString());
    }

    private String validDate() {
        String value = required(dateField.getText(), "Exam date");
        LocalDate.parse(value);
        return value;
    }

    private String required(String value, String label) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty.");
        return trimmed;
    }

    private void clearFields() {
        idField.setText("");
        courseField.setText("");
        nameField.setText("");
        dateField.setText("");
        table.clearSelection();
    }

    private void showError(Exception e) {
        AppLogger.error("EXAM_ERROR", e.getMessage(), e);
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
