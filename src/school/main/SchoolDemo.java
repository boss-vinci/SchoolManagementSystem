package school.main;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import school.model.SchoolData.*;
import school.exception.SchoolExceptions.*;
import school.service.Repository;

public class SchoolDemo {

    public static <T extends Identifiable> void displayItem(T item) {
        System.out.println("Record ID: " + item.getId());
    }

    public static void displayIds(List<? extends Identifiable> items) {

        for (Identifiable item : items) {
            System.out.println(item.getId());
        }
    }

    public static Student findStudent(
            Repository<Student> repository, String id)
            throws StudentNotFoundException {

        Student student = repository.findById(id);

        if (student == null) {
            throw new StudentNotFoundException(
                "Student not found: " + id
            );
        }

        return student;
    }

    public static Grade calculateGrade(double score) {

        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new InvalidScoreException("Invalid score");
        }

        if (score >= 70) return Grade.A;
        if (score >= 60) return Grade.B;
        if (score >= 50) return Grade.C;
        if (score >= 45) return Grade.D;
        if (score >= 40) return Grade.E;

        return Grade.F;
    }

    public static void main(String[] args) {

        System.out.println("SCHOOL MANAGEMENT SYSTEM");

        Repository<Student> students =
            new Repository.InMemory<>();

        Student student1 = new Student(
            "ST001",
            "Vincent Michael",
            Gender.MALE,
            StudentLevel.LEVEL_100
        );

        Student student2 = new Student(
            "ST002",
            "Jane Williams",
            Gender.FEMALE,
            StudentLevel.LEVEL_200
        );

        students.save(student1);
        students.save(student2);

        Course course = new Course(
            "SEN101",
            "Java Programming"
        );

        Teacher teacher = new Teacher(
            "TC001",
            "John Smith",
            course
        );

        teacher.display();

        Department department = new Department(
            "Software Engineering"
        );

        department.addStudent(student1);
        department.addStudent(student2);

        department.display();

        Set<String> registrations = new HashSet<>();

        registrations.add("SEN101");
        registrations.add("SEN102");
        registrations.add("SEN101");

        System.out.println(
            "Unique Course Registrations: " +
            registrations.size()
        );

        Queue<Student> serviceQueue = new ArrayDeque<>();

        serviceQueue.add(student1);
        serviceQueue.add(student2);

        System.out.println(
            "Serving Student: " +
            serviceQueue.remove().getName()
        );

        Deque<String> history = new ArrayDeque<>();

        history.push("Registered ST001");
        history.push("Registered ST002");

        System.out.println(
            "Last Operation: " + history.pop()
        );

        PaymentTransaction transaction =
            new PaymentTransaction(student1);

        transaction.addPayment(
            50000,
            PaymentMethod.TRANSFER
        );

        transaction.addPayment(
            25000,
            PaymentMethod.CASH
        );

        System.out.println(
            "Student: " + transaction.getStudentId()
        );

        System.out.println(
            "Total Payment: NGN " + transaction.getTotal()
        );

        List<Student> allStudents = students.findAll();

        Iterator<Student> iterator = allStudents.iterator();

        while (iterator.hasNext()) {
            iterator.next().display();
        }

        displayItem(student1);
        displayItem(course);
        displayIds(allStudents);

        System.out.println(
            "Grade: " + calculateGrade(85)
        );

        try {

            Student found = findStudent(students, "ST999");

            found.display();

        } catch (StudentNotFoundException e) {

            System.out.println(e.getMessage());

        } finally {

            System.out.println("Student search completed.");

        }

        try {

            calculateGrade(150);

        } catch (InvalidScoreException e) {

            System.out.println(e.getMessage());

        }

        System.out.println(
            "Total Students: " + allStudents.size()
        );

        System.out.println("Program completed successfully.");
    }
} 