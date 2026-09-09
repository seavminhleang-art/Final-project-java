package com.proctor.model.service;

import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.repository.PortalRepository;
import com.proctor.model.entity.Result;

import java.util.List;

public class PortalService {
    private final PortalRepository portalRepository;

    public PortalService(PortalRepository portalRepository) {
        this.portalRepository = portalRepository;
    }

    public List<Result> getStudentHistory(int studentId) {
        return portalRepository.getStudentHistory(studentId);
    }

    public List<LeaderboardEntry> getGlobalLeaderboard() {
        return portalRepository.getGlobalLeaderboard();
    }
}