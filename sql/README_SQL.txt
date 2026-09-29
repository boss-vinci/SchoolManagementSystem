SCHOOL MANAGEMENT SYSTEM - DATABASE SCRIPTS

Database engine: H2 embedded database
Database JDBC URL: jdbc:h2:./data/school;DATABASE_TO_LOWER=TRUE
Database file created at runtime: data\school.mv.db

Files:
1. schema.sql      - Creates the full relational schema.
2. sample_data.sql - Inserts demonstration data into an empty/demo database.

The application also contains school.main.DatabaseSetup, which creates the same schema automatically through JDBC.
Do not publish or submit your personal runtime database file. Submit the SQL scripts instead.
