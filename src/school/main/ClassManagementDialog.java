package school.main;

import school.service.StudentDatabase;
import school.util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ClassManagementDialog extends JDialog {

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField departmentField = new JTextField();
    private final JTextField levelField = new JTextField();

    private final DefaultTableModel model = new DefaultTableModel(
        new String[] {"Class ID", "Class Name", "Department", "Level"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(model);

    public ClassManagementDialog(Frame owner) {
        super(owner, "Class Management", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("CLASS MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        form.add(new JLabel("Class ID"));
        form.add(new JLabel("Class Name"));
        form.add(new JLabel("Department"));
        form.add(new JLabel("Level"));
        form.add(idField);
        form.add(nameField);
        form.add(departmentField);
        form.add(levelField);
        center.add(form, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> loadSelection());
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("Add Class");
        JButton update = new JButton("Update Class");
        JButton delete = new JButton("Delete Class");
        JButton refresh = new JButton("Refresh");
        JButton clear = new JButton("Clear");
        JButton close = new JButton("Close");

        add.addActionListener(e -> addClassRecord());
        update.addActionListener(e -> updateClassRecord());
        delete.addActionListener(e -> deleteClassRecord());
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
            CREATE TABLE IF NOT EXISTS classes (
                class_id VARCHAR(50) PRIMARY KEY,
                class_name VARCHAR(150) NOT NULL,
                department_id INTEGER,
                level INTEGER NOT NULL,
                FOREIGN KEY (department_id) REFERENCES departments(department_id)
            )
            """;
        try (Connection connection = StudentDatabase.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addClassRecord() {
        try {
            String id = required(idField.getText(), "Class ID");
            String name = required(nameField.getText(), "Class name");
            String department = required(departmentField.getText(), "Department");
            int level = parseLevel();
            int departmentId = StudentDatabase.addDepartment(department);

            String sql = "INSERT INTO classes (class_id, class_name, department_id, level) VALUES (?, ?, ?, ?)";
            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.setString(2, name);
                statement.setInt(3, departmentId);
                statement.setInt(4, level);
                statement.executeUpdate();
            }
            AppLogger.info("CLASS_CREATED", "Class created: " + id);
            clearFields();
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateClassRecord() {
        String id = required(idField.getText(), "Class ID");
        try {
            String name = required(nameField.getText(), "Class name");
            String department = required(departmentField.getText(), "Department");
            int level = parseLevel();
            int departmentId = StudentDatabase.addDepartment(department);

            String sql = "UPDATE classes SET class_name = ?, department_id = ?, level = ? WHERE class_id = ?";
            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                statement.setInt(2, departmentId);
                statement.setInt(3, level);
                statement.setString(4, id);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Class not found: " + id);
                }
            }
            AppLogger.info("CLASS_UPDATED", "Class updated: " + id);
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteClassRecord() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a class first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete class " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM classes WHERE class_id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
            AppLogger.info("CLASS_DELETED", "Class deleted: " + id);
            clearFields();
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void refresh() {
        model.setRowCount(0);
        String sql = """
            SELECT c.class_id, c.class_name, c.level, d.department_name
            FROM classes c
            LEFT JOIN departments d ON c.department_id = d.department_id
            ORDER BY c.class_id
            """;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[] {
                    result.getString("class_id"),
                    result.getString("class_name"),
                    result.getString("department_name"),
                    result.getInt("level")
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
        nameField.setText(model.getValueAt(row, 1).toString());
        departmentField.setText(model.getValueAt(row, 2) == null ? "" : model.getValueAt(row, 2).toString());
        levelField.setText(model.getValueAt(row, 3).toString());
    }

    private int parseLevel() {
        int level = Integer.parseInt(levelField.getText().trim());
        if (level <= 0) throw new IllegalArgumentException("Level must be greater than zero.");
        return level;
    }

    private String required(String value, String label) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty.");
        return trimmed;
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        departmentField.setText("");
        levelField.setText("");
        table.clearSelection();
    }

    private void showError(Exception e) {
        AppLogger.error("CLASS_ERROR", e.getMessage(), e);
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
