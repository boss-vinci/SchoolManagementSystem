interface Payable {

    double calculatePayment();

    default void showPayment() {
        System.out.println("Payment information available.");
    }

    static void paymentMessage() {
        System.out.println("School Payment System");
    }
}

interface Printable {
    void print();
}

interface Searchable {
    boolean matchesId(String id);
}

abstract class Person {

    private String id;
    private String firstName;
    private String lastName;
    private String gender;
    private String phone;
    private String email;

    private static int totalPeople = 0;

    public Person(String id, String firstName,
                  String lastName, String gender,
                  String phone, String email) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;

        totalPeople++;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static int getTotalPeople() {
        return totalPeople;
    }

    public void displayInfo() {
        System.out.println("ID: " + id);
        System.out.println("Name: " + getFullName());
        System.out.println("Gender: " + gender);
        System.out.println("Phone: " + phone);
        System.out.println("Email: " + email);
    }

    public abstract void displayRole();

    public abstract double calculatePayment();
}

class Department {

    private String departmentName;

    public Department(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String name) {
        this.departmentName = name;
    }

    public void displayDepartment() {
        System.out.println("Department: " + departmentName);
    }
}

class Course {

    private String courseCode;
    private String courseTitle;

    public Course(String courseCode, String courseTitle) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String title) {
        this.courseTitle = title;
    }

    public void displayCourse() {
        System.out.println(courseCode + " - " + courseTitle);
    }
}

class Student extends Person implements Payable, Searchable {

    private Department department;
    private int level;
    private double schoolFees;

    public Student(String id, String firstName,
                   String lastName, String gender,
                   String phone, String email,
                   Department department,
                   int level, double schoolFees) {

        super(id, firstName, lastName, gender, phone, email);

        this.department = department;
        this.level = level;
        this.schoolFees = schoolFees;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public void displayRole() {
        System.out.println("Role: STUDENT");
        displayInfo();
        department.displayDepartment();
        System.out.println("Level: " + level);
    }

    @Override
    public double calculatePayment() {
        return schoolFees;
    }

    @Override
    public boolean matchesId(String id) {
        return getId().equalsIgnoreCase(id);
    }
}

class Teacher extends Person implements Payable, Printable {

    private String subject;
    private double salary;

    public Teacher(String id, String firstName,
                   String lastName, String gender,
                   String phone, String email,
                   String subject, double salary) {

        super(id, firstName, lastName, gender, phone, email);

        this.subject = subject;
        this.salary = salary;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public void displayRole() {
        System.out.println("Role: TEACHER");
        displayInfo();
        System.out.println("Subject: " + subject);
    }

    @Override
    public double calculatePayment() {
        return salary;
    }

    @Override
    public void print() {
        System.out.println("Teacher: " + getFullName());
        System.out.println("Subject: " + subject);
    }
}

class SeniorTeacher extends Teacher {

    private double bonus;

    public SeniorTeacher(String id, String firstName,
                         String lastName, String gender,
                         String phone, String email,
                         String subject, double salary,
                         double bonus) {

        super(id, firstName, lastName, gender, phone, email,
              subject, salary);

        this.bonus = bonus;
    }

    @Override
    public void displayRole() {
        System.out.println("Role: SENIOR TEACHER");
        displayInfo();
        System.out.println("Subject: " + getSubject());
    }

    @Override
    public double calculatePayment() {
        return getSalary() + bonus;
    }
}

class Administrator extends Person {

    private double salary;

    public Administrator(String id, String firstName,
                         String lastName, String gender,
                         String phone, String email,
                         double salary) {

        super(id, firstName, lastName, gender, phone, email);

        this.salary = salary;
    }

    @Override
    public void displayRole() {
        System.out.println("Role: ADMINISTRATOR");
        displayInfo();
    }

    @Override
    public double calculatePayment() {
        return salary;
    }
}

class Result {

    private double score;

    public Result(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String calculateGrade() {

        if (score >= 70) return "A";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        if (score >= 45) return "D";
        if (score >= 40) return "E";

        return "F";
    }
}

class Payment {

    private double amount;

    public Payment(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void displayPayment() {
        System.out.println("Payment: NGN " + amount);
    }
}

class Attendance {

    private boolean present;

    public Attendance(boolean present) {
        this.present = present;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public void displayAttendance() {
        System.out.println(present ? "Present" : "Absent");
    }
}

class Report implements Printable {

    @Override
    public void print() {
        System.out.println("School Report");
    }

    public void print(String title) {
        System.out.println(title);
    }

    public void print(String title, boolean detailed) {
        System.out.println(title);

        if (detailed) {
            System.out.println("Detailed report enabled.");
        }
    }
}

public class SchoolOOPDemo {

    public static void main(String[] args) {

        System.out.println("==============================");
        System.out.println("  SCHOOL MANAGEMENT SYSTEM");
        System.out.println("       OOP DEMONSTRATION");
        System.out.println("==============================");

        Department department =
            new Department("Software Engineering");

        Student student = new Student(
            "ST001",
            "Vincent",
            "Michael",
            "Male",
            "08012345678",
            "student@example.com",
            department,
            100,
            180000
        );

        Teacher teacher = new Teacher(
            "TC001",
            "John",
            "Smith",
            "Male",
            "08098765432",
            "teacher@example.com",
            "Java Programming",
            250000
        );

        Administrator admin = new Administrator(
            "AD001",
            "Grace",
            "Williams",
            "Female",
            "08055555555",
            "admin@example.com",
            300000
        );

        SeniorTeacher seniorTeacher = new SeniorTeacher(
            "TC002",
            "James",
            "Brown",
            "Male",
            "08066666666",
            "senior@example.com",
            "Software Engineering",
            300000,
            50000
        );

        Person[] people = {
            student,
            teacher,
            admin,
            seniorTeacher
        };

        for (Person person : people) {

            System.out.println("\n------------------");

            person.displayRole();

            System.out.println(
                "Payment Amount: NGN " +
                person.calculatePayment()
            );
        }

        Person person = student;

        System.out.println("\nUPCASTING:");

        person.displayRole();

        if (person instanceof Student) {

            Student s = (Student) person;

            System.out.println("DOWNCASTING:");

            System.out.println(
                "Student Level: " + s.getLevel()
            );
        }

        System.out.println("\nINTERFACES:");

        Payable.paymentMessage();

        student.showPayment();

        System.out.println(
            "Student Payment: NGN " +
            student.calculatePayment()
        );

        System.out.println(
            "Student Found: " +
            student.matchesId("ST001")
        );

        teacher.print();

        System.out.println("\nOTHER SCHOOL OBJECTS:");

        Course course = new Course(
            "SEN101",
            "Introduction to Java"
        );

        course.displayCourse();

        Result result = new Result(85);

        System.out.println(
            "Grade: " + result.calculateGrade()
        );

        Payment payment = new Payment(50000);

        payment.displayPayment();

        Attendance attendance = new Attendance(true);

        attendance.displayAttendance();

        Report report = new Report();

        report.print();

        report.print("Student Report");

        report.print("Detailed Student Report", true);

        System.out.println(
            "\nTotal People Created: " +
            Person.getTotalPeople()
        );

        System.out.println("\nOOP Demonstration Completed!");
    }
} 