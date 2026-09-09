package com.proctor.view;

import com.proctor.model.enums.Role;
import com.proctor.util.TuiHelper;

public class AuthViews {

    public static String renderLogin(String identifier, String password, int focusedField, String errorMessage) {
        return renderLogin(identifier, password, focusedField, errorMessage, "");
    }

    public static String renderLogin(String identifier, String password, int focusedField, String errorMessage, String infoBanner) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("PROCTOR", "Digital Quiz Platform"));
        sb.append("\n");

        sb.append("  ").append(TuiHelper.bold("Sign In to Your Account")).append("\n\n");

        sb.append(TuiHelper.inputBox("Email or Username", identifier, focusedField == 0, 86, false, "e.g. user@proctor.edu or username"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Password", password, focusedField == 1, 86, true, "enter password"));
        sb.append("\n\n");

        String btnSignIn = (focusedField == 2) ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Sign In ]") : TuiHelper.dim("[   Sign In   ]");
        String btnSignUp = (focusedField == 3) ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Sign Up ]") : TuiHelper.dim("[   Sign Up   ]");
        String btnForgot = (focusedField == 4) ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Forgot Password ]") : TuiHelper.dim("[   Forgot Password   ]");
        String btnExit = (focusedField == 5) ? TuiHelper.bold(TuiHelper.RED + "[ ▶ Exit ]") : TuiHelper.dim("[   Exit   ]");

        int totalBtnsWidth = TuiHelper.visibleLength(btnSignIn) + 2
                           + TuiHelper.visibleLength(btnSignUp) + 2
                           + TuiHelper.visibleLength(btnForgot) + 2
                           + TuiHelper.visibleLength(btnExit);
        int btnPad = Math.max(0, (100 - totalBtnsWidth) / 2);
        sb.append(" ".repeat(btnPad)).append(btnSignIn).append("  ").append(btnSignUp).append("  ").append(btnForgot).append("  ").append(btnExit).append("\n\n");

        if (infoBanner != null && !infoBanner.isBlank()) {
            sb.append("  ").append(infoBanner).append("\n\n");
        }

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [←/→] Select Action  •  [Enter] Submit\n"));
        return sb.toString();
    }

    public static String renderRegister(String fullName, String email, String username, String password, String confirmPassword, Role selectedRole, int focusedField, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("PROCTOR", "Create a New Account"));
        sb.append("\n");

        sb.append("  ").append(TuiHelper.bold("Join as a Student or Teacher")).append("\n\n");

        sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 86, false, "e.g. Jane Doe"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Email Address", email, focusedField == 1, 86, false, "e.g. jane@proctor.edu"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Username", username, focusedField == 2, 86, false, "e.g. janedoe"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Password", password, focusedField == 3, 86, true, "create a secure password"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Confirm Password", confirmPassword, focusedField == 4, 86, true, "re-enter your password"));
        sb.append("\n");

        String roleHelp = "Press Space or ←/→ to switch";
        sb.append(TuiHelper.selectBox("Account Role", selectedRole == Role.STUDENT ? "Student" : "Teacher", focusedField == 5, 86, roleHelp));
        sb.append("\n\n");

        sb.append(TuiHelper.buttonRow("Register", focusedField == 6, "Back to Login", focusedField == 7)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Submit  •  [Esc] Back to Login\n"));
        return sb.toString();
    }
}