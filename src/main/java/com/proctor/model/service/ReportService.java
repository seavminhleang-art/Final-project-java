package com.proctor.model.service;

import com.proctor.config.Config;
import com.proctor.model.entity.QuizPerformanceDTO;
import com.proctor.model.entity.SubjectReportDTO;
import com.proctor.model.entity.SystemOverviewDTO;
import com.proctor.model.repository.ReportRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportService {
    private final ReportRepository reportRepository;
    private final String outputDir;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
        this.outputDir = Config.get("reports.output_dir", "./reports/output");
        new File(outputDir).mkdirs();
    }

    public String generateQuizPerformanceReport(Integer subjectId) {
        List<QuizPerformanceDTO> data = reportRepository.getQuizPerformanceData(subjectId);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = outputDir + "/quiz_performance_" + timestamp + ".pdf";

        try {
            JasperDesign design = createBaseDesign("Quiz Performance Report", "Assessment & Score Analytics");

            JRDesignBand colHeader = new JRDesignBand();
            colHeader.setHeight(25);
            addText(colHeader, "QUIZ TITLE", 10, 0, 200, 20, true);
            addText(colHeader, "SUBJ", 220, 0, 60, 20, true);
            addText(colHeader, "ATTEMPTS", 290, 0, 70, 20, true);
            addText(colHeader, "AVG SCORE", 370, 0, 70, 20, true);
            addText(colHeader, "PASS RATE", 450, 0, 70, 20, true);
            design.setColumnHeader(colHeader);

            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();

            if (data.isEmpty()) {
                JRDesignBand emptyBand = new JRDesignBand();
                emptyBand.setHeight(30);
                addText(emptyBand, "No quiz performance records found.", 10, 5, 400, 20, false);
                detailSection.addBand(emptyBand);
            } else {
                for (QuizPerformanceDTO item : data) {
                    JRDesignBand rowBand = new JRDesignBand();
                    rowBand.setHeight(20);
                    addText(rowBand, item.getQuizTitle(), 10, 1, 200, 18, false);
                    addText(rowBand, item.getSubjectCode() != null ? item.getSubjectCode() : "-", 220, 1, 60, 18, false);
                    addText(rowBand, String.valueOf(item.getTotalAttempts()), 290, 1, 70, 18, false);
                    addText(rowBand, String.format("%.1f%%", item.getAvgScore()), 370, 1, 70, 18, false);
                    addText(rowBand, String.format("%.1f%%", item.getPassRate()), 450, 1, 70, 18, false);
                    detailSection.addBand(rowBand);
                }
            }

            exportPdf(design, fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("PDF generation error: " + e.getMessage());
            return null;
        }
    }

    public String generateSystemOverviewReport() {
        SystemOverviewDTO data = reportRepository.getSystemOverviewData();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = outputDir + "/system_overview_" + timestamp + ".pdf";

        try {
            JasperDesign design = createBaseDesign("System Overview Report", "Overall Platform Performance & Metrics");
            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
            JRDesignBand detailBand = new JRDesignBand();

            int y = 10;
            addText(detailBand, "Total Users Registered: " + data.getTotalUsers(), 20, y, 400, 20, false); y += 22;
            addText(detailBand, "  • Total Teachers: " + data.getTotalTeachers(), 30, y, 400, 20, false); y += 22;
            addText(detailBand, "  • Total Students: " + data.getTotalStudents(), 30, y, 400, 20, false); y += 22;
            addText(detailBand, "Total Subjects Configured: " + data.getTotalSubjects(), 20, y, 400, 20, false); y += 22;
            addText(detailBand, "Total Quizzes Created: " + data.getTotalQuizzes(), 20, y, 400, 20, false); y += 22;
            addText(detailBand, "Total Examinations Taken: " + data.getTotalAttempts(), 20, y, 400, 20, false); y += 22;
            addText(detailBand, "Overall System Pass Rate: " + String.format("%.1f%%", data.getOverallPassRate()), 20, y, 400, 20, true); y += 30;

            detailBand.setHeight(y);
            detailSection.addBand(detailBand);

            exportPdf(design, fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("PDF generation error: " + e.getMessage());
            return null;
        }
    }

    public String generateSubjectReport() {
        List<SubjectReportDTO> data = reportRepository.getSubjectSummaryData();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = outputDir + "/subject_summary_" + timestamp + ".pdf";

        try {
            JasperDesign design = createBaseDesign("Subject Summary Report", "Department & Course Statistics");

            JRDesignBand colHeader = new JRDesignBand();
            colHeader.setHeight(25);
            addText(colHeader, "CODE", 10, 0, 70, 20, true);
            addText(colHeader, "SUBJECT NAME", 90, 0, 180, 20, true);
            addText(colHeader, "TEACHERS", 280, 0, 70, 20, true);
            addText(colHeader, "QUIZZES", 360, 0, 60, 20, true);
            addText(colHeader, "ATTEMPTS", 430, 0, 70, 20, true);
            design.setColumnHeader(colHeader);

            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();

            if (data.isEmpty()) {
                JRDesignBand emptyBand = new JRDesignBand();
                emptyBand.setHeight(30);
                addText(emptyBand, "No subject summary records found.", 10, 5, 400, 20, false);
                detailSection.addBand(emptyBand);
            } else {
                for (SubjectReportDTO item : data) {
                    JRDesignBand rowBand = new JRDesignBand();
                    rowBand.setHeight(20);
                    addText(rowBand, item.getSubjectCode(), 10, 1, 70, 18, false);
                    addText(rowBand, item.getSubjectName(), 90, 1, 180, 18, false);
                    addText(rowBand, String.valueOf(item.getTeacherCount()), 280, 1, 70, 18, false);
                    addText(rowBand, String.valueOf(item.getQuizCount()), 360, 1, 60, 18, false);
                    addText(rowBand, String.valueOf(item.getTotalAttempts()), 430, 1, 70, 18, false);
                    detailSection.addBand(rowBand);
                }
            }

            exportPdf(design, fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("PDF generation error: " + e.getMessage());
            return null;
        }
    }

    private JasperDesign createBaseDesign(String title, String subtitle) {
        JasperDesign design = new JasperDesign();
        design.setName("ProctorReport");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(535);
        design.setLeftMargin(30);
        design.setRightMargin(30);
        design.setTopMargin(30);
        design.setBottomMargin(30);

        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(60);

        JRDesignStaticText titleText = new JRDesignStaticText();
        titleText.setText("PROCTOR - " + title);
        titleText.setX(0);
        titleText.setY(5);
        titleText.setWidth(535);
        titleText.setHeight(25);
        titleText.setFontSize(16f);
        titleText.setBold(true);
        titleBand.addElement(titleText);

        JRDesignStaticText subText = new JRDesignStaticText();
        String genDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        subText.setText(subtitle + " • Generated: " + genDate);
        subText.setX(0);
        subText.setY(32);
        subText.setWidth(535);
        subText.setHeight(18);
        subText.setFontSize(10f);
        titleBand.addElement(subText);

        design.setTitle(titleBand);
        return design;
    }

    private void addText(JRDesignBand band, String text, int x, int y, int width, int height, boolean bold) {
        JRDesignStaticText st = new JRDesignStaticText();
        st.setText(text != null ? text : "");
        st.setX(x);
        st.setY(y);
        st.setWidth(width);
        st.setHeight(height);
        st.setBold(bold);
        st.setFontSize(10f);
        band.addElement(st);
    }

    private void exportPdf(JasperDesign design, String filePath) throws JRException {
        JasperReport report = JasperCompileManager.compileReport(design);
        JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), new JREmptyDataSource());
        JasperExportManager.exportReportToPdfFile(print, filePath);
    }
}