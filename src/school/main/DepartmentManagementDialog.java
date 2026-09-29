package school.main;

import school.service.StudentDatabase;
import school.util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DepartmentManagementDialog extends JDialog {

    private final DefaultTableModel model = new DefaultTableModel(
        new String[] {"Department ID", "Department Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(model);
    private final JTextField nameField = new JTextField();

    public DepartmentManagementDialog(Frame owner) {
        super(owner, "Department Management", true);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("DEPARTMENT MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                nameField.setText(model.getValueAt(row, 1).toString());
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(new JLabel("Department Name:"), BorderLayout.WEST);
        form.add(nameField, BorderLayout.CENTER);
        south.add(form, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("Add Department");
        JButton rename = new JButton("Rename Selected");
        JButton delete = new JButton("Delete Selected");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");

        add.addActionListener(e -> addDepartment());
        rename.addActionListener(e -> renameDepartment());
        delete.addActionListener(e -> deleteDepartment());
        refresh.addActionListener(e -> refresh());
        close.addActionListener(e -> dispose());

        buttons.add(add);
        buttons.add(rename);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refresh();
        setVisible(true);
    }

    private void addDepartment() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Department name cannot be empty.");
            }
            StudentDatabase.addDepartment(name);
            AppLogger.info("DEPARTMENT_CREATED", "Department created: " + name);
            nameField.setText("");
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void renameDepartment() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a department first.");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Department name cannot be empty.");
            return;
        }

        String sql = "UPDATE departments SET department_name = ? WHERE department_id = ?";
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, id);
            statement.executeUpdate();
            AppLogger.info("DEPARTMENT_UPDATED", "Department updated: " + id + " -> " + name);
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteDepartment() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a department first.");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String name = model.getValueAt(row, 1).toString();
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete department " + name + "?\nDepartments already used by students, teachers, courses or classes cannot be deleted.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM departments WHERE department_id = ?";
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            AppLogger.info("DEPARTMENT_DELETED", "Department deleted: " + id + " (" + name + ")");
            nameField.setText("");
            refresh();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "This department is still referenced by another school record and cannot be deleted.",
                "Department In Use",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void refresh() {
        model.setRowCount(0);
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name";
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[] {
                    result.getInt("department_id"),
                    result.getString("department_name")
                });
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        AppLogger.error("DEPARTMENT_ERROR", e.getMessage(), e);
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
