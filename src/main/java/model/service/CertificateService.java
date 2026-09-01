package model.service;

import java.util.List;

public interface CertificateService {
    /** Generates a PDF certificate for one passed attempt and returns the file path it was saved to. */
    String generate(Long attemptId);

    /** Generates a certificate for every passed attempt on a quiz. Returns the file paths written. */
    List<String> generateAllForQuiz(Long quizId);
}
