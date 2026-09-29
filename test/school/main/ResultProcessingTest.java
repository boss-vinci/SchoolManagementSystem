package school.main;

import org.junit.jupiter.api.Test;

import school.exception.SchoolExceptions.InvalidScoreException;
import school.model.SchoolData.Grade;

import static org.junit.jupiter.api.Assertions.*;

class ResultProcessingTest {

    @Test
    void calculatesGradesAtImportantBoundaries() {
        assertEquals(Grade.A, SchoolDemo.calculateGrade(70));
        assertEquals(Grade.B, SchoolDemo.calculateGrade(60));
        assertEquals(Grade.C, SchoolDemo.calculateGrade(50));
        assertEquals(Grade.D, SchoolDemo.calculateGrade(45));
        assertEquals(Grade.E, SchoolDemo.calculateGrade(40));
        assertEquals(Grade.F, SchoolDemo.calculateGrade(39));
    }

    @Test
    void rejectsScoreAboveOneHundred() {
        assertThrows(
            InvalidScoreException.class,
            () -> SchoolDemo.calculateGrade(101)
        );
    }

    @Test
    void rejectsNegativeScore() {
        assertThrows(
            InvalidScoreException.class,
            () -> SchoolDemo.calculateGrade(-1)
        );
    }
}
