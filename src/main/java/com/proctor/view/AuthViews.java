package com.proctor.view;

import com.proctor.model.enums.Role;
import com.proctor.util.TuiHelper;

import java.util.ArrayList;
import java.util.List;

public class AuthViews {

    public static String renderStartup(int focusedButton) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("PROCTOR"));
        sb.append("\n");

        sb.append(TuiHelper.boxTitle("Digital Assessment Platform")).append("\n\n");

        String btnLogin = (focusedButton == 0)
                ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Log In ]")
                : TuiHelper.dim("[   Log In   ]");
        String btnRegister = (focusedButton == 1)
                ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Register ]")
                : TuiHelper.dim("[   Register   ]");
        String btnExit = (focusedButton == 2)
                ? TuiHelper.bold(TuiHelper.RED + "[ ▶ Exit ]")
                : TuiHelper.dim("[   Exit   ]");

        sb.append(btnLogin).append("\n\n\n\n");
        sb.append(btnRegister).append("\n\n\n\n");
        sb.append(btnExit).append("\n\n");

        sb.append(TuiHelper.dim("  [↑/↓] Navigate  •  [Enter] Confirm  •  [Esc] Quit\n"));
        return sb.toString();
    }

    public static String renderLogin(String identifier, String password, int focusedField, String errorMessage, String infoBanner) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("LOG IN"));
        sb.append("\n");

        sb.append(TuiHelper.boxTitle("Sign In to Your Account")).append("\n\n");

        sb.append(TuiHelper.inputBox("Email or Username", identifier, focusedField == 0, 102, false, "e.g. user@proctor.edu or username"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Password", password, focusedField == 1, 102, true, "enter password"));
        sb.append("\n\n");

        String btnSignIn = (focusedField == 2) ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Log In ]") : TuiHelper.dim("[   Log In   ]");
        String btnForgot = (focusedField == 3) ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Forgot Password ]") : TuiHelper.dim("[   Forgot Password   ]");
        String btnExit = (focusedField == 4) ? TuiHelper.bold(TuiHelper.RED + "[ ▶ Back ]") : TuiHelper.dim("[   Back   ]");

        int totalBtnsWidth = TuiHelper.visibleLength(btnSignIn) + 2
                           + TuiHelper.visibleLength(btnForgot) + 2
                           + TuiHelper.visibleLength(btnExit);
        int btnPad = Math.max(0, (116 - totalBtnsWidth) / 2);
        sb.append(" ".repeat(btnPad)).append(btnSignIn).append("  ").append(btnForgot).append("  ").append(btnExit).append("\n\n");

        if (infoBanner != null && !infoBanner.isBlank()) {
            sb.append("  ").append(infoBanner).append("\n\n");
        }

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [←/→] Select Action  •  [Enter] Submit  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderRegisterRole(int focusedButton) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("REGISTER"));
        sb.append("\n");

        sb.append(TuiHelper.boxTitle("Choose Account Type")).append("\n\n");

        String btnStudent = (focusedButton == 0)
                ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Student ]")
                : TuiHelper.dim("[   Student   ]");
        String btnTeacher = (focusedButton == 1)
                ? TuiHelper.bold(TuiHelper.NAVY_BLUE + "[ ▶ Teacher ]")
                : TuiHelper.dim("[   Teacher   ]");
        String btnBack = (focusedButton == 2)
                ? TuiHelper.bold(TuiHelper.RED + "[ ▶ Back ]")
                : TuiHelper.dim("[   Back   ]");

        sb.append(btnStudent).append("\n\n\n\n");
        sb.append(btnTeacher).append("\n\n\n\n");
        sb.append(btnBack).append("\n\n");

        sb.append(TuiHelper.dim("  [↑/↓] Navigate  •  [Enter] Confirm  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderRegister(Role targetRole, String fullName, String email, String username, String password, String confirmPassword, String birthday, String gender, int focusedField, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String roleLabel = (targetRole == Role.TEACHER) ? "Teacher" : "Student";
        sb.append(TuiHelper.header("REGISTER"));
        sb.append("\n");

        sb.append(TuiHelper.boxTitle(roleLabel + " Registration")).append("\n\n");

        sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 102, false, "e.g. Jane Doe"));
        sb.append("\n");

        String genderVal = (gender != null && !gender.isBlank()) ? gender : "Male";
        String genderHelp = "Press Space or ←/→ to switch";
        sb.append(TuiHelper.selectBox("Gender", genderVal, focusedField == 1, 102, genderHelp));
        sb.append("\n");

        String birthdayDisplay = TuiHelper.birthdayMask(birthday, focusedField == 2);
        sb.append(TuiHelper.inputBox("Date of Birth", birthdayDisplay, focusedField == 2, 102, false, "DD - MM - YYYY"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Email Address", email, focusedField == 3, 102, false, "e.g. jane@proctor.edu"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Username", username, focusedField == 4, 102, false, "e.g. janedoe"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Password", password, focusedField == 5, 102, true, "create a secure password"));
        sb.append("\n");

        sb.append(TuiHelper.inputBox("Confirm Password", confirmPassword, focusedField == 6, 102, true, "re-enter your password"));
        sb.append("\n\n");

        sb.append(TuiHelper.buttonRow("Register", focusedField == 7, "Back", focusedField == 8)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm / Submit  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderTeacherRegister(String fullName, String email, String username, String password,
                                               String confirmPassword, String birthday, String gender,
                                               String academicDegree, String educationBackground, String specialization,
                                               int focusedField, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("REGISTER"));
        sb.append("\n");

        int numInputFields = 10;
        int activeFieldDisplay = Math.min(numInputFields, focusedField + 1);
        sb.append(TuiHelper.boxTitle("Teacher Registration", String.format("Field %d of %d", activeFieldDisplay, numInputFields))).append("\n\n");

        List<String> fieldWidgets = new ArrayList<>();
        fieldWidgets.add(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 102, false, "e.g. Jane Doe"));

        String genderVal = (gender != null && !gender.isBlank()) ? gender : "Male";
        String genderHelp = "Press Space or ←/→ to switch";
        fieldWidgets.add(TuiHelper.selectBox("Gender", genderVal, focusedField == 1, 102, genderHelp));

        String birthdayDisplay = TuiHelper.birthdayMask(birthday, focusedField == 2);
        fieldWidgets.add(TuiHelper.inputBox("Date of Birth", birthdayDisplay, focusedField == 2, 102, false, "DD - MM - YYYY"));

        fieldWidgets.add(TuiHelper.inputBox("Academic Degree / Qualification", academicDegree, focusedField == 3, 102, false, "e.g. Ph.D. in Computer Science, Master of Science"));
        fieldWidgets.add(TuiHelper.inputBox("Education Background (University)", educationBackground, focusedField == 4, 102, false, "e.g. Stanford University, MIT, RUPP"));
        fieldWidgets.add(TuiHelper.inputBox("Primary Subject / Specialization", specialization, focusedField == 5, 102, false, "e.g. Software Engineering, Mathematics, Java"));

        fieldWidgets.add(TuiHelper.inputBox("Email Address", email, focusedField == 6, 102, false, "e.g. jane@proctor.edu"));
        fieldWidgets.add(TuiHelper.inputBox("Username", username, focusedField == 7, 102, false, "e.g. janedoe"));
        fieldWidgets.add(TuiHelper.inputBox("Password", password, focusedField == 8, 102, true, "create a secure password"));
        fieldWidgets.add(TuiHelper.inputBox("Confirm Password", confirmPassword, focusedField == 9, 102, true, "re-enter your password"));

        int windowSize = 5;
        int startField = Math.max(0, Math.min(Math.min(focusedField, numInputFields - 1) - 1, numInputFields - windowSize));
        int endField = Math.min(numInputFields, startField + windowSize);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            sb.append(fieldWidgets.get(f)).append("\n");
        }

        if (endField < numInputFields) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press ↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Register", focusedField == 10, "Back", focusedField == 11)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm / Submit  •  [Esc] Back\n"));
        return sb.toString();
    }
}