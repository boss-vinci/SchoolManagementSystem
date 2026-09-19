import java.util.Scanner;

public class StudentResults {

    static final int MAX_STUDENTS = 50;
    static final int MAX_COURSES = 10;

    Scanner input = new Scanner(System.in);

    String[] studentIds = new String[MAX_STUDENTS];
    String[] studentNames = new String[MAX_STUDENTS];

    double[][] scores = new double[MAX_STUDENTS][MAX_COURSES];

    int[] courseCounts = new int[MAX_STUDENTS];

    int studentCount = 0;

    public static void main(String[] args) {

        StudentResults system = new StudentResults();

        system.start();

    }

    // MAIN MENU

    void start() {

        int choice;

        while (true) {

            System.out.println("\n==================================");
            System.out.println("     SCHOOL MANAGEMENT SYSTEM");
            System.out.println("       RESULT MANAGEMENT");
            System.out.println("==================================");

            System.out.println("1. Register Student");
            System.out.println("2. Display All Students");
            System.out.println("3. Search Student");
            System.out.println("4. Highest and Lowest Student");
            System.out.println("5. Overall Average");
            System.out.println("6. Sort Students by Average");
            System.out.println("0. Exit");

            choice = readInt("Enter choice: ");

            switch (choice) {

                case 1:
                    registerStudent();
                    break;

                case 2:
                    displayStudents();
                    break;

                case 3:
                    searchStudent();
                    break;

                case 4:
                    highestLowest();
                    break;

                case 5:
                    overallAverage();
                    break;

                case 6:
                    sortStudents();
                    break;

                case 0:
                    System.out.println("Goodbye!");
                    input.close();
                    return;

                default:
                    System.out.println("Invalid choice!");
                    continue;
            }
        }
    }

    // REGISTER STUDENT

    void registerStudent() {

        if (studentCount >= MAX_STUDENTS) {
            System.out.println("Student storage is full.");
            return;
        }

        System.out.print("Enter Student ID: ");
        String id = input.nextLine().trim();

        if (id.isEmpty() || findStudent(id) != -1) {
            System.out.println("Invalid or duplicate Student ID!");
            return;
        }

        System.out.print("Enter Student Name: ");
        String name = input.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Student name cannot be empty.");
            return;
        }

        int numberOfCourses;

        do {

            numberOfCourses = readInt("Number of courses (1-10): ");

            if (numberOfCourses < 1 ||
                numberOfCourses > MAX_COURSES) {

                System.out.println("Invalid number of courses.");

            }

        } while (numberOfCourses < 1 ||
                 numberOfCourses > MAX_COURSES);

        studentIds[studentCount] = id;
        studentNames[studentCount] = name;

        courseCounts[studentCount] = numberOfCourses;

        // ENTER COURSE SCORES

        for (int i = 0; i < numberOfCourses; i++) {

            double score;

            do {

                score = readDouble("Enter Course " +
                        (i + 1) + " score: ");

                if (score < 0 || score > 100) {

                    System.out.println(
                        "Score must be between 0 and 100."
                    );

                    continue;
                }

                break;

            } while (true);

            scores[studentCount][i] = score;
        }

        studentCount++;

        System.out.println("Student registered successfully!");

    }

    // DISPLAY ALL STUDENTS

    void displayStudents() {

        if (studentCount == 0) {
            System.out.println("No students registered.");
            return;
        }

        for (int i = 0; i < studentCount; i++) {

            displayResult(i);

        }

    }

    // SEARCH STUDENT

    int findStudent(String id) {

        for (int i = 0; i < studentCount; i++) {

            if (studentIds[i].equalsIgnoreCase(id)) {
                return i;
            }
        }

        return -1;
    }

    void searchStudent() {

        System.out.print("Enter Student ID: ");

        String id = input.nextLine();

        displayResult(id);

    }

    // METHOD OVERLOADING

    void displayResult(String id) {

        int index = findStudent(id);

        if (index == -1) {
            System.out.println("Student not found!");
            return;
        }

        displayResult(index);

    }

    void displayResult(int index) {

        double total = 0;
        int passed = 0;
        int failed = 0;

        double highest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;

        System.out.println("\n----------------------------");

        System.out.println("Student ID: " + studentIds[index]);
        System.out.println("Name: " + studentNames[index]);

        // TRAVERSE COURSE SCORES

        for (int j = 0; j < courseCounts[index]; j++) {

            double score = scores[index][j];

            System.out.println(
                "Course " + (j + 1) + ": " + score
            );

            total += score;

            if (score > highest) {
                highest = score;
            }

            if (score < lowest) {
                lowest = score;
            }

            if (score >= 40) {
                passed++;
            } else {
                failed++;
            }
        }

        double average = calculateAverage(index);

        System.out.println("Total Score: " + total);
        System.out.println("Average Score: " + average);
        System.out.println("Highest Score: " + highest);
        System.out.println("Lowest Score: " + lowest);

        System.out.println("Courses Passed: " + passed);
        System.out.println("Courses Failed: " + failed);

        System.out.println(
            "Grade: " + calculateGrade(average)
        );

        System.out.println("----------------------------");
    }

    // CALCULATE AVERAGE

    double calculateAverage(int index) {

        double total = 0;

        for (int i = 0; i < courseCounts[index]; i++) {

            total += scores[index][i];

        }

        return total / courseCounts[index];

    }

    // STATIC METHOD

    static String calculateGrade(double score) {

        if (score >= 70) {
            return "A";
        } else if (score >= 60) {
            return "B";
        } else if (score >= 50) {
            return "C";
        } else if (score >= 45) {
            return "D";
        } else if (score >= 40) {
            return "E";
        } else {
            return "F";
        }

    }

    // HIGHEST AND LOWEST STUDENT

    void highestLowest() {

        if (studentCount == 0) {
            System.out.println("No students registered.");
            return;
        }

        int highestIndex = 0;
        int lowestIndex = 0;

        for (int i = 1; i < studentCount; i++) {

            if (calculateAverage(i) >
                calculateAverage(highestIndex)) {

                highestIndex = i;
            }

            if (calculateAverage(i) <
                calculateAverage(lowestIndex)) {

                lowestIndex = i;
            }
        }

        System.out.println("\nHighest Average Student:");
        displayResult(highestIndex);

        System.out.println("\nLowest Average Student:");
        displayResult(lowestIndex);

    }

    // OVERALL AVERAGE

    void overallAverage() {

        if (studentCount == 0) {
            System.out.println("No students registered.");
            return;
        }

        double total = 0;

        for (int i = 0; i < studentCount; i++) {

            total += calculateAverage(i);

        }

        System.out.println(
            "Overall Student Average: " +
            total / studentCount
        );

    }

    // SORT STUDENTS BY AVERAGE

    void sortStudents() {

        for (int i = 0; i < studentCount - 1; i++) {

            for (int j = 0; j < studentCount - i - 1; j++) {

                if (calculateAverage(j) <
                    calculateAverage(j + 1)) {

                    // Swap IDs

                    String tempId = studentIds[j];

                    studentIds[j] = studentIds[j + 1];

                    studentIds[j + 1] = tempId;

                    // Swap names

                    String tempName = studentNames[j];

                    studentNames[j] = studentNames[j + 1];

                    studentNames[j + 1] = tempName;

                    // Swap scores

                    double[] tempScores = scores[j];

                    scores[j] = scores[j + 1];

                    scores[j + 1] = tempScores;

                    // Swap course counts

                    int tempCount = courseCounts[j];

                    courseCounts[j] = courseCounts[j + 1];

                    courseCounts[j + 1] = tempCount;

                }
            }
        }

        System.out.println("Students sorted by average.");

        displayStudents();

    }

    // SAFE INTEGER INPUT

    int readInt(String message) {

        while (true) {

            try {

                System.out.print(message);

                return Integer.parseInt(
                    input.nextLine().trim()
                );

            } catch (NumberFormatException e) {

                System.out.println("Enter a valid number.");

            }
        }
    }

    // SAFE DOUBLE INPUT

    double readDouble(String message) {

        while (true) {

            try {

                System.out.print(message);

                double value = Double.parseDouble(
                    input.nextLine().trim()
                );

                if (Double.isNaN(value) ||
                    Double.isInfinite(value)) {

                    System.out.println("Enter a finite score.");
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {

                System.out.println("Enter a valid score.");

            }
        }
    }

}