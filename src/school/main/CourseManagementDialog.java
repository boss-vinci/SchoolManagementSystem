package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CourseManagementDialog extends JDialog {

    private JTextField codeField = new JTextField();
    private JTextField nameField = new JTextField();
    private JTextField departmentField = new JTextField();

    private JComboBox<String> teacherBox = new JComboBox<>();

    private DefaultTableModel courseModel;
    private DefaultTableModel enrollmentModel;

    private JTable courseTable;
    private JTable enrollmentTable;

    public CourseManagementDialog(JFrame owner) {

        super(owner, "Course Management", true);

        setSize(1100, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));

        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));

        form.add(new JLabel("Course Code"));
        form.add(new JLabel("Course Name"));
        form.add(new JLabel("Department"));
        form.add(new JLabel("Teacher"));

        form.add(codeField);
        form.add(nameField);
        form.add(departmentField);
        form.add(teacherBox);

        JPanel actions = new JPanel(new FlowLayout());

        JButton register = new JButton("Register Course");
        JButton assign = new JButton("Assign Teacher");
        JButton refresh = new JButton("Refresh");

        register.addActionListener(e -> registerCourse());
        assign.addActionListener(e -> assignTeacher());
        refresh.addActionListener(e -> refreshAll());

        actions.add(register);
        actions.add(assign);
        actions.add(refresh);

        JPanel top = new JPanel(new BorderLayout(5, 10));

        top.add(form, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        courseModel = new DefaultTableModel(
            new String[]{
                "Course Code",
                "Course Name",
                "Department",
                "Teacher ID"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        courseTable = new JTable(courseModel);

        courseTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        enrollmentModel = new DefaultTableModel(
            new String[]{
                "Student ID",
                "Student Name",
                "Course Code",
                "Course Name"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        enrollmentTable = new JTable(enrollmentModel);

        enrollmentTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(
            "Courses",
            new JScrollPane(courseTable)
        );

        tabs.addTab(
            "Student Enrollments",
            new JScrollPane(enrollmentTable)
        );

        JPanel bottom = new JPanel(new FlowLayout());

        JButton enroll = new JButton("Enroll Student");
        JButton remove = new JButton("Remove Enrollment");
        JButton close = new JButton("Close");

        enroll.addActionListener(e -> enrollStudent());
        remove.addActionListener(e -> removeEnrollment());
        close.addActionListener(e -> dispose());

        bottom.add(enroll);
        bottom.add(remove);
        bottom.add(close);

        main.add(top, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        refreshAll();

        setVisible(true);
    }

    private void refreshAll() {

        refreshTeachers();
        refreshCourses();
        refreshEnrollments();
    }

    private void refreshTeachers() {

        String sql = """
            SELECT teacher_id
            FROM teachers
            ORDER BY teacher_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            teacherBox.removeAllItems();

            teacherBox.addItem("Unassigned");

            while (result.next()) {
                teacherBox.addItem(
                    result.getString("teacher_id")
                );
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void registerCourse() {

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String department = departmentField.getText().trim();

        if (code.isEmpty() || name.isEmpty() ||
            department.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Please complete all course fields."
            );

            return;
        }

        try {

            int departmentId =
                StudentDatabase.addDepartment(department);

            String sql = """
                INSERT INTO courses
                (course_id, course_name, department_id, teacher_id)
                VALUES (?, ?, ?, ?)
                """;

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, code);
                statement.setString(2, name);
                statement.setInt(3, departmentId);

                String teacher =
                    (String) teacherBox.getSelectedItem();

                if (teacher == null ||
                    teacher.equals("Unassigned")) {

                    statement.setNull(4, Types.VARCHAR);

                } else {

                    statement.setString(4, teacher);
                }

                statement.executeUpdate();
            }

            JOptionPane.showMessageDialog(
                this,
                "Course registered successfully."
            );

            codeField.setText("");
            nameField.setText("");
            departmentField.setText("");

            refreshCourses();

        } catch (SQLException e) {
            showError(e);
        }
    }

    private String selectedCourse() {

        int row = courseTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a course in the Courses tab."
            );

            return null;
        }

        int modelRow =
            courseTable.convertRowIndexToModel(row);

        return courseModel.getValueAt(modelRow, 0).toString();
    }

    private void assignTeacher() {

        String courseId = selectedCourse();

        if (courseId == null) return;

        String teacher =
            (String) teacherBox.getSelectedItem();

        String sql = """
            UPDATE courses
            SET teacher_id = ?
            WHERE course_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            if (teacher == null ||
                teacher.equals("Unassigned")) {

                statement.setNull(1, Types.VARCHAR);

            } else {

                statement.setString(1, teacher);
            }

            statement.setString(2, courseId);

            statement.executeUpdate();

            JOptionPane.showMessageDialog(
                this,
                "Course teacher updated successfully."
            );

            refreshCourses();

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void refreshCourses() {

        String sql = """
            SELECT c.course_id,
                   c.course_name,
                   d.department_name,
                   c.teacher_id
            FROM courses c
            LEFT JOIN departments d
            ON c.department_id = d.department_id
            ORDER BY c.course_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            courseModel.setRowCount(0);

            while (result.next()) {

                courseModel.addRow(new Object[]{
                    result.getString("course_id"),
                    result.getString("course_name"),
                    result.getString("department_name"),
                    result.getString("teacher_id")
                });
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void enrollStudent() {

        String courseId = selectedCourse();

        if (courseId == null) return;

        String studentId = JOptionPane.showInputDialog(
            this,
            "Enter Student ID:"
        );

        if (studentId == null) return;

        studentId = studentId.trim();

        if (studentId.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Student ID cannot be empty."
            );

            return;
        }

        String sql = """
            INSERT INTO enrollments
            (student_id, course_id)
            VALUES (?, ?)
            ON CONFLICT(student_id, course_id) DO NOTHING
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, courseId);

            int rows = statement.executeUpdate();

            if (rows > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Student enrolled successfully."
                );

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Student is already enrolled."
                );
            }

            refreshEnrollments();

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void refreshEnrollments() {

        String sql = """
            SELECT e.student_id,
                   s.first_name,
                   s.last_name,
                   e.course_id,
                   c.course_name
            FROM enrollments e
            JOIN students s
            ON e.student_id = s.student_id
            JOIN courses c
            ON e.course_id = c.course_id
            ORDER BY e.student_id, e.course_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            enrollmentModel.setRowCount(0);

            while (result.next()) {

                String fullName =
                    result.getString("first_name") + " " +
                    result.getString("last_name");

                enrollmentModel.addRow(new Object[]{
                    result.getString("student_id"),
                    fullName,
                    result.getString("course_id"),
                    result.getString("course_name")
                });
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void removeEnrollment() {

        int row = enrollmentTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Select an enrollment first."
            );

            return;
        }

        int modelRow =
            enrollmentTable.convertRowIndexToModel(row);

        String studentId =
            enrollmentModel.getValueAt(modelRow, 0).toString();

        String courseId =
            enrollmentModel.getValueAt(modelRow, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove this course enrollment?",
            "Confirm",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = """
            DELETE FROM enrollments
            WHERE student_id = ?
            AND course_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, courseId);

            statement.executeUpdate();

            refreshEnrollments();

            JOptionPane.showMessageDialog(
                this,
                "Enrollment removed successfully."
            );

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void showError(Exception e) {

        JOptionPane.showMessageDialog(
            this,
            e.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
} 