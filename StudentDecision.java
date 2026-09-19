import java.util.Scanner;
import java.time.LocalDate;
import java.time.Period;

public class StudentDecision {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.println("================================");
        System.out.println("    SCHOOL MANAGEMENT SYSTEM");
        System.out.println("       DECISION MAKING");
        System.out.println("================================");

        // Collect student information

        System.out.print("Enter Student ID: ");
        String studentId = input.nextLine();

        System.out.print("Enter First Name: ");
        String firstName = input.nextLine();

        System.out.print("Enter Last Name: ");
        String lastName = input.nextLine();

        System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
        LocalDate dateOfBirth = LocalDate.parse(input.nextLine());

        System.out.print("Enter Level: ");
        int level = input.nextInt();

        System.out.print("Enter Examination Score: ");
        int score = input.nextInt();

        // Calculate age

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        String fullName = firstName.trim() + " " + lastName.trim();

        // Validate student level
        // Example levels: 100 to 500

        boolean validLevel;

        switch (level) {

            case 100:
            case 200:
            case 300:
            case 400:
            case 500:
                validLevel = true;
                break;

            default:
                validLevel = false;
        }

        // Check age and level using nested if

        if (age >= 16) {

            if (validLevel) {
                System.out.println("Student meets registration requirements.");
            } else {
                System.out.println("Invalid student level.");
            }

        } else {
            System.out.println("Student must be at least 16 years old.");
        }

        // Ternary operator

        String status = (age >= 16 && validLevel)
                ? "REGISTERED"
                : "NOT ELIGIBLE";

        // Calculate grade

        String grade;

        if (score < 0 || score > 100) {
            grade = "INVALID SCORE";
        } else if (score >= 70) {
            grade = "A";
        } else if (score >= 60) {
            grade = "B";
        } else if (score >= 50) {
            grade = "C";
        } else if (score >= 45) {
            grade = "D";
        } else if (score >= 40) {
            grade = "E";
        } else {
            grade = "F";
        }

        // Display results

        System.out.println();
        System.out.println("================================");
        System.out.println("        STUDENT RESULT");
        System.out.println("================================");

        System.out.println("Student ID: " + studentId);
        System.out.println("Full Name: " + fullName);
        System.out.println("Age: " + age);
        System.out.println("Level: " + level);
        System.out.println("Registration Status: " + status);
        System.out.println("Score: " + score);
        System.out.println("Grade: " + grade);

        System.out.println("================================");

        input.close();
    }
} 