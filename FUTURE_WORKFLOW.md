# Neuro-Learn — Future Workflow Plan

> Baseline: commit `a0eb976` (Chapter 2 Quadratics) · Written 2026-11-09
> Supersedes the *sequencing* in the older plans now kept in [`docs/history/`](docs/history/)
> (`next_steps_plan.md`, `future_scope.md`, `implementation_plan.md`, `task.md`, `walkthrough.md`,
> `research_and_aiml_scope.md`, `COMMIT_HISTORY_AND_WORK_DONE.md`, `neurolearn_migration_plan.md`).
> Those files stay as history; this file is the single source of "what next".

---

## 1. Where the project actually stands

### Done and working
| Area | State |
|---|---|
| Backend | Spring Boot 3.4.3 · 8 controllers · 7 services · 7 entities · Flyway V1–V7 |
| Adaptive engine | BKT (`AdaptiveEngineService`, `BktParameters`) + INCREASE/MAINTAIN/DECREASE + reason strings |
| Content | Chapter 1 (Linear, 6 sections + boss) and Chapter 2 (Quadratic, 6 sections + boss), Batman / Superman / Supergirl themes |
| Student UX | Home → Roadmap → Learn → Test (duel arena) → Feedback → Boss fight, XP/levels, sounds |
| Dashboards | Student, Teacher (bottlenecks, roster, drill-down), Parent (link child, report card) |
| Auth | JWT issue/verify, BCrypt, 3 roles, 1-click demo accounts (V5) |

### Gaps the existing docs don't mention (found by reading the code)

**Security / correctness — these are real bugs, fix first**
1. `SecurityConfig.java` ends with `.anyRequest().permitAll()` → **every endpoint is public**. JWT is parsed but never required.
2. No role checks on `/api/teacher/**` — `GET /api/teacher/student/{id}` returns any student's data to anyone (even unauthenticated). Only `ParentService` checks roles.
3. `DashboardController.getDashboardByStudent` ignores `{studentId}` and returns the caller's own dashboard.
4. `TeacherService` uses `profileRepository.findAll()` — a teacher sees *every* student; there is no class/enrollment concept.
5. JWT secret and DB password are hard-coded in `application.yml`; CORS is `*` + `allowCredentials(true)`.

**README "Guaranteed Deliverables" still not delivered**
6. **LLM-assisted question generation** (README §18 "AI") — nothing exists server-side. The only Gemini code is dead client-side code.
7. **Testing** — only `BktEngineTest` (5 unit tests) + a `contextLoads` test that needs a live Postgres. No integration tests, no CI.
8. **Deployment** — `docker-compose.yml` only runs Postgres. No Dockerfile for backend or frontend.
9. **Fixed-vs-adaptive evaluation** (README §22) — not built; needed for the report's "measurable evaluation".

**Code hygiene**
10. Dead frontend code (imported by nothing reachable): `utils/mcqGemini.js`, `questionGenerator.js`, `storyProblemGemini.js`, `gemini.js`, `mastery.js`, `engines/gemini/*`, `engines/voiceEngine.js`, `components/ModuleDashboard.jsx`, `PixelDashboard.jsx`, `SettingsModal.jsx`, `PixelSettingsModal.jsx`, `modules/moduleBank.js`, plus the `@google/genai` dependency.
11. Curriculum is defined in **three places** that must be kept in sync by hand: `Frontend/src/data/chapters.js`, `ProgressService.CHAPTER_SECTIONS`, and ~450 lines of hard-coded story text in `Frontend/src/utils/api.js`. Chapter 3 would mean editing all three.
12. `legacy-python/` still tracked (incl. `.pyc` and a `.pt` model); no root `.gitignore`. `Frontend/README.md` is the Vite template.
13. Navigation is a `useState("home")` state machine in `App.jsx` — no URLs, refresh loses place, no back button.

**Documentation drift**
14. BKT P(T) is **0.10** in `COMMIT_HISTORY…md`, **0.15** in README §26, **0.18** in code.
15. Section unlock uses `P(L) ≥ 0.40 OR 3 correct`, while docs present 0.85 as the mastery gate — explain both thresholds or they look contradictory.
16. README §18 checklist is entirely unticked though most items are done.
17. **Scope conflict:** README §17/§19 say engagement/ADHD analytics are *out of scope*; `next_steps_plan.md` says *"the entire remaining scope is the ADHD Engagement Analytics Layer"*. Decide which framing the report uses (see §4).

---

## 2. Phased workflow

Ordering principle: **fix what a grader/demo can break → deliver what the README promised → then stretch.**
Effort in focused hours; adjust to the submission date.

### Phase 0 — Repo hygiene (≈3 h)
- [x] Create `docs/` and move all root planning `.md` files into `docs/history/`; keep only `README.md` + this file at root.
- [x] Add root `.gitignore` (`__pycache__/`, `*.pyc`, `.env`, `target/`, `node_modules/`, `dist/`).
- [ ] Delete `legacy-python/` — **waiting on team decision** (§4). Tracked `.pyc` files are already untracked.
- [x] Delete dead frontend files listed in gap #10; `npm uninstall @google/genai`. `npm run build` passes.
- [ ] Get `npm run lint` green — was already failing before cleanup (87 problems); 65 remain, all in live files (`TestPage`, `LearnPage`, `BossFightPage`, `CharacterReactionPopup`, `soundEffects`, `api.js`, …). Needed before CI in Phase 2.
- [x] Replace `Frontend/README.md` with real run instructions; add a "Quick start" (docker → mvnw → npm) to the main README.
- [x] Fix doc drift #14–16 (README §26 now matches `BktParameters.java` and explains the 0.40 unlock vs 0.85 mastery; README §18 checklist ticked).

### Phase 1 — Security & correctness (≈6 h) — **must ship before any demo**
- [ ] `SecurityConfig`: `/api/auth/**`, `/api/health` public; everything else `.authenticated()`. Enable `@EnableMethodSecurity`.
- [ ] `@PreAuthorize("hasRole('TEACHER')")` on `TeacherController`, `hasRole('PARENT')` on `ParentController`, `hasRole('STUDENT')` on sessions/progress. Confirm `UserPrincipal` authorities are `ROLE_*`.
- [ ] Fix `DashboardController.getDashboardByStudent` (use the id + check access) or delete it if unused by the UI.
- [ ] Move `jwt.secret`, DB creds to env vars with dev-only defaults in a `application-dev.yml`; restrict CORS to `http://localhost:5173` (+ deployed origin).
- [ ] Global `@RestControllerAdvice` returning consistent JSON errors (400/401/403/404) instead of stack traces.
- [ ] Frontend: on 401, clear token and route to Auth page.

### Phase 2 — Test & CI foundation (≈8 h)
- [ ] Add Testcontainers (Postgres 16) so `contextLoads` and integration tests run without a manual DB.
- [ ] Integration tests: register/login → start session → next-question → attempt → complete → dashboard updated (README §23 success path as a test).
- [ ] Authorization tests: student hitting `/api/teacher/**` → 403; parent viewing an unlinked child → 403; anonymous → 401.
- [ ] Unit tests for `ProgressService` unlock rules and `TeacherService` bottleneck rule.
- [ ] GitHub Actions: `mvnw verify` + `npm ci && npm run lint && npm run build` on every PR.

### Phase 3 — Teacher classes (≈6 h) — makes the teacher dashboard honest
- [ ] `V8__add_classes.sql`: `classes(id, teacher_user_id, name, join_code)` + `class_enrollments(class_id, student_profile_id)`; seed demo class for V5 accounts.
- [ ] Student joins via join code; `TeacherService` scopes all queries to the teacher's classes; class switcher in Teacher dashboard.

### Phase 4 — LLM question generation (README guaranteed deliverable) (≈8 h)
Design per README §8: **offline, teacher-reviewed, never in the student loop.**
- [ ] `V9__question_review_status.sql`: `questions.status` (`APPROVED` default for existing / `PENDING` / `REJECTED`), `questions.source` (`SEED` / `LLM`), `created_by`.
- [ ] `ai/QuestionGenerationService` (Spring `RestClient` → Gemini; key from `GEMINI_API_KEY`): prompt with topic, difficulty, character theme, N; strict JSON schema; validate 4 options, 1 correct, dedupe vs existing text; retries.
- [ ] `POST /api/teacher/questions/generate` → saves as `PENDING`; `GET /api/teacher/questions?status=PENDING`; `POST …/{id}/approve|reject`; edit before approve.
- [ ] Question selection queries filter `status = 'APPROVED'`.
- [ ] Teacher UI tab "Question Bank": generate form + review queue.
- [ ] Unit-test the validator with a mocked LLM response (no network in CI).

### Phase 5 — Evaluation: fixed vs adaptive (≈6 h) — gives the report numbers
- [ ] `SimulationService` (or a JUnit/CLI runner): simulated students with known true-skill curves answer questions under (a) fixed difficulty order and (b) the adaptive engine.
- [ ] Metrics: questions-to-mastery, final P(L) error vs true skill, % time in "productive struggle" band, difficulty trajectory.
- [ ] Output a CSV + charts for the report; document method in `docs/evaluation.md`.
- [ ] Reuse the same simulator later as the synthetic-data generator for DKT (future_scope §6.3).

### Phase 6 — Deployment (≈5 h)
- [ ] `backend-java/Dockerfile` (multi-stage maven → JRE 17), `Frontend/Dockerfile` (build → nginx, proxy `/api`).
- [ ] Extend `docker-compose.yml` to the full stack + `.env.example`; one command `docker compose up` runs the demo.
- [ ] Optional: deploy to Render/Railway/Fly for a live URL.

### Phase 7 — Engagement analytics (stretch; from `next_steps_plan.md` A–D) (≈12 h)
Only after 0–6. Renumber migrations (V10+), and reuse the plan already written there:
- [ ] A: `learning_sessions.completion_status` + `@Scheduled` abandonment job (+ `@EnableScheduling`).
- [ ] B: `AnalyticsService` — response-time mean/σ/z-variance; oscillation `[C,W,C,W]` classifier.
- [ ] C: composite **Engagement Pattern Flag** (🟢/🟡/🔴) in teacher roster with the *"not a clinical assessment"* tooltip and "flagged only" filter.
- [ ] D: parent weekly-activity chart + response-time trend.

### Phase 8 — Content scalability (stretch, ≈8 h) — do before writing Chapter 3
- [ ] Move chapters/sections/story text into DB (`chapters`, `sections`, `section_briefings` per theme) and serve via `GET /api/curriculum`.
- [ ] Delete `CHAPTER_SECTIONS` map and the story blob in `api.js`; `chapters.js` keeps only character art.
- [ ] Optional: react-router for real URLs (`/roadmap/:chapter`, `/learn/:section`, `/dashboard`).

### Phase 9 — Research track (next semester / capstone)
IRT 2PL calibration (needs ≥200 real users) → DKT/LSTM on simulated + real sequences as a Python microservice → Socratic LLM hints → curriculum RAG. Details already in `future_scope.md` §6 and `research_and_aiml_scope.md` Part 2.

---

## 3. Recommended order & cut lines

```
Phase 0 ─► Phase 1 ─► Phase 2 ─► Phase 4 ─► Phase 5 ─► Phase 6     ← minimum for a defensible submission (~36 h)
                         └──► Phase 3 (fits anywhere after 1)
                                                         └──► Phase 7 ─► Phase 8 ─► Phase 9   ← stretch
```
- **Short on time?** Do 0, 1, 2 (just the success-path + authz tests), 4, 6. Skip 3 and 5 and say so in the report's future work.
- **Want the ADHD/engagement angle in the report?** Swap Phase 7 ahead of Phase 5, and update README §17/§19 so the report doesn't contradict itself.

## 4. Decisions needed from the team
1. **Positioning:** keep README's "engagement is future work" or adopt `next_steps_plan.md`'s "engagement analytics is the remaining scope"? (Affects Phase 7's priority and README wording.)
2. **Submission date** — determines the cut line above.
3. **`legacy-python/`** — delete or keep as archive?
4. **Live deployment** needed, or is `docker compose up` enough for grading?

## 5. Working conventions going forward
- **Branch per phase** (`feat/security-hardening`, `feat/llm-questions`, …) → PR → CI green → merge. Stop committing straight to `main`.
- **Flyway:** never edit an applied migration; next free number is **V8**. Seed data stays in its own migrations.
- **Definition of done per item:** backend tests pass, `npm run lint && npm run build` pass, README/API docs updated, demo account path still works.
- **One plan file:** update this file's checkboxes as work lands instead of adding new top-level `.md` plans.
