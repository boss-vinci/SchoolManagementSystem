package school.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import school.model.SchoolData.Gender;
import school.model.SchoolData.PaymentMethod;
import school.model.SchoolData.PaymentTransaction;
import school.model.SchoolData.Student;
import school.model.SchoolData.StudentLevel;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAndValidationTest {

    private Student student;
    private PaymentTransaction transaction;

    @BeforeEach
    void setUp() {
        student = new Student(
            "PAY001",
            "Payment Test",
            Gender.FEMALE,
            StudentLevel.LEVEL_200
        );

        transaction = new PaymentTransaction(student);
    }

    @Test
    void calculatesTotalPaymentCorrectly() {
        transaction.addPayment(50000, PaymentMethod.TRANSFER);
        transaction.addPayment(25000, PaymentMethod.CASH);

        assertEquals(75000.0, transaction.getTotal(), 0.001);
    }

    @Test
    void rejectsInvalidPaymentAmount() {
        assertThrows(
            IllegalArgumentException.class,
            () -> transaction.addPayment(-500, PaymentMethod.CARD)
        );
    }

    @Test
    void rejectsBlankStudentId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Student(
                "   ",
                "Invalid Student",
                Gender.MALE,
                StudentLevel.LEVEL_100
            )
        );
    }
}
