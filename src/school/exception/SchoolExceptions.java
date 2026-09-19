package school.exception;

public class SchoolExceptions {

    public static class StudentNotFoundException extends Exception {

        public StudentNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidScoreException extends RuntimeException {

        public InvalidScoreException(String message) {
            super(message);
        }
    }

    public static class InvalidPaymentException extends RuntimeException {

        public InvalidPaymentException(String message) {
            super(message);
        }
    }

    public static class InvalidLoginException extends Exception {

        public InvalidLoginException(String message) {
            super(message);
        }
    }
} 