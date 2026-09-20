package school.main;

import school.service.StudentDatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class PaymentManagementDialog extends JDialog {

    private JComboBox<String> studentBox = new JComboBox<>();

    private JTextField amountField = new JTextField();

    private JComboBox<String> methodBox =
        new JComboBox<>(new String[]{
            "CASH", "TRANSFER", "CARD"
        });

    private JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private DefaultTableModel tableModel;
    private JTable paymentTable;

    private JLabel totalLabel =
        new JLabel("Total Payments: NGN 0.00");

    public PaymentManagementDialog(JFrame owner) {

        super(owner, "Payment Management", true);

        setSize(1050, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));

        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel form = new JPanel(new GridLayout(2, 4, 10, 10));

        form.add(new JLabel("Student"));
        form.add(new JLabel("Amount (NGN)"));
        form.add(new JLabel("Payment Method"));
        form.add(new JLabel("Date (YYYY-MM-DD)"));

        form.add(studentBox);
        form.add(amountField);
        form.add(methodBox);
        form.add(dateField);

        tableModel = new DefaultTableModel(
            new String[]{
                "Payment ID",
                "Student ID",
                "Student Name",
                "Amount",
                "Date",
                "Method"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = new JTable(tableModel);

        paymentTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JPanel buttons = new JPanel(new FlowLayout());

        JButton record = new JButton("Record Payment");
        JButton delete = new JButton("Delete Payment");
        JButton refresh = new JButton("Refresh");
        JButton close = new JButton("Close");

        record.addActionListener(e -> recordPayment());
        delete.addActionListener(e -> deletePayment());
        refresh.addActionListener(e -> refreshAll());
        close.addActionListener(e -> dispose());

        buttons.add(record);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);

        JPanel bottom = new JPanel(new BorderLayout());

        bottom.add(buttons, BorderLayout.CENTER);
        bottom.add(totalLabel, BorderLayout.SOUTH);

        main.add(form, BorderLayout.NORTH);

        main.add(
            new JScrollPane(paymentTable),
            BorderLayout.CENTER
        );

        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        refreshAll();

        setVisible(true);
    }

    private void refreshAll() {

        refreshStudents();
        refreshPayments();
    }

    private void refreshStudents() {

        String sql = """
            SELECT student_id, first_name, last_name
            FROM students
            ORDER BY student_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            studentBox.removeAllItems();

            while (result.next()) {

                studentBox.addItem(
                    result.getString("student_id") + " | " +
                    result.getString("first_name") + " " +
                    result.getString("last_name")
                );
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void recordPayment() {

        String student =
            (String) studentBox.getSelectedItem();

        if (student == null) {

            JOptionPane.showMessageDialog(
                this,
                "Register a student first."
            );

            return;
        }

        try {

            double amount = Double.parseDouble(
                amountField.getText().trim()
            );

            if (!Double.isFinite(amount) || amount <= 0) {

                throw new IllegalArgumentException(
                    "Enter a valid positive amount."
                );
            }

            String date =
                LocalDate.parse(dateField.getText().trim()).toString();

            String studentId =
                student.substring(0, student.indexOf(" | "));

            String method =
                (String) methodBox.getSelectedItem();

            String sql = """
                INSERT INTO payments
                (student_id, amount, payment_date, payment_method)
                VALUES (?, ?, ?, ?)
                """;

            try (Connection connection = StudentDatabase.connect();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, studentId);
                statement.setDouble(2, amount);
                statement.setString(3, date);
                statement.setString(4, method);

                statement.executeUpdate();
            }

            JOptionPane.showMessageDialog(
                this,
                "Payment recorded successfully."
            );

            amountField.setText("");

            refreshPayments();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void refreshPayments() {

        String sql = """
            SELECT p.payment_id,
                   p.student_id,
                   s.first_name,
                   s.last_name,
                   p.amount,
                   p.payment_date,
                   p.payment_method
            FROM payments p
            JOIN students s
            ON p.student_id = s.student_id
            ORDER BY p.payment_id DESC
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            tableModel.setRowCount(0);

            while (result.next()) {

                tableModel.addRow(new Object[]{
                    result.getInt("payment_id"),
                    result.getString("student_id"),
                    result.getString("first_name") + " " +
                    result.getString("last_name"),
                    result.getDouble("amount"),
                    result.getString("payment_date"),
                    result.getString("payment_method")
                });
            }

            updateTotal();

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateTotal() {

        String sql = """
            SELECT COALESCE(SUM(amount), 0) AS total
            FROM payments
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {

                totalLabel.setText(
                    String.format(
                        "Total Payments: NGN %,.2f",
                        result.getDouble("total")
                    )
                );
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deletePayment() {

        int row = paymentTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Select a payment first."
            );

            return;
        }

        int modelRow =
            paymentTable.convertRowIndexToModel(row);

        int id = (Integer) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this payment record?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql =
            "DELETE FROM payments WHERE payment_id = ?";

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            statement.executeUpdate();

            refreshPayments();

            JOptionPane.showMessageDialog(
                this,
                "Payment deleted successfully."
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