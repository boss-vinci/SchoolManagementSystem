package school.model;

import java.util.ArrayList;
import java.util.List;

public class SchoolData {

    public interface Identifiable {
        String getId();
    }

    public enum Gender {
        MALE, FEMALE
    }

    public enum StudentLevel {
        LEVEL_100, LEVEL_200, LEVEL_300, LEVEL_400, LEVEL_500
    }

    public enum StudentStatus {
        ACTIVE, SUSPENDED, GRADUATED
    }

    public enum PaymentMethod {
        CASH, TRANSFER, CARD
    }

    public enum StaffRole {
        TEACHER, ADMINISTRATOR, ACCOUNTANT
    }

    public enum Grade {
        A, B, C, D, E, F
    }

    public static class Student implements Identifiable {

        private String id;
        private String name;
        private Gender gender;
        private StudentLevel level;
        private StudentStatus status;

        public Student(String id, String name,
                       Gender gender, StudentLevel level) {

            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Invalid ID");
            }

            this.id = id;
            this.name = name;
            this.gender = gender;
            this.level = level;
            this.status = StudentStatus.ACTIVE;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public StudentLevel getLevel() {
            return level;
        }

        public void setLevel(StudentLevel level) {
            this.level = level;
        }

        public StudentStatus getStatus() {
            return status;
        }

        public void setStatus(StudentStatus status) {
            this.status = status;
        }

        public void display() {
            System.out.println(id + " - " + name);
            System.out.println("Gender: " + gender);
            System.out.println("Level: " + level);
            System.out.println("Status: " + status);
        }
    }

    public static class Course implements Identifiable {

        private String code;
        private String title;

        public Course(String code, String title) {
            this.code = code;
            this.title = title;
        }

        public String getId() {
            return code;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class Teacher implements Identifiable {

        private String id;
        private String name;
        private Course course;
        private StaffRole role = StaffRole.TEACHER;

        public Teacher(String id, String name, Course course) {
            this.id = id;
            this.name = name;
            this.course = course;
        }

        public String getId() {
            return id;
        }

        public void display() {
            System.out.println(name + " teaches " + course.getTitle());
            System.out.println("Role: " + role);
        }
    }

    public static class Department {

        private String name;
        private List<Student> students = new ArrayList<>();

        public Department(String name) {
            this.name = name;
        }

        public void addStudent(Student student) {
            if (!students.contains(student)) {
                students.add(student);
            }
        }

        public void display() {
            System.out.println("Department: " + name);

            for (Student student : students) {
                student.display();
            }
        }
    }

    public static class PaymentTransaction {

        private Student student;
        private List<PaymentRecord> records = new ArrayList<>();

        public PaymentTransaction(Student student) {
            this.student = student;
        }

        public void addPayment(double amount, PaymentMethod method) {

            if (!Double.isFinite(amount) || amount <= 0) {
                throw new IllegalArgumentException("Invalid payment");
            }

            records.add(new PaymentRecord(amount, method));
        }

        public double getTotal() {

            double total = 0;

            for (PaymentRecord record : records) {
                total += record.amount;
            }

            return total;
        }

        public String getStudentId() {
            return student.getId();
        }

        public static class PaymentRecord {

            private final double amount;
            private final PaymentMethod method;

            private PaymentRecord(double amount, PaymentMethod method) {
                this.amount = amount;
                this.method = method;
            }

            public double getAmount() {
                return amount;
            }

            public PaymentMethod getMethod() {
                return method;
            }
        }
    }
} 