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

// Auth API Calls
export async function loginUser({ email, password }) {
  return post("/auth/login", { email, password });
}

export async function registerUser({ name, email, password, role }) {
  return post("/auth/register", { name, email, password, role });
}

export async function getMe() {
  return get("/auth/me");
}

// Learning Loop APIs
export async function generateStory({
  character,
  sectionTitle,
  sectionTopic,
  sessionAccuracy = 0,
}) {
  const body = {
    character,
    section_title: sectionTitle,
    section_topic: sectionTopic,
    session_accuracy: sessionAccuracy,
  };
  return post("/story/generate", body);
}

export async function generateQuestion({
  character,
  topic,
  difficulty = 2,
  questionNumber = 1,
  totalQuestions = 10,
  sessionAccuracy = 0,
  previousQuestions = [],
}) {
  const body = {
    character,
    topic,
    difficulty,
    question_number: questionNumber,
    total_questions: totalQuestions,
    session_accuracy: sessionAccuracy,
    previous_questions: previousQuestions,
  };
  return post("/questions/generate", body);
}

export async function generateFeedback({ character, totalCorrect, totalQuestions, sectionTitle }) {
  return post("/feedback/generate", {
    character,
    total_correct: totalCorrect,
    total_questions: totalQuestions,
    section_title: sectionTitle,
  });
}

export async function generateEvaluation({ character, chapterTitle, topic }) {
  return post("/evaluate/generate", {
    character,
    chapter_title: chapterTitle,
    topic,
  });
}

export async function updateProgress({ studentId = "default", sectionId, correct, responseTime = 0, xpEarned = 0 }) {
  return post("/progress/update", {
    student_id: studentId,
    section_id: sectionId,
    correct,
    response_time: responseTime,
    xp_earned: xpEarned,
  });
}

export async function getDashboard(studentId = "default") {
  return get(`/dashboard/${studentId}`);
}
