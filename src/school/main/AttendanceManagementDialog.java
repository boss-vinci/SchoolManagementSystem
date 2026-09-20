package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class AttendanceManagementDialog extends JDialog {

    private JComboBox<String> enrollmentBox = new JComboBox<>();

    private JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private JComboBox<String> statusBox =
        new JComboBox<>(new String[]{"PRESENT", "ABSENT"});

    private DefaultTableModel tableModel;
    private JTable attendanceTable;

    public AttendanceManagementDialog(JFrame owner) {

        super(owner, "Attendance Management", true);

        setSize(1000, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));

        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 3, 10, 10));

        form.add(new JLabel("Student Enrollment"));
        form.add(new JLabel("Date (YYYY-MM-DD)"));
        form.add(new JLabel("Attendance Status"));

        form.add(enrollmentBox);
        form.add(dateField);
        form.add(statusBox);

        tableModel = new DefaultTableModel(
            new String[]{
                "ID", "Student ID", "Student Name",
                "Course", "Date", "Status"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        attendanceTable = new JTable(tableModel);

        attendanceTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JPanel buttons = new JPanel(new FlowLayout());

        JButton record = new JButton("Record Attendance");
        JButton delete = new JButton("Delete Record");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");

        record.addActionListener(e -> recordAttendance());
        delete.addActionListener(e -> deleteAttendance());
        refresh.addActionListener(e -> refreshAll());
        close.addActionListener(e -> dispose());

        buttons.add(record);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);

        main.add(form, BorderLayout.NORTH);

        main.add(
            new JScrollPane(attendanceTable),
            BorderLayout.CENTER
        );

        main.add(buttons, BorderLayout.SOUTH);

        add(main);

        refreshAll();

        setVisible(true);
    }

    private void refreshAll() {

        refreshEnrollments();
        refreshAttendance();
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

    private void recordAttendance() {

        String enrollment =
            (String) enrollmentBox.getSelectedItem();

        if (enrollment == null) {

            JOptionPane.showMessageDialog(
                this,
                "Enroll a student in a course first."
            );

            return;
        }

        try {

            String date =
                LocalDate.parse(dateField.getText().trim()).toString();

            int separator = enrollment.indexOf(" | ");

            String studentId = enrollment.substring(0, separator);
            String courseId = enrollment.substring(separator + 3);

            String status =
                (String) statusBox.getSelectedItem();

            String sql = """
                INSERT INTO attendance
                (student_id, course_id, attendance_date, status)
                VALUES (?, ?, ?, ?)
                """;

            try (Connection connection = StudentDatabase.connect()) {

                String check = """
                    SELECT COUNT(*)
                    FROM attendance
                    WHERE student_id = ?
                    AND course_id = ?
                    AND attendance_date = ?
                    """;

                try (PreparedStatement statement =
                         connection.prepareStatement(check)) {

                    statement.setString(1, studentId);
                    statement.setString(2, courseId);
                    statement.setString(3, date);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next() && result.getInt(1) > 0) {

                            JOptionPane.showMessageDialog(
                                this,
                                "Attendance already recorded for this date."
                            );

                            return;
                        }
                    }
                }

                try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                    statement.setString(1, studentId);
                    statement.setString(2, courseId);
                    statement.setString(3, date);
                    statement.setString(4, status);

                    statement.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Attendance recorded successfully."
            );

            refreshAttendance();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void refreshAttendance() {

        String sql = """
            SELECT a.attendance_id,
                   a.student_id,
                   s.first_name,
                   s.last_name,
                   a.course_id,
                   a.attendance_date,
                   a.status
            FROM attendance a
            JOIN students s
            ON a.student_id = s.student_id
            ORDER BY a.attendance_date DESC, a.attendance_id DESC
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            tableModel.setRowCount(0);

            while (result.next()) {

                tableModel.addRow(new Object[]{
                    result.getInt("attendance_id"),
                    result.getString("student_id"),
                    result.getString("first_name") + " " +
                    result.getString("last_name"),
                    result.getString("course_id"),
                    result.getString("attendance_date"),
                    result.getString("status")
                });
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteAttendance() {

        int row = attendanceTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Select an attendance record first."
            );

            return;
        }

        int modelRow =
            attendanceTable.convertRowIndexToModel(row);

        int id = (Integer) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this attendance record?",
            "Confirm",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql =
            "DELETE FROM attendance WHERE attendance_id = ?";

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            statement.executeUpdate();

            refreshAttendance();

            JOptionPane.showMessageDialog(
                this,
                "Attendance deleted successfully."
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