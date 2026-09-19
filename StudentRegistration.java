import java.util.Scanner;
import java.time.LocalDate;

public class StudentRegistration {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.println("=================================");
        System.out.println("     SCHOOL MANAGEMENT SYSTEM");
        System.out.println("       STUDENT REGISTRATION");
        System.out.println("=================================");

        // Collect student information

        System.out.print("Enter Student ID: ");
        String studentId = input.nextLine();

        System.out.print("Enter First Name: ");
        String firstName = input.nextLine();

        System.out.print("Enter Last Name: ");
        String lastName = input.nextLine();

        System.out.print("Enter Gender: ");
        String gender = input.nextLine();

        System.out.print("Enter Birth Year: ");
        int birthYear = input.nextInt();

        System.out.print("Enter Birth Month: ");
        int birthMonth = input.nextInt();

        System.out.print("Enter Birth Day: ");
        int birthDay = input.nextInt();

        input.nextLine();

        System.out.print("Enter Email: ");
        String email = input.nextLine();

        System.out.print("Enter Phone Number: ");
        String phoneNumber = input.nextLine();

        System.out.print("Enter Department: ");
        String department = input.nextLine();

        System.out.print("Enter Level: ");
        String level = input.nextLine();

        // Calculate student age

        LocalDate dateOfBirth = LocalDate.of(
            birthYear, birthMonth, birthDay
        );

        LocalDate today = LocalDate.now();

        int age = today.getYear() - birthYear;

        if (today.getDayOfYear() < dateOfBirth.getDayOfYear()
                && today.getYear() == birthYear) {
            age--;
        } else if (today.getMonthValue() < birthMonth ||
                (today.getMonthValue() == birthMonth &&
                 today.getDayOfMonth() < birthDay)) {
            age--;
        }

        // Combine names

        String fullName = firstName.trim() + " " + lastName.trim();

        // Determine registration status

        String status = age >= 16 ?
                "REGISTERED" : "NOT ELIGIBLE";

        // Display student information

        System.out.println();
        System.out.println("=================================");
        System.out.println("      STUDENT INFORMATION");
        System.out.println("=================================");

        System.out.println("Student ID: " + studentId);
        System.out.println("Full Name: " + fullName);
        System.out.println("Gender: " + gender);
        System.out.println("Date of Birth: " + dateOfBirth);
        System.out.println("Age: " + age);
        System.out.println("Email: " + email);
        System.out.println("Phone Number: " + phoneNumber);
        System.out.println("Department: " + department);
        System.out.println("Level: " + level);
        System.out.println("Registration Status: " + status);

        System.out.println("=================================");

        input.close();
    }
} 