const BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";

function getAuthHeaders() {
  const token = localStorage.getItem("neurolearn_token");
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

async function post(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const errObj = await res.json().catch(() => ({ message: "Unknown error" }));
    throw new Error(errObj.message || `API ${path} failed with status ${res.status}`);
  }
  return res.json();
}

async function get(path) {
  const res = await fetch(`${BASE}${path}`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const errObj = await res.json().catch(() => ({ message: "Unknown error" }));
    throw new Error(errObj.message || `API ${path} failed with status ${res.status}`);
  }
  return res.json();
}

// -------------------------------------------------------------
// Auth API Calls
// -------------------------------------------------------------
export async function loginUser({ email, password }) {
  return post("/auth/login", { email, password });
}

export async function registerUser({ name, email, password, role }) {
  return post("/auth/register", { name, email, password, role });
}

export async function getMe() {
  return get("/auth/me");
}

// -------------------------------------------------------------
// Question Bank APIs (Spring Boot)
// -------------------------------------------------------------
export async function fetchQuestions({ topic, difficulty }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  if (difficulty) params.append("difficulty", difficulty);
  const query = params.toString() ? `?${params.toString()}` : "";
  return get(`/questions${query}`);
}

export async function fetchRandomQuestion({ topic, difficulty }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  if (difficulty) params.append("difficulty", difficulty);
  const query = params.toString() ? `?${params.toString()}` : "";
  return get(`/questions/random${query}`);
}

export async function fetchTopics() {
  return get("/questions/topics");
}

// -------------------------------------------------------------
// Learning Session & Adaptive Engine APIs (Spring Boot)
// -------------------------------------------------------------
export async function startLearningSession({ topic }) {
  return post("/sessions/start", { topic });
}

export async function fetchNextAdaptiveQuestion({ sessionId, topic }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  const query = params.toString() ? `?${params.toString()}` : "";
  return get(`/sessions/${sessionId}/next-question${query}`);
}

export async function recordQuestionAttempt({ sessionId, questionId, correct, responseTimeMs, attemptNumber = 1 }) {
  return post(`/sessions/${sessionId}/attempt`, {
    questionId,
    correct,
    responseTimeMs,
    attemptNumber,
  });
}

export async function completeLearningSession({ sessionId }) {
  return post(`/sessions/${sessionId}/complete`, {});
}

// -------------------------------------------------------------
// Progress & Curriculum Roadmap APIs (Spring Boot)
// -------------------------------------------------------------
export async function fetchProgress() {
  return get("/progress");
}

export async function claimBossVictory() {
  return post("/progress/boss-victory", {});
}

// -------------------------------------------------------------
// Dashboard API (Spring Boot)
// -------------------------------------------------------------
export async function getDashboard(studentId = "") {
  return get(`/dashboard`);
}

// -------------------------------------------------------------
// Teacher Analytics APIs
// -------------------------------------------------------------
export async function fetchTeacherAnalytics() {
  return get("/teacher/analytics");
}

export async function fetchTeacherStudentDashboard(studentProfileId) {
  return get(`/teacher/student/${studentProfileId}`);
}

// -------------------------------------------------------------
// Parent Portal APIs
// -------------------------------------------------------------
export async function linkChild(studentEmail) {
  return post("/parent/link", { studentEmail });
}

export async function fetchParentChildren() {
  return get("/parent/children");
}

export async function fetchChildReportCard(studentProfileId) {
  return get(`/parent/child/${studentProfileId}`);
}

// -------------------------------------------------------------
// Story & Feedback APIs (Batman / Section content generator)
// -------------------------------------------------------------
export async function generateStory({ character, sectionTitle, sectionTopic, sessionAccuracy = 0 }) {
  return Promise.resolve({
    story: `🦇 Batcomputer Mission Briefing: Welcome to ${sectionTitle}. Gotham is under threat from encoded frequency attacks. By mastering ${sectionTopic}, Batman can calculate intercept vectors, balance turbine grids, and dismantle the syndicate's security locks. Prepare your calculation console, hero.`,
    concept_summary: `Linear equations in two variables (ax + by = c) allow us to model two related quantities simultaneously. On the coordinate grid, every valid (x, y) coordinate forms a straight line of trajectory.`,
    examples: [
      {
        problem: "Verify if (3, 2) lies on 2x + 3y = 12",
        solution: "Substitute x=3, y=2: 2(3) + 3(2) = 6 + 6 = 12. Coordinate confirmed on path!",
      },
    ],
  });
}

export async function generateFeedback({ character, totalCorrect, totalQuestions, sectionTitle }) {
  const pct = Math.round((totalCorrect / Math.max(1, totalQuestions)) * 100);
  let message = "";
  if (pct >= 80) {
    message = `🦇 "Exceptional work. Your precision in ${sectionTitle} has secured Gotham's grid. The Batcomputer registers your mastery." — Batman`;
  } else if (pct >= 50) {
    message = `🦇 "Good operational effort. We held the perimeter, but review the linear formulas before our next encounter with the syndicate." — Batman`;
  } else {
    message = `🦇 "Regroup and recalibrate. Every detective encounters resistance. Review the fundamentals and take the challenge once more." — Batman`;
  }

  return Promise.resolve({
    message,
    xp_earned: totalCorrect * 15 + 10,
    accuracy: pct,
  });
}

export async function generateEvaluation({ character, chapterTitle, topic }) {
  return Promise.resolve({
    message: `Evaluation completed for ${chapterTitle}.`,
    score: 85,
  });
}

export async function updateProgress({ studentId = "default", sectionId, correct, responseTime = 0, xpEarned = 0 }) {
  return post("/progress/update", {
    student_id: studentId,
    section_id: sectionId,
    correct,
    response_time: responseTime,
    xp_earned: xpEarned,
  }).catch(() => null);
}
