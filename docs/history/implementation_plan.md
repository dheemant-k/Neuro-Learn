# Implementation Plan - Teacher Classroom Analytics & Parent Child-Linking Dashboards

Empower **TEACHER** and **PARENT** user roles with dedicated, data-rich dashboards that utilize the real-time Bayesian Knowledge Tracing (BKT) engine and PostgreSQL database.

---

## 1. Problem Statement & Architecture Goals

### Current Limitations:
1. **Generic Dashboard:** Currently, `GET /api/dashboard` always returns the logged-in user's own `StudentProfile`. A registered `TEACHER` or `PARENT` sees an empty student dashboard with 0 XP and no insight into actual students.
2. **Missing Parent-Student Association:** No database relationship exists between a parent account and their child's student account.
3. **No Class-Wide Aggregation:** Teachers cannot see class rosters, aggregate BKT mastery, or identify curriculum bottlenecks (e.g. which topics students are failing).

### Architecture Goals:
1. **Database Schema:**
   - Create Flyway migration `V4__add_parent_student_links.sql` with `parent_student_links` table (`parent_user_id`, `student_user_id`, `created_at`).
2. **Teacher Command Center:**
   - Class-wide metrics: Total Students, Class Avg XP, Class Avg Accuracy, Class Avg BKT $P(L)$.
   - Section-by-section class mastery with **Curriculum Bottleneck Alerts** (e.g. *"⚠️ High Struggle: 55% of students need review on Elimination"*).
   - Searchable Student Roster table with XP, Level, BKT Mastery, and interactive drill-down into any student's individual dashboard.
3. **Parent Portal:**
   - Child Account Linker: Enter student email to establish link.
   - Child Switcher: Supports multiple linked children.
   - Comprehensive Child Report Card: Level, XP, Bayesian skill progress bars, and pedagogical recommendations for parents.
4. **Role-Aware Frontend Dashboard:**
   - `DashboardPage.jsx` dynamically mounts the appropriate UI based on `user.role` (`STUDENT`, `TEACHER`, `PARENT`).

---

## Proposed Changes

### Database Layer

#### [NEW] [V4__add_parent_student_links.sql](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/resources/db/migration/V4__add_parent_student_links.sql)
- Table `parent_student_links` with foreign keys to `users(id)` and unique constraint on `(parent_user_id, student_user_id)`.

---

### Backend (Spring Boot)

#### [NEW] [ParentStudentLink.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/entity/ParentStudentLink.java) & [ParentStudentLinkRepository.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/repository/ParentStudentLinkRepository.java)
- JPA Entity and Repository mapping parent-student connections.

#### [NEW] [TeacherAnalyticsResponse.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/dto/TeacherAnalyticsResponse.java)
- DTO containing class summary metrics, topic mastery breakdown with alerts, and student roster items.

#### [NEW] [TeacherService.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/service/TeacherService.java) & [TeacherController.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/controller/TeacherController.java)
- `GET /api/teacher/analytics`: Returns aggregate metrics, weak topic alerts, and roster.
- `GET /api/teacher/student/{studentId}`: Returns individual student dashboard for teacher inspection.

#### [NEW] [ParentService.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/service/ParentService.java) & [ParentController.java](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/backend-java/src/main/java/com/neurolearn/controller/ParentController.java)
- `POST /api/parent/link`: Associates parent with student via email.
- `GET /api/parent/children`: Returns list of linked children.
- `GET /api/parent/child/{studentId}`: Returns child's detailed report card with AI tips.

---

### Frontend

#### [MODIFY] [api.js](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/Frontend/src/utils/api.js)
- Add `fetchTeacherAnalytics()`, `fetchTeacherStudentDashboard()`, `linkChild()`, `fetchParentChildren()`, and `fetchChildDashboard()`.

#### [MODIFY] [DashboardPage.jsx](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/Frontend/src/pages/DashboardPage.jsx)
- Modularize into three tabbed / role-switched views:
  - **Student View:** Personal XP, level, section BKT bars.
  - **Teacher View:** Class analytics cards, bottleneck alert badges, topic distribution, and student roster table with drill-down inspector modal.
  - **Parent View:** Link student form, child selector tabs, detailed progress card, and parent guidance recommendations.

#### [MODIFY] [HomePage.jsx](file:///C:/coding/sem5proj/SE-JAVA/Neuro-Learn/Frontend/src/pages/HomePage.jsx)
- Adapt button text to display `Teacher Dashboard` or `Parent Dashboard` according to `user.role`.

---

## Verification Plan

### Automated Tests
1. Backend compilation:
   ```powershell
   cd backend-java; .\mvnw.cmd compile
   ```
2. Frontend build verification:
   ```powershell
   cd Frontend; npm run build
   ```

### Manual & Integration Verification
1. Register/login as a `TEACHER` $\to$ navigate to dashboard:
   - Verify class analytics (total students, average accuracy, average $P(L)$).
   - Verify roster list shows registered students.
   - Click "Inspect Student" to view individual student's BKT performance.
2. Register/login as a `PARENT`:
   - Enter student email to link child account.
   - Verify child appears in linked children list.
   - Verify child's detailed BKT mastery and AI parenting advice appear.
3. Log in as a `STUDENT`:
   - Verify student still sees personal learning metrics without interference.
