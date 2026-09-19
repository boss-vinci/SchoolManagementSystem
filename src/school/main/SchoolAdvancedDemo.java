package school.main;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class SchoolAdvancedDemo {

    static class StudentRecord implements Comparable<StudentRecord>, Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String department;
        private int level;
        private double gpa;
        private LocalDate dateOfBirth;

        public StudentRecord(String id, String name, String department,
                             int level, double gpa, LocalDate dateOfBirth) {

            this.id = id;
            this.name = name;
            this.department = department;
            this.level = level;
            this.gpa = gpa;
            this.dateOfBirth = dateOfBirth;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDepartment() {
            return department;
        }

        public int getLevel() {
            return level;
        }

        public double getGpa() {
            return gpa;
        }

        public int getAge() {
            return Period.between(dateOfBirth, LocalDate.now()).getYears();
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        @Override
        public int compareTo(StudentRecord other) {
            return id.compareTo(other.id);
        }

        @Override
        public String toString() {
            return id + " | " + name + " | " +
                   department + " | Level " + level +
                   " | GPA: " + gpa;
        }

        public String toFileRecord() {
            return id + "|" + name + "|" + department + "|" +
                   level + "|" + gpa + "|" + dateOfBirth;
        }
    }

    static Comparator<StudentRecord> getComparator(int choice) {

        switch (choice) {

            case 1:
                return Comparator.comparing(StudentRecord::getId);

            case 2:
                return Comparator.comparing(StudentRecord::getName);

            case 3:
                return Comparator.comparingInt(StudentRecord::getAge);

            case 4:
                return Comparator.comparingDouble(StudentRecord::getGpa);

            case 5:
                return Comparator.comparing(StudentRecord::getDepartment);

            case 6:
                return Comparator.comparingInt(StudentRecord::getLevel);

            default:
                return Comparator.comparing(StudentRecord::getId);
        }
    }

    static void displayStudents(List<StudentRecord> students) {

        for (StudentRecord student : students) {
            System.out.println(student);
        }
    }

    static void sortingDemo(List<StudentRecord> students) {

        System.out.println("\nSORT STUDENTS");

        System.out.println("1. Student ID");
        System.out.println("2. Name");
        System.out.println("3. Age");
        System.out.println("4. GPA");
        System.out.println("5. Department");
        System.out.println("6. Level");

        Scanner input = new Scanner(System.in);

        System.out.print("Choose sorting method: ");

        int choice;

        try {
            choice = Integer.parseInt(input.nextLine());
        } catch (NumberFormatException e) {
            choice = 1;
        }

        List<StudentRecord> sorted = new ArrayList<>(students);

        sorted.sort(getComparator(choice));

        displayStudents(sorted);

        List<StudentRecord> naturalOrder = new ArrayList<>(students);

        Collections.sort(naturalOrder);

        System.out.println("\nNatural ID Order:");

        displayStudents(naturalOrder);
    }

    static void lambdaDemo(List<StudentRecord> students) {

        System.out.println("\nLAMBDA OPERATIONS");

        Predicate<StudentRecord> highGpa =
            student -> student.getGpa() > 3.5;

        Consumer<StudentRecord> printer =
            student -> System.out.println(student);

        Function<StudentRecord, String> nameExtractor =
            student -> student.getName();

        Supplier<String> schoolName =
            () -> "School Management System";

        System.out.println(schoolName.get());

        System.out.println("\nStudents with GPA above 3.5:");

        students.stream()
                .filter(highGpa)
                .forEach(printer);

        System.out.println("\nStudent Names:");

        students.stream()
                .map(nameExtractor)
                .forEach(System.out::println);

        System.out.println("\nSoftware Engineering Students:");

        students.stream()
                .filter(s -> s.getDepartment().equalsIgnoreCase(
                    "Software Engineering"))
                .forEach(printer);

        System.out.println("\nNames Starting With A:");

        students.stream()
                .filter(s -> s.getName().startsWith("A"))
                .forEach(printer);
    }

    static void streamDemo(List<StudentRecord> students) {

        System.out.println("\nSTREAM PROCESSING");

        long count = students.stream().count();

        double average = students.stream()
                .mapToDouble(StudentRecord::getGpa)
                .average()
                .orElse(0);

        double highest = students.stream()
                .mapToDouble(StudentRecord::getGpa)
                .max()
                .orElse(0);

        double lowest = students.stream()
                .mapToDouble(StudentRecord::getGpa)
                .min()
                .orElse(0);

        double total = students.stream()
                .map(StudentRecord::getGpa)
                .reduce(0.0, Double::sum);

        System.out.println("Total Students: " + count);
        System.out.println("Average GPA: " + average);
        System.out.println("Highest GPA: " + highest);
        System.out.println("Lowest GPA: " + lowest);
        System.out.println("Total GPA: " + total);

        List<StudentRecord> sorted = students.stream()
                .sorted(Comparator.comparingDouble(
                    StudentRecord::getGpa).reversed())
                .collect(Collectors.toList());

        System.out.println("\nStudents Sorted by GPA:");

        displayStudents(sorted);

        Map<String, List<StudentRecord>> byDepartment =
            students.stream().collect(
                Collectors.groupingBy(StudentRecord::getDepartment)
            );

        System.out.println("\nGrouped by Department:");

        byDepartment.forEach((department, list) -> {

            System.out.println(department);

            list.forEach(System.out::println);
        });

        Map<Integer, List<StudentRecord>> byLevel =
            students.stream().collect(
                Collectors.groupingBy(StudentRecord::getLevel)
            );

        System.out.println("\nGrouped by Level:");

        byLevel.forEach((level, list) -> {

            System.out.println("Level " + level);

            list.forEach(System.out::println);
        });
    }

    static void dateDemo() {

        System.out.println("\nDATE AND TIME");

        LocalDate today = LocalDate.now();

        LocalTime currentTime = LocalTime.now();

        LocalDateTime registrationDate = LocalDateTime.now();

        LocalDate examinationDate = today.plusDays(30);

        LocalDate paymentDueDate = today.plusDays(14);

        LocalDate semesterEnd = today.plusMonths(4);

        Period semesterDuration = Period.between(today, semesterEnd);

        Duration duration = Duration.between(
            currentTime, currentTime.plusHours(2)
        );

        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

        System.out.println("Today: " + today.format(formatter));

        System.out.println("Current Time: " + currentTime);

        System.out.println("Registration Date: " + registrationDate);

        System.out.println("Examination Date: " + examinationDate);

        System.out.println(
            "Days Until Examination: " +
            java.time.temporal.ChronoUnit.DAYS.between(
                today, examinationDate)
        );

        System.out.println("Payment Due: " + paymentDueDate);

        System.out.println(
            "Semester Duration: " + semesterDuration.toTotalMonths()
            + " months"
        );

        System.out.println(
            "Session Duration: " + duration.toHours() + " hours"
        );
    }

    static void fileDemo(List<StudentRecord> students)
            throws IOException {

        System.out.println("\nFILE MANAGEMENT");

        Path directory = Path.of("data");

        Files.createDirectories(directory);

        Path file = directory.resolve("students.txt");

        List<String> records = new ArrayList<>();

        for (StudentRecord student : students) {
            records.add(student.toFileRecord());
        }

        Files.write(
            file,
            records,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        String extraRecord =
            "ST004|Zainab Musa|Computer Science|300|3.6|2004-04-12";

        Files.writeString(
            file,
            extraRecord + System.lineSeparator(),
            StandardOpenOption.APPEND
        );

        System.out.println("Records saved successfully.");

        System.out.println("\nReading Records:");

        List<String> savedRecords = Files.readAllLines(file);

        savedRecords.forEach(System.out::println);

        System.out.println("\nSearching ST002:");

        for (String record : savedRecords) {

            if (record.startsWith("ST002|")) {
                System.out.println(record);
            }
        }

        List<String> updated = new ArrayList<>();

        for (String record : savedRecords) {

            if (record.startsWith("ST002|")) {

                String[] fields = record.split("\\|", -1);

                fields[4] = "4.0";

                record = String.join("|", fields);
            }

            updated.add(record);
        }

        Files.write(
            file,
            updated,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println("Student ST002 updated.");

        updated.removeIf(record -> record.startsWith("ST004|"));

        Files.write(
            file,
            updated,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println("Student ST004 deleted.");

        System.out.println("\nFinal File Contents:");

        try (BufferedReader reader =
                 Files.newBufferedReader(file)) {

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    static void serializationDemo(List<StudentRecord> students)
            throws IOException, ClassNotFoundException {

        System.out.println("\nSERIALIZATION");

        Path directory = Path.of("data");

        Files.createDirectories(directory);

        Path file = directory.resolve("students.ser");

        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(file))) {

            output.writeObject(students);
        }

        System.out.println("Objects saved successfully.");

        try (ObjectInputStream input = new ObjectInputStream(
                Files.newInputStream(file))) {

            Object restored = input.readObject();

            if (restored instanceof List<?>) {

                List<?> records = (List<?>) restored;

                System.out.println("Restored Objects:");

                records.forEach(System.out::println);
            }
        }
    }

    public static void main(String[] args) {

        List<StudentRecord> students = new ArrayList<>();

        students.add(new StudentRecord(
            "ST001", "Ada Johnson", "Software Engineering",
            100, 3.8, LocalDate.of(2005, 3, 10)
        ));

        students.add(new StudentRecord(
            "ST002", "Chidi Okafor", "Computer Science",
            200, 3.2, LocalDate.of(2004, 7, 15)
        ));

        students.add(new StudentRecord(
            "ST003", "Amaka James", "Software Engineering",
            100, 4.5, LocalDate.of(2006, 1, 20)
        ));

        System.out.println("SCHOOL MANAGEMENT SYSTEM");

        sortingDemo(students);

        lambdaDemo(students);

        streamDemo(students);

        dateDemo();

        try {

            fileDemo(students);

            serializationDemo(students);

        } catch (IOException | ClassNotFoundException e) {

            System.out.println("Storage error: " + e.getMessage());
        }

        System.out.println("\nAdvanced module completed successfully.");
    }
} 