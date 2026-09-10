CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    is_enabled BOOLEAN DEFAULT TRUE,
    date_of_birth DATE,
    gender VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subjects (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_subjects (
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    subject_id INT REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, subject_id)
);

CREATE TABLE IF NOT EXISTS questions (
    id SERIAL PRIMARY KEY,
    quiz_id INT REFERENCES quizzes(id) ON DELETE CASCADE,
    subject_id INT REFERENCES subjects(id) ON DELETE SET NULL,
    created_by INT REFERENCES users(id),
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('MCQ', 'TRUE_FALSE', 'SHORT_ANSWER')),
    difficulty VARCHAR(10) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    points NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    explanation TEXT,
    ai_generated BOOLEAN DEFAULT FALSE,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_options (
    id SERIAL PRIMARY KEY,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS quizzes (
    id SERIAL PRIMARY KEY,
    subject_id INT REFERENCES subjects(id),
    created_by INT REFERENCES users(id),
    assessment_type VARCHAR(20) NOT NULL DEFAULT 'QUIZ',
    quiz_question_type VARCHAR(30),
    title VARCHAR(200) NOT NULL,
    topic VARCHAR(100),
    description TEXT,
    time_limit_mins INT,
    pass_score INT NOT NULL DEFAULT 50,
    randomize_questions BOOLEAN DEFAULT TRUE,
    randomize_answers BOOLEAN DEFAULT TRUE,
    show_answers_after BOOLEAN DEFAULT FALSE,
    max_attempts INT DEFAULT 1,
    is_published BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    quiz_id INT REFERENCES quizzes(id) ON DELETE CASCADE,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    question_order INT NOT NULL,
    PRIMARY KEY (quiz_id, question_id)
);

CREATE TABLE IF NOT EXISTS attempts (
    id SERIAL PRIMARY KEY,
    quiz_id INT REFERENCES quizzes(id),
    student_id INT REFERENCES users(id),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'TURNED_IN', 'GRADED', 'AUTO_SUBMITTED'))
);

CREATE TABLE IF NOT EXISTS attempt_answers (
    id SERIAL PRIMARY KEY,
    attempt_id INT REFERENCES attempts(id) ON DELETE CASCADE,
    question_id INT REFERENCES questions(id),
    selected_option_id INT REFERENCES question_options(id),
    text_answer TEXT,
    ai_score INT,
    ai_feedback TEXT,
    teacher_feedback TEXT,
    is_correct BOOLEAN,
    points_awarded NUMERIC(5,2) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS results (
    id SERIAL PRIMARY KEY,
    attempt_id INT UNIQUE REFERENCES attempts(id) ON DELETE CASCADE,
    student_id INT REFERENCES users(id),
    quiz_id INT REFERENCES quizzes(id),
    total_points NUMERIC(7,2),
    max_points NUMERIC(7,2),
    percentage NUMERIC(5,2),
    passed BOOLEAN,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inbox_messages (
    id SERIAL PRIMARY KEY,
    sender_id INT REFERENCES users(id) ON DELETE SET NULL,
    recipient_id INT REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    target_id INT,
    proposed_password_hash VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);