package com.proctor.view;

import com.proctor.util.TuiHelper;

public class ReportViews {

    public static String renderReportMenu(String[] reports, int selectedIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.DIALOG_MARKER);
        sb.append(TuiHelper.header("REPORTS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Reports & Analytics", "Export PDF Reports")).append("\n\n");

        for (int i = 0; i < reports.length; i++) {
            if (i == selectedIndex) {
                sb.append(TuiHelper.cyan("  ▶ " + TuiHelper.bold(reports[i]))).append("\n\n");
            } else {
                sb.append("    ").append(reports[i]).append("\n\n");
            }
        }

        sb.append("\n");
        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Select Report  •  [Enter] Generate PDF  •  [Esc] Back\n"));
        return sb.toString();
    }
}