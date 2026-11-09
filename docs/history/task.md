# Tasks: Phase 4 - Adaptive Engine & Real-Time Student / Parent Dashboard

- [x] Implement `DashboardResponse` DTO and `DashboardService` in Spring Boot (`/api/dashboard`) <!-- id: 0 -->
- [x] Create `DashboardController.java` to serve real student XP, level, accuracy, topic breakdown, and attempt history <!-- id: 1 -->
- [x] Implement `adaptive/` package: `PerformanceSnapshot`, `DifficultyAction`, `AdaptiveDecision`, and `AdaptiveEngineService` <!-- id: 2 -->
- [x] Implement `GET /api/sessions/{id}/next-question` dynamically selecting questions based on the Adaptive Engine <!-- id: 3 -->
- [x] Connect `DashboardPage.jsx` and `api.js` to real Spring Boot dashboard endpoint <!-- id: 4 -->
- [x] Wire `TestPage.jsx` to fetch sequential questions via `/next-question` and display adaptive reason badges <!-- id: 5 -->
- [x] Verify backend compilation (`mvnw.cmd compile`) and frontend build (`npm run build`) <!-- id: 6 -->
