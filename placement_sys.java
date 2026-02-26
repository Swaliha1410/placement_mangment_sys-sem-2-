/*
Smart Placement + Internship System (single-file)

-- Run this SQL first in MySQL (adjust DB name/user/password as needed) --

CREATE DATABASE IF NOT EXISTS project1;
USE project1;

-- users table (students & admins & company HR if desired)
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  role ENUM('admin','student','company') NOT NULL,
  name VARCHAR(150),
  cgpa FLOAT DEFAULT 0,
  branch VARCHAR(100),
  skills TEXT
);

-- companies table (for placement jobs)
CREATE TABLE IF NOT EXISTS companies (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  min_cgpa FLOAT DEFAULT 0,
  branch_required VARCHAR(100),
  required_skills TEXT,
  visit_date DATE
);

-- job applications / placement
CREATE TABLE IF NOT EXISTS applications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT,
  company_id INT,
  status ENUM('applied','shortlisted','offered','placed','rejected') DEFAULT 'applied',
  score FLOAT DEFAULT 0,
  applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- internships table
CREATE TABLE IF NOT EXISTS internships (
  id INT AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(150),
  role VARCHAR(150),
  stipend DECIMAL(10,2),
  duration VARCHAR(50),
  mode ENUM('Remote','On-Site','Hybrid') DEFAULT 'Remote',
  eligibility_cgpa FLOAT DEFAULT 0,
  eligibility_skills TEXT,
  posted_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- internship applications
CREATE TABLE IF NOT EXISTS internship_applications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT,
  internship_id INT,
  status ENUM('Applied','Eligible','Rejected','Accepted','ConvertedToJob') DEFAULT 'Applied',
  applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (internship_id) REFERENCES internships(id) ON DELETE CASCADE
);

-- sample admin + student
INSERT IGNORE INTO users (username,password,role,name,cgpa,branch,skills) VALUES
('admin1','admin123','admin','Administrator',0,NULL,NULL),
('alice','s123','student','Alice',8.2,'CSE','java,sql,python'),
('bob','s123','student','Bob',7.1,'ECE','c,python');

*/

import java.sql.*;
import java.util.*;


class DBConnection {
    static String Url = "jdbc:mysql://localhost:3306/backup";
    static String User = "root";
    static String pass = ""; // change if needed
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Url, User, pass);
    }
}

class Login {
    private static final Scanner sc = new Scanner(System.in);
    // in-memory DS
    private static final HashMap<Integer, List<Integer>> jobShortlists = new HashMap<>(); // companyId -> studentIds
    private static final HashMap<Integer, List<Integer>> internshipShortlists = new HashMap<>(); // internshipId -> studentIds
    private static final HashSet<Integer> placedStudents = new HashSet<>(); // studentIds placed (job)
    private static final HashSet<Integer> acceptedInterns = new HashSet<>(); // studentIds accepted as interns
    // DECLARATION OF QUEUE
    private static final MyQue jobApplicationQueue = new MyQue(100);

    private static void processNextJobApplication() {
        int sid = jobApplicationQueue.dequeue();
        if (sid == -1) {
            System.out.println("No applications in queue.");
        } else {
            System.out.println("Processing job application for student ID: " + sid);
            // here you could also fetch details from DB for that student if needed
        }
    }



    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);

        try (Connection c = DBConnection.getConnection()) {
            System.out.println("DB connected.");
        } catch (SQLException e) {
            System.err.println("DB connect failed: " + e.getMessage());
            return;
        }
        boolean b1=true;

        while (b1) {
            System.out.println("       ╔═══════════════════════════════════════════════╗ ");
            System.out.println("       ║ WELCOME TO SMART PLACEMENT MANAGEMENT SYSTEM  ║ ");
            System.out.println("       ╚═══════════════════════════════════════════════╝ ");
            System.out.println("\n" + "            -----------");
            System.out.println("             MAIN MENU:_ ");
            System.out.println("            -----------" + "\n");
            System.out.println("       1) Register:- ");
            System.out.println("       2) Login:- ");
            System.out.println("       3) Exit....");

            System.out.print("> ");
            int ch=InputHelper.getIntInput("Enter your choice: ");
            switch (ch) {
                case 1:
                { register();
                    break;}
                case 2:
                {  login();
                    break;}
                case 3: {
                    System.out.println("Exit");
                    b1 = false;
                    break;
                }
                default:
                    System.out.println("Invalid option."); break;

            }
        }
    }

    // ---------------- Registration ----------------
    private static void register() {
        Scanner sc=new Scanner(System.in);
        try (Connection con = DBConnection.getConnection()) {

            String role = "";
            while (true) {
                role = InputHelper.getValidStringInput("       Role (student/admin/company):- ");

                if (role.equals("student") || role.equals("admin") || role.equals("company")) {
                    break;
                } else {
                    System.out.println(" Invalid role! Please enter 'student', 'admin', or 'company'.");
                }
            }

            System.out.print("Username (unique): ");
            String uname = sc.nextLine().trim();
            String  pwd = InputHelper.checkpwd();
            String name = InputHelper.getValidStringInput(" name: ");
            float cgpa = 0f;
            String branch = null;
            String skills = null;
            if (role.equals("student")) {
                InputHelper ip = new InputHelper();
                double cg = ip.getdoubleInput("CGPA (e.g. 7.5): ");
                try {
                    cgpa = (float) cg;
                } catch (Exception ignored) {
                }
                branch = InputHelper.getValidStringInput("Branch: ");
                System.out.print("Skills (comma-separated): ");
                skills = sc.nextLine().trim().toLowerCase();
            }
            System.out.print("Enter email: ");
            String email=InputHelper.checkEmail();
            String sql = "INSERT INTO users(username,password,role,name,cgpa,branch,skills,email) VALUES(?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, uname);
                ps.setString(2, pwd);
                ps.setString(3, role);
                ps.setString(4, name);
                ps.setFloat(5, cgpa);
                ps.setString(6, branch);
                ps.setString(7, skills);
                ps.setString(8,email);
                ps.executeUpdate();
                System.out.println("Registered successfully as " + role + ".");
            } catch (SQLIntegrityConstraintViolationException ex) {
                System.out.println("Username taken. Choose another.");
            }
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
        }
    }

    // ---------------- Login ----------------
    private static void login() {
        System.out.print("Username: ");
        String uname = sc.next().trim();
        System.out.print("Password: ");
        String pwd = sc.next().trim();
        String q = "SELECT id, role, name FROM users WHERE username=? AND password=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            ps.setString(1, uname);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                { System.out.println("Invalid credentials.");
                    return; }
                int id = rs.getInt("id");
                String role = rs.getString("role");
                String name = rs.getString("name");
                System.out.println("Welcome " + name + " (" + role + ")");
                if (role.equals("admin"))
                    adminMenu(id);
                else if (role.equals("company"))
                    companyMenu(id);
                else studentMenu(id);
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
    }
    static void viewCompanies(Connection con) throws SQLException {
        String sql = "SELECT * from companies";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        System.out.println("\n" + "            -----------");
        System.out.println("            Registered Companies:_ ");
        System.out.println("            -----------" + "\n");
        while (rs.next()) {
            System.out.println("ID: " + rs.getInt("id"));
            System.out.println("Name: " + rs.getString("name"));
            System.out.println("Industry: " + rs.getString("industry"));
            System.out.println("min cgpa : "+rs.getString("min_cgpa"));
            System.out.println("Location: " + rs.getString("location"));
            System.out.println("------------------------------------");
        }
    }

    // ---------------- Admin Menu ----------------
    private static void adminMenu(int adminId) {
        while (true) {
            System.out.println("\n" + "            -----------");
            System.out.println("             ADMIN MENU:_ ");
            System.out.println("            -----------" + "\n");
            System.out.println("      1) Add Company (job):- ");
            System.out.println("      2) Post Internship: -");
            System.out.println("      3) Shortlist top-K for Job:- ");
            System.out.println("      4) Shortlist top-K for Internship:- ");
            System.out.println("      5) Offer/Place Students (jobs):- ");
            System.out.println("      6) Accept Interns / Convert to Job:- ");
            System.out.println("      7) View Reports (placed/accepted):- ");
            System.out.println("      8) Logout");
            System.out.println("      9) Process Job Application (Queue)");

            System.out.print("> ");
            int ch = InputHelper.getIntInput("Enter choice: ");
            switch (ch) {
                case 1: addCompany(); break;
                case 2: postInternship(); break;
                case 3: shortlistJobTopK(); break;
                case 4: shortlistInternTopK(); break;
                case 5: offerPlaceJobs(); break;
                case 6: acceptInternsOrConvert(); break;
                case 7: viewReports(); break;
                case 8: return;
                case 9: processNextJobApplication(); break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------- Company Menu (optional role) ----------------
    private static void companyMenu(int companyUserId) {
        // companies could post internships/jobs through admin interface or their own menu
        System.out.println("Company role currently uses admin features. Logging in to Admin Menu is recommended.");
        adminMenu(companyUserId);
    }

    // ---------------- Student Menu ----------------
    private static void studentMenu(int studentId) {
        while (true) {
            System.out.println("\n" + "            -----------");
            System.out.println("             STUDENT MENU:- ");
            System.out.println("            -----------" + "\n");
            System.out.println("      1) View/Update Profile:- ");
            System.out.println("      2) View Eligible Companies (jobs):- ");
            System.out.println("      3) Apply to Job:- ");
            System.out.println("      4) View My Job Applications:- ");
            System.out.println("      5) View Internships:- ");
            System.out.println("      6) Apply to Internship:- ");
            System.out.println("      7) View My Internship Applications:- ");
            System.out.println("      8) Logout ");
            System.out.print("> ");
            int  ch = InputHelper.getIntInput("Enter choice: ");
            switch (ch) {
                case 1: studentProfile(studentId); break;
                case 2: viewEligibleJobs(studentId); break;
                case 3: applyToJob(studentId); break;
                case 4: viewMyJobApplications(studentId); break;
                case 5: viewInternships(); break;
                case 6: applyToInternship(studentId); break;
                case 7: viewMyInternApplications(studentId); break;
                case 8: return;
                default: System.out.println("Invalid.");
            }
        }
    }

    // ---------------- Admin Functions: Jobs & Internships ----------------
    private static void addCompany() {
        try (Connection con = DBConnection.getConnection()) {
            String name = InputHelper.getValidStringInput("      Company name:- ");
            double minCgpa = InputHelper.getdoubleInput("      MIN CGPA:- ");
            String branch = InputHelper.getValidStringInput("      Branch required (or Any):- ");
            System.out.print("      Required skills (comma-separated or blank):- ");
            String skills = sc.nextLine().trim().toLowerCase();
            String d =InputHelper.getDate("      visit_date:- ");
            java.sql.Date visit = null;
            if (!d.isEmpty()) visit = java.sql.Date.valueOf(d);
            String sql = "INSERT INTO companies(name,min_cgpa,branch_required,required_skills,visit_date) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setDouble(2, minCgpa);
                ps.setString(3, branch);
                ps.setString(4, skills.isEmpty() ? null : skills);
                if (visit != null) ps.setDate(5, visit);
                else ps.setNull(5, Types.DATE);
                ps.executeUpdate();
                System.out.println("Company/job added.");
            }
        } catch (SQLException e) { System.out.println("Add company error: " + e.getMessage()); }
    }

    private static void postInternship() {
        try (Connection con = DBConnection.getConnection()) {


            String cname = InputHelper.getValidStringInput("      comapny name:  ");
            String role = InputHelper.getValidStringInput("      Intern role: ");
            double stipend = InputHelper.getdoubleInput("      Stipend (number): ");
            String duration = InputHelper.getValidStringInput("      Duration (e.g., 3 months):  ");
            String mode = InputHelper.getValidStringInput("      Mode (Remote/On-Site/Hybrid): ");
            double cg = InputHelper.getdoubleInput("      Eligibility CGPA: ");
            System.out.print("      Eligibility skills (comma-separated or blank): ");
            String skills = sc.nextLine().trim().toLowerCase();
            String sql = "INSERT INTO internships(company_name,role,stipend,duration,mode,eligibility_cgpa,eligibility_skills) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, cname);
                ps.setString(2, role);
                ps.setDouble(3, stipend);
                ps.setString(4, duration);
                ps.setString(5, mode);
                ps.setDouble(6, cg);
                ps.setString(7, skills.isEmpty() ? null : skills);
                ps.executeUpdate();
                System.out.println("Internship posted.");
            }
        } catch (SQLException e)
        { System.out.println("Post internship error: " + e.getMessage()); }
    }

    // ---------------- Shortlisting (PriorityQueue) ----------------
    private static void shortlistJobTopK() {
        try (Connection con = DBConnection.getConnection()) {
            viewCompanies(con);
            System.out.print("Enter company ID to shortlist for: "); int cid = parseInt(sc.nextLine().trim(), -1);
            if (cid < 0) return;

            // get company criteria
            String cq = "SELECT min_cgpa, branch_required, required_skills FROM companies WHERE id=?";
            float minCg = 0f; String branchReq = null, reqSkills = null;
            try (PreparedStatement ps = con.prepareStatement(cq)) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Company not found.");
                        return; }
                    minCg = rs.getFloat("min_cgpa");
                    branchReq = rs.getString("branch_required");
                    reqSkills = rs.getString("required_skills");
                }
            }

            // priority queue by score
            PriorityQueue<StudentScore> pq = new PriorityQueue<>(Comparator.comparingDouble(StudentScore::getScore).reversed());
            String uq = "SELECT id,username,name,cgpa,branch,skills FROM users WHERE role='student'";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(uq)) {
                while (rs.next()) {
                    int sid = rs.getInt("id"); float scg = rs.getFloat("cgpa"); String sbranch = rs.getString("branch"); String sskills = rs.getString("skills");
                    boolean branchOk = branchReq == null || branchReq.equalsIgnoreCase("any") || (sbranch != null && sbranch.equalsIgnoreCase(branchReq));
                    if (!branchOk || scg < minCg) continue;
                    double skillFrac = reqSkills == null || reqSkills.trim().isEmpty() ? 1.0 : computeSkillMatchFraction(reqSkills, sskills);
                    double score = (scg / 10.0) * 6.0 + skillFrac * 4.0;
                    pq.offer(new StudentScore(sid, rs.getString("username"), rs.getString("name"), scg, score));
                }
            }

            if (pq.isEmpty()) { System.out.println("No eligible students."); return; }
            System.out.print("How many to shortlist? "); int k = parseInt(sc.nextLine().trim(), 3);
            List<Integer> shortlisted = new ArrayList<>();
            for (int i = 0; i < k && !pq.isEmpty(); i++) {
                StudentScore s = pq.poll();
                System.out.printf("%d) %s (%s) CGPA: %.2f Score: %.2f%n", i+1, s.name, s.username, s.cgpa, s.score);
                shortlisted.add(s.id);
                upsertApplication(con, s.id, cid, "shortlisted", s.score);
            }
            jobShortlists.put(cid, shortlisted);
            System.out.println("Shortlisted saved in memory & DB.");
        } catch (SQLException e) { System.out.println("Shortlist job error: " + e.getMessage()); }
    }

    private static void shortlistInternTopK() {
        try (Connection con = DBConnection.getConnection()) {
            viewInternships();
            int iid = InputHelper.getIntInput("      Enter internship ID to shortlist for:- ");
            if (iid < 0) return;

            // fetch internship criteria
            String iq = "SELECT eligibility_cgpa, eligibility_skills FROM internships WHERE id=?";
            float minCg = 0f; String reqSkills = null;
            try (PreparedStatement ps = con.prepareStatement(iq)) {
                ps.setInt(1, iid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                    { System.out.println("Internship not found.!!!");
                        return; }
                    minCg = rs.getFloat("eligibility_cgpa");
                    reqSkills = rs.getString("eligibility_skills");
                }
            }

            // PQ by cgpa
            PriorityQueue<StudentScore> pq = new PriorityQueue<>(Comparator.comparingDouble(StudentScore::getScore).reversed());
            String uq = "SELECT id,username,name,cgpa,skills FROM users WHERE role='student'";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(uq)) {
                while (rs.next()) {
                    int sid = rs.getInt("id");
                    float scg = rs.getFloat("cgpa");
                    String sskills = rs.getString("skills");
                    if (scg < minCg) continue;
                    double skillFrac = reqSkills == null || reqSkills.trim().isEmpty() ? 1.0 : computeSkillMatchFraction(reqSkills, sskills);
                    double score = (scg / 10.0) * 7.0 + skillFrac * 3.0; // weigh CGPA a bit higher
                    pq.offer(new StudentScore(sid, rs.getString("username"),
                            rs.getString("name"), scg, score));
                }
            }

            if (pq.isEmpty()) { System.out.println("No eligible interns."); return; }
            System.out.print("How many interns to shortlist? ");
            int k = parseInt(sc.nextLine().trim(), 3);
            List<Integer> shortlisted = new ArrayList<>();
            for (int i = 0; i < k && !pq.isEmpty(); i++) {
                StudentScore s = pq.poll();
                System.out.printf("%d) %s (%s) CGPA: %.2f Score: %.2f%n", i+1, s.name, s.username, s.cgpa, s.score);
                shortlisted.add(s.id);
                upsertInternApplication(con, s.id, iid, "Eligible");
            }
            internshipShortlists.put(iid, shortlisted);
            System.out.println("Intern shortlisting completed.");
        } catch (SQLException e)
        { System.out.println("Shortlist intern error: " + e.getMessage()); }
    }

    // ---------------- Offer/Place Jobs ----------------
    private static void offerPlaceJobs() {
        try (Connection con = DBConnection.getConnection()) {
            viewCompanies(con);
            int cid = InputHelper.getIntInput("Enter company ID to offer from: ");
            if (cid < 0) return;
            List<Integer> shortlisted = jobShortlists.getOrDefault(cid, fetchShortlistedFromDB(con, cid));
            if (shortlisted.isEmpty())
            { System.out.println("No shortlisted students.");
                return; }
            System.out.println("Shortlisted: " + shortlisted);
            System.out.print("Enter student ID to offer (or 'all'): ");
            String in = sc.nextLine().trim();
            if (in.equalsIgnoreCase("all")) {
                for (int sid : shortlisted) makeOfferAndPlace(con, sid, cid);
            } else {
                try { int sid = Integer.parseInt(in); makeOfferAndPlace(con, sid, cid); }
                catch (NumberFormatException ex) { System.out.println("Invalid."); }
            }
        } catch (SQLException e)
        { System.out.println("Offer error: " + e.getMessage()); }
    }

    private static void makeOfferAndPlace(Connection con, int studentId, int companyId) throws SQLException {
        if (placedStudents.contains(studentId))
        { System.out.println("Student " + studentId + " already placed."); return; }
        updateApplicationStatus(con, studentId, companyId, "offered");
        System.out.print("Mark as PLACED now? (y/n): ");
        String ans = sc.nextLine().trim();
        if (ans.equalsIgnoreCase("y")) {
            updateApplicationStatus(con, studentId, companyId, "placed");
            placedStudents.add(studentId);
            System.out.println("Student " + studentId + " placed.");
        } else System.out.println("Offered (not placed) to student " + studentId);
    }

    // ---------------- Accept interns or convert to job ----------------
    private static void acceptInternsOrConvert() {
        try (Connection con = DBConnection.getConnection()) {
            viewInternships();
            int iid = InputHelper.getIntInput("Enter company ID to offer from: ");
            if (iid < 0) return;
            List<Integer> shortlisted = internshipShortlists.getOrDefault(iid, fetchShortlistedInternsFromDB(con, iid));
            if (shortlisted.isEmpty()) { System.out.println("No shortlisted interns."); return; }
            System.out.println("Shortlisted interns: " + shortlisted);
            System.out.print("Enter student ID to accept (or 'all'): ");
            String in = sc.nextLine().trim();
            List<Integer> toAccept = new ArrayList<>();
            if (in.equalsIgnoreCase("all")) toAccept.addAll(shortlisted);
            else try { toAccept.add(Integer.parseInt(in)); } catch (NumberFormatException ex) { System.out.println("Invalid."); return; }

            for (int sid : toAccept) {
                // Accept intern
                updateInternAppStatus(con, sid, iid, "Accepted");
                acceptedInterns.add(sid);
                System.out.println("Student " + sid + " accepted as intern.");
                System.out.print("Convert this intern to job placement now? (y/n): ");
                String ans = sc.nextLine().trim();
                if (ans.equalsIgnoreCase("y")) {
                    // admin can create a company/job or map to existing company; for demo we'll create a "Converted" company
                    String compName = "Converted_From_Internship_" + iid;
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO companies(name,min_cgpa,branch_required,required_skills) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, compName); ps.setFloat(2, 0f); ps.setString(3, "Any"); ps.setString(4, null);
                        ps.executeUpdate();
                        try (ResultSet gk = ps.getGeneratedKeys()) {
                            if (gk.next()) {
                                int newCid = gk.getInt(1);
                                // insert application + mark placed
                                upsertApplication(con, sid, newCid, "placed", 0.0);
                                placedStudents.add(sid);
                                // mark internship application converted
                                updateInternAppStatus(con, sid, iid, "ConvertedToJob");
                                System.out.println("Converted to job and marked placed (company id: " + newCid + ").");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) { System.out.println("Accept/Convert error: " + e.getMessage()); }
    }

    // ---------------- Reports ----------------
    private static void viewReports() {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Placed Students:");
            String q = "SELECT u.id,u.username,u.name,c.name AS company FROM applications a JOIN users u ON a.student_id=u.id JOIN companies c ON a.company_id=c.id WHERE a.status='placed'";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(q)) {
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %s%n", rs.getInt("id"), rs.getString("username"), rs.getString("name"), rs.getString("company"));
                }
            }
            System.out.println("\nAccepted Interns:");
            String iq = "SELECT ia.id,u.id AS sid,u.username,u.name, i.role FROM internship_applications ia JOIN users u ON ia.student_id=u.id JOIN internships i ON ia.internship_id=i.id WHERE ia.status='Accepted'";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(iq)) {
                while (rs.next()) {
                    System.out.printf("InternAppID:%d | Student:%d|%s | Role:%s%n", rs.getInt("id"), rs.getInt("sid"), rs.getString("username"), rs.getString("role"));
                }
            }
        } catch (SQLException e) { System.out.println("Report error: " + e.getMessage()); }
    }

    // ---------------- Student functions ----------------
    private static void studentProfile(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            String q = "SELECT username,name,cgpa,branch,skills FROM users WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(q)) {
                ps.setInt(1, sid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Not found.");
                        return; }
                    System.out.println("Username: " + rs.getString("username"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.printf("CGPA: %.2f%n", rs.getFloat("cgpa"));
                    System.out.println("Branch: " + rs.getString("branch"));
                    System.out.println("Skills: " + rs.getString("skills"));
                }
            }
            System.out.print("Update profile? (y/n): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("y")) return;
            System.out.print("New CGPA (blank to keep): ");
            String cgS = sc.nextLine().trim();
            System.out.print("New Branch (blank to keep): ");
            String br = sc.nextLine().trim();
            System.out.print("New Skills (comma-separated, blank to keep): ");
            String sk = sc.nextLine().trim().toLowerCase();
            StringBuilder sb = new StringBuilder("UPDATE users SET ");
            List<Object> params = new ArrayList<>();
            if (!cgS.isEmpty()) { sb.append("cgpa=?, ");
                params.add(parseFloat(cgS, null)); }
            if (!br.isEmpty()) { sb.append("branch=?, ");
                params.add(br); }
            if (!sk.isEmpty()) { sb.append("skills=?, ");
                params.add(sk); }
            if (params.isEmpty())
            { System.out.println("No updates."); return; }
            int idx = sb.lastIndexOf(", ");
            sb.delete(idx, sb.length());
            sb.append(" WHERE id=?");
            params.add(sid);
            try (PreparedStatement ups = con.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++)
                    ups.setObject(i + 1, params.get(i));
                int r = ups.executeUpdate();
                System.out.println(r > 0 ? "Profile updated." : "Update failed.");
            }
        } catch (SQLException e) { System.out.println("Profile error: " + e.getMessage()); }
    }

    private static void viewEligibleJobs(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            // get student
            String uq = "SELECT cgpa,branch,skills,username FROM users WHERE id=?";
            float scgpa; String sbranch, sskills, uname;
            try (PreparedStatement ps = con.prepareStatement(uq)) {
                ps.setInt(1, sid); try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Student not found."); return; }
                    scgpa = rs.getFloat("cgpa"); sbranch = rs.getString("branch");
                    sskills = rs.getString("skills");
                    uname = rs.getString("username");
                }
            }
            String cq = "SELECT id,name,min_cgpa,branch_required,required_skills,visit_date FROM companies ORDER BY id";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(cq)) {
                System.out.println("Eligible companies for " + uname + ":");
                boolean any = false;
                while (rs.next()) {
                    int cid = rs.getInt("id");
                    float min = rs.getFloat("min_cgpa");
                    String rbranch = rs.getString("branch_required");
                    String req = rs.getString("required_skills");
                    boolean cgOk = scgpa >= min;
                    boolean branchOk = (rbranch == null || rbranch.equalsIgnoreCase("any") || (sbranch != null && sbranch.equalsIgnoreCase(rbranch)));
                    double skillMatch = req == null ? 1.0 : computeSkillMatchFraction(req, sskills);
                    if (cgOk && branchOk) {
                        System.out.printf("%d | %s | MinCGPA: %.2f | Branch: %s | SkillMatch: %.2f | Visit: %s%n",
                                cid, rs.getString("name"), min, rbranch, skillMatch, rs.getDate("visit_date"));
                        any = true;
                    }
                }
                if (!any) System.out.println("No matching jobs.");
            }
        } catch (SQLException e) { System.out.println("Eligible job error: " + e.getMessage()); }
    }

    private static void applyToJob(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            viewCompanies(con);
            int cid = InputHelper.getIntInput("Enter company ID to apply: ");
            if (cid < 0) return;
            // duplication check
            try (PreparedStatement dup = con.prepareStatement("SELECT COUNT(*) FROM applications WHERE student_id=? AND company_id=?")) {
                dup.setInt(1, sid); dup.setInt(2, cid);
                try (ResultSet dr = dup.executeQuery()) { if (dr.next() && dr.getInt(1) > 0)
                { System.out.println("Already applied."); return; } }
            }
            EligibilityResult er = computeEligibilityAndScore(con, sid, cid);
            if (!er.isEligible) {
                System.out.println("Not eligible: " + er.reason); return; }
            upsertApplication(con, sid, cid, "applied", er.score);
            System.out.println("Applied to job. Score: " + String.format("%.2f", er.score));

// enqueue student ID in our custom queue
            jobApplicationQueue.enqueue(sid);
            System.out.println("Student " + sid + " added to job application queue.");
        } catch (SQLException e) { System.out.println("Apply job error: " + e.getMessage()); }
    }

    private static void viewMyJobApplications(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            String q = "SELECT a.id,c.name,a.status,a.score,a.applied_on FROM applications a JOIN companies c ON a.company_id=c.id WHERE a.student_id=? ORDER BY a.applied_on DESC";
            try (PreparedStatement ps = con.prepareStatement(q)) {
                ps.setInt(1, sid); try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("AppID | Company | Status | Score | AppliedOn");
                    boolean any = false;
                    while (rs.next()) { any = true; System.out.printf("%d | %s | %s | %.2f | %s%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getFloat(4), rs.getTimestamp(5)); }
                    if (!any) System.out.println("No job applications.");
                }
            }
        } catch (SQLException e) { System.out.println("View my job apps error: " + e.getMessage()); }
    }

    // ---------------- Internships ----------------
    private static void viewInternships() {
        try (Connection con = DBConnection.getConnection()) {
            String q = "SELECT id,company_name,role,stipend,duration,mode,eligibility_cgpa,eligibility_skills,posted_on FROM internships ORDER BY posted_on DESC";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(q)) {
                System.out.println("ID | Company | Role | Stipend | Duration | Mode | MinCGPA | Skills | PostedOn");
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %.2f | %s | %s | %.2f | %s | %s%n",
                            rs.getInt("id"), rs.getString("company_name"), rs.getString("role"), rs.getDouble("stipend"),
                            rs.getString("duration"), rs.getString("mode"), rs.getFloat("eligibility_cgpa"),
                            rs.getString("eligibility_skills"), rs.getTimestamp("posted_on"));
                }
            }
        } catch (SQLException e) { System.out.println("View internships error: " + e.getMessage()); }
    }

    private static void applyToInternship(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            viewInternships();
            int iid = InputHelper.getIntInput("Enter internship ID to apply: ");
            if (iid < 0) return;
            // dup check
            try (PreparedStatement dup = con.prepareStatement("SELECT COUNT(*) FROM internship_applications WHERE student_id=? AND internship_id=?")) {
                dup.setInt(1, sid); dup.setInt(2, iid); try (ResultSet dr = dup.executeQuery()) { if (dr.next() && dr.getInt(1) > 0) { System.out.println("Already applied."); return; } }
            }
            // check eligibility
            String sQ = "SELECT cgpa,skills FROM users WHERE id=?";
            float scgpa; String sskills;
            try (PreparedStatement sp = con.prepareStatement(sQ)) {
                sp.setInt(1, sid); try (ResultSet rs = sp.executeQuery()) { if (!rs.next()) { System.out.println("Student not found."); return; } scgpa = rs.getFloat(1); sskills = rs.getString(2); }
            }
            String iQ = "SELECT eligibility_cgpa,eligibility_skills FROM internships WHERE id=?";
            float minCg; String req;
            try (PreparedStatement ip = con.prepareStatement(iQ)) {
                ip.setInt(1, iid); try (ResultSet ir = ip.executeQuery()) { if (!ir.next()) { System.out.println("Internship not found."); return; } minCg = ir.getFloat(1); req = ir.getString(2); }
            }
            if (scgpa < minCg) {
                // insert with status Rejected (still record)
                upsertInternApplication(con, sid, iid, "Rejected");
                System.out.println("CGPA below requirement. Application recorded as Rejected.");
                return;
            }
            double skillFrac = req == null || req.trim().isEmpty() ? 1.0 : computeSkillMatchFraction(req, sskills);
            if (skillFrac == 0.0) {
                upsertInternApplication(con, sid, iid, "Rejected");
                System.out.println("No required skill matched. Application recorded as Rejected.");
                return;
            }
            upsertInternApplication(con, sid, iid, "Eligible");
            System.out.println("Application submitted and marked Eligible. Skill match: " + String.format("%.2f", skillFrac));
        } catch (SQLException e) { System.out.println("Apply intern error: " + e.getMessage()); }
    }

    private static void viewMyInternApplications(int sid) {
        try (Connection con = DBConnection.getConnection()) {
            String q = "SELECT ia.id,i.company_name,i.role,ia.status,ia.applied_on FROM internship_applications ia JOIN internships i ON ia.internship_id=i.id WHERE ia.student_id=? ORDER BY ia.applied_on DESC";
            try (PreparedStatement ps = con.prepareStatement(q)) {
                ps.setInt(1, sid); try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("AppID | Company | Role | Status | AppliedOn");
                    boolean any = false;
                    while (rs.next()) { any = true; System.out.printf("%d | %s | %s | %s | %s%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5)); }
                    if (!any) System.out.println("No internship applications.");
                }
            }
        } catch (SQLException e) { System.out.println("View my intern apps error: " + e.getMessage()); }
    }

    // ---------------- DB Helpers ----------------

    // Upsert job application (insert if not exists else update)
    private static void upsertApplication(Connection con, int studentId, int companyId, String status, double score) throws SQLException {
        String sel = "SELECT id FROM applications WHERE student_id=? AND company_id=?";
        try (PreparedStatement ps = con.prepareStatement(sel)) {
            ps.setInt(1, studentId); ps.setInt(2, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int aid = rs.getInt(1);
                    try (PreparedStatement up = con.prepareStatement("UPDATE applications SET status=?,score=? WHERE id=?")) {
                        up.setString(1, status); up.setDouble(2, score); up.setInt(3, aid); up.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ins = con.prepareStatement("INSERT INTO applications(student_id,company_id,status,score) VALUES(?,?,?,?)")) {
                        ins.setInt(1, studentId); ins.setInt(2, companyId); ins.setString(3, status); ins.setDouble(4, score); ins.executeUpdate();
                    }
                }
            }
        }
    }

    private static void updateApplicationStatus(Connection con, int studentId, int companyId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE applications SET status=? WHERE student_id=? AND company_id=?")) {
            ps.setString(1, status); ps.setInt(2, studentId); ps.setInt(3, companyId); ps.executeUpdate();
        }
    }

    private static List<Integer> fetchShortlistedFromDB(Connection con, int cid) throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT student_id FROM applications WHERE company_id=? AND status='shortlisted'")) {
            ps.setInt(1, cid); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getInt(1)); }
        }
        return out;
    }

    private static void upsertInternApplication(Connection con, int studentId, int internshipId, String status) throws SQLException {
        String sel = "SELECT id FROM internship_applications WHERE student_id=? AND internship_id=?";
        try (PreparedStatement ps = con.prepareStatement(sel)) {
            ps.setInt(1, studentId); ps.setInt(2, internshipId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    try (PreparedStatement up = con.prepareStatement("UPDATE internship_applications SET status=? WHERE id=?")) {
                        up.setString(1, status); up.setInt(2, id); up.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ins = con.prepareStatement("INSERT INTO internship_applications(student_id,internship_id,status) VALUES(?,?,?)")) {
                        ins.setInt(1, studentId); ins.setInt(2, internshipId); ins.setString(3, status); ins.executeUpdate();
                    }
                }
            }
        }
    }

    private static void updateInternAppStatus(Connection con, int studentId, int internshipId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE internship_applications SET status=? WHERE student_id=? AND internship_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, studentId);
            ps.setInt(3, internshipId);
            ps.executeUpdate();
        }
    }

    private static List<Integer> fetchShortlistedInternsFromDB(Connection con, int iid) throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT student_id FROM internship_applications WHERE internship_id=? AND status IN ('Eligible','Accepted')")) {
            ps.setInt(1, iid);
            try (ResultSet rs = ps.executeQuery())
            { while (rs.next()) out.add(rs.getInt(1)); }
        }
        return out;
    }

    // ---------------- Eligibility & utility helpers ----------------
    private static EligibilityResult computeEligibilityAndScore(Connection con, int studentId, int companyId) throws SQLException {
        String sQ = "SELECT cgpa,branch,skills FROM users WHERE id=?";
        float scg = 0f; String sbranch = null, sskills = null;
        try (PreparedStatement ps = con.prepareStatement(sQ)) { ps.setInt(1, studentId); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return new EligibilityResult(false,0,"Student not found"); scg = rs.getFloat(1); sbranch = rs.getString(2); sskills = rs.getString(3); } }
        String cQ = "SELECT min_cgpa,branch_required,required_skills FROM companies WHERE id=?";
        float minCg = 0f; String branchReq=null, reqSkills=null;
        try (PreparedStatement ps = con.prepareStatement(cQ)) { ps.setInt(1, companyId); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return new EligibilityResult(false,0,"Company not found"); minCg = rs.getFloat(1); branchReq = rs.getString(2); reqSkills = rs.getString(3); } }
        if (scg < minCg) return new EligibilityResult(false,0,"CGPA too low");
        if (branchReq != null && !branchReq.equalsIgnoreCase("any") && (sbranch==null || !sbranch.equalsIgnoreCase(branchReq))) return new EligibilityResult(false,0,"Branch mismatch");
        double skillFrac = (reqSkills==null || reqSkills.trim().isEmpty()) ? 1.0 : computeSkillMatchFraction(reqSkills, sskills);
        double score = (scg/10.0)*6.0 + skillFrac*4.0;
        return new EligibilityResult(true, score, "Eligible");
    }

    // fraction of req skills matched (0..1)
    private static double computeSkillMatchFraction(String reqSkills, String studentSkills) {
        if (reqSkills==null || reqSkills.trim().isEmpty()) return 1.0;
        if (studentSkills==null || studentSkills.trim().isEmpty()) return 0.0;
        String[] req = reqSkills.toLowerCase().split("\\s*,\\s*");
        Set<String> sset = new HashSet<>();
        for (String t : studentSkills.toLowerCase().split("\\s*,\\s*")) if (!t.isEmpty()) sset.add(t.trim());
        int matched=0;
        for (String r : req) if (!r.isEmpty() && sset.contains(r.trim())) matched++;
        return req.length==0 ? 0.0 : (double)matched/req.length;
    }

    private static float parseFloat(String s, float def) {
        if (s==null || s.isEmpty()) return def;
        try { return Float.parseFloat(s); } catch (Exception e) { return def; }
    }
    private static Float parseFloat(String s, Float def) {
        if (s==null || s.isEmpty()) return def;
        try { return Float.parseFloat(s); } catch (Exception e) { return def; }
    }
    private static double parseDouble(String s, double def) {
        if (s==null || s.isEmpty()) return def;
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
    private static int parseInt(String s, int def) {
        if (s==null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static int parseIntOrExit(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }

    // small helper classes
    private static class StudentScore {
        int id; String username, name; double cgpa; double score;
        StudentScore(int id, String username, String name, double cgpa, double score) { this.id=id; this.username=username; this.name=name; this.cgpa=cgpa; this.score=score; }
        public double getScore() { return score; }
    }
    private static class EligibilityResult { boolean isEligible; double score; String reason; EligibilityResult(boolean e, double s, String r) { isEligible=e; score=s; reason=r; } }
}
class InputHelper
{
    private static final Scanner scanner = new Scanner(System.in);


    public static  String getDate(String prompt ) {
        Scanner sc=new Scanner(System.in);

        sc.nextLine();
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine();

            if (input.length() == 10 && input.charAt(4) == '-' && input.charAt(7) == '-') {
                try {
                    int year = Integer.parseInt(input.substring(0, 4));
                    int month = Integer.parseInt(input.substring(5, 7));
                    int day = Integer.parseInt(input.substring(8, 10));

                    if (month < 1 || month > 12) {
                        System.out.println("Invalid month. Please enter between 01 and 12.");
                        continue;
                    }

                    int maxDays;
                    switch (month) {
                        case 2:
                            boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                            maxDays = isLeap ? 29 : 28;
                            break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                            maxDays = 30;
                            break;
                        default:
                            maxDays = 31;
                    }

                    if (day >= 1 && day <= maxDays) {
                        return input;
                    } else {
                        System.out.println("Invalid day for the selected month. Allowed upto : " + maxDays);
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Date contains non-numeric parts. Please enter valid numbers.");
                }
            } else {
                System.out.println("Wrong format! Please use YYYY-MM-DD.");
            }
        }
    }

    public static  String checkEmail() {


        boolean b = true;
        String email = "";
        while (b) {
            email = scanner.next();
            int index = email.indexOf("@");
            if (index != -1) {
                String domain = email.substring(index);
                if (domain.equals("@gmail.com") || domain.equals("@yahoo.com")) {
                    b = false;
                } else {
                    System.out.println("Invalid email domain! Please enter again!!");
                    System.out.print("Email: ");
                }
            } else {
                System.out.println("Invalid Email! Please enter again!");
                System.out.print("Email: ");
            }
        }
        return email;
    }

    public static double getdoubleInput(String prompt) {
        while (true) {

            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.next());
            } catch (Exception e) {
                System.out.println(" Invalid input. Please enter a valid decimal number (e.g., 7.5).");
                scanner.nextLine(); // clear buffer
            }
        }
    }

    public  static  int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.next());
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.nextLine();
            }
        }
    }
    public  static String getValidStringInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.next();

            if (input.isEmpty()) {
                System.out.println("Invalid input. Input cannot be empty.");
                continue;
            }

            boolean containsDigit = false;
            boolean containsSpecialChar = false;

            for (char ch : input.toCharArray()) {
                if (Character.isDigit(ch)) {
                    containsDigit = true;
                    break;
                } else if (!Character.isLetter(ch) && !Character.isWhitespace(ch)) {
                    containsSpecialChar = true;
                    break;
                }
            }

            if (containsDigit) {
                System.out.println("Invalid input. Input should not contain numbers.");
            } else if (containsSpecialChar) {
                System.out.println("Invalid input. Input should not contain special characters.");
            } else {
                return input;
            }
        }
    }
    public static String checkpwd() {
        String pass;

        // Prompt for a valid 6-digit password
        while (true) {
            System.out.print("Password (6 digits): ");
            pass = scanner.next();

            if (pass.matches("\\d{6}")) {
                break;
            } else {
                System.out.println("Invalid input. Password must be exactly 6 digits.");
            }
        }

        // Confirm password: allow 3 attempts
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Confirm Password: ");
            String confirm = scanner.next();

            if (confirm.equals(pass)) {
                return pass; // Password confirmed
            } else {
                System.out.println("Password mismatch. Attempt " + attempt + " of 3.");
            }
        }

        // Failed confirmation
        System.out.println("Too many failed attempts.");
        return "error";
    }

    public static void main(String[] args) {
        String result = checkpwd();

        if (!result.equals("error")) {
            System.out.println("Password confirmed: " + result);
        } else {
            System.out.println("Password setup failed.");
        }
    }
}
class MyQue {
    private int[] arr;
    private int front, rear, size, capacity;

    public MyQue(int capacity) {
        this.capacity = capacity;
        arr = new int[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    // enqueue
    public void enqueue(int data) {
        if (isFull()) {
            System.out.println("Queue is full, cannot add: " + data);
            return;
        }
        rear = (rear + 1) % capacity;
        arr[rear] = data;
        size++;
    }

    // dequeue
    public int dequeue() {
        if (isEmpty()) {
            System.out.println(" Queue is empty.");
            return -1; // return special value when empty
        }
        int data = arr[front];
        front = (front + 1) % capacity;
        size--;
        return data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int getSize() {
        return size;
    }

    public int peek() {
        if (isEmpty())
        {
            return -1;
        }
        return arr[front];
    }
}
