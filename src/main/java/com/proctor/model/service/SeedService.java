package com.proctor.model.service;

import com.proctor.config.Config;
import com.proctor.model.entity.*;
import com.proctor.model.enums.*;
import com.proctor.model.repository.*;
import com.proctor.util.PasswordUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

public class SeedService {

    public static final String ADMIN_USERNAME = Config.get("admin.username", "admin");
    public static final String ADMIN_EMAIL    = Config.get("admin.email",    "admin@proctor.edu");
    public static final String ADMIN_PASSWORD = Config.get("admin.password", "admin123");
    public static final String ADMIN_FULLNAME = Config.get("admin.fullname", "System Administrator");

    private static final String DEMO_PW = "Tongan123";

    private final UserRepository     userRepository;
    private final SubjectRepository  subjectRepository;
    private final QuizRepository     quizRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository  attemptRepository;
    private final ResultRepository   resultRepository;
    private final InboxRepository    inboxRepository;

    private Map<String, Quiz> quizCache;

    public SeedService(UserRepository userRepository) {
        this(userRepository, new SubjectRepository(), new QuizRepository(),
             new QuestionRepository(), new AttemptRepository(),
             new ResultRepository(), new InboxRepository());
    }

    public SeedService(UserRepository userRepository, SubjectRepository subjectRepository) {
        this(userRepository, subjectRepository, new QuizRepository(),
             new QuestionRepository(), new AttemptRepository(),
             new ResultRepository(), new InboxRepository());
    }

    public SeedService(UserRepository u, SubjectRepository s, QuizRepository qz,
                       QuestionRepository qq, AttemptRepository at,
                       ResultRepository r, InboxRepository ib) {
        this.userRepository    = u;
        this.subjectRepository = s;
        this.quizRepository    = qz;
        this.questionRepository = qq;
        this.attemptRepository = at;
        this.resultRepository  = r;
        this.inboxRepository   = ib;
    }

    public void seedDefaultSubjects() {
        if (subjectRepository == null) return;
        for (PredefinedSubject ps : PredefinedSubject.values()) {
            Optional<Subject> byCode = subjectRepository.findByCode(ps.getCode());
            if (byCode.isPresent()) {
                Subject s = byCode.get();
                if (!s.getName().equals(ps.getDisplayName()) ||
                        !ps.getDescription().equals(s.getDescription()) ||
                        !s.isEnabled()) {
                    s.setName(ps.getDisplayName());
                    s.setDescription(ps.getDescription());
                    s.setEnabled(true);
                    subjectRepository.update(s);
                }
                continue;
            }
            Optional<Subject> byName = subjectRepository.findByName(ps.getDisplayName());
            if (byName.isPresent()) {
                Subject s = byName.get();
                s.setCode(ps.getCode());
                s.setDescription(ps.getDescription());
                s.setEnabled(true);
                subjectRepository.update(s);
                continue;
            }
            Subject newSubject = Subject.builder()
                    .code(ps.getCode()).name(ps.getDisplayName())
                    .description(ps.getDescription()).enabled(true).build();
            subjectRepository.create(newSubject);
        }
    }

    public void seedDefaultAdmin() {
        Optional<User> existing = userRepository.findByUsername(ADMIN_USERNAME);
        if (existing.isEmpty()) {
            User admin = User.builder()
                    .email(ADMIN_EMAIL).username(ADMIN_USERNAME)
                    .passwordHash(PasswordUtils.hash(ADMIN_PASSWORD))
                    .fullName(ADMIN_FULLNAME).role(Role.ADMIN).enabled(true).build();
            userRepository.create(admin);
        } else {
            User admin = existing.get();
            boolean needUpdate = false;
            if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
                admin.setRole(Role.ADMIN); admin.setEnabled(true); needUpdate = true;
            }
            if (admin.getEmail() == null || !admin.getEmail().contains("@")) {
                admin.setEmail(ADMIN_EMAIL); needUpdate = true;
            }
            if (needUpdate) userRepository.update(admin);
        }
    }

    public void seedAll() {
        Map<String, User> users = new LinkedHashMap<>();
        users.putAll(seedDemoTeachers());
        users.putAll(seedDemoStudents());
        Map<String, Integer> sids = loadSubjectIds();
        List<Quiz> quizzes = seedDemoAssessments(users, sids);
        seedDemoBankQuestions(users, sids);
        seedDemoAttempts(users, quizzes);
        seedDemoInbox(users, quizzes);
    }

    private Map<String, User> seedDemoTeachers() {
        Map<String, User> map = new LinkedHashMap<>();
        map.put("dr.nguyen", findOrCreateUser("dr.nguyen", "nguyen.van.minh@proctor.edu",
            "Nguyen Van Minh", Role.TEACHER, "Male", LocalDate.of(1982, 4, 15),
            "Doctor of Computer Science", "Vietnam National University", "Back-End Development & Algorithms"));
        map.put("ms.linda", findOrCreateUser("ms.linda", "linda.hartman@proctor.edu",
            "Linda Hartman", Role.TEACHER, "Female", LocalDate.of(1989, 9, 3),
            "Master of Software Engineering", "University of Amsterdam", "Web & UI Development"));
        map.put("mr.rafael", findOrCreateUser("mr.rafael", "rafael.santos@proctor.edu",
            "Rafael Santos", Role.TEACHER, "Male", LocalDate.of(1985, 12, 20),
            "Master of Computer Science", "University of Sao Paulo", "Systems Programming & Security"));
        return map;
    }

    private Map<String, User> seedDemoStudents() {
        Map<String, User> map = new LinkedHashMap<>();
        map.put("alice",   findOrCreateUser("alice",   "alice.tran@proctor.edu",       "Alice Tran",        Role.STUDENT, "Female", LocalDate.of(2002, 3,  7),  null, null, null));
        map.put("bob",     findOrCreateUser("bob",     "bob.kimura@proctor.edu",        "Bob Kimura",        Role.STUDENT, "Male",   LocalDate.of(2001, 7, 22),  null, null, null));
        map.put("charlie", findOrCreateUser("charlie", "charlie.dinh@proctor.edu",      "Charlie Dinh",      Role.STUDENT, "Male",   LocalDate.of(2003, 1, 14),  null, null, null));
        map.put("diana",   findOrCreateUser("diana",   "diana.okonkwo@proctor.edu",     "Diana Okonkwo",     Role.STUDENT, "Female", LocalDate.of(2002, 11, 5),  null, null, null));
        map.put("ethan",   findOrCreateUser("ethan",   "ethan.park@proctor.edu",        "Ethan Park",        Role.STUDENT, "Male",   LocalDate.of(2001, 6, 30),  null, null, null));
        map.put("fatima",  findOrCreateUser("fatima",  "fatima.alrashid@proctor.edu",   "Fatima Al-Rashid",  Role.STUDENT, "Female", LocalDate.of(2000, 9, 18),  null, null, null));
        map.put("george",  findOrCreateUser("george",  "george.liu@proctor.edu",        "George Liu",        Role.STUDENT, "Male",   LocalDate.of(2003, 4, 25),  null, null, null));
        map.put("hana",    findOrCreateUser("hana",    "hana.yamamoto@proctor.edu",     "Hana Yamamoto",     Role.STUDENT, "Female", LocalDate.of(2002, 8, 11),  null, null, null));
        return map;
    }

    private User findOrCreateUser(String username, String email, String fullName, Role role,
            String gender, LocalDate dob, String degree, String edu, String spec) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = User.builder()
                .username(username).email(email)
                .passwordHash(PasswordUtils.hash(DEMO_PW))
                .fullName(fullName).role(role).enabled(true)
                .gender(gender).dateOfBirth(dob)
                .academicDegree(degree).educationBackground(edu).specialization(spec)
                .build();
            userRepository.create(u);
            return u;
        });
    }

    private Map<String, Integer> loadSubjectIds() {
        Map<String, Integer> map = new HashMap<>();
        for (PredefinedSubject ps : PredefinedSubject.values()) {
            subjectRepository.findByCode(ps.getCode())
                .ifPresent(s -> map.put(ps.getCode(), s.getId()));
        }
        return map;
    }

    private void initQuizCache() {
        if (quizCache != null) return;
        quizCache = new HashMap<>();
        for (Quiz q : quizRepository.findAll(null, null, null, null, null, false)) {
            quizCache.put(q.getTitle().toLowerCase().trim(), q);
        }
    }

    private Quiz findOrCreateQuiz(String title, String topic, String desc,
            AssessmentType type, Integer subjectId, int createdBy,
            Integer timeLimitMins, int passScore, boolean rq, boolean ra, boolean sa,
            Integer speedSecs, QuestionType quizQType) {
        initQuizCache();
        String key = title.toLowerCase().trim();
        if (quizCache.containsKey(key)) return quizCache.get(key);
        Quiz quiz = Quiz.builder()
            .title(title).topic(topic).description(desc)
            .assessmentType(type).subjectId(subjectId).createdBy(createdBy)
            .timeLimitMins(timeLimitMins).passScore(passScore)
            .randomizeQuestions(rq).randomizeAnswers(ra).showAnswersAfter(sa)
            .published(true).speedSecondsPerQuestion(speedSecs).quizQuestionType(quizQType)
            .build();
        quizRepository.create(quiz);
        quizCache.put(key, quiz);
        return quiz;
    }

    private boolean hasQuestions(int quizId) {
        return !questionRepository.findByQuizId(quizId).isEmpty();
    }

    private void addMcq(int qid, Integer sid, int uid, Difficulty d, double pts,
            String text, String exp, String correct, String w1, String w2, String w3) {
        questionRepository.create(Question.builder()
            .quizId(qid).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.MCQ).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(List.of(opt(correct, true), opt(w1, false), opt(w2, false), opt(w3, false)))
            .build());
    }

    private void addTf(int qid, Integer sid, int uid, Difficulty d, double pts,
            String text, boolean answer, String exp) {
        questionRepository.create(Question.builder()
            .quizId(qid).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.TRUE_FALSE).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(List.of(opt("True", answer), opt("False", !answer)))
            .build());
    }

    private void addSa(int qid, Integer sid, int uid, Difficulty d, double pts,
            String text, String exp) {
        questionRepository.create(Question.builder()
            .quizId(qid).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.SHORT_ANSWER).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(new ArrayList<>())
            .build());
    }

    private void addBankMcq(Integer sid, int uid, Difficulty d, double pts,
            String text, String exp, String correct, String w1, String w2, String w3) {
        questionRepository.create(Question.builder()
            .quizId(null).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.MCQ).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(List.of(opt(correct, true), opt(w1, false), opt(w2, false), opt(w3, false)))
            .build());
    }

    private void addBankTf(Integer sid, int uid, Difficulty d, double pts,
            String text, boolean answer, String exp) {
        questionRepository.create(Question.builder()
            .quizId(null).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.TRUE_FALSE).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(List.of(opt("True", answer), opt("False", !answer)))
            .build());
    }

    private void addBankSa(Integer sid, int uid, Difficulty d, double pts, String text, String exp) {
        questionRepository.create(Question.builder()
            .quizId(null).subjectId(sid).createdBy(uid)
            .questionText(text).questionType(QuestionType.SHORT_ANSWER).difficulty(d)
            .points(pts).explanation(exp).enabled(true)
            .options(new ArrayList<>())
            .build());
    }

    private QuestionOption opt(String text, boolean correct) {
        return QuestionOption.builder().optionText(text).correct(correct).build();
    }

    private List<Quiz> seedDemoAssessments(Map<String, User> users, Map<String, Integer> sids) {
        User ng = users.get("dr.nguyen");
        User li = users.get("ms.linda");
        User ra = users.get("mr.rafael");
        List<Quiz> all = new ArrayList<>();

        all.add(seedQ01(ng, sids)); all.add(seedQ02(li, sids)); all.add(seedQ03(ng, sids));
        all.add(seedQ04(ng, sids)); all.add(seedQ05(ra, sids)); all.add(seedQ06(li, sids));
        all.add(seedQ07(ng, sids)); all.add(seedQ08(ra, sids)); all.add(seedQ09(li, sids));
        all.add(seedQ10(ng, sids)); all.add(seedQ11(ra, sids)); all.add(seedQ12(li, sids));
        all.add(seedQ13(li, sids)); all.add(seedQ14(ng, sids)); all.add(seedQ15(ng, sids));

        all.add(seedE01(ng, sids)); all.add(seedE02(li, sids)); all.add(seedE03(ng, sids));
        all.add(seedE04(ra, sids)); all.add(seedE05(ng, sids)); all.add(seedE06(li, sids));
        all.add(seedE07(ra, sids)); all.add(seedE08(li, sids)); all.add(seedE09(ng, sids));
        all.add(seedE10(ra, sids)); all.add(seedE11(ng, sids)); all.add(seedE12(li, sids));
        all.add(seedE13(li, sids)); all.add(seedE14(ra, sids)); all.add(seedE15(ng, sids));

        all.add(seedS01(ng, sids)); all.add(seedS02(ng, sids)); all.add(seedS03(ra, sids));
        all.add(seedS04(ng, sids)); all.add(seedS05(li, sids)); all.add(seedS06(ng, sids));
        all.add(seedS07(li, sids)); all.add(seedS08(ra, sids)); all.add(seedS09(li, sids));
        all.add(seedS10(li, sids)); all.add(seedS11(ng, sids)); all.add(seedS12(ra, sids));

        return all;
    }

    private Quiz seedQ01(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("JavaScript Fundamentals", "Core JavaScript",
            "Test your knowledge of JavaScript types, scoping, and core language features.",
            AssessmentType.QUIZ, s.get("JS"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `typeof null` return in JavaScript?", "typeof null returns 'object' — a well-known JavaScript quirk.", "\"object\"", "\"null\"", "\"undefined\"", "\"boolean\"");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which method converts a JSON string to a JavaScript object?", "JSON.parse() deserializes JSON; JSON.stringify() serializes to JSON.", "JSON.parse()", "JSON.stringify()", "JSON.convert()", "JSON.decode()");
        addTf( id, sid, uid, Difficulty.EASY,   1, "Variables declared with `let` are block-scoped in JavaScript.", true, "let and const are block-scoped; var is function-scoped.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `typeof function(){}` return in JavaScript?", "typeof applied to a function literal returns the string 'function'.", "\"function\"", "\"object\"", "\"callable\"", "\"undefined\"");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Arrow functions in JavaScript have their own `this` binding.", false, "Arrow functions lexically inherit `this` from the surrounding scope — they do NOT have their own `this`.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which array method returns a new array without mutating the original?", "map() returns a new array; push(), pop(), splice() mutate the original.", "map()", "push()", "pop()", "splice()");
        return q;
    }

    private Quiz seedQ02(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("HTML & CSS Basics", "Web Fundamentals",
            "Fundamental quiz covering HTML structure, semantic elements, and core CSS styling.",
            AssessmentType.QUIZ, s.get("HTML"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("HTML");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which HTML tag defines a hyperlink?", "The <a> (anchor) element creates hyperlinks using the href attribute.", "<a>", "<link>", "<href>", "<nav>");
        addTf( id, sid, uid, Difficulty.EASY,   1, "The <div> element is an inline-level element.", false, "<div> is a block-level element; <span> is the inline equivalent.");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which CSS property controls text size?", "font-size sets the size of the text.", "font-size", "text-size", "font-style", "text-font");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does CSS `display: flex` enable on a container?", "display: flex activates the Flexbox layout model on a container element.", "Flexbox layout", "Grid layout", "Float layout", "Table layout");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "CSS `position: sticky` combines relative and fixed positioning behavior.", true, "sticky elements scroll with the page until a threshold is hit, then behave as fixed.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which CSS selector has the highest specificity?", "ID selectors outrank class and element selectors in the specificity hierarchy.", "#id", ".class", "element", "*");
        return q;
    }

    private Quiz seedQ03(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Python Essentials", "Python Basics",
            "A quiz on Python syntax, data types, and core built-in functions.",
            AssessmentType.QUIZ, s.get("PYTHON"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PYTHON");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `type(3.14)` return in Python?", "3.14 is a float literal in Python.", "<class 'float'>", "<class 'int'>", "<class 'double'>", "<class 'number'>");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which keyword defines a function in Python?", "def begins a function definition; lambda defines anonymous functions.", "def", "function", "func", "define");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `list.append(x)` do?", "append() adds x to the end of the list in-place.", "Adds x to the end of the list", "Inserts x at index 0", "Removes x from the list", "Sorts the list");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `len(\"hello\")` return?", "'hello' has 5 characters.", "5", "4", "6", "Error");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which of the following creates a valid Python dictionary?", "Dictionaries use curly braces with key:value pairs.", "{\"key\": \"value\"}", "{1, 2, 3}", "[\"key\", \"value\"]", "(\"key\", \"value\")");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the time complexity of `list.pop(0)` in Python?", "Removing from the front requires shifting all remaining elements — O(n).", "O(n)", "O(1)", "O(log n)", "O(n\u00b2)");
        return q;
    }

    private Quiz seedQ04(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("SQL Query Fundamentals", "Database Basics",
            "Quiz covering basic SQL SELECT queries, filtering, and result ordering.",
            AssessmentType.QUIZ, s.get("SQL"), u.getId(), 25, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("SQL");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL command retrieves data from a table?", "SELECT is the DML command for reading data.", "SELECT", "INSERT", "UPDATE", "DELETE");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL clause filters individual rows in a query?", "WHERE filters rows; HAVING filters groups after GROUP BY.", "WHERE", "ORDER BY", "GROUP BY", "HAVING");
        addSa( id, sid, uid, Difficulty.MEDIUM, 2, "Write a SQL statement to select all records from a table named `students`.", "SELECT * FROM students;");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the DISTINCT keyword do in a SELECT statement?", "DISTINCT removes duplicate rows from the result set.", "Removes duplicate rows from results", "Removes all rows", "Sorts results", "Counts rows");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which JOIN type returns all rows from both tables, with NULLs where there is no match?", "FULL OUTER JOIN combines LEFT and RIGHT JOIN results.", "FULL OUTER JOIN", "INNER JOIN", "LEFT JOIN", "CROSS JOIN");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain the difference between the WHERE and HAVING clauses in SQL.", "WHERE filters individual rows before grouping is applied. HAVING filters groups after GROUP BY has been executed. WHERE cannot reference aggregate functions; HAVING can.");
        return q;
    }

    private Quiz seedQ05(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Linux Command Line", "Shell Basics",
            "Test your knowledge of essential Linux commands and shell scripting fundamentals.",
            AssessmentType.QUIZ, s.get("LINUX"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("LINUX");
        addTf( id, sid, uid, Difficulty.EASY,   1, "`ls -la` shows hidden files and displays detailed file information.", true, "The -a flag shows hidden files (starting with .); -l shows a detailed (long) listing.");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which command prints the current working directory?", "pwd (print working directory) outputs the full current path.", "pwd", "dir", "cd", "ls");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What permissions does `chmod 755 script.sh` set?", "7=rwx for owner; 5=r-x for group and others.", "Owner: rwx, Group/Others: r-x", "Owner: rw-, Group/Others: r--", "Owner: rwx, Group/Others: rwx", "Owner: r-x, Group/Others: r-x");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "The `grep` command searches for text patterns in files.", true, "grep (Global Regular Expression Print) searches for patterns in files and standard input.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which command displays real-time process resource usage?", "top provides an interactive real-time view of running processes and system load.", "top", "ps", "kill", "jobs");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `sudo !!` do in bash?", "!! refers to the previous command; sudo !! re-runs it with root privileges.", "Re-runs the last command with sudo privileges", "Kills the current process", "Lists sudo permissions", "Runs the command twice");
        return q;
    }

    private Quiz seedQ06(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("TypeScript Deep Dive", "TypeScript Features",
            "A quiz on TypeScript's type system, interfaces, and advanced type features.",
            AssessmentType.QUIZ, s.get("TS"), u.getId(), 20, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("TS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is TypeScript?", "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript.", "A typed superset of JavaScript", "A database query language", "A CSS preprocessor", "A runtime environment");
        addTf( id, sid, uid, Difficulty.EASY,   1, "TypeScript compiles directly to machine code.", false, "TypeScript transpiles to JavaScript, which is then executed by a JavaScript engine.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `interface` keyword do in TypeScript?", "interface defines a structural contract describing the shape of an object.", "Defines a contract for object shapes", "Creates a class", "Imports modules", "Declares variables");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a Union type in TypeScript?", "Union types allow a variable to hold one of several specified types, written with |.", "A type that can be one of several types (written with |)", "A type that extends another", "A type with no value", "A type for arrays only");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `?` mean in `interface User { age?: number }`?", "? marks a property as optional — it may or may not be present on the object.", "age is optional", "age is required", "age is readonly", "age can be null");
        addTf( id, sid, uid, Difficulty.HARD,   1, "TypeScript's `enum` compiles to a JavaScript object at runtime.", true, "Numeric enums compile to a two-way mapping JS object; const enums are inlined and erased.");
        return q;
    }

    private Quiz seedQ07(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Java Collections Framework", "Java Data Structures",
            "Quiz covering Java's core collection types: List, Set, Map, and their trade-offs.",
            AssessmentType.QUIZ, s.get("JAVA"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JAVA");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Java collection allows duplicate elements?", "List allows duplicates; Set does not.", "List", "Set", "Map (keys)", "TreeSet");
        addTf( id, sid, uid, Difficulty.EASY,   1, "ArrayList is backed by a resizable array.", true, "ArrayList internally stores elements in an array that grows dynamically as elements are added.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the average time complexity of HashMap.get()?", "HashMap provides O(1) average-case lookups using a hash function.", "O(1)", "O(n)", "O(log n)", "O(n\u00b2)");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which collection preserves insertion order?", "LinkedList and LinkedHashMap preserve insertion order.", "LinkedList", "HashSet", "HashMap", "TreeMap");
        addTf( id, sid, uid, Difficulty.HARD,   1, "ConcurrentHashMap allows null keys.", false, "ConcurrentHashMap does not permit null keys or values to avoid ambiguity in concurrent operations.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `Collections.unmodifiableList()` return?", "It returns a read-only view; mutations throw UnsupportedOperationException.", "A read-only view of the list", "An empty list", "A sorted copy", "A new mutable list");
        return q;
    }

    private Quiz seedQ08(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("C++ Pointers & Memory", "Memory Management",
            "Quiz on pointer arithmetic, dynamic memory allocation, and RAII in C++.",
            AssessmentType.QUIZ, s.get("C++"), u.getId(), 25, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("C++");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does a pointer variable hold in C++?", "A pointer holds a memory address, not a value directly.", "A memory address", "A value", "A reference", "A type");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which operator dereferences a pointer in C++?", "The unary * operator dereferences a pointer to access the value at that address.", "*", "&", "->", "::");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a dangling pointer?", "A dangling pointer points to memory that has been freed or gone out of scope.", "A pointer to freed or out-of-scope memory", "An uninitialized pointer", "A null pointer", "A pointer to a function");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `new` return in C++?", "new allocates heap memory and returns a pointer to it.", "A pointer to allocated heap memory", "A value", "A reference", "void");
        addTf( id, sid, uid, Difficulty.HARD,   1, "Every `new` in C++ must be matched with a corresponding `delete`.", true, "Failing to delete heap-allocated memory causes memory leaks. Use smart pointers to automate this.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is RAII in C++?", "RAII ties resource lifetime to object scope — resources are released in the destructor.", "A pattern where resource lifetime is tied to object scope", "A memory allocator", "A pointer type", "A compiler flag");
        return q;
    }

    private Quiz seedQ09(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("PHP Web Scripting", "PHP Fundamentals",
            "Basic PHP syntax, server-side scripting, and superglobal variables.",
            AssessmentType.QUIZ, s.get("PHP"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PHP");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How is PHP code embedded in an HTML file?", "PHP code is wrapped in <?php ... ?> tags.", "<?php ... ?>", "<js>...</js>", "{{ ... }}", "{% ... %}");
        addTf( id, sid, uid, Difficulty.EASY,   1, "PHP is a server-side scripting language.", true, "PHP code runs on the web server; the resulting HTML is sent to the client's browser.");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How do you declare a variable in PHP?", "PHP variables are prefixed with the $ sign.", "$name", "var name", "let name", "name:");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which PHP superglobal contains HTTP POST form data?", "$_POST holds data submitted via HTTP POST.", "$_POST", "$_GET", "$_SESSION", "$_SERVER");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "PHP arrays can hold mixed data types.", true, "PHP arrays are heterogeneous — they can contain strings, integers, objects, and more.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the PHP `===` operator check?", "=== checks both value AND type equality (strict comparison).", "Value and type equality (strict)", "Value equality only", "Reference equality", "Type only");
        return q;
    }

    private Quiz seedQ10(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Go Concurrency Basics", "Goroutines & Channels",
            "Test your knowledge of Go's concurrency model: goroutines, channels, and select.",
            AssessmentType.QUIZ, s.get("GO"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("GO");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which keyword starts a goroutine in Go?", "The go keyword before a function call starts it as a concurrent goroutine.", "go", "async", "thread", "spawn");
        addTf( id, sid, uid, Difficulty.EASY,   1, "Goroutines are lightweight threads managed by the Go runtime.", true, "Goroutines are much lighter than OS threads and are scheduled cooperatively by the Go runtime.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a channel in Go used for?", "Channels are the primary mechanism for goroutines to communicate and synchronize.", "Communication and synchronization between goroutines", "Error handling", "Memory allocation", "Type assertion");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which built-in function closes a Go channel?", "close(ch) signals that no more values will be sent on the channel.", "close(ch)", "ch.close()", "ch.done()", "delete(ch)");
        addTf( id, sid, uid, Difficulty.HARD,   1, "A buffered channel blocks the sender only when the buffer is full.", true, "A buffered channel allows sends without blocking until the buffer capacity is exceeded.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does a `select` statement do in Go?", "select waits on multiple channel operations and proceeds with the first one ready.", "Waits on multiple channel operations, proceeding with the first ready", "Selects a database row", "Imports packages", "Starts a goroutine");
        return q;
    }

    private Quiz seedQ11(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Rust Ownership Model", "Memory Safety",
            "Quiz on Rust's ownership, borrowing, and lifetime system for memory-safe programming.",
            AssessmentType.QUIZ, s.get("RUST"), u.getId(), 25, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("RUST");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How many owners can a Rust value have at one time?", "Each value in Rust has exactly one owner at any given time.", "One", "Two", "Unlimited", "Zero");
        addTf( id, sid, uid, Difficulty.EASY,   1, "When a Rust value goes out of scope, it is automatically dropped.", true, "Rust calls `drop` at scope end, freeing memory deterministically without a garbage collector.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is borrowing in Rust?", "Borrowing lets you reference a value without taking ownership.", "Taking a reference without taking ownership", "Copying a value", "Moving a value", "Cloning a value");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "You can have multiple mutable references to the same value simultaneously in Rust.", false, "Rust enforces: either one mutable reference OR any number of immutable references — never both at once.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the lifetime annotation `'a` express in Rust?", "Lifetimes describe how long references must remain valid to prevent dangling pointers.", "How long a reference is valid", "A deprecated code marker", "An async context", "A constant");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which Rust smart pointer provides shared ownership via reference counting?", "Rc<T> enables multiple owners via reference counting (single-threaded only).", "Rc<T>", "Box<T>", "Cell<T>", "RefCell<T>");
        return q;
    }

    private Quiz seedQ12(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Kotlin Coroutines", "Async Programming",
            "Covers Kotlin coroutine builders, suspending functions, and coroutine scopes.",
            AssessmentType.QUIZ, s.get("KOTLIN"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("KOTLIN");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is a coroutine in Kotlin?", "A coroutine is a lightweight concurrency primitive that can be suspended and resumed.", "A suspendable, lightweight concurrency primitive", "A Kotlin class", "A data structure", "A type annotation");
        addTf( id, sid, uid, Difficulty.EASY,   1, "`suspend` functions can only be called from a coroutine or another `suspend` function.", true, "Suspension requires a coroutine context — suspend functions cannot be called from regular blocking code.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which coroutine builder returns a `Deferred<T>` for async computation?", "async returns a Deferred which holds a future result; launch returns a Job.", "async", "launch", "runBlocking", "withContext");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is `Dispatchers.IO` optimized for?", "Dispatchers.IO uses an elastic thread pool designed for blocking I/O operations.", "Blocking I/O operations", "UI updates on the main thread", "CPU-intensive computation", "Network-only requests");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`runBlocking` blocks the calling thread until all its coroutines complete.", true, "runBlocking bridges regular blocking code with the coroutine world by blocking the current thread.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is structured concurrency in Kotlin?", "Structured concurrency ensures child coroutines finish before their parent scope completes.", "Child coroutines are bound to their parent scope's lifetime", "Using blocking threads", "Writing sequential async code", "Sharing mutable state safely");
        return q;
    }

    private Quiz seedQ13(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("CSS Flexbox & Grid", "Advanced Layout",
            "Covers CSS Flexbox and Grid layout systems including alignment and track sizing.",
            AssessmentType.QUIZ, s.get("CSS"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("CSS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which CSS property enables Flexbox layout on a container?", "display: flex turns an element into a flex container.", "display: flex", "display: block", "display: grid", "display: inline-flex");
        addTf( id, sid, uid, Difficulty.EASY,   1, "`flex-direction: column` stacks flex items vertically.", true, "flex-direction: column changes the main axis to vertical, stacking items top to bottom.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which Flexbox property aligns items along the cross axis?", "align-items controls the cross axis; justify-content controls the main axis.", "align-items", "justify-content", "flex-wrap", "flex-flow");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `grid-template-columns: repeat(3, 1fr)` create?", "1fr is one fraction of available space; repeat(3, 1fr) creates 3 equal columns.", "3 equal-width columns", "3 rows of equal height", "A 3x3 grid", "3 auto-sized columns");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`justify-content: space-between` places equal space between items with no space at the edges.", true, "space-between distributes gaps between items; space-around also adds half-gap at the edges.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the `grid-area` property allow you to do?", "grid-area names a grid item and places it using template area names.", "Name and place a grid item using template areas", "Define column tracks", "Set grid gap", "Create new rows");
        return q;
    }

    private Quiz seedQ14(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Python Data Structures", "Collections & Complexity",
            "Quiz on Python's built-in data structures and their performance characteristics.",
            AssessmentType.QUIZ, s.get("PYTHON"), u.getId(), 20, 60, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PYTHON");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Python data structure is ordered and mutable?", "Lists are ordered (indexed) and mutable; tuples are ordered but immutable.", "list", "tuple", "set", "frozenset");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is the result of `{1, 2, 2, 3}` in Python?", "Sets automatically eliminate duplicates.", "{1, 2, 3}", "{1, 2, 2, 3}", "[1, 2, 3]", "Error");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the average time complexity of accessing a list element by index?", "Python lists use contiguous memory, so index access is O(1).", "O(1)", "O(n)", "O(log n)", "O(n\u00b2)");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Python tuples are immutable.", true, "Tuples cannot be modified after creation; use a list if you need a mutable ordered sequence.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What advantage does `collections.deque` have over a list?", "deque provides O(1) appends and pops from both ends; list.pop(0) is O(n).", "O(1) appends and pops from both ends", "Better random access by index", "Built-in thread safety", "Automatic sorting");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the difference between `copy.copy()` and `copy.deepcopy()`?", "deepcopy recursively copies all nested objects; copy only copies the top-level container.", "deepcopy recursively copies nested objects; copy only copies the top level", "No practical difference", "copy is always faster for all types", "deepcopy only works on lists");
        return q;
    }

    private Quiz seedQ15(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("SQL Joins & Aggregation", "Advanced SQL",
            "Advanced SQL quiz covering JOIN types, aggregate functions, and GROUP BY queries.",
            AssessmentType.QUIZ, s.get("SQL"), u.getId(), 25, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("SQL");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `COUNT(*)` return in SQL?", "COUNT(*) counts all rows, including those with NULL values.", "The total number of rows including NULLs", "The count of non-null values only", "The maximum value", "The sum of all values");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which JOIN type returns only rows with matching values in both tables?", "INNER JOIN returns rows only where the join condition is met in both tables.", "INNER JOIN", "LEFT JOIN", "FULL OUTER JOIN", "CROSS JOIN");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which SQL clause filters results after grouping?", "HAVING filters groups; WHERE filters rows before grouping.", "HAVING", "WHERE", "FILTER", "ORDER BY");
        addSa( id, sid, uid, Difficulty.MEDIUM, 2, "Write a SQL query to count the number of students per department.", "SELECT department, COUNT(*) AS student_count FROM students GROUP BY department;");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `COALESCE(NULL, NULL, 3, NULL)` return?", "COALESCE returns the first non-NULL value in the list.", "3", "NULL", "0", "Error");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain what a LEFT JOIN returns when there are no matching rows in the right table.", "A LEFT JOIN returns all rows from the left table. For rows with no match in the right table, all columns from the right table are NULL.");
        return q;
    }

    private Quiz seedE01(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Midterm: Java OOP", "Object-Oriented Design",
            "Comprehensive midterm examination on Java object-oriented programming principles.",
            AssessmentType.EXAM, s.get("JAVA"), u.getId(), 60, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JAVA");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What are the four pillars of object-oriented programming?", "Encapsulation, Inheritance, Polymorphism, and Abstraction are the four OOP pillars.", "Encapsulation, Inheritance, Polymorphism, Abstraction", "Classes, Objects, Methods, Variables", "Functions, Loops, Arrays, Conditions", "Packages, Imports, Interfaces, Exceptions");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which keyword prevents a Java class from being subclassed?", "final prevents inheritance when applied to a class.", "final", "static", "abstract", "private");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "An abstract class can contain both abstract and concrete methods.", true, "Abstract classes may have fully implemented methods alongside abstract ones.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is method overloading in Java?", "Overloading means multiple methods share a name but have different parameter lists.", "Defining multiple methods with the same name but different parameters", "Overriding a parent class method in a subclass", "Hiding a parent class field", "Calling a virtual method at runtime");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `interface` keyword create in Java?", "An interface defines an abstract contract that classes can implement.", "An abstract contract with method signatures", "A concrete subclass", "An enum type", "A package declaration");
        addSa( id, sid, uid, Difficulty.MEDIUM, 2, "Explain the difference between `==` and `.equals()` in Java.", "== checks reference equality — are both variables pointing to the same object in memory? .equals() checks value equality — do the objects represent the same content? Always use .equals() when comparing String values.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the purpose of the `super` keyword in Java?", "super refers to the parent class; used to call parent constructors or overridden methods.", "References the parent class constructor or method", "References the current object (this)", "Declares a superclass", "Creates a new subclass");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is the key difference between abstract classes and interfaces in Java?", "Abstract classes can have state (instance variables) and concrete method implementations. Interfaces define pure contracts. Java 8+ allows default and static methods in interfaces, but interfaces cannot have constructors or instance fields.");
        return q;
    }

    private Quiz seedE02(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Final: Web Development", "Frontend & Backend",
            "Final examination covering HTTP, HTML semantics, CSS box model, and web architecture.",
            AssessmentType.EXAM, s.get("HTML"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("HTML");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does HTTP stand for?", "HyperText Transfer Protocol is the foundation of data communication on the Web.", "HyperText Transfer Protocol", "High-Tech Transfer Process", "HyperText Transmission Port", "Hyper Transfer Technical Protocol");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which HTML5 element represents a navigation section?", "<nav> is the semantic HTML5 element for site navigation menus.", "<nav>", "<header>", "<section>", "<aside>");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "HTTPS encrypts data transmitted between the client and server.", true, "HTTPS uses TLS/SSL to encrypt data in transit, protecting against eavesdropping.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the purpose of `<meta charset=\"UTF-8\">`?", "This meta tag declares the character encoding for the HTML document.", "Defines the character encoding of the document", "Sets the page title", "Links a stylesheet", "Configures the viewport");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which CSS property creates space between an element's content and its border?", "padding creates space inside the border; margin creates space outside.", "padding", "margin", "border-spacing", "gap");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the CSS cascade?", "The cascade determines which CSS rule wins when multiple rules target the same element.", "The priority order for resolving conflicting style rules", "A CSS animation type", "A layout algorithm", "A selector combinator");
        addSa( id, sid, uid, Difficulty.MEDIUM, 2, "Explain the difference between the HTML `id` and `class` attributes.", "id must be unique within a page — only one element should have a given id. class can be shared by many elements, making it ideal for grouping elements that share the same styles or behavior.");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Describe the CSS box model and its four components.", "The CSS box model describes how every element is rendered as a rectangular box with four layers (inside out): content (the actual text/image), padding (inner spacing), border (outline around the padding), and margin (outer spacing from other elements).");
        return q;
    }

    private Quiz seedE03(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Practical: SQL Mastery", "Database Programming",
            "Practical SQL exam covering advanced queries, transactions, indexes, and normalization.",
            AssessmentType.EXAM, s.get("SQL"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("SQL");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL keyword eliminates duplicate rows in a result set?", "DISTINCT removes duplicate rows from query results.", "DISTINCT", "UNIQUE", "FILTER", "GROUP");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL command removes all rows from a table without deleting the table structure?", "TRUNCATE quickly removes all rows; DELETE removes rows with optional conditions.", "TRUNCATE", "DELETE", "DROP", "REMOVE");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does a PRIMARY KEY constraint guarantee?", "A PRIMARY KEY uniquely identifies each row and enforces NOT NULL.", "Unique, non-null identification of each row", "An index on any column", "A foreign key reference", "A default value for a column");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "A FOREIGN KEY constraint enforces referential integrity between tables.", true, "FOREIGN KEY ensures values in a column must reference valid rows in the referenced table.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which SQL clause sorts the result set?", "ORDER BY sorts rows ascending (ASC) or descending (DESC).", "ORDER BY", "SORT BY", "GROUP BY", "ARRANGE BY");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain what database normalization is and name the first three normal forms.", "Normalization reduces redundancy and improves data integrity. 1NF: atomic values, no repeating groups. 2NF: 1NF + no partial dependencies on composite keys. 3NF: 2NF + no transitive dependencies on non-key columns.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does an index do in a database?", "An index creates a faster lookup structure at the cost of extra write overhead.", "Speeds up data retrieval at the cost of extra write overhead", "Removes duplicate rows", "Enforces referential integrity", "Creates a backup of data");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Describe the ACID properties of a database transaction.", "Atomicity: all operations succeed or all are rolled back. Consistency: the DB transitions between valid states. Isolation: concurrent transactions don't interfere with each other. Durability: committed changes persist even after crashes.");
        return q;
    }

    private Quiz seedE04(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("C++ Systems Programming", "Low-Level Computing",
            "Exam on C++ low-level programming, templates, STL, and performance concepts.",
            AssessmentType.EXAM, s.get("C++"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("C++");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is `std::vector` in C++?", "std::vector is a dynamic-size array in the C++ Standard Template Library.", "A dynamic resizable array in the STL", "A fixed-size array", "A linked list", "A hash map");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does a virtual function enable in C++?", "Virtual functions allow runtime polymorphism through dynamic dispatch via the v-table.", "Runtime polymorphism via dynamic dispatch", "Compile-time method binding", "Template specialization", "Operator overloading");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "A pure virtual function (`= 0`) makes its class abstract in C++.", true, "A class with at least one pure virtual function is abstract and cannot be instantiated directly.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the purpose of `std::unique_ptr`?", "unique_ptr provides exclusive single-owner smart pointer semantics with RAII.", "A smart pointer with single exclusive ownership", "A shared pointer", "A raw pointer wrapper", "A thread-safe pointer");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `std::move()` do in C++?", "std::move casts to an rvalue reference, enabling transfer of resources without copying.", "Casts to an rvalue reference to enable move semantics", "Physically relocates memory blocks", "Deletes the original variable", "Makes a deep copy");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is template metaprogramming in C++?", "Template metaprogramming performs computation at compile time using C++ templates.", "Using templates to perform computation at compile time", "Runtime reflection in C++", "Macro preprocessing", "Dynamic type checking");
        addTf( id, sid, uid, Difficulty.HARD,   1, "The Rule of Five states a class defining a destructor should also define copy/move constructors and assignment operators.", true, "The Rule of Five ensures correct resource management when a class manages a resource.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the time complexity of `std::sort` in C++?", "std::sort uses introsort, guaranteeing O(n log n) in the worst case.", "O(n log n)", "O(n\u00b2)", "O(n)", "O(log n)");
        return q;
    }

    private Quiz seedE05(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Python Data Analysis", "Scientific Computing",
            "Exam on Python's data analysis ecosystem including NumPy, Pandas, and vectorization.",
            AssessmentType.EXAM, s.get("PYTHON"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PYTHON");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Python library is the foundation of numerical computing?", "NumPy provides multi-dimensional arrays and fast mathematical operations.", "NumPy", "Pandas", "Matplotlib", "SciPy");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "In Pandas, what is a DataFrame?", "A DataFrame is a 2D labeled tabular data structure, like a spreadsheet.", "A 2D labeled tabular data structure", "A 1D array of values", "A database connection object", "A plot object");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "NumPy operations are generally faster than equivalent Python loops.", true, "NumPy uses vectorized C/Fortran code, avoiding Python's interpreter overhead on each element.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `df.dropna()` do in Pandas?", "dropna() removes rows (or columns) containing NaN (missing) values.", "Removes rows with missing values", "Fills missing values with 0", "Sorts by missing values count", "Adds a new column");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `df.groupby('category').mean()` compute?", "groupby groups rows by 'category', then mean() computes the average per group.", "Groups rows by 'category' and computes the mean of each group", "Sorts the DataFrame by 'category'", "Filters rows where category equals 'mean'", "Counts values per category");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain the difference between `loc` and `iloc` in Pandas.", "loc is label-based — you use row/column index labels. iloc is position-based — you use integer positions (0, 1, 2...). Both accept slices and boolean arrays.");
        addTf( id, sid, uid, Difficulty.HARD,   1, "Broadcasting in NumPy allows arithmetic operations between arrays of different shapes.", true, "Broadcasting stretches smaller arrays across larger ones without copying data, enabling efficient element-wise operations.");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is vectorization in NumPy and why is it preferred over Python loops?", "Vectorization applies operations to entire arrays at once using optimized C/Fortran code. It avoids Python interpreter overhead per element, resulting in significantly faster execution for numerical computations.");
        return q;
    }

    private Quiz seedE06(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("JavaScript Advanced Concepts", "Closures, Async & Prototypes",
            "Advanced JavaScript exam covering closures, prototypal inheritance, Promises, and event loop.",
            AssessmentType.EXAM, s.get("JS"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JS");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a closure in JavaScript?", "A closure is a function that retains access to its outer scope after the outer function has returned.", "A function that retains access to its outer scope variables", "A function with no parameters", "An immediately invoked function expression", "A method on an object");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "JavaScript uses prototypal inheritance rather than classical class-based inheritance.", true, "JS objects inherit directly from other objects via prototype chains; ES6 class syntax is syntactic sugar over this.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `Promise.all([p1, p2, p3])` do?", "Promise.all resolves when all promises resolve, or rejects as soon as any one rejects.", "Resolves when all promises resolve; rejects if any single promise rejects", "Resolves with the first promise to complete", "Runs promises sequentially", "Ignores any rejections");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the JavaScript event loop?", "The event loop enables non-blocking async by processing callbacks from the queue when the call stack is empty.", "The mechanism that handles async callbacks using a call stack and callback queue", "A for-loop variant for DOM events", "A browser-specific API only", "A way to listen for keyboard events");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`async/await` is syntactic sugar over Promises in JavaScript.", true, "async functions return Promises; await pauses execution until the Promise settles without blocking the thread.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the output of `console.log(1); setTimeout(()=>console.log(2),0); console.log(3);`?", "setTimeout is a macrotask and runs after the current synchronous code completes.", "1, 3, 2", "1, 2, 3", "2, 1, 3", "3, 1, 2");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "The `WeakMap` in JavaScript prevents its keys from being garbage collected.", false, "WeakMap holds weak references — if a key object has no other references, it can be garbage collected.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the JavaScript `Proxy` object allow you to do?", "Proxy intercepts and customizes fundamental object operations like property access and assignment.", "Intercepting and customizing operations on objects", "Creating a deep copy of an object", "Encrypting object properties", "Converting objects to JSON");
        return q;
    }

    private Quiz seedE07(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Linux System Administration", "Server Management",
            "Exam covering Linux file systems, permissions, process management, and shell scripting.",
            AssessmentType.EXAM, s.get("LINUX"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("LINUX");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which command shows disk usage of files and directories?", "du (disk usage) shows space consumed by files; df shows available space on filesystems.", "du", "df", "ls -s", "stat");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How do you view the last 20 lines of a log file in Linux?", "tail -n 20 shows the last N lines; head shows from the beginning.", "tail -n 20 filename", "head -n 20 filename", "cat -20 filename", "grep -20 filename");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "The `/etc/passwd` file stores user account information in Linux.", true, "/etc/passwd holds account info (username, UID, shell); /etc/shadow holds password hashes.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `kill -9 PID` do in Linux?", "Signal 9 (SIGKILL) forcibly terminates a process without cleanup.", "Forcibly terminates a process immediately with SIGKILL", "Gracefully requests a process to stop", "Pauses a process", "Restarts a process");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which command lists open network ports on a Linux system?", "ss (socket statistics) or netstat show network connections and listening ports.", "ss -tlnp", "ifconfig", "ping", "traceroute");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain the difference between hard links and symbolic links in Linux.", "A hard link is another directory entry pointing to the same inode (same file data). A symbolic (soft) link is a pointer to a file path. Hard links cannot span filesystems; symlinks can. Deleting the original file breaks symlinks but not hard links.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `crontab -e` do?", "crontab -e opens the cron job editor for the current user.", "Opens the cron job schedule for editing", "Executes all cron jobs immediately", "Lists all active cron jobs", "Deletes all cron jobs");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is the difference between a process and a thread in Linux?", "A process is an independent program with its own memory space and resources. A thread is a lighter execution unit inside a process that shares the process's memory. Thread context switching is faster than process context switching.");
        return q;
    }

    private Quiz seedE08(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("TypeScript & React Patterns", "Frontend Architecture",
            "Exam on TypeScript generics, utility types, and patterns used in modern frontend development.",
            AssessmentType.EXAM, s.get("TS"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("TS");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What are TypeScript generics used for?", "Generics write type-safe, reusable code that works across multiple types.", "Writing reusable, type-safe code across multiple types", "Importing modules", "Defining enum values", "Creating class hierarchies");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the utility type `Partial<T>` do?", "Partial<T> makes all properties of T optional.", "Makes all properties of T optional", "Makes all properties required", "Makes all properties readonly", "Removes all properties from T");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "TypeScript's `never` type represents a value that can never occur.", true, "never is used for functions that never return (throw or loop forever) and in exhaustive type checks.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is a discriminated union in TypeScript?", "A discriminated union uses a shared literal property to narrow types in conditionals.", "A union type with a common literal property for type narrowing", "An intersection of unrelated types", "An intersection type", "A generic constraint");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`keyof T` in TypeScript produces the union of all property names of type T.", true, "keyof produces a string/number literal union of T's property keys.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `as const` do in TypeScript?", "as const infers the most specific literal types and marks the value as readonly.", "Infers narrowest literal types and makes the value readonly", "Casts to a constant type", "Creates an immutable variable at runtime", "Disables type checking for a value");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is type narrowing in TypeScript?", "Type narrowing refines a wider type to a more specific one within a conditional block.", "Refining a wider type to a more specific type using conditionals or guards", "Widening a type to accept more values", "Converting between primitive types", "Casting with as");
        addTf( id, sid, uid, Difficulty.HARD,   1, "TypeScript uses structural typing — two types are compatible if they have the same shape, regardless of name.", true, "TypeScript checks that a type has at least the required properties, not that it shares a name with another type.");
        return q;
    }

    private Quiz seedE09(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Database Design Final", "Schema & Architecture",
            "Final exam on relational DB design, ER modeling, normalization, and query optimization.",
            AssessmentType.EXAM, s.get("SQL"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("SQL");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does ER stand for in an ER diagram?", "ER stands for Entity-Relationship, used to model data at a conceptual level.", "Entity-Relationship", "Element-Record", "Event-Response", "End-Result");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a composite key?", "A composite key uses two or more columns together to uniquely identify a row.", "A primary key made up of two or more columns", "A key that references another table", "A unique index on one column", "An auto-incrementing surrogate key");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Denormalization can improve read performance at the cost of data redundancy.", true, "Denormalization reduces JOIN operations for reads but increases storage and update complexity.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a surrogate key in a database?", "A surrogate key is an artificial system-generated key (like an auto-increment ID).", "An artificial system-generated key (e.g., auto-increment ID)", "A natural key derived from real-world data", "A key borrowed from another table", "A composite key");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What type of queries does a B-Tree index best optimize?", "B-Tree indexes support efficient range queries and sorted lookups.", "Range queries and sorted lookups", "Full-text search operations only", "Bitmap operations", "Hash-based equality lookups only");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain the difference between OLTP and OLAP systems.", "OLTP (Online Transaction Processing) handles many short, concurrent real-time transactions. OLAP (Online Analytical Processing) handles complex, read-heavy analytical queries on large historical datasets. OLTP is normalized for write efficiency; OLAP is often denormalized for read speed.");
        addTf( id, sid, uid, Difficulty.HARD,   1, "A SQL view is a stored SELECT query that can be queried like a table.", true, "A view is a virtual table defined by a query. Materialized views cache the result set for performance.");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What are the trade-offs of adding an index to a database table?", "Indexes speed up SELECT queries by avoiding full table scans. However, they slow INSERT, UPDATE, and DELETE operations because the index must be maintained. They also consume additional disk space. Indexes benefit frequently-queried, high-cardinality columns most.");
        return q;
    }

    private Quiz seedE10(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Rust Systems Safety Exam", "Ownership & Concurrency",
            "Comprehensive exam on Rust's type system, lifetimes, concurrency, and error handling.",
            AssessmentType.EXAM, s.get("RUST"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("RUST");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `Result<T, E>` represent in Rust?", "Result is either Ok(T) on success or Err(E) on failure, forcing explicit error handling.", "Either Ok(T) on success or Err(E) on failure", "A nullable value like Option", "A future/promise", "A boolean result");
        addTf( id, sid, uid, Difficulty.EASY,   1, "In Rust, data races are prevented at compile time.", true, "Rust's ownership and borrow checker enforces exclusive mutable access, making data races impossible.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is `Arc<T>` used for in Rust?", "Arc (Atomically Reference Counted) provides thread-safe shared ownership.", "Thread-safe shared ownership with atomic reference counting", "Single-threaded shared ownership", "A mutable smart pointer", "An async runtime handle");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `?` operator do in Rust?", "? propagates errors — returns Err early if the result is an error, otherwise unwraps Ok.", "Propagates errors — returns Err from the function if the result is Err", "Marks a value as optional", "Unwraps with a default value", "Checks if a value is null");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`Mutex<T>` in Rust provides interior mutability with mutual exclusion.", true, "Mutex<T> allows shared references to mutate inner data by locking, preventing concurrent access.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is a lifetime elision rule in Rust?", "Lifetime elision lets the compiler infer common lifetime patterns without explicit annotations.", "A compiler rule that infers lifetimes in common patterns without explicit annotation", "A way to extend a variable's lifetime past its scope", "A garbage collection trigger", "A macro expansion rule");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Rust's `Option<T>` replaces null values by wrapping an optional value.", true, "Option<T> is Some(T) when a value exists, or None when absent, eliminating null pointer exceptions.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is an `unsafe` block in Rust?", "unsafe blocks allow operations the compiler cannot statically guarantee are memory-safe.", "A block where the programmer takes responsibility for safety, enabling raw pointers etc.", "Code that panics on any error", "Code without type checking", "A performance optimization hint");
        return q;
    }

    private Quiz seedE11(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Go Microservices Architecture", "Distributed Systems",
            "Exam on building microservices in Go using HTTP, gRPC, channels, and context patterns.",
            AssessmentType.EXAM, s.get("GO"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("GO");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Go package is used to build HTTP servers?", "The net/http package provides HTTP server and client functionality.", "net/http", "http.server", "gorilla/mux", "web.http");
        addTf( id, sid, uid, Difficulty.EASY,   1, "Go interfaces are satisfied implicitly — no `implements` keyword is needed.", true, "Go uses structural typing: if a type has the required methods, it satisfies the interface automatically.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is `context.Context` used for in Go?", "context carries deadlines, cancellation signals, and request-scoped values across API boundaries.", "Carrying deadlines, cancellations, and request-scoped values across function calls", "Storing global application variables", "Managing goroutine pools", "Connecting to databases");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `defer` keyword do in Go?", "defer schedules a function call to run just before the enclosing function returns.", "Schedules a function call to run when the enclosing function returns", "Delays execution by one second", "Runs a function asynchronously", "Catches panics");
        addTf( id, sid, uid, Difficulty.HARD,   1, "`sync.WaitGroup` is used to wait for a collection of goroutines to finish.", true, "WaitGroup uses Add, Done, and Wait to coordinate the completion of multiple goroutines.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What language is a gRPC service definition written in?", "gRPC services are defined in Protocol Buffer (.proto) files.", "Protocol Buffers (.proto files)", "JSON schema", "YAML", "OpenAPI specification");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Go did not support generics until version 1.18.", true, "Go added generics (type parameters) in version 1.18, released in March 2022.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the `errgroup` package provide in Go?", "errgroup synchronizes a group of goroutines and collects the first non-nil error.", "Runs goroutines concurrently and returns the first error encountered", "Logs errors from HTTP servers", "Defines a typed error hierarchy", "Implements a retry mechanism");
        return q;
    }

    private Quiz seedE12(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Kotlin Android Development", "Mobile & JVM",
            "Exam covering Kotlin for Android: data classes, sealed classes, extensions, and flows.",
            AssessmentType.EXAM, s.get("KOTLIN"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("KOTLIN");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does a Kotlin `data class` auto-generate?", "data class generates equals, hashCode, toString, copy, and componentN functions.", "equals(), hashCode(), toString(), copy(), and componentN()", "Only toString()", "Only equals() and hashCode()", "A database schema");
        addTf( id, sid, uid, Difficulty.EASY,   1, "In Kotlin, `val` declares a mutable variable.", false, "val is immutable (read-only); var is mutable. val is equivalent to Java's final.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a sealed class in Kotlin?", "A sealed class restricts its subclasses to a known, closed set defined in the same file.", "A class with a restricted, known set of subclasses", "A class that cannot be instantiated", "An abstract class with no methods", "An interface with a default implementation");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is an extension function in Kotlin?", "Extension functions add new behavior to existing classes without modifying their source.", "A function added to an existing class without modifying it", "A function defined inside another function", "A function that returns another function", "An overloaded constructor");
        addTf( id, sid, uid, Difficulty.HARD,   1, "Kotlin's `companion object` is equivalent to Java's static members.", true, "companion object members belong to the class itself, not to instances — like Java's static.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the Elvis operator `?:` do in Kotlin?", "?: returns the right-hand value if the left-hand expression is null.", "Returns the right-hand value if the left-hand side is null", "A null safety check", "A boolean OR operator", "A range operator");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "Kotlin's `when` expression is a more powerful replacement for Java's switch.", true, "Kotlin's when can match types, ranges, conditions, and arbitrary expressions, and can be used as an expression.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `Flow<T>` represent in Kotlin Coroutines?", "Flow is a cold asynchronous stream that can emit multiple values over time.", "A cold asynchronous data stream emitting multiple values", "A single async result (like Deferred)", "A shared mutable state holder", "A one-shot callback");
        return q;
    }

    private Quiz seedE13(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("PHP Backend Patterns", "Server-Side Architecture",
            "Exam on PHP OOP, design patterns, security best practices, and database interaction.",
            AssessmentType.EXAM, s.get("PHP"), u.getId(), 75, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PHP");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which PHP function escapes HTML special characters to prevent XSS?", "htmlspecialchars() converts <, >, &, \" to HTML entities.", "htmlspecialchars()", "strip_tags()", "addslashes()", "urlencode()");
        addTf( id, sid, uid, Difficulty.EASY,   1, "PHP sessions store user data on the server side.", true, "Sessions store data on the server; only a session ID is sent to the client as a cookie.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the recommended way to prevent SQL injection in PHP?", "PDO prepared statements with bound parameters prevent SQL injection.", "PDO prepared statements with bound parameters", "htmlspecialchars() on all inputs", "addslashes() on user input", "mysql_real_escape_string()");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does PHP Composer do?", "Composer is PHP's dependency manager for managing packages and autoloading.", "Manages PHP packages and dependencies", "Compiles PHP to bytecode", "Runs PHP scripts in parallel", "Generates documentation");
        addTf( id, sid, uid, Difficulty.HARD,   1, "PHP traits allow code reuse across classes without inheritance.", true, "Traits are a horizontal code-reuse mechanism in PHP for single-inheritance languages.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the Singleton design pattern?", "Singleton ensures only one instance of a class exists and provides global access to it.", "Ensures only one instance of a class exists", "Creates a new instance on each call", "Manages a pool of reusable objects", "Allows only static methods in a class");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "PDO (PHP Data Objects) supports multiple database backends.", true, "PDO provides a consistent interface for MySQL, PostgreSQL, SQLite, and other databases.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which HTTP method should be used for idempotent read operations in a REST API?", "GET is safe and idempotent — it retrieves data without side effects.", "GET", "POST", "PATCH", "PUT");
        return q;
    }

    private Quiz seedE14(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("C Language & Pointers Exam", "Systems Programming Foundations",
            "Exam covering C fundamentals: memory layout, pointers, structs, and common pitfalls.",
            AssessmentType.EXAM, s.get("C"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("C");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `malloc()` do in C?", "malloc allocates a specified number of bytes on the heap and returns a void pointer.", "Allocates memory on the heap and returns a pointer", "Allocates memory on the stack", "Frees dynamically allocated memory", "Resizes existing allocated memory");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which function releases dynamically allocated memory in C?", "free() releases heap memory allocated by malloc, calloc, or realloc.", "free()", "delete()", "release()", "dealloc()");
        addTf( id, sid, uid, Difficulty.MEDIUM, 1, "In C, an array name decays to a pointer to its first element when used in expressions.", true, "An array name, when used in an expression, is converted to a pointer to its first element.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a struct in C?", "A struct groups variables of different types under a single user-defined name.", "A user-defined type grouping variables of different types", "A built-in collection type", "A function pointer type", "A dynamic array");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is a function pointer in C?", "A function pointer stores the memory address of a function, enabling indirect calls.", "A variable that holds the address of a function for indirect calling", "A pointer to a struct member", "A C++ concept only", "A pointer to the return value of a function");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is the difference between the stack and the heap in C memory management?", "The stack is automatically managed — local variables are allocated on function entry and freed on return. The heap is manually managed with malloc/free — data persists until explicitly freed. The stack is faster and smaller; the heap allows larger, dynamic allocations.");
        addTf( id, sid, uid, Difficulty.HARD,   1, "Buffer overflow vulnerabilities exploit C's lack of automatic array bounds checking.", true, "C does not check array bounds; writing past an array's end can corrupt memory and is a classic security vulnerability.");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is a segmentation fault in C and name one common cause.", "A segfault occurs when a program accesses memory it is not permitted to access, causing the OS to terminate it. Common causes: dereferencing a null or dangling pointer, accessing freed memory, or writing past array bounds.");
        return q;
    }

    private Quiz seedE15(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Python OOP & Design Patterns", "Advanced Python",
            "Exam on Python's OOP features, magic methods, metaclasses, and common design patterns.",
            AssessmentType.EXAM, s.get("PYTHON"), u.getId(), 90, 65, true, true, false, null, null);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PYTHON");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is the `__init__` method in Python?", "__init__ is called when a new object is instantiated; it initializes the object's attributes.", "The constructor called when a new object is created", "A static factory method", "The destructor method", "A class method decorator");
        addTf( id, sid, uid, Difficulty.EASY,   1, "Python supports multiple inheritance.", true, "Python classes can inherit from multiple base classes; MRO (C3 linearization) determines method resolution order.");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `@staticmethod` do in Python?", "A static method doesn't receive cls or self — it's like a regular function in a class's namespace.", "Defines a method that receives neither cls nor self", "Caches the method result", "Makes the method read-only", "Creates a class-level variable");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the Observer design pattern?", "Observer defines a one-to-many dependency so dependents are notified when one object changes state.", "One object notifies many dependents when its state changes", "One object creates many instances", "Multiple objects share a single resource", "An object delegates work to a helper");
        addTf( id, sid, uid, Difficulty.HARD,   1, "Python's `__slots__` reduces memory usage by preventing instance `__dict__` creation.", true, "__slots__ uses a fixed array instead of a dict for instance attributes, saving memory per object.");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is a metaclass in Python?", "A metaclass is the class of a class — it defines how classes themselves are created and behave.", "The class that defines how a class behaves (class of a class)", "A parent class", "A class that cannot be instantiated", "A singleton class");
        addSa( id, sid, uid, Difficulty.HARD,   2, "Explain Python's Method Resolution Order (MRO) in multiple inheritance.", "MRO determines the order in which base classes are searched for a method. Python uses the C3 linearization algorithm. Inspect it with ClassName.mro() or ClassName.__mro__. It ensures each class appears before its parents, preserving a consistent left-to-right search order.");
        addSa( id, sid, uid, Difficulty.HARD,   2, "What is the difference between `@classmethod` and `@staticmethod` in Python?", "@classmethod receives the class (cls) as the first argument and can access or modify class state. @staticmethod receives no implicit first argument and cannot access class or instance state. Use @classmethod for factory/alternative constructors; use @staticmethod for utility functions logically grouped in a class.");
        return q;
    }

    private Quiz seedS01(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("JS Speed Drill", "JavaScript Rapid Fire", "Fast-paced JavaScript quiz — 15 seconds per question.",
            AssessmentType.SPEED, s.get("JS"), u.getId(), null, 60, true, true, false, 15, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which keyword declares a block-scoped variable in JavaScript?", "let is block-scoped; var is function-scoped.", "let", "var", "def", "scope");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `===` check in JavaScript?", "=== is strict equality — value AND type must match.", "Value and type equality", "Value only", "Type only", "Reference equality");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which JS method removes and returns the last array element?", "pop() removes the last element; push() adds to the end.", "pop()", "push()", "shift()", "slice()");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `NaN === NaN` evaluate to in JavaScript?", "NaN is the only value not equal to itself — use Number.isNaN() to check.", "false", "true", "NaN", "undefined");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the correct way to deep-clone a simple JSON-serializable object?", "JSON.parse(JSON.stringify(obj)) is a simple deep clone for JSON-compatible data.", "JSON.parse(JSON.stringify(obj))", "Object.assign({}, obj)", "{ ...obj }", "obj.clone()");
        return q;
    }

    private Quiz seedS02(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Python Quick Fire", "Python Rapid MCQ", "Fast Python quiz — 12 seconds per question.",
            AssessmentType.SPEED, s.get("PYTHON"), u.getId(), null, 60, true, true, false, 12, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("PYTHON");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is the Python syntax for a list comprehension?", "[expr for item in iterable] is the list comprehension syntax.", "[x for x in range(10)]", "(x for x in range(10))", "{x for x in range(10)}", "list(x for x in range(10))");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which built-in function returns the largest item in an iterable?", "max() returns the largest value in an iterable.", "max()", "min()", "sum()", "abs()");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `dict.get(key, default)` return if the key is absent?", "get() returns the default value (not a KeyError) when the key is missing.", "The default value", "None always", "0", "Raises KeyError");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "How do you reverse a list `lst` in-place in Python?", "list.reverse() modifies the list in-place; reversed() returns an iterator.", "lst.reverse()", "lst[::-1]", "reversed(lst)", "lst.sort(reverse=True)");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the output of `bool([])` in Python?", "An empty list is falsy in Python.", "False", "True", "None", "0");
        return q;
    }

    private Quiz seedS03(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Linux Blitz", "Linux Command Blitz", "Rapid Linux command quiz — 20 seconds per question.",
            AssessmentType.SPEED, s.get("LINUX"), u.getId(), null, 60, true, true, false, 20, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("LINUX");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which command lists the contents of a directory?", "ls lists directory contents.", "ls", "dir", "list", "show");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which command moves or renames a file in Linux?", "mv moves or renames files and directories.", "mv", "cp", "rm", "ln");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which command searches for a text pattern in files?", "grep searches for patterns within files or standard input.", "grep", "find", "sed", "awk");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `ps aux` display?", "ps aux shows all running processes with detailed information.", "All running processes with detailed info", "Disk usage statistics", "Network connections", "Memory usage map");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which command follows a file in real time as it grows (last 100 lines)?", "tail -f follows a growing file; -n sets the initial line count.", "tail -fn 100 filename", "head -n 100 filename", "cat -100 filename", "less +F filename");
        return q;
    }

    private Quiz seedS04(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("SQL Flash Quiz", "SQL Rapid Fire", "Fast SQL recall quiz — 18 seconds per question.",
            AssessmentType.SPEED, s.get("SQL"), u.getId(), null, 60, true, true, false, 18, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("SQL");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL command removes a table and all its data permanently?", "DROP TABLE removes the table structure and all rows.", "DROP TABLE", "DELETE TABLE", "TRUNCATE TABLE", "REMOVE TABLE");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which SQL clause limits the number of rows returned?", "LIMIT (MySQL/PostgreSQL) restricts the result set size.", "LIMIT", "RESTRICT", "MAX ROWS", "FIRST");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the SQL `AVG()` function compute?", "AVG() computes the arithmetic mean of non-null values in a column.", "The average of non-null values in a column", "The maximum value", "The row count", "The column sum");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is a subquery in SQL?", "A subquery is a SELECT statement nested inside another SQL statement.", "A SELECT query nested inside another query", "A stored procedure", "A database view", "A trigger");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which SQL keyword defines the window for a window function?", "OVER defines the partition and ordering for window functions.", "OVER", "WITHIN", "PARTITION", "WINDOW");
        return q;
    }

    private Quiz seedS05(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("HTML Blitz Round", "HTML Rapid MCQ", "Speed quiz on HTML tags, attributes, and semantics — 15 seconds per question.",
            AssessmentType.SPEED, s.get("HTML"), u.getId(), null, 60, true, true, false, 15, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("HTML");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which HTML tag creates a numbered (ordered) list?", "<ol> creates an ordered list; <ul> creates an unordered list.", "<ol>", "<ul>", "<li>", "<dl>");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does the `src` attribute in an `<img>` tag specify?", "src specifies the URL of the image file to display.", "The URL of the image to display", "The image alt text", "The image file size", "The image display format");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which HTML input type renders a date picker?", "<input type=\"date\"> shows a native date picker.", "date", "calendar", "datetime", "picker");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `<form method=\"post\">` do?", "method=post sends form data in the HTTP request body.", "Sends form data in the HTTP request body", "Appends form data to the URL as query parameters", "Submits the form via GET", "Encrypts the form data");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "Which HTML5 element wraps self-contained, distributable content such as an article?", "<article> wraps independently distributable content like a news article.", "<article>", "<section>", "<div>", "<content>");
        return q;
    }

    private Quiz seedS06(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Java Syntax Sprint", "Java Rapid Fire", "Rapid Java syntax and concepts quiz — 15 seconds per question.",
            AssessmentType.SPEED, s.get("JAVA"), u.getId(), null, 60, true, true, false, 15, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("JAVA");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Java primitive type stores true/false values?", "boolean stores true or false.", "boolean", "bit", "flag", "bool");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `System.out.println()` do?", "println prints the argument to standard output followed by a newline.", "Prints text to standard output with a newline", "Reads from standard input", "Writes to a file", "Logs a message to stderr");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which keyword is used to inherit a class in Java?", "extends is used for class inheritance; implements is for interfaces.", "extends", "implements", "inherits", "derives");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the default value of an uninitialized int field in a Java class?", "Numeric fields default to 0; booleans to false; object references to null.", "0", "null", "-1", "undefined");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the `transient` keyword do in Java?", "transient excludes a field from Java object serialization.", "Marks a field to be excluded during serialization", "Makes a field thread-safe", "Prevents field modification", "Makes a field belong to the class (static)");
        return q;
    }

    private Quiz seedS07(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("CSS Rapid Fire", "CSS Speed Round", "Lightning-fast CSS properties quiz — 12 seconds per question.",
            AssessmentType.SPEED, s.get("CSS"), u.getId(), null, 60, true, true, false, 12, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("CSS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which CSS property sets the text color?", "color sets the foreground (text) color of an element.", "color", "text-color", "font-color", "foreground");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which CSS property adds space outside an element's border?", "margin adds space outside; padding adds space inside the border.", "margin", "padding", "border", "spacing");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which CSS unit is relative to the viewport width?", "vw = 1% of the viewport width.", "vw", "em", "rem", "px");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which CSS property hides an element while keeping it in the document flow?", "visibility: hidden hides but preserves space; display: none removes from flow.", "visibility: hidden", "display: none", "opacity: 0", "z-index: -1");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the CSS `z-index` property control?", "z-index sets the stacking order of positioned elements along the z-axis.", "The stacking order of positioned elements", "The zoom level", "The border width", "The column count");
        return q;
    }

    private Quiz seedS08(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("C++ Memory Sprint", "Pointers & Smart Ptrs", "C++ memory management speed quiz — 20 seconds per question.",
            AssessmentType.SPEED, s.get("C++"), u.getId(), null, 60, true, true, false, 20, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("C++");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which C++ smart pointer allows shared ownership?", "shared_ptr uses reference counting for shared ownership among multiple owners.", "std::shared_ptr", "std::unique_ptr", "std::weak_ptr", "std::auto_ptr");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What does `nullptr` represent in modern C++?", "nullptr is the null pointer constant in C++ (preferred over NULL or 0).", "A null pointer constant", "Integer 0", "An empty string", "An undefined variable");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `std::move()` enable in C++?", "std::move enables move semantics, transferring ownership without copying.", "Move semantics — transferring ownership without copying", "A deep copy of the object", "Deleting a pointer", "Atomic memory operations");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "Which C++ operator accesses members through a pointer?", "-> dereferences a pointer and accesses a member in one step.", "->", ".", "::", "&");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is placement new in C++?", "Placement new constructs an object at a specific, pre-allocated memory address.", "Constructs an object at a specific memory address", "Allocates on the stack", "Reallocates existing memory", "A debug-mode allocator");
        return q;
    }

    private Quiz seedS09(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("TypeScript Blitz", "TS Type System Speed", "TypeScript type system speed quiz — 15 seconds per question.",
            AssessmentType.SPEED, s.get("TS"), u.getId(), null, 60, true, true, false, 15, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("TS");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which TypeScript type disables type checking for a value?", "any disables all type checking — avoid it where possible.", "any", "unknown", "void", "never");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How do you annotate a function's return type in TypeScript?", "Return type annotation follows the parameter list with a colon.", "function greet(): string {}", "function greet() => string {}", "function greet() -> string {}", "string function greet() {}");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the utility type `Required<T>` do?", "Required<T> makes all optional properties of T required.", "Makes all optional properties of T required", "Makes all properties optional", "Removes all properties", "Makes all properties readonly");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What is the key difference between `unknown` and `any` in TypeScript?", "unknown requires type narrowing before use; any bypasses all type checks.", "unknown requires type narrowing before use; any bypasses all checks", "They are functionally identical", "unknown is deprecated in newer TypeScript", "any is stricter than unknown");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does the `infer` keyword do inside TypeScript conditional types?", "infer introduces a type variable that captures a matched type within a conditional.", "Extracts and names a type variable within a conditional type", "Imports a type from another module", "Asserts a type cast", "Adds a generic constraint");
        return q;
    }

    private Quiz seedS10(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Kotlin Speed Round", "Kotlin Rapid Fire", "Quick Kotlin syntax quiz — 18 seconds per question.",
            AssessmentType.SPEED, s.get("KOTLIN"), u.getId(), null, 60, true, true, false, 18, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("KOTLIN");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How do you declare an immutable variable in Kotlin?", "val is read-only; var is mutable.", "val", "var", "const", "final");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Kotlin function applies a transformation to each collection element?", "map transforms each element and returns a new list.", "map", "filter", "forEach", "reduce");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the safe-call operator `?.` do in Kotlin?", "?. calls the method only if the receiver is non-null; otherwise returns null.", "Calls the method only if the receiver is non-null, returning null otherwise", "Throws NullPointerException if null", "Casts to a nullable type", "Converts null to a default value");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `also` scope function do in Kotlin?", "also runs a block for side effects and returns the original receiver unchanged.", "Performs an action and returns the original receiver", "Transforms and returns a new value", "Runs a block and returns its result", "Converts the receiver to a string");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is the purpose of the `inline` modifier for Kotlin functions?", "inline copies the function body at the call site, avoiding lambda object allocation overhead.", "Copies the function body at call site to avoid lambda allocation overhead", "Makes the function private", "Marks the function as deprecated", "Prevents the function from being overridden");
        return q;
    }

    private Quiz seedS11(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Go Lightning Quiz", "Go Speed Round", "Fast Go language quiz — 12 seconds per question.",
            AssessmentType.SPEED, s.get("GO"), u.getId(), null, 60, true, true, false, 12, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("GO");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "How do you declare and initialize a variable in Go using short syntax?", ":= is the short variable declaration operator.", "x := 5", "var x = 5", "int x = 5", "x = 5");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Go function prints formatted output to stdout?", "fmt.Printf formats and prints to standard output.", "fmt.Printf", "fmt.Println", "print()", "log.Printf");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does the `range` keyword do in a Go for loop?", "range iterates over slices, maps, strings, channels, returning index and value.", "Iterates over a collection, returning index and value", "Creates a numeric range object", "Generates random numbers", "Declares a range type");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "How does Go handle errors idiomatically?", "Go returns errors as the last return value; callers check err != nil.", "Return error as the last value and check err != nil", "Use try/catch blocks", "Use panic/recover for all errors", "Ignore errors with the blank identifier _");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What does `make(chan int, 5)` create in Go?", "make with capacity creates a buffered channel that can hold up to 5 values.", "A buffered integer channel with capacity 5", "An unbuffered integer channel", "A goroutine pool of 5", "A slice of 5 integers");
        return q;
    }

    private Quiz seedS12(User u, Map<String, Integer> s) {
        Quiz q = findOrCreateQuiz("Rust Safety Sprint", "Rust Rapid Fire", "Rust ownership and safety speed quiz — 20 seconds per question.",
            AssessmentType.SPEED, s.get("RUST"), u.getId(), null, 60, true, true, false, 20, QuestionType.MCQ);
        if (hasQuestions(q.getId())) return q;
        int id = q.getId(), uid = u.getId(); Integer sid = s.get("RUST");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "What is the Rust equivalent of a null/optional value?", "Option<T> represents an optional value — Some(T) or None.", "Option<T>", "Result<T, E>", "Maybe<T>", "Nullable<T>");
        addMcq(id, sid, uid, Difficulty.EASY,   1, "Which Rust trait must a type implement to be copied by value?", "Copy allows values to be duplicated by bit-copying.", "Copy", "Clone", "Sized", "Drop");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `Box<T>` provide in Rust?", "Box<T> allocates a value on the heap with single ownership.", "Heap allocation with single ownership", "Shared ownership via reference counting", "Interior mutability", "Thread-safe shared ownership");
        addMcq(id, sid, uid, Difficulty.MEDIUM, 1, "What does `#[derive(Debug)]` do in Rust?", "#[derive(Debug)] auto-generates a Debug implementation for printing with {:?}.", "Auto-generates a Debug implementation for the type", "Enables runtime debugging mode", "Marks the type as unsafe", "Implements Display for the type");
        addMcq(id, sid, uid, Difficulty.HARD,   1, "What is a trait object in Rust (e.g., `dyn Trait`)?", "A trait object enables runtime polymorphism via dynamic dispatch.", "A pointer to a type implementing a trait, enabling runtime polymorphism", "A generic type parameter", "A compile-time constant", "A reference-counted type");
        return q;
    }

    private void seedDemoBankQuestions(Map<String, User> users, Map<String, Integer> sids) {
        User ng = users.get("dr.nguyen");
        User li = users.get("ms.linda");
        User ra = users.get("mr.rafael");

        if (!questionRepository.findBankQuestions(ng.getId(), null, null, null, null).isEmpty()) return;

        Integer jsId   = sids.get("JS");
        Integer pyId   = sids.get("PYTHON");
        Integer sqlId  = sids.get("SQL");
        Integer javaId = sids.get("JAVA");
        Integer cppId  = sids.get("C++");
        Integer linuxId = sids.get("LINUX");
        Integer tsId   = sids.get("TS");

        addBankMcq(jsId, ng.getId(), Difficulty.EASY,   1, "Which method adds an element to the end of a JavaScript array?",
            "push() adds to the end; pop() removes from the end.", "push()", "pop()", "shift()", "splice()");
        addBankTf(jsId, ng.getId(), Difficulty.MEDIUM, 1, "JavaScript is a statically-typed language.",
            false, "JavaScript is dynamically typed — types are resolved at runtime, not compile time.");
        addBankMcq(jsId, ng.getId(), Difficulty.HARD,   2, "What does 'Maximum call stack size exceeded' indicate in JavaScript?",
            "This error occurs when infinite recursion fills the call stack beyond the engine's limit.", "Infinite recursion", "A memory leak", "A syntax error", "A type mismatch");
        addBankSa(jsId, li.getId(), Difficulty.HARD, 2, "Explain the difference between synchronous and asynchronous code execution in JavaScript.",
            "Synchronous code runs line-by-line, blocking the thread until each statement finishes. Asynchronous code (Promises, async/await, callbacks) allows non-blocking execution via the event loop, enabling I/O operations without freezing the page.");

        addBankMcq(pyId, ng.getId(), Difficulty.EASY,   1, "Which Python keyword makes a function into a generator?",
            "yield pauses the function and yields a value; calling next() resumes it.", "yield", "return", "generate", "async");
        addBankMcq(pyId, ng.getId(), Difficulty.MEDIUM, 1, "What does `zip([1,2,3], ['a','b','c'])` produce?",
            "zip pairs elements from each iterable into tuples.", "A zip object of tuples: (1,'a'), (2,'b'), (3,'c')", "A flat merged list", "A dict", "An error");
        addBankTf(pyId, ng.getId(), Difficulty.MEDIUM, 1, "Python's `is` operator checks value equality.",
            false, "`is` checks identity (same object in memory); `==` checks value equality.");
        addBankSa(pyId, ng.getId(), Difficulty.HARD, 2, "What is a Python decorator and how does it work?",
            "A decorator is a function that wraps another function to extend its behavior without modifying it. Applied with @decorator_name above the function definition. Common uses: logging, caching, authorization.");

        addBankMcq(sqlId, ng.getId(), Difficulty.EASY,   1, "Which SQL aggregate function counts rows?",
            "COUNT() counts rows (or non-null values in a specified column).", "COUNT()", "SUM()", "AVG()", "MAX()");
        addBankMcq(sqlId, ng.getId(), Difficulty.MEDIUM, 1.5, "What is a SQL transaction?",
            "A transaction groups SQL statements into a single atomic unit with ACID properties.",
            "A group of SQL statements treated as a single atomic unit", "A type of JOIN", "A stored procedure", "A database backup operation");
        addBankTf(sqlId, ng.getId(), Difficulty.HARD, 2, "A UNIQUE constraint allows multiple NULL values in the same column.",
            true, "Most databases treat NULL as unknown; UNIQUE allows multiple NULLs since NULL != NULL.");

        addBankMcq(javaId, ng.getId(), Difficulty.EASY,   1, "Which Java access modifier restricts access to the same class only?",
            "private limits access to the declaring class.", "private", "protected", "public", "default (package-private)");
        addBankMcq(javaId, ng.getId(), Difficulty.MEDIUM, 1, "What does the `static` keyword mean when applied to a Java field or method?",
            "static means the member belongs to the class itself, not to any particular instance.",
            "Belongs to the class, not to instances", "Cannot be changed after initialization", "Is automatically inherited", "Is thread-safe");
        addBankTf(javaId, ng.getId(), Difficulty.MEDIUM, 1, "Java supports multiple inheritance through classes.",
            false, "Java supports multiple inheritance only through interfaces, not through class extension.");
        addBankSa(javaId, ng.getId(), Difficulty.HARD, 2, "Explain the difference between checked and unchecked exceptions in Java.",
            "Checked exceptions must be declared with `throws` or caught in a try-catch block (e.g., IOException). Unchecked exceptions extend RuntimeException and do not require explicit handling (e.g., NullPointerException, ArrayIndexOutOfBoundsException).");

        addBankMcq(cppId, ra.getId(), Difficulty.EASY,   1, "What does `std::vector` provide in C++?",
            "std::vector is a dynamic-size, contiguous array in the STL.", "A dynamic resizable array", "A fixed-size array", "A doubly-linked list", "A hash map");
        addBankMcq(cppId, ra.getId(), Difficulty.MEDIUM, 1.5, "What is a virtual function in C++?",
            "virtual functions enable runtime polymorphism via the v-table dispatch mechanism.",
            "A function resolved at runtime based on the object's actual type", "A function that returns void", "A function defined outside the class", "A function that cannot be overridden");
        addBankTf(cppId, ra.getId(), Difficulty.HARD, 2, "A pure virtual function must be overridden in all concrete derived classes.",
            true, "A class with a pure virtual function is abstract. Concrete subclasses must implement all pure virtual functions.");

        addBankMcq(linuxId, ra.getId(), Difficulty.MEDIUM, 1, "Which command shows currently running processes in Linux?",
            "ps shows a snapshot of running processes; top shows them in real time.", "ps", "ls", "jobs", "htop");

        addBankMcq(tsId, li.getId(), Difficulty.MEDIUM, 1, "What does the `readonly` modifier do in TypeScript?",
            "readonly prevents reassignment of a property after initialization.",
            "Prevents a property from being reassigned after initialization", "Makes a property private", "Makes a property optional", "Makes a property nullable");
    }

    private void seedDemoAttempts(Map<String, User> users, List<Quiz> allQuizzes) {
        List<Quiz> quizType = new ArrayList<>();
        List<Quiz> examType = new ArrayList<>();
        for (Quiz q : allQuizzes) {
            if (q.getAssessmentType() == AssessmentType.QUIZ) quizType.add(q);
            else if (q.getAssessmentType() == AssessmentType.EXAM) examType.add(q);
        }

        User alice   = users.get("alice");
        User bob     = users.get("bob");
        User charlie = users.get("charlie");
        User diana   = users.get("diana");
        User ethan   = users.get("ethan");
        User fatima  = users.get("fatima");
        User george  = users.get("george");
        User hana    = users.get("hana");

        for (int i = 0; i < quizType.size(); i++) {
            Quiz qz = quizType.get(i);
            List<Question> qs = questionRepository.findByQuizId(qz.getId());
            if (qs.isEmpty()) continue;
            int total = qs.size(), psc = qz.getPassScore();
            if (i < 5) {

                seedGraded(qz.getId(), alice.getId(),   qs, total - 1, psc);
                seedGraded(qz.getId(), bob.getId(),     qs, total - 1, psc);
                seedGraded(qz.getId(), charlie.getId(), qs, 2,         psc);
                seedGraded(qz.getId(), diana.getId(),   qs, 1,         psc);
            } else if (i < 10) {

                seedGraded(qz.getId(), alice.getId(),  qs, total,     psc);
                seedGraded(qz.getId(), bob.getId(),    qs, total - 1, psc);
                seedGraded(qz.getId(), ethan.getId(),  qs, 2,         psc);
                seedGraded(qz.getId(), fatima.getId(), qs, total - 1, psc);
            } else {

                seedGraded(qz.getId(), bob.getId(),    qs, total - 1, psc);
                seedGraded(qz.getId(), charlie.getId(),qs, 2,         psc);
                seedGraded(qz.getId(), george.getId(), qs, total - 1, psc);
                seedGraded(qz.getId(), hana.getId(),   qs, total - 1, psc);
            }
        }

        for (int i = 0; i < examType.size(); i++) {
            Quiz ex = examType.get(i);
            List<Question> qs = questionRepository.findByQuizId(ex.getId());
            if (qs.isEmpty()) continue;
            int total = qs.size(), psc = ex.getPassScore();
            if (i < 5) {

                seedGraded(ex.getId(), ethan.getId(),  qs, total - 1, psc);
                seedGraded(ex.getId(), fatima.getId(), qs, total,     psc);
                seedGraded(ex.getId(), george.getId(), qs, 2,         psc);
                seedPending(ex.getId(), hana.getId(),  qs);
            } else if (i < 10) {

                seedGraded(ex.getId(), alice.getId(),   qs, total - 1, psc);
                seedGraded(ex.getId(), charlie.getId(), qs, 2,         psc);
                seedPending(ex.getId(), diana.getId(),  qs);
            } else {

                seedPending(ex.getId(), bob.getId(),     qs);
                seedPending(ex.getId(), charlie.getId(), qs);
            }
        }
    }

    private void seedGraded(int quizId, int studentId, List<Question> questions, int correctCount, int passScore) {
        if (attemptRepository.findLatestAttempt(quizId, studentId).isPresent()) return;

        Attempt attempt = attemptRepository.createAttempt(quizId, studentId);
        if (attempt == null) return;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            boolean shouldBeCorrect = i < correctCount;
            if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                String text = shouldBeCorrect
                    ? "This is a well-structured answer that addresses all key aspects of the question with clear reasoning and relevant technical detail."
                    : "I think the answer is something related to what we covered in class, but I'm not entirely certain.";
                attemptRepository.saveAnswer(attempt.getId(), q.getId(), null, text);
            } else {
                QuestionOption target = null;
                if (shouldBeCorrect) {
                    target = q.getOptions().stream().filter(QuestionOption::isCorrect).findFirst().orElse(null);
                }
                if (target == null) {
                    target = q.getOptions().stream().filter(o -> !o.isCorrect()).findFirst().orElse(
                        q.getOptions().isEmpty() ? null : q.getOptions().get(0));
                }
                attemptRepository.saveAnswer(attempt.getId(), q.getId(),
                    target != null ? target.getId() : null, null);
            }
        }

        attemptRepository.finalizeAttempt(attempt.getId(), AttemptStatus.TURNED_IN);

        List<AttemptAnswer> answers = attemptRepository.getAttemptAnswers(attempt.getId());
        double total = 0, max = 0;
        for (AttemptAnswer aa : answers) {
            Question q = questions.stream()
                .filter(x -> x.getId() != null && x.getId().equals(aa.getQuestionId()))
                .findFirst().orElse(null);
            if (q == null) continue;
            max += q.getPoints();

            boolean correct;
            double pts;
            Integer aiScore = null;
            String aiFeedback = null;
            String teacherFeedback = null;

            if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                String txt = aa.getTextAnswer() != null ? aa.getTextAnswer() : "";
                correct = txt.length() > 60;
                pts = correct ? q.getPoints() : q.getPoints() * 0.4;
                aiScore = correct ? 88 : 43;
                aiFeedback = correct
                    ? "Strong answer demonstrating solid conceptual understanding with good technical detail."
                    : "Answer lacks depth and key technical concepts. Review the relevant material.";
                teacherFeedback = correct ? "Well done!" : "Please review and revise.";
            } else {
                QuestionOption correctOpt = q.getOptions().stream()
                    .filter(QuestionOption::isCorrect).findFirst().orElse(null);
                correct = correctOpt != null && aa.getSelectedOptionId() != null
                    && correctOpt.getId().equals(aa.getSelectedOptionId());
                pts = correct ? q.getPoints() : 0;
            }

            attemptRepository.updateAnswerGrade(aa.getId(), correct, pts, aiScore, aiFeedback, teacherFeedback);
            total += pts;
        }

        double pct = max > 0 ? total / max * 100.0 : 0;
        resultRepository.saveResult(Result.builder()
            .attemptId(attempt.getId()).studentId(studentId).quizId(quizId)
            .totalPoints(total).maxPoints(max).percentage(pct).passed(pct >= passScore)
            .build());
        attemptRepository.finalizeAttempt(attempt.getId(), AttemptStatus.GRADED);
    }

    private void seedPending(int quizId, int studentId, List<Question> questions) {
        if (attemptRepository.findLatestAttempt(quizId, studentId).isPresent()) return;

        Attempt attempt = attemptRepository.createAttempt(quizId, studentId);
        if (attempt == null) return;

        for (Question q : questions) {
            if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                attemptRepository.saveAnswer(attempt.getId(), q.getId(), null,
                    "My answer based on my understanding of the material covered in this module.");
            } else {
                QuestionOption opt = q.getOptions().isEmpty() ? null : q.getOptions().get(0);
                attemptRepository.saveAnswer(attempt.getId(), q.getId(),
                    opt != null ? opt.getId() : null, null);
            }
        }
        attemptRepository.finalizeAttempt(attempt.getId(), AttemptStatus.TURNED_IN);
    }

    private void seedDemoInbox(Map<String, User> users, List<Quiz> quizzes) {
        User alice   = users.get("alice");
        User bob     = users.get("bob");
        User charlie = users.get("charlie");
        User diana   = users.get("diana");
        User hana    = users.get("hana");
        User ethan   = users.get("ethan");
        User fatima  = users.get("fatima");
        User george  = users.get("george");
        User nguyen  = users.get("dr.nguyen");
        User linda   = users.get("ms.linda");
        User rafael  = users.get("mr.rafael");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        createNotification(null, alice.getId(),  "Welcome to Proctor!", "Welcome to the Proctor assessment platform. Your account is ready to use.", null, true, now);
        createNotification(null, bob.getId(),    "Welcome to Proctor!", "Welcome to the Proctor assessment platform. Your account is ready to use.", null, true, now);
        createNotification(null, charlie.getId(),"Welcome to Proctor!", "Welcome to Proctor. Start exploring available quizzes and exams in the portal.", null, true, now);
        createNotification(null, fatima.getId(), "Welcome to Proctor!", "Welcome to Proctor. Your account has been set up. Browse subjects and take quizzes!", null, true, now);

        Quiz jsQuiz   = findQuizByPartialTitle(quizzes, "JavaScript Fundamentals");
        Quiz webExam  = findQuizByPartialTitle(quizzes, "Final: Web Development");
        Quiz javaExam = findQuizByPartialTitle(quizzes, "Midterm: Java OOP");

        if (jsQuiz != null)
            createNotification(nguyen.getId(), alice.getId(), "Your JavaScript Fundamentals quiz has been graded",
                "Your submission for JavaScript Fundamentals has been reviewed. Check your results in the portal.", jsQuiz.getId(), false, now);
        if (webExam != null) {
            createNotification(linda.getId(), diana.getId(), "Final: Web Development exam is now published",
                "The Web Development final exam is now live. Head to the portal to attempt it.", webExam.getId(), false, now);
            createNotification(linda.getId(), ethan.getId(), "Final: Web Development exam is now published",
                "The Web Development final exam has been published and is ready for you to take.", webExam.getId(), false, now);
        }
        if (javaExam != null)
            createNotification(nguyen.getId(), ethan.getId(), "Your Java OOP midterm has been graded",
                "Your Midterm: Java OOP exam has been reviewed and graded. View your result in the portal.", javaExam.getId(), false, now);

        Quiz pythonQuiz = findQuizByPartialTitle(quizzes, "Python Essentials");
        Quiz htmlQuiz   = findQuizByPartialTitle(quizzes, "HTML & CSS Basics");
        Quiz linuxExam  = findQuizByPartialTitle(quizzes, "Linux System Administration");

        if (jsQuiz != null)
            createRetake(alice.getId(), nguyen.getId(), InboxMessageType.QUIZ_RETAKE, jsQuiz.getId(),
                "Retake Request - JavaScript Fundamentals",
                "I struggled with the closures and arrow functions section. I've reviewed the material thoroughly and feel ready to retake. Please approve.",
                InboxStatus.PENDING, now);
        if (pythonQuiz != null)
            createRetake(bob.getId(), nguyen.getId(), InboxMessageType.QUIZ_RETAKE, pythonQuiz.getId(),
                "Retake Request - Python Essentials",
                "I scored below passing. I've studied Python lists, functions, and time complexity more carefully this time.",
                InboxStatus.APPROVED, now);
        if (htmlQuiz != null)
            createRetake(george.getId(), linda.getId(), InboxMessageType.QUIZ_RETAKE, htmlQuiz.getId(),
                "Retake Request - HTML & CSS Basics",
                "I need more practice with CSS specificity and the box model. Requesting a retake after thorough review.",
                InboxStatus.PENDING, now);
        if (javaExam != null)
            createRetake(charlie.getId(), nguyen.getId(), InboxMessageType.EXAM_RETAKE, javaExam.getId(),
                "Retake Request - Midterm: Java OOP",
                "I was unprepared for the abstract class vs interface question. I have since reviewed all OOP chapters in depth.",
                InboxStatus.PENDING, now);
        if (webExam != null)
            createRetake(diana.getId(), linda.getId(), InboxMessageType.EXAM_RETAKE, webExam.getId(),
                "Retake Request - Final: Web Development",
                "I missed the exam due to a personal emergency. I am requesting consideration for a makeup attempt.",
                InboxStatus.REJECTED, now);
        if (linuxExam != null)
            createRetake(hana.getId(), rafael.getId(), InboxMessageType.EXAM_RETAKE, linuxExam.getId(),
                "Retake Request - Linux System Administration",
                "I scored 58% which is just below the passing threshold. I believe I understand the material well enough to pass on a retake.",
                InboxStatus.PENDING, now);
    }

    private Quiz findQuizByPartialTitle(List<Quiz> quizzes, String partial) {
        return quizzes.stream()
            .filter(q -> q.getTitle().equalsIgnoreCase(partial) || q.getTitle().contains(partial))
            .findFirst().orElse(null);
    }

    private void createNotification(Integer senderId, int recipientId, String title, String body,
            Integer targetId, boolean read, Timestamp ts) {
        List<InboxMessage> existing = inboxRepository.findByRecipientId(recipientId);
        for (InboxMessage m : existing) {
            if (m.getTitle().equalsIgnoreCase(title) &&
                Objects.equals(m.getSenderId(), senderId)) return;
        }
        inboxRepository.create(InboxMessage.builder()
            .senderId(senderId).recipientId(recipientId)
            .type(InboxMessageType.NOTIFICATION)
            .title(title).body(body).targetId(targetId)
            .status(InboxStatus.RESOLVED).read(read).createdAt(ts)
            .build());
    }

    private void createRetake(int senderId, int recipientId, InboxMessageType type,
            int targetId, String title, String body, InboxStatus status, Timestamp ts) {
        if (status == InboxStatus.PENDING && inboxRepository.hasPendingRequest(senderId, type, targetId)) return;

        List<InboxMessage> existing = inboxRepository.findByRecipientId(recipientId);
        for (InboxMessage m : existing) {
            if (m.getTitle().equalsIgnoreCase(title) && Integer.valueOf(senderId).equals(m.getSenderId())) return;
        }
        inboxRepository.create(InboxMessage.builder()
            .senderId(senderId).recipientId(recipientId)
            .type(type).title(title).body(body).targetId(targetId)
            .status(status).read(status != InboxStatus.PENDING).createdAt(ts)
            .build());
    }
}
