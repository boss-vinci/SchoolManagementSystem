package school.main;

import school.controller.StudentController;
import school.factory.UserFactory;
import school.model.user.SystemUser;
import school.util.DatabaseManager;

import javax.swing.*;
import java.awt.*;

/**
 * Read-only architecture screen used to demonstrate the implemented patterns.
 */
public final class DesignPatternsDialog extends JDialog {

    private final JTextArea output = new JTextArea();

    public DesignPatternsDialog(Frame owner) {
        super(owner, "System Architecture", true);

        setSize(900, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));

        JLabel heading = new JLabel(
            "SYSTEM ARCHITECTURE & DESIGN PATTERNS",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JTextArea description = new JTextArea(patternDescription());
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        description.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setRows(8);
        output.setText(
            "Architecture ready. Click Run Architecture Check to verify the live implementations.\n"
        );

        JButton check = new JButton("Run Architecture Check");
        JButton close = new JButton("Close");

        check.addActionListener(e -> runChecks());
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(check);
        buttons.add(close);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(new JScrollPane(description), BorderLayout.CENTER);
        center.add(new JScrollPane(output), BorderLayout.SOUTH);

        add(heading, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        setVisible(true);
    }

    private String patternDescription() {
        return """
            SINGLETON — DatabaseManager
            One shared database manager provides JDBC connections to the application.
            Why: centralizes connection configuration and avoids creating multiple manager objects.

            FACTORY — UserFactory
            Creates AdminUser, TeacherUser, StudentUser or AccountantUser from the authenticated role.
            Why: user-object creation is centralized instead of spreading role-specific construction logic.

            REPOSITORY — StudentRepository / JdbcStudentRepository
            The repository contains student SQL and persistence operations.
            Why: the GUI and business layer no longer need to know JDBC query details.

            SERVICE LAYER — StudentService
            Contains student validation, registration, update and deletion business rules.
            Why: business rules stay independent from the Swing user interface and database implementation.

            MVC — StudentRecord + SchoolApp + StudentController
            Model: StudentRecord
            View: the Swing student-management screen in SchoolApp
            Controller: StudentController
            Why: separates student data, user-interface rendering and application actions.
            """;
    }

    private void runChecks() {
        StringBuilder text = new StringBuilder();

        DatabaseManager first = DatabaseManager.getInstance();
        DatabaseManager second = DatabaseManager.getInstance();

        text.append("Singleton: ")
            .append(first == second ? "PASS - one DatabaseManager instance" : "FAIL")
            .append('\n');

        SystemUser admin = UserFactory.create("demo-admin", "ADMIN", null);
        SystemUser teacher = UserFactory.create("demo-teacher", "TEACHER", "T001");
        SystemUser student = UserFactory.create("demo-student", "STUDENT", "ST001");
        SystemUser accountant = UserFactory.create("demo-accountant", "ACCOUNTANT", null);

        text.append("Factory: PASS - created ")
            .append(admin.getClass().getSimpleName()).append(", ")
            .append(teacher.getClass().getSimpleName()).append(", ")
            .append(student.getClass().getSimpleName()).append(", ")
            .append(accountant.getClass().getSimpleName()).append('\n');

        try {
            int count = StudentController.createDefault().getStudents().size();
            text.append("Repository + Service + MVC: PASS - controller loaded ")
                .append(count)
                .append(" student record(s) through the layered architecture.\n");
        } catch (Exception e) {
            text.append("Repository + Service + MVC: ERROR - ")
                .append(e.getMessage())
                .append('\n');
        }

        output.setText(text.toString());
    }
}
