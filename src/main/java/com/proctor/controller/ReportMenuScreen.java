package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.model.service.ReportService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ReportViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class ReportMenuScreen implements Screen {
    private final ReportService reportService;
    private final AuthService authService;
    private int selectedIndex = 0;
    private String bannerMessage = "";

    private final String[] adminReports = {
            "1. System Overview Report (Platform Summary PDF)",
            "2. Subject Summary Report (Course Statistics PDF)",
            "3. Quiz Performance Report (All Assessments PDF)"
    };

    private final String[] teacherReports = {
            "1. Quiz Performance Report (Assessments & Scores PDF)",
            "2. Subject Summary Report (Course Statistics PDF)"
    };

    public ReportMenuScreen(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    private String[] getReports() {
        User u = Session.getCurrentUser().orElse(null);
        if (u != null && u.getRole() == Role.ADMIN) {
            return adminReports;
        }
        return teacherReports;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            String[] reports = getReports();

            if (KeyUtil.isEsc(k)) {
                User u = Session.getCurrentUser().orElse(null);
                if (u != null && u.getRole() == Role.ADMIN) {
                    return ScreenResult.navigate(new AdminDashboardScreen(authService));
                } else {
                    return ScreenResult.navigate(new TeacherDashboardScreen(authService));
                }
            }

            if (KeyUtil.isUp(k)) {
                selectedIndex = (selectedIndex - 1 + reports.length) % reports.length;
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = (selectedIndex + 1) % reports.length;
            } else if (KeyUtil.isEnter(k)) {
                generateSelectedReport();
            }
        }
        return ScreenResult.stay(this);
    }

    private void generateSelectedReport() {
        User u = Session.getCurrentUser().orElse(null);
        boolean isAdmin = u != null && u.getRole() == Role.ADMIN;
        String generatedPath = null;

        if (isAdmin) {
            switch (selectedIndex) {
                case 0 -> generatedPath = reportService.generateSystemOverviewReport();
                case 1 -> generatedPath = reportService.generateSubjectReport();
                case 2 -> generatedPath = reportService.generateQuizPerformanceReport(null);
            }
        } else {
            switch (selectedIndex) {
                case 0 -> generatedPath = reportService.generateQuizPerformanceReport(null);
                case 1 -> generatedPath = reportService.generateSubjectReport();
            }
        }

        if (generatedPath != null) {
            bannerMessage = TuiHelper.green("✔ PDF Report generated successfully!\n  Saved to: " + TuiHelper.bold(generatedPath));
        } else {
            bannerMessage = TuiHelper.red("✖ Failed to generate PDF report.");
        }
    }

    @Override
    public String view() {
        return ReportViews.renderReportMenu(getReports(), selectedIndex, bannerMessage);
    }
}