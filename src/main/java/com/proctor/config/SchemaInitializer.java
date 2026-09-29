package com.proctor.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaInitializer {
    private static volatile boolean initialized = false;

    // Execute base DDL schema and apply incremental column migrations
    public static synchronized void initialize() {
        if (initialized) return;
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                System.err.println("schema.sql not found in classpath.");
                return;
            }

            // Read base schema DDL script
            String sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                // Execute base tables and constraints
                stmt.execute(sql);

                // Run incremental column and table compatibility migrations
                stmt.execute("DO $$ BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'is_active') THEN " +
                        "ALTER TABLE users RENAME COLUMN is_active TO is_enabled; END IF; END $$;");
                stmt.execute("DO $$ BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'subjects' AND column_name = 'is_active') THEN " +
                        "ALTER TABLE subjects RENAME COLUMN is_active TO is_enabled; END IF; END $$;");
                stmt.execute("DO $$ BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'questions' AND column_name = 'is_active') THEN " +
                        "ALTER TABLE questions RENAME COLUMN is_active TO is_enabled; END IF; END $$;");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;");
                stmt.execute("ALTER TABLE subjects ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;");
                stmt.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(100);");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50);");
                stmt.execute("UPDATE users SET email = username WHERE (email IS NULL OR email = '') AND username IS NOT NULL;");
                stmt.execute("UPDATE users SET username = SPLIT_PART(email, '@', 1) WHERE (username IS NULL OR username = '') AND email IS NOT NULL;");
                stmt.execute("UPDATE users SET email = 'admin@proctor.edu' WHERE username = 'admin' AND (email IS NULL OR email = '' OR email = 'admin');");
                stmt.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS topic VARCHAR(100);");
                stmt.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;");
                stmt.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS assessment_type VARCHAR(20) DEFAULT 'QUIZ';");
                stmt.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS quiz_question_type VARCHAR(30);");
                stmt.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS speed_quiz_seconds_per_question INT;");
                stmt.execute("UPDATE quizzes SET assessment_type = 'QUIZ' WHERE assessment_type IS NULL;");
                stmt.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS quiz_id INT REFERENCES quizzes(id) ON DELETE CASCADE;");
                stmt.execute("ALTER TABLE attempt_answers ADD COLUMN IF NOT EXISTS teacher_feedback TEXT;");
                stmt.execute("DELETE FROM attempt_answers a USING attempt_answers b WHERE a.id < b.id AND a.attempt_id = b.attempt_id AND a.question_id = b.question_id;");
                stmt.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_attempt_question') THEN ALTER TABLE attempt_answers ADD CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id); END IF; END $$;");
                stmt.execute("UPDATE attempts SET status = 'TURNED_IN' WHERE status = 'SUBMITTED';");
                stmt.execute("ALTER TABLE attempts DROP CONSTRAINT IF EXISTS attempts_status_check;");
                stmt.execute("ALTER TABLE attempts ADD CONSTRAINT attempts_status_check CHECK (status IN ('IN_PROGRESS', 'TURNED_IN', 'GRADED', 'AUTO_SUBMITTED'));");
                stmt.execute("CREATE TABLE IF NOT EXISTS inbox_messages (" +
                        "id SERIAL PRIMARY KEY, " +
                        "sender_id INT REFERENCES users(id) ON DELETE SET NULL, " +
                        "recipient_id INT REFERENCES users(id) ON DELETE CASCADE, " +
                        "type VARCHAR(30) NOT NULL, " +
                        "title VARCHAR(200) NOT NULL, " +
                        "body TEXT NOT NULL, " +
                        "target_id INT, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                        "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "resolved_at TIMESTAMP);");
                stmt.execute("ALTER TABLE inbox_messages ADD COLUMN IF NOT EXISTS proposed_password_hash VARCHAR(255);");
                stmt.execute("ALTER TABLE quizzes ALTER COLUMN pass_score SET DEFAULT 50;");
                stmt.execute("ALTER TABLE quizzes ALTER COLUMN randomize_questions SET DEFAULT TRUE;");
                stmt.execute("ALTER TABLE quizzes ALTER COLUMN randomize_answers SET DEFAULT TRUE;");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS academic_degree VARCHAR(100);");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS education_background VARCHAR(150);");
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(100);");
                stmt.execute("INSERT INTO subjects (code, name, description, is_enabled) " +
                        "SELECT 'HTML', 'HTML', 'HyperText Markup Language & Web Structure', TRUE " +
                        "WHERE NOT EXISTS (SELECT 1 FROM subjects WHERE code = 'HTML');");
                stmt.execute("INSERT INTO subjects (code, name, description, is_enabled) " +
                        "SELECT 'CSS', 'CSS', 'Cascading Style Sheets & Responsive Styling', TRUE " +
                        "WHERE NOT EXISTS (SELECT 1 FROM subjects WHERE code = 'CSS');");

                stmt.execute("UPDATE questions SET subject_id = (SELECT id FROM subjects WHERE code = 'CSS' LIMIT 1) " +
                        "WHERE subject_id = (SELECT id FROM subjects WHERE code = 'HTML/CSS' LIMIT 1) AND (LOWER(question_text) LIKE '%css%' OR LOWER(COALESCE(explanation, '')) LIKE '%css%');");
                stmt.execute("UPDATE questions SET subject_id = (SELECT id FROM subjects WHERE code = 'HTML' LIMIT 1) " +
                        "WHERE subject_id = (SELECT id FROM subjects WHERE code = 'HTML/CSS' LIMIT 1);");
                stmt.execute("UPDATE quizzes SET subject_id = (SELECT id FROM subjects WHERE code = 'CSS' LIMIT 1) " +
                        "WHERE subject_id = (SELECT id FROM subjects WHERE code = 'HTML/CSS' LIMIT 1) AND LOWER(title) LIKE '%css%';");
                stmt.execute("UPDATE quizzes SET subject_id = (SELECT id FROM subjects WHERE code = 'HTML' LIMIT 1) " +
                        "WHERE subject_id = (SELECT id FROM subjects WHERE code = 'HTML/CSS' LIMIT 1);");

                stmt.execute("DELETE FROM subjects WHERE code = 'HTML/CSS';");

                stmt.execute("UPDATE quizzes SET subject_id = NULL WHERE subject_id IS NOT NULL AND subject_id NOT IN (SELECT id FROM subjects);");
                stmt.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quizzes_subject_id_fkey') THEN " +
                        "ALTER TABLE quizzes ADD CONSTRAINT quizzes_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL; END IF; END $$;");

                stmt.execute("DROP TABLE IF EXISTS user_subjects CASCADE;");

                stmt.execute("ALTER TABLE quizzes DROP COLUMN IF EXISTS max_attempts;");

                stmt.execute("CREATE TABLE IF NOT EXISTS email_verification_tokens (" +
                        "id SERIAL PRIMARY KEY, " +
                        "email VARCHAR(150) NOT NULL, " +
                        "code VARCHAR(10) NOT NULL, " +
                        "purpose VARCHAR(30) NOT NULL, " +
                        "expires_at TIMESTAMP NOT NULL, " +
                        "used BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);");
                initialized = true;
            }
        } catch (Exception e) {
            System.err.println("Database schema initialization error: " + e.getMessage());
            throw new RuntimeException("Database connection or schema initialization failed: " + e.getMessage(), e);
        }
    }
}
