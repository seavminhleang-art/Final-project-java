package com.proctor.model.service;

import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.entity.Result;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.model.repository.PortalRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PortalService {
    private final PortalRepository portalRepository;

    public PortalService(PortalRepository portalRepository) {
        this.portalRepository = portalRepository;
    }

    public List<Result> getStudentHistory(int studentId) {
        Optional<User> callerOpt = Session.getCurrentUser();
        if (callerOpt.isPresent()) {
            User caller = callerOpt.get();
            if (caller.getRole() == Role.STUDENT && !Objects.equals(caller.getId(), studentId)) {
                return Collections.emptyList();
            }
        }
        return portalRepository.getStudentHistory(studentId);
    }

    public List<LeaderboardEntry> getQuizLeaderboard() {
        return portalRepository.getQuizLeaderboard();
    }

    public List<LeaderboardEntry> getExamLeaderboard() {
        return portalRepository.getExamLeaderboard();
    }

    public List<LeaderboardEntry> getGlobalLeaderboard() {
        return portalRepository.getGlobalLeaderboard();
    }

    public List<LeaderboardEntry> getSpeedQuizLeaderboard() {
        return portalRepository.getSpeedQuizLeaderboard();
    }
}
