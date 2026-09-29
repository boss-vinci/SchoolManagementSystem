-- Sample demonstration data for the School Management System.
-- Run after schema.sql on an empty/demo database.
-- User accounts are intentionally omitted because passwords are created securely by the application.

INSERT INTO departments (department_name) VALUES ('Software Engineering');
INSERT INTO departments (department_name) VALUES ('Computer Science');

INSERT INTO classes (class_id, class_name, department_id, level)
SELECT 'SWE100A', 'Software Engineering 100A', department_id, 100
FROM departments WHERE department_name = 'Software Engineering';

INSERT INTO students
(student_id, first_name, last_name, gender, date_of_birth, email, phone, department_id, level)
SELECT 'ST001', 'James', 'Test', 'MALE', '2008-03-10', 'james.test@example.com', '08000000001', department_id, 200
FROM departments WHERE department_name = 'Software Engineering';

INSERT INTO teachers
(teacher_id, first_name, last_name, email, department_id)
SELECT 'T001', 'Sarah', 'Demo', 'sarah.demo@example.com', department_id
FROM departments WHERE department_name = 'Software Engineering';

INSERT INTO courses (course_id, course_name, department_id, teacher_id)
SELECT 'SEN101', 'Introduction to Software Engineering', department_id, 'T001'
FROM departments WHERE department_name = 'Software Engineering';

INSERT INTO examinations (exam_id, course_id, exam_name, exam_date)
VALUES ('EX001', 'SEN101', 'First Semester Examination', '2026-12-10');

INSERT INTO enrollments (student_id, course_id) VALUES ('ST001', 'SEN101');
INSERT INTO results (student_id, course_id, score, grade) VALUES ('ST001', 'SEN101', 65, 'B');
INSERT INTO attendance (student_id, course_id, attendance_date, status) VALUES ('ST001', 'SEN101', '2026-09-29', 'PRESENT');
INSERT INTO payments (student_id, amount, payment_date, payment_method) VALUES ('ST001', 50000.00, '2026-09-29', 'TRANSFER');
