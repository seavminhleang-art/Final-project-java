package com.proctor.model.service;

import com.proctor.config.Config;
import com.proctor.model.entity.QuizPerformanceDTO;
import com.proctor.model.entity.SubjectReportDTO;
import com.proctor.model.entity.SystemOverviewDTO;
import com.proctor.model.repository.ReportRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;

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
            JRDesignBand detail = (JRDesignBand) design.getDetailSection().getBands()[0];

            int y = 0;
            addText(detail, "QUIZ TITLE", 10, y, 200, 20, true);
            addText(detail, "SUBJ", 220, y, 60, 20, true);
            addText(detail, "ATTEMPTS", 290, y, 70, 20, true);
            addText(detail, "AVG SCORE", 370, y, 70, 20, true);
            addText(detail, "PASS RATE", 450, y, 70, 20, true);
            y += 25;

            for (QuizPerformanceDTO item : data) {
                addText(detail, item.getQuizTitle(), 10, y, 200, 18, false);
                addText(detail, item.getSubjectCode() != null ? item.getSubjectCode() : "-", 220, y, 60, 18, false);
                addText(detail, String.valueOf(item.getTotalAttempts()), 290, y, 70, 18, false);
                addText(detail, String.format("%.1f%%", item.getAvgScore()), 370, y, 70, 18, false);
                addText(detail, String.format("%.1f%%", item.getPassRate()), 450, y, 70, 18, false);
                y += 20;
            }

            detail.setHeight(Math.max(50, y + 20));
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
            JRDesignBand detail = (JRDesignBand) design.getDetailSection().getBands()[0];

            int y = 10;
            addText(detail, "Total Users Registered: " + data.getTotalUsers(), 20, y, 400, 20, false); y += 22;
            addText(detail, "  • Total Teachers: " + data.getTotalTeachers(), 30, y, 400, 20, false); y += 22;
            addText(detail, "  • Total Students: " + data.getTotalStudents(), 30, y, 400, 20, false); y += 22;
            addText(detail, "Total Subjects Configured: " + data.getTotalSubjects(), 20, y, 400, 20, false); y += 22;
            addText(detail, "Total Quizzes Created: " + data.getTotalQuizzes(), 20, y, 400, 20, false); y += 22;
            addText(detail, "Total Examinations Taken: " + data.getTotalAttempts(), 20, y, 400, 20, false); y += 22;
            addText(detail, "Overall System Pass Rate: " + String.format("%.1f%%", data.getOverallPassRate()), 20, y, 400, 20, true); y += 30;

            detail.setHeight(y);
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
            JRDesignBand detail = (JRDesignBand) design.getDetailSection().getBands()[0];

            int y = 0;
            addText(detail, "CODE", 10, y, 70, 20, true);
            addText(detail, "SUBJECT NAME", 90, y, 180, 20, true);
            addText(detail, "TEACHERS", 280, y, 70, 20, true);
            addText(detail, "QUIZZES", 360, y, 60, 20, true);
            addText(detail, "ATTEMPTS", 430, y, 70, 20, true);
            y += 25;

            for (SubjectReportDTO item : data) {
                addText(detail, item.getSubjectCode(), 10, y, 70, 18, false);
                addText(detail, item.getSubjectName(), 90, y, 180, 18, false);
                addText(detail, String.valueOf(item.getTeacherCount()), 280, y, 70, 18, false);
                addText(detail, String.valueOf(item.getQuizCount()), 360, y, 60, 18, false);
                addText(detail, String.valueOf(item.getTotalAttempts()), 430, y, 70, 18, false);
                y += 20;
            }

            detail.setHeight(Math.max(50, y + 20));
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

        JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(100);
        detailSection.addBand(detailBand);

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