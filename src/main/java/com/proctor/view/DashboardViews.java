package com.proctor.view;

import com.proctor.util.TuiHelper;

public class DashboardViews {

    // Render role dashboard header, navigation menu items, and control shortcuts
    public static String renderDashboard(String headerTitle, String userFullName, String userIdentifier, String[] menuItems, int selectedIndex) {
        StringBuilder sb = new StringBuilder();
        // Header banner
        String safeTitle = (headerTitle != null && !headerTitle.isBlank()) ? headerTitle : "PROCTOR";
        sb.append(TuiHelper.header(safeTitle));
        sb.append("\n");

        // Role title and user info banner
        String upperTitle = safeTitle.toUpperCase();
        String portalRole = upperTitle.contains("ADMIN") ? "Admin Portal"
                : (upperTitle.contains("TEACHER") ? "Teacher Portal" : "Student Portal");
        String safeName = (userFullName != null && !userFullName.isBlank()) ? userFullName : "N/A";
        String safeIdentifier = (userIdentifier != null && !userIdentifier.isBlank()) ? userIdentifier : "";
        String userInfo = safeIdentifier.isEmpty() ? "Logged in as: " + safeName : "Logged in as: " + safeName + " (" + safeIdentifier + ")";
        sb.append(TuiHelper.boxTitle(portalRole, userInfo)).append("\n\n");

        // Interactive dashboard menu options
        String[] safeItems = menuItems != null ? menuItems : new String[0];
        for (int i = 0; i < safeItems.length; i++) {
            String item = safeItems[i] != null ? safeItems[i] : "";
            if (i == selectedIndex) {
                sb.append("    ").append(TuiHelper.BUTTON_MARKER).append(TuiHelper.bold(TuiHelper.NAVY_BLUE + item)).append("\n\n");
            } else {
                sb.append("    ").append(TuiHelper.BUTTON_MARKER).append(item).append("\n\n");
            }
        }

        // Bottom navigation and selection shortcut hints
        sb.append("\n");
        sb.append(TuiHelper.dim("  [↑/↓] Navigate  •  [1-9] Select  •  [Enter] Confirm  •  [Esc] Quit\n"));
        return sb.toString();
    }
}
