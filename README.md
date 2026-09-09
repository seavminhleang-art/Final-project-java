# Proctor

Proctor is a terminal user interface (TUI) examination, quiz, and question bank management platform built in Java with a classic layered Model-View-Controller (MVC) architecture.

---

## 1. Prerequisites

Before running Proctor on any machine, ensure the following software is installed:

### Required:
1. **Java 21 (JDK 21)**
   - **Ubuntu/Debian**: `sudo apt update && sudo apt install -y openjdk-21-jdk`
   - **macOS**: `brew install openjdk@21`
   - **Windows**: Install Eclipse Temurin 21 or Oracle JDK 21
   - Verify installation: `java -version` (must report version 21)

2. **PostgreSQL (v14 or higher)**
   - Make sure the PostgreSQL service is active:
     - **Linux**: `sudo systemctl start postgresql`
     - **macOS**: `brew services start postgresql@16`
     - **Windows**: Start the service via Services manager

### Optional (AI Features):
3. **Ollama** (required only for AI quiz/exam generation and AI grading)
   - Install from [https://ollama.com](https://ollama.com)
   - Pull the recommended model: `ollama run llama3.2:3b`

---

## 2. Setup & Installation

### Step 1: Create the Database
Create an empty database named `proctor_db`:

```bash
# Via PostgreSQL command line
sudo -u postgres psql -c "CREATE DATABASE proctor_db;"

# Or inside the psql prompt:
CREATE DATABASE proctor_db;
```

> **Note**: You do not need to create tables or run schema files manually. Proctor's schema manager automatically initializes all tables, constraints, and migrations on first startup.

### Step 2: Configure Database Credentials
Copy the example configuration file:

```bash
cp config.properties.example config.properties
```

Open `config.properties` and adjust the credentials if your PostgreSQL user/password differs from default `postgres`/`postgres`:

```properties
db.url=jdbc:postgresql://localhost:5432/proctor_db
db.username=postgres
db.password=postgres
```

### Step 3: Run the Application

On **Linux / macOS**:
```bash
chmod +x gradlew run proctor
./run
```
*(Or run directly with `./gradlew run`)*

On **Windows**:
```cmd
gradlew.bat run
```

---

## 3. Initial Login Credentials

On first run, Proctor automatically seeds the default system administrator:
- **Username**: `admin`
- **Password**: `admin123`

---

## 4. User Roles & Features

- **System Administrator**: Manage user accounts, approve/reject user-submitted password resets, review audit logs, and export JasperReports PDF analytics.
- **Teacher**: Author questions, create single-type Quizzes or mixed-type Exams, generate assessments using AI, review student answer sheets, and grade open-ended submissions with optional AI assistance.
- **Student**: View available assessments, take timed quizzes/exams with live countdown clocks, request retakes/makeups, review graded answer sheets, and inspect the global leaderboard.

---

## 5. Development & Build

- Build distribution package: `./gradlew installDist assemble`
- Clean build artifacts: `./gradlew clean`
- Run application: `./run` or `./gradlew run`
