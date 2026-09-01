package model.service.impl;

import model.service.CertificateService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CertificateServiceImpl implements CertificateService {
    private static final String TEMPLATE_RESOURCE = "/certificate.jrxml";
    private static final Path OUTPUT_DIR = Path.of("certificates");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final ExamService examService;
    private final QuizService quizService;
    private final UserService userService;

    // Compiling a JRXML is relatively expensive; the compiled report is immutable, so cache it.
    private JasperReport compiledTemplate;

    public CertificateServiceImpl(ExamService examService, QuizService quizService, UserService userService) {
        this.examService = examService;
        this.quizService = quizService;
        this.userService = userService;
    }

    @Override
    public String generate(Long attemptId) {
        Attempt attempt = examService.get(attemptId);
        if (!Boolean.TRUE.equals(attempt.getPassed())) {
            throw new IllegalStateException("Certificates can only be generated for passed attempts.");
        }
        Quiz quiz = quizService.get(attempt.getQuizId());
        User student = userService.get(attempt.getStudentId());

        Map<String, Object> params = new HashMap<>();
        params.put("studentName", student.getFullName());
        params.put("quizTitle", quiz.getTitle());
        params.put("scoreText", attempt.getScore() + " / " + attempt.getTotalMarks());
        params.put("percentageText", attempt.getPercentage().toString());
        params.put("dateText", (attempt.getEndTime() == null ? attempt.getStartTime() : attempt.getEndTime())
                .format(DATE_FORMAT));
        params.put("certificateId", "CERT-" + attempt.getQuizId() + "-" + attempt.getStudentId() + "-" + attempt.getId());

        try {
            JasperReport report = compiledReport();
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            Files.createDirectories(OUTPUT_DIR);
            Path outputPath = OUTPUT_DIR.resolve("certificate_attempt_" + attemptId + ".pdf");
            JasperExportManager.exportReportToPdfFile(print, outputPath.toString());
            return outputPath.toAbsolutePath().toString();
        } catch (JRException | IOException e) {
            throw new IllegalStateException("Failed to generate certificate: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> generateAllForQuiz(Long quizId) {
        List<String> paths = new ArrayList<>();
        for (Attempt attempt : examService.attemptsForQuiz(quizId)) {
            if (Boolean.TRUE.equals(attempt.getPassed())) {
                paths.add(generate(attempt.getId()));
            }
        }
        return paths;
    }

    private synchronized JasperReport compiledReport() throws JRException {
        if (compiledTemplate == null) {
            try (InputStream in = getClass().getResourceAsStream(TEMPLATE_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Certificate template not found on classpath: " + TEMPLATE_RESOURCE);
                }
                compiledTemplate = JasperCompileManager.compileReport(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read certificate template", e);
            }
        }
        return compiledTemplate;
    }
}
