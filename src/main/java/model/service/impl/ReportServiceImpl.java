package model.service.impl;

import model.entity.User;
import model.entity.enums.Role;
import model.repository.UserRepo;
import model.service.ReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {
    private final AttemptDao attemptDao;
    private final UserRepo userRepo;
    private final SubjectDao subjectDao;
    private final QuestionDao questionDao;
    private final QuizDao quizDao;

    public ReportServiceImpl(AttemptDao attemptDao, UserRepo userRepo, SubjectDao subjectDao,
                             QuestionDao questionDao, QuizDao quizDao) {
        this.attemptDao = attemptDao;
        this.userRepo = userRepo;
        this.subjectDao = subjectDao;
        this.questionDao = questionDao;
        this.quizDao = quizDao;
    }

    @Override
    public QuizStats statsForQuiz(Long quizId) {
        List<Attempt> finished = attemptDao.findByQuiz(quizId).stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .toList();

        long attemptCount = finished.size();
        long passCount = finished.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        long failCount = attemptCount - passCount;

        BigDecimal avg = average(finished);
        BigDecimal highest = finished.stream().map(Attempt::getPercentage)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal lowest = finished.stream().map(Attempt::getPercentage)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        return new QuizStats(attemptCount, passCount, failCount, avg, highest, lowest);
    }

    private BigDecimal average(List<Attempt> attempts) {
        if (attempts.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = attempts.stream().map(Attempt::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(attempts.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public PlatformStats platformStats() {
        List<User> users = userRepo.findAll();
        Map<String, Long> byRole = users.stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));
        for (Role r : Role.values()) {
            byRole.putIfAbsent(r.name(), 0L);
        }

        long subjects = subjectDao.findAll().size();
        long quizzes = quizDao.findAll().size();
        long attempts = quizDao.findAll().stream()
                .mapToLong(q -> attemptDao.findByQuiz(q.getId()).size())
                .sum();

        return new ReportService.PlatformStats(users.size(), byRole, subjects, questionCount(), quizzes, attempts);
    }

    private long questionCount() {
        return subjectDao.findAll().stream()
                .mapToLong(s -> questionDao.findBySubject(s.getId()).size())
                .sum();
    }

    @Override
    public List<Attempt> leaderboard(Long quizId, int limit) {
        return attemptDao.findByQuiz(quizId).stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Attempt::getPercentage, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }
}
