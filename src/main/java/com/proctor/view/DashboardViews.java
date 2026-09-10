package com.proctor.view;

import com.proctor.util.TuiHelper;

public class DashboardViews {

    public static String renderDashboard(String headerTitle, String userFullName, String userIdentifier, String[] menuItems, int selectedIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header(headerTitle));
        sb.append("\n");

        String portalRole = headerTitle.toUpperCase().contains("ADMIN") ? "Admin Portal"
                : (headerTitle.toUpperCase().contains("TEACHER") ? "Teacher Portal" : "Student Portal");
        String userInfo = "Logged in as: " + userFullName + " (" + userIdentifier + ")";
        sb.append(TuiHelper.boxTitle(portalRole, userInfo)).append("\n\n");

        for (int i = 0; i < menuItems.length; i++) {
            if (i == selectedIndex) {
                sb.append(TuiHelper.cyan("  ▶ " + TuiHelper.bold(menuItems[i]))).append("\n\n");
            } else {
                sb.append("    ").append(menuItems[i]).append("\n\n");
            }
        }

        sb.append("\n");
        sb.append(TuiHelper.dim("  [↑/↓] Navigate  •  [Enter] Select  •  [Esc] Exit\n"));
        return sb.toString();
    }
}