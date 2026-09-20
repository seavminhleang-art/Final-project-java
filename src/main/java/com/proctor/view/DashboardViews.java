package com.proctor.view;

import com.proctor.util.TuiHelper;

public class DashboardViews {

    public static String renderDashboard(String headerTitle, String userFullName, String userIdentifier, String[] menuItems, int selectedIndex) {
        StringBuilder sb = new StringBuilder();
        String safeTitle = (headerTitle != null && !headerTitle.isBlank()) ? headerTitle : "PROCTOR";
        sb.append(TuiHelper.header(safeTitle));
        sb.append("\n");

        String upperTitle = safeTitle.toUpperCase();
        String portalRole = upperTitle.contains("ADMIN") ? "Admin Portal"
                : (upperTitle.contains("TEACHER") ? "Teacher Portal" : "Student Portal");
        String safeName = (userFullName != null && !userFullName.isBlank()) ? userFullName : "User";
        String safeIdentifier = (userIdentifier != null && !userIdentifier.isBlank()) ? userIdentifier : "";
        String userInfo = safeIdentifier.isEmpty() ? "Logged in as: " + safeName : "Logged in as: " + safeName + " (" + safeIdentifier + ")";
        sb.append(TuiHelper.boxTitle(portalRole, userInfo)).append("\n\n");

        String[] safeItems = menuItems != null ? menuItems : new String[0];
        for (int i = 0; i < safeItems.length; i++) {
            String item = safeItems[i] != null ? safeItems[i] : "";
            if (i == selectedIndex) {
                sb.append("    ").append(TuiHelper.bold(TuiHelper.NAVY_BLUE + item)).append("\n\n");
            } else {
                sb.append("    ").append(item).append("\n\n");
            }
        }

        sb.append("\n");
        sb.append(TuiHelper.dim("  [↑/↓] Navigate  •  [Enter] Select  •  [Esc] Quit\n"));
        return sb.toString();
    }
}