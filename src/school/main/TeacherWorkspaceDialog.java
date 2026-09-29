package school.main;

import school.service.RoleAuthService;
import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class TeacherWorkspaceDialog extends JDialog {

    private final String teacherId;
    private final JComboBox<String> courseBox = new JComboBox<>();
    private final DefaultTableModel studentsModel = new DefaultTableModel(
        new String[]{"Student ID", "Name", "Course", "Score", "Grade", "Attendance"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable studentsTable = new JTable(studentsModel);

    public TeacherWorkspaceDialog(JFrame owner, RoleAuthService.Session session) {
        super(owner, "Teacher Workspace", true);
        if (session == null || !"TEACHER".equals(session.getRole()) ||
                session.getProfileId() == null) {
            throw new SecurityException("Teacher access required.");
        }
        teacherId = session.getProfileId();
        setSize(1050, 570);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(new JLabel("My assigned course:"), BorderLayout.WEST);
        top.add(courseBox, BorderLayout.CENTER);
        courseBox.addActionListener(e -> refreshStudents());

        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel actions = new JPanel(new FlowLayout());
        JButton result = new JButton("Enter / Update Result");
        JButton attendance = new JButton("Mark Attendance");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");
        result.addActionListener(e -> saveResult());
        attendance.addActionListener(e -> saveAttendance());
        refresh.addActionListener(e -> refreshCourses());
        close.addActionListener(e -> dispose());
        actions.add(result);
        actions.add(attendance);
        actions.add(refresh);
        actions.add(close);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        main.add(top, BorderLayout.NORTH);
        main.add(new JScrollPane(studentsTable), BorderLayout.CENTER);
        main.add(actions, BorderLayout.SOUTH);
        add(main);
        refreshCourses();
        setVisible(true);
    }

    private String selectedCourse() {
        Object value = courseBox.getSelectedItem();
        if (value == null) return null;
        String text = value.toString();
        int separator = text.indexOf(" | ");
        return separator < 0 ? null : text.substring(0, separator);
    }

    private String selectedStudent() {
        int row = studentsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a student in your course first.");
            return null;
        }
        return studentsModel.getValueAt(studentsTable.convertRowIndexToModel(row), 0).toString();
    }

    private void refreshCourses() {
        String previous = selectedCourse();
        String sql = "SELECT course_id, course_name FROM courses WHERE teacher_id = ? ORDER BY course_id";
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            try (ResultSet result = statement.executeQuery()) {
                courseBox.removeAllItems();
                while (result.next()) {
                    courseBox.addItem(result.getString("course_id") + " | " +
                            result.getString("course_name"));
                }
            }
            if (previous != null) {
                for (int i = 0; i < courseBox.getItemCount(); i++) {
                    if (courseBox.getItemAt(i).startsWith(previous + " | ")) {
                        courseBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
            refreshStudents();
        } catch (SQLException e) { showError(e); }
    }

    private void refreshStudents() {
        studentsModel.setRowCount(0);
        String course = selectedCourse();
        if (course == null) return;
        String sql = """
            SELECT s.student_id, s.first_name, s.last_name, e.course_id,
                   (SELECT r.score FROM results r WHERE r.student_id=e.student_id
                     AND r.course_id=e.course_id ORDER BY r.result_id DESC LIMIT 1) AS score,
                   (SELECT r.grade FROM results r WHERE r.student_id=e.student_id
                     AND r.course_id=e.course_id ORDER BY r.result_id DESC LIMIT 1) AS grade,
                   (SELECT a.status FROM attendance a WHERE a.student_id=e.student_id
                     AND a.course_id=e.course_id ORDER BY a.attendance_date DESC,
                     a.attendance_id DESC LIMIT 1) AS attendance
              FROM enrollments e JOIN students s ON s.student_id=e.student_id
              JOIN courses c ON c.course_id=e.course_id
             WHERE e.course_id=? AND c.teacher_id=? ORDER BY s.student_id
            """;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, course);
            statement.setString(2, teacherId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    studentsModel.addRow(new Object[]{
                        result.getString("student_id"),
                        result.getString("first_name") + " " + result.getString("last_name"),
                        result.getString("course_id"), result.getObject("score"),
                        result.getString("grade"), result.getString("attendance")
                    });
                }
            }
        } catch (SQLException e) { showError(e); }
    }

    private void saveResult() {
        String course = selectedCourse();
        String student = selectedStudent();
        if (course == null || student == null) return;
        String value = JOptionPane.showInputDialog(this, "Score (0–100):");
        if (value == null) return;
        try {
            double score = Double.parseDouble(value.trim());
            if (!Double.isFinite(score) || score < 0 || score > 100) {
                throw new IllegalArgumentException("Score must be between 0 and 100.");
            }
            String grade = score >= 70 ? "A" : score >= 60 ? "B" :
                           score >= 50 ? "C" : score >= 45 ? "D" :
                           score >= 40 ? "E" : "F";
            try (Connection connection = StudentDatabase.connect()) {
                connection.setAutoCommit(false);
                try {
                    String update = """
                        UPDATE results SET score=?, grade=?
                        WHERE student_id=? AND course_id=?
                        AND EXISTS (SELECT 1 FROM courses WHERE course_id=? AND teacher_id=?)
                        """;
                    int changed;
                    try (PreparedStatement statement = connection.prepareStatement(update)) {
                        statement.setDouble(1, score); statement.setString(2, grade);
                        statement.setString(3, student); statement.setString(4, course);
                        statement.setString(5, course); statement.setString(6, teacherId);
                        changed = statement.executeUpdate();
                    }
                    if (changed == 0) {
                        String insert = """
                            INSERT INTO results (student_id, course_id, score, grade)
                            SELECT e.student_id, e.course_id, ?, ? FROM enrollments e
                            JOIN courses c ON c.course_id=e.course_id
                            WHERE e.student_id=? AND e.course_id=? AND c.teacher_id=?
                            """;
                        try (PreparedStatement statement = connection.prepareStatement(insert)) {
                            statement.setDouble(1, score); statement.setString(2, grade);
                            statement.setString(3, student); statement.setString(4, course);
                            statement.setString(5, teacherId);
                            changed = statement.executeUpdate();
                        }
                    }
                    if (changed == 0) throw new SecurityException("Course assignment is no longer valid.");
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }
            refreshStudents();
            JOptionPane.showMessageDialog(this, "Result saved.");
        } catch (SQLException | RuntimeException e) { showError(e); }
    }

    private void saveAttendance() {
        String course = selectedCourse();
        String student = selectedStudent();
        if (course == null || student == null) return;
        Object status = JOptionPane.showInputDialog(this, "Attendance for " + LocalDate.now(),
                "Mark attendance", JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"PRESENT", "ABSENT"}, "PRESENT");
        if (status == null) return;
        String sql = """
            INSERT INTO attendance (student_id, course_id, attendance_date, status)
            SELECT e.student_id, e.course_id, ?, ? FROM enrollments e
            JOIN courses c ON c.course_id=e.course_id
            WHERE e.student_id=? AND e.course_id=? AND c.teacher_id=?
            AND NOT EXISTS (SELECT 1 FROM attendance a
              WHERE a.student_id=e.student_id AND a.course_id=e.course_id
                AND a.attendance_date=?)
            """;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String date = LocalDate.now().toString();
            statement.setString(1, date); statement.setString(2, status.toString());
            statement.setString(3, student); statement.setString(4, course);
            statement.setString(5, teacherId); statement.setString(6, date);
            int changed = statement.executeUpdate();
            JOptionPane.showMessageDialog(this, changed == 1
                    ? "Attendance recorded." : "Already recorded, or course access changed.");
            refreshStudents();
        } catch (SQLException e) { showError(e); }
    }

    private void showError(Exception error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
