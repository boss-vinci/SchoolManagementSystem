package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TeacherManagementDialog extends JDialog {

    private JTextField idField = new JTextField();
    private JTextField firstNameField = new JTextField();
    private JTextField lastNameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JTextField departmentField = new JTextField();

    private DefaultTableModel tableModel;
    private JTable teacherTable;

    public TeacherManagementDialog(JFrame owner) {

        super(owner, "Teacher Management", true);

        setSize(1000, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 5, 8, 8));

        form.add(new JLabel("Teacher ID"));
        form.add(new JLabel("First Name"));
        form.add(new JLabel("Last Name"));
        form.add(new JLabel("Email"));
        form.add(new JLabel("Department"));

        form.add(idField);
        form.add(firstNameField);
        form.add(lastNameField);
        form.add(emailField);
        form.add(departmentField);

        String[] columns = {
            "Teacher ID",
            "First Name",
            "Last Name",
            "Email",
            "Department"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        teacherTable = new JTable(tableModel);

        teacherTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JPanel buttons = new JPanel(new FlowLayout());

        JButton add = new JButton("Register Teacher");
        JButton update = new JButton("Update Email");
        JButton delete = new JButton("Delete Teacher");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");

        add.addActionListener(e -> registerTeacher());
        update.addActionListener(e -> updateTeacher());
        delete.addActionListener(e -> deleteTeacher());
        refresh.addActionListener(e -> refreshTeachers());
        close.addActionListener(e -> dispose());

        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(teacherTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);

        refreshTeachers();

        setVisible(true);
    }

    private void registerTeacher() {

        String id = idField.getText().trim();
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String department = departmentField.getText().trim();

        if (id.isEmpty() || first.isEmpty() ||
            last.isEmpty() || department.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Please complete all required fields."
            );

            return;
        }

        try {

            int departmentId =
                StudentDatabase.addDepartment(department);

            String sql = """
                INSERT INTO teachers
                (teacher_id, first_name, last_name,
                 email, department_id)
                VALUES (?, ?, ?, ?, ?)
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

                statement.executeUpdate();
            }

            JOptionPane.showMessageDialog(
                this,
                "Teacher registered successfully."
            );

            clearFields();

            refreshTeachers();

        } catch (SQLException e) {

            showError(e);
        }
    }

    private void refreshTeachers() {

        String sql = """
            SELECT t.teacher_id,
                   t.first_name,
                   t.last_name,
                   t.email,
                   d.department_name
            FROM teachers t
            LEFT JOIN departments d
            ON t.department_id = d.department_id
            ORDER BY t.teacher_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            tableModel.setRowCount(0);

            while (result.next()) {

                tableModel.addRow(new Object[] {
                    result.getString("teacher_id"),
                    result.getString("first_name"),
                    result.getString("last_name"),
                    result.getString("email"),
                    result.getString("department_name")
                });
            }

        } catch (SQLException e) {

            showError(e);
        }
    }

    private String getSelectedTeacherId() {

        int row = teacherTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a teacher first."
            );

            return null;
        }

        int modelRow = teacherTable.convertRowIndexToModel(row);

        return tableModel.getValueAt(modelRow, 0).toString();
    }

    private void updateTeacher() {

        String id = getSelectedTeacherId();

        if (id == null) return;

        String email = JOptionPane.showInputDialog(
            this,
            "Enter new email address:"
        );

        if (email == null) return;

        String sql = """
            UPDATE teachers
            SET email = ?
            WHERE teacher_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            email = email.trim();

            if (email.isEmpty()) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, email);
            }

            statement.setString(2, id);

            int rows = statement.executeUpdate();

            if (rows > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Teacher updated successfully."
                );

                refreshTeachers();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Teacher not found."
                );
            }

        } catch (SQLException e) {

            showError(e);
        }
    }

    private void deleteTeacher() {

        String id = getSelectedTeacherId();

        if (id == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete teacher " + id + "?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = """
            DELETE FROM teachers
            WHERE teacher_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int rows = statement.executeUpdate();

            if (rows > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Teacher deleted successfully."
                );

                refreshTeachers();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Teacher not found."
                );
            }

        } catch (SQLException e) {

            showError(e);
        }
    }

    private void clearFields() {

        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        departmentField.setText("");
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