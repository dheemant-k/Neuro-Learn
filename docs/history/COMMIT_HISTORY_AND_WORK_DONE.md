# Neuro-Learn: Commit History & Current Work Done

> **Project:** Neuro-Learn — Adaptive Gamified Intelligent Tutoring System  
> **Repository:** `c:\coding\sem5proj\SE-JAVA\Neuro-Learn`  
> **Latest Commit:** `a0eb976` (Chapter 2 Quadratic Equations, dynamic multi-chapter roadmap, and boss fights)  
> **Status Date:** November 2026  

---

## 1. Executive Summary & Current State

Neuro-Learn has evolved from a legacy single-script prototype into a full-stack, enterprise-grade Intelligent Tutoring System (ITS) powered by **Spring Boot 3.4.3**, **React 18 + Vite + Tailwind CSS**, and **PostgreSQL 16**. 

The system features real-time **Bayesian Knowledge Tracing (BKT)** for student mastery estimation, dynamic multi-chapter roadmaps, boss-fight gamification with character theming (Superman, Batman, Supergirl), dedicated multi-role portals (**Student**, **Teacher**, and **Parent**), and Flyway database version control.

---

## 2. Complete Inventory of Current Work Done

### 2.1 Backend Architecture (Spring Boot & Security)
* **Framework:** Spring Boot 3.4.3 running on Java 17/21.
* **Authentication & Authorization:**
  * Stateless JWT authentication (`JwtTokenProvider`, `JwtAuthenticationFilter`).
  * Role-based access control supporting `STUDENT`, `TEACHER`, and `PARENT` roles.
  * BCrypt password hashing and Spring Security 6 filter chain configuration (`SecurityConfig.java`).
* **Database & ORM:**
  * Spring Data JPA / Hibernate mapped to PostgreSQL.
  * Automated database migration pipeline managed via **Flyway** (7 versioned migrations from V1 to V7).
  * 7 core JPA entities: `User`, `StudentProfile`, `Question`, `QuestionAttempt`, `TopicProgress`, `LearningSession`, `ParentStudentLink`.

### 2.2 Pedagogical AI Engine: Bayesian Knowledge Tracing (BKT)
* **Mathematical Foundation:** Implements Corbett & Anderson's Hidden Markov Model formulation:
  $$\begin{aligned}
  P(L_{t-1} \mid \text{correct}) &= \frac{P(L_{t-1}) \cdot (1 - P(S))}{P(L_{t-1}) \cdot (1 - P(S)) + (1 - P(L_{t-1})) \cdot P(G)} \\
  P(L_{t-1} \mid \text{incorrect}) &= \frac{P(L_{t-1}) \cdot P(S)}{P(L_{t-1}) \cdot P(S) + (1 - P(L_{t-1})) \cdot (1 - P(G))} \\
  P(L_t) &= P(L_{t-1} \mid \text{obs}) + (1 - P(L_{t-1} \mid \text{obs})) \cdot P(T)
  \end{aligned}$$
* **Core Engine Components:**
  * `AdaptiveEngineService.java`: Evaluates question attempts, updates posterior mastery probabilities, and determines dynamic difficulty adjustment (`INCREASE`, `MAINTAIN`, `DECREASE`).
  * `BktParameters.java`: Encapsulates default parameters ($P(L_0) = 0.20$, $P(T) = 0.18$, $P(S) = 0.10$, $P(G) = 0.25$) with per-topic calibration support.
  * Multi-topic mastery tracking persisted in the `topic_progress` table.

### 2.3 Curriculum & Question Bank
* **Chapter 1: Linear Equations & Systems:**
  * Topics: Graphing, substitution, elimination, word problems, final boss.
  * Dual-themed narrative question banks (Batman / Gotham City and Superman / Metropolis).
* **Chapter 2: Quadratic Equations & Parabolas:**
  * Topics: `quad_intro`, `quad_factoring`, `quad_formula`, `quad_discriminant`, `quad_graphing`, `quad_word_problems`, `quad_final_boss`.
  * Dynamic multi-chapter roadmap and themed questions across Superman, Batman, and Supergirl skins.
* **Flyway Migrations Log:**
  1. `V1__init_schema.sql`: Core schema (users, profiles, questions, attempts, sessions, progress).
  2. `V2__seed_batman_questions.sql`: Initial Gotham/Batman question bank.
  3. `V3__add_bkt_fields.sql`: BKT parameter fields and posterior tracking.
  4. `V4__add_parent_student_links.sql`: Parent-student relational join table.
  5. `V5__seed_demo_accounts_and_analytics.sql`: Pre-seeded demo teacher, parent, and student accounts.
  6. `V6__add_character_theme_and_superman_questions.sql`: Superman questions bank and `character_theme` column.
  7. `V7__add_chapter2_quadratic_questions.sql`: Full Chapter 2 Quadratic curriculum and boss questions.

### 2.4 Multi-Stakeholder Portals & Dashboards

#### A. Student Experience & Gamification
* **Interactive Duel Arena:** Superhero (Superman / Batman) vs. Villain (Lex Luthor / Joker) combat arena with live animated dialogue bubbles, audio cues, and battle particle effects.
* **Dynamic Multi-Chapter Roadmap (`RoadmapPage.jsx`):** Node-based progression tree with unlock conditions, mastery stars, and chapter selectors.
* **Boss Arena (`BossFightPage.jsx`):** High-stakes timed duel with boss health bars, shield bars, and combo multipliers.
* **Personalized Feedback (`FeedbackPage.jsx` & `LearnPage.jsx`):** Adaptive step-by-step mathematical explanations themed to the student's chosen character.

#### B. Teacher Command Center (`TeacherController.java` & `TeacherDashboard`)
* Class-wide aggregate metrics: Total Students, Class Average XP, Class Accuracy, Average Bayesian Mastery $P(L)$.
* **Automated Curriculum Bottleneck Alerts:** Algorithmically highlights topics where struggling students exceed mastered students or where average $P(L) < 0.65$.
* **Interactive Student Roster:** Searchable and sortable roster table with individual XP, accuracy, attempt counts, and status badges.
* **Drill-Down Modal:** Allows teachers to inspect any individual student's detailed BKT mastery breakdown across all curriculum sections.

#### C. Parent Portal (`ParentController.java` & `ParentDashboard`)
* **Child Linking System:** Allows parents to securely link multiple child student accounts by email address.
* **Child Switcher:** Seamless tab navigation between multiple children.
* **Comprehensive Child Report Card:** Displays Bayesian progress, strengths, areas for growth, and evidence-based pedagogical recommendations for parents.

---

## 3. Complete Git Commit History

| Commit Hash | Date | Author | Commit Message & Description |
|:---:|:---:|:---:|---|
| **`a0eb976`** | 2026-11-01 | dheemant-k | **feat: Chapter 2 Quadratic Equations with character-themed questions, dynamic multi-chapter roadmap, and boss fights**<br>• Seeded 40+ quadratic questions across 7 subtopics (V7 migration)<br>• Added multi-chapter selection to roadmap<br>• Updated backend progress controllers to track multiple chapters |
| **`3d5820d`** | 2026-10-27 | dheemant-k | **feat: character-aware FeedbackPage UI overhaul, LearnPage rewrite, RoadmapPage boss text theming, and api.js character-aware story/feedback generators**<br>• Fully themed learning and feedback views based on active superhero/villain pairing |
| **`497b2f3`** | 2026-10-22 | dheemant-k | **feat: Superman-themed questions bank (Flyway V6), dynamic character-based question serving, enhanced question display UI, and Superman vs Lex Luthor Boss Arena**<br>• V6 migration introducing `character_theme` column<br>• Seeded Superman curriculum questions |
| **`175157b`** | 2026-10-22 | dheemant-k | **feat: permanent interactive side character panels (Superman & Lex Luthor) with animated dialog bubbles and particle effects**<br>• Visual overhaul of the duel arena layout |
| **`76fbad2`** | 2026-10-15 | dheemant-k | **feat: overhaul quiz into Superman vs LexCorp interactive duel arena with audio & assists**<br>• Added Web Audio API sound effects, hint system, and battle meters |
| **`96bfd77`** | 2026-10-14 | dheemant-k | **feat: pre-seed demo accounts with rich analytics and add 1-click login**<br>• Added quick demo credentials for Student, Teacher, and Parent |
| **`33ddfac`** | 2026-10-14 | dheemant-k | **docs: add BKT mathematical formulas and academic research citations to README**<br>• Formalized ITS literature citations (Corbett & Anderson, Bloom 2-Sigma) |
| **`d6c91b2`** | 2026-10-12 | dheemant-k | **feat: Teacher and Parent dashboards - Option 2 complete**<br>• Implemented `TeacherService`, `ParentService`, and Flyway `V4__add_parent_student_links.sql`<br>• Added curriculum bottleneck alerts and child report cards |
| **`816ed67`** | 2026-10-09 | dheemant-k | **feat: persistent roadmap & interactive boss fight arena**<br>• Created `RoadmapPage.jsx` and `BossFightPage.jsx` with persistent unlock state |
| **`0e5d06d`** | 2026-09-23 | dheemant-k | **feat: implement Bayesian Knowledge Tracing (BKT) adaptive engine & option shuffling**<br>• Implemented real-time HMM Bayesian updates on every question attempt |
| **`8ffb5c4`** | 2026-09-17 | dheemant-k | **feat: Phase 3 & 4 - Batman Question Bank, Adaptive Engine & Dashboard**<br>• Created Batman linear equations bank and initial student dashboard |
| **`df149a8`** | 2026-09-09 | dheemant-k | **feat: Phase 1 & 2 - Spring Boot backend + JWT auth infrastructure**<br>• Set up Spring Boot project structure, Postgres Flyway pipeline, and JWT auth |
| **`c5c1e7a`** | 2026-03-12 | Parth Arora | **Update README with new preview section** |
| **`f18ddce`** | 2026-03-12 | Parth2632 | **Add UI screenshots and update README** |
| **`fff4e5b`** | 2026-03-10 | ShuchihNegi | **Update API key placeholder in app.py** |
| **`750cdfc`** | 2026-03-10 | ShuchihNegi | **Delete backend/__pycache__ directory** |
| **`1d57654`** | 2026-03-10 | Shuchih-Negi | **Initial commit** |

---

## 4. Next Implementation Steps (Future Scope)

For next development phases (refer to [next_steps_plan.md](next_steps_plan.md) and [future_scope.md](future_scope.md)):
1. **Phase A (V8 Migration & Session Abandonment):** Add `completion_status` to `learning_sessions`, create `student_analytics` table, and implement session timeout scheduler.
2. **Phase B (Engagement & Response Time Analytics):** Add response time mean/variance endpoint and oscillation error pattern classifier (`[C, W, C, W]`).
3. **Phase C (Engagement Pattern Flag):** Surface composite attention indicators (`TYPICAL`, `MONITOR`, `HIGH`) in the Teacher Dashboard roster.
4. **Phase D (Parent Activity Trends):** Weekly active days charts and response time trends in the Parent Report Card.
5. **Phase E (IRT Calibration Script):** Offline Python Item Response Theory (2PL) calibration script.
