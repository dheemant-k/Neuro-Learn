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
export async function fetchQuestions({ topic, difficulty, character, characterTheme }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  if (difficulty) params.append("difficulty", difficulty);
  const theme = characterTheme || character;
  if (theme) params.append("characterTheme", theme);
  const query = params.toString() ? `?${params.toString()}` : "";
  return get(`/questions${query}`);
}

export async function fetchRandomQuestion({ topic, difficulty, character, characterTheme }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  if (difficulty) params.append("difficulty", difficulty);
  const theme = characterTheme || character;
  if (theme) params.append("characterTheme", theme);
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

export async function fetchNextAdaptiveQuestion({ sessionId, topic, character, characterTheme }) {
  const params = new URLSearchParams();
  if (topic) params.append("topic", topic);
  const theme = characterTheme || character;
  if (theme) params.append("characterTheme", theme);
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
// Story & Feedback APIs (Dynamic Character Lore & Mission Engine)
// -------------------------------------------------------------
export async function generateStory({ character, sectionTitle, sectionTopic, sessionAccuracy = 0 }) {
  const charStr = (typeof character === "string" ? character : character?.name || character?.id || "Superman").toLowerCase();
  const isBatman = charStr.includes("batman");
  const isSupergirl = charStr.includes("supergirl");
  const titleLower = (sectionTitle || "").toLowerCase();

  let title = `${sectionTitle} Briefing`;
  let story = "";
  let key_points = [];
  let examples = [];

  if (isBatman) {
    if (titleLower.includes("intro")) {
      title = "Arkham Perimeter Sensor Check";
      story = `🦇 Batcomputer Mission Briefing: Welcome to ${sectionTitle}. Gotham City is under threat from encoded frequency attacks by the syndicate. To secure the perimeter, we model two variable sensors (x and y) using linear equations in the standard form ax + by = c. Every point (x, y) that satisfies the equation lies on our security line. Prepare your console, detective.`;
      key_points = [
        "A linear equation in two variables has standard form ax + by = c (a, b != 0).",
        "Variables x and y are degree 1; no xy cross-products or exponents allowed.",
        "An ordered pair (x, y) is a solution if it satisfies the equation when substituted.",
      ];
      examples = [
        { problem: "Check if Wayne Manor coordinate (3, 4) lies on 2x + y = 10", solution: "Substitute x=3, y=4: 2(3) + 4 = 6 + 4 = 10. The coordinate lies on the line!" },
      ];
    } else if (titleLower.includes("graph")) {
      title = "Batmobile Tactical Coordinates";
      story = `🦇 Gotham Mapping Dispatch: Plotting equations on the coordinate grid allows Batman to trace pursuit routes across Gotham. By determining where a flight path hits the ground (x-intercept) and radar ceiling (y-intercept), we establish exact trajectory intercepts.`;
      key_points = [
        "x-intercept is where the graph crosses the x-axis (set y = 0).",
        "y-intercept is where the graph crosses the y-axis (set x = 0).",
        "Slope m = (y2 - y1) / (x2 - x1) determines trajectory steepness.",
      ];
      examples = [
        { problem: "Find the x-intercept of Batmobile patrol line 3x + 2y = 18", solution: "Set y=0: 3x = 18 => x = 6. Intercept is (6, 0)." },
      ];
    } else if (titleLower.includes("pair") || titleLower.includes("equation")) {
      title = "Syndicate Dual Intercept Grid";
      story = `🦇 Batcomputer Systems Analysis: Two separate criminal signals form a system of simultaneous linear equations. Where the two lines intersect is their common meeting point. If the lines are parallel, their signals never cross!`;
      key_points = [
        "Intersecting lines: Unique solution (consistent and independent).",
        "Parallel lines: No solution (inconsistent, slopes match but intercepts differ).",
        "Coincident lines: Infinite solutions (dependent, equations are scalar multiples).",
      ];
      examples = [
        { problem: "Find intersection of signal lines x + y = 10 and x - y = 4", solution: "Add equations: 2x = 14 => x = 7. Then y = 3. Point is (7, 3)." },
      ];
    } else if (titleLower.includes("substitut")) {
      title = "Wayne Forensics Substitution Protocol";
      story = `🦇 Batcave Forensic Decryption: When one variable is readily isolated, we substitute its entire expression into the second equation. This collapses two unknown coordinates into a single solvable variable.`;
      key_points = [
        "Step 1: Express one variable in terms of the other (e.g. y = 12 - 3x).",
        "Step 2: Substitute that expression into the second equation.",
        "Step 3: Solve for the remaining variable and back-substitute.",
      ];
      examples = [
        { problem: "Solve y = 2x and x + y = 12 using substitution", solution: "x + 2x = 12 => 3x = 12 => x = 4. Then y = 8. Solution: (4, 8)." },
      ];
    } else if (titleLower.includes("eliminat")) {
      title = "Frequency Jammer Elimination Protocol";
      story = `🦇 Batcomputer Elimination Protocol: When equations are in standard form, we eliminate a variable by multiplying equations to match coefficients and adding or subtracting them to cancel out interference.`;
      key_points = [
        "Step 1: Multiply equations so coefficients of x or y match.",
        "Step 2: Add if signs differ; subtract if signs match.",
        "Step 3: Solve the resulting single variable equation.",
      ];
      examples = [
        { problem: "Solve 2x + y = 11 and 3x - y = 9 by elimination", solution: "Add equations to eliminate y: 5x = 20 => x = 4. Then y = 3." },
      ];
    } else {
      title = "Gotham City Logistics Analysis";
      story = `🦇 Detective Tactical Briefing: Real-world problems translate directly into systems of two equations with two unknowns. Define your variables, construct the equations, and solve with deduction.`;
      key_points = [
        "Define unknown variables x and y with appropriate units.",
        "Construct two independent linear equations from the clues.",
        "Solve and verify your answers in the context of the problem.",
      ];
      examples = [
        { problem: "Clark buys 2 digital and 3 print passes for $35; 4 digital and 3 print for $49. Find 1 digital pass (x).", solution: "Subtract 1st from 2nd: 2x = 14 => x = $7." },
      ];
    }
  } else if (isSupergirl) {
    if (titleLower.includes("intro")) {
      title = "DEO Satellite Array Briefing";
      story = `🦸 DEO Tactical Briefing: Supergirl here! S.T.A.R. Labs and the DEO are tracking anomalous energy signatures across National City. We model signal intensity (x) and atmospheric height (y) using linear equations in two variables: ax + by = c. Let's calibrate the array coordinates and safeguard the skies!`;
      key_points = [
        "Standard linear form: ax + by = c (a, b != 0).",
        "Linear systems represent constant rate relationships across coordinates.",
        "Any coordinate (x, y) satisfying the equation lies on our trajectory.",
      ];
      examples = [
        { problem: "Does DEO coordinate (5, 2) lie on 3x - 2y = 11?", solution: "Substitute x=5, y=2: 3(5) - 2(2) = 15 - 4 = 11. Coordinate verified on trajectory!" },
      ];
    } else if (titleLower.includes("graph")) {
      title = "Supersonic Flight Trajectory Mapping";
      story = `🦸 National City Skyway Plotting: Plotting linear equations on the coordinate grid gives us exact flight vectors. Finding where our trajectory crosses the ground beacon (x-intercept) and satellite orbit (y-intercept) ensures zero collateral damage.`;
      key_points = [
        "x-intercept is found by setting altitude y = 0.",
        "y-intercept is found by setting horizontal distance x = 0.",
        "Slope m reflects our climb or descent rate.",
      ];
      examples = [
        { problem: "Find y-intercept of flight path 4x + 3y = 24", solution: "Set x=0: 3y = 24 => y = 8. Satellite intercept is (0, 8)." },
      ];
    } else {
      title = "Cosmic Energy Balance Protocol";
      story = `🦸 DEO Dual Matrix Analysis: When two cosmic probes emit signals simultaneously, solving their system of linear equations reveals their exact rendezvous coordinates in the sky.`;
      key_points = [
        "Intersection coordinate represents simultaneous solution for both systems.",
        "Substitution and elimination allow us to isolate variables at high speed.",
      ];
      examples = [
        { problem: "Solve x + y = 14 and x - y = 6", solution: "Add: 2x = 20 => x = 10. Then y = 4. Coordinates: (10, 4)." },
      ];
    }
  } else {
    // Superman (Default)
    if (titleLower.includes("intro")) {
      title = "Metropolis Solar Defense Grid";
      story = `🦸 Metropolis Solar Grid Briefing: Superman here! LexCorp has recalibrated Metropolis's solar power relay. We need to model two interdependent variables—solar absorption rate (x) and battery storage (y)—using linear equations in the standard form ax + by = c. Each ordered pair (x, y) that balances the equation represents a stable power node. Let's calibrate the coordinates and protect the city!`;
      key_points = [
        "A linear equation in two variables has standard form ax + by = c, where a, b, and c are constants.",
        "The degree of each variable is 1, and no variable product (xy) exists.",
        "An ordered pair (x, y) is a solution if substituting it makes both sides of the equation equal.",
      ];
      examples = [
        { problem: "Does (4, 3) lie on the solar perimeter 3x + y = 15?", solution: "Substitute x=4, y=3: 3(4) + 3 = 12 + 3 = 15. The coordinate balances the equation, so it lies on the perimeter!" },
      ];
    } else if (titleLower.includes("graph")) {
      title = "Skyway Trajectory Mapping";
      story = `🦸 Daily Planet Airspace Briefing: When we plot flight vectors over Metropolis, every linear equation forms a continuous line across the coordinate plane. By finding where our flight trajectory intercepts the ground (x-intercept) and beacon height (y-intercept), we can predict intercept coordinates with pinpoint accuracy.`;
      key_points = [
        "The x-intercept is where the line crosses the ground axis (set y = 0).",
        "The y-intercept is where the line crosses the vertical axis (set x = 0).",
        "Slope m = (y2 - y1) / (x2 - x1) measures the rate of ascent and direction of trajectory.",
      ];
      examples = [
        { problem: "Find the x-intercept of the monorail line 2x + 3y = 12", solution: "Set y=0: 2x + 3(0) = 12 => 2x = 12 => x = 6. The intercept is (6, 0)." },
      ];
    } else if (titleLower.includes("pair") || titleLower.includes("equation")) {
      title = "LexCorp Drone Interception Grid";
      story = `🦸 Dual Vector Surveillance: LexCorp has launched synchronized drones across Metropolis. When two linear flight equations exist simultaneously, they form a system of equations. Their point of intersection (x, y) represents the exact point of contact. If the lines are parallel, they never intersect!`;
      key_points = [
        "Intersecting lines: Unique single solution (consistent and independent system).",
        "Parallel lines: No solution (inconsistent system; slopes are equal, intercepts differ).",
        "Coincident lines: Infinitely many solutions (dependent system; equations are scalar multiples).",
      ];
      examples = [
        { problem: "Find intersection of drone paths x + y = 10 and x - y = 4", solution: "Add equations: 2x = 14 => x = 7. Substitute into x + y = 10 => 7 + y = 10 => y = 3. Point is (7, 3)." },
      ];
    } else if (titleLower.includes("substitut")) {
      title = "Fortress Crystal Encryption Breach";
      story = `🦸 Fortress of Solitude Encryption: To crack LexCorp's frequency scrambler, we use the substitution method. We isolate one variable in terms of the other from one equation, then substitute that entire expression into the second equation. This collapses two unknown coordinates into a single solvable variable!`;
      key_points = [
        "Step 1: Express one variable in terms of the other (e.g. y = 12 - 3x).",
        "Step 2: Substitute this expression into the second equation.",
        "Step 3: Solve the resulting single-variable equation.",
        "Step 4: Back-substitute into the first equation to find the second variable.",
      ];
      examples = [
        { problem: "Solve y = 2x and x + y = 12 using substitution", solution: "Substitute y into the 2nd equation: x + 2x = 12 => 3x = 12 => x = 4. Then y = 2(4) = 8. Solution: (4, 8)." },
      ];
    } else if (titleLower.includes("eliminat")) {
      title = "Cadmus Frequency Elimination Protocol";
      story = `🦸 Bio-Frequency Elimination: LexCorp jammers can be completely cancelled out using the elimination method! By multiplying one or both equations so that coefficients of either x or y match, we add or subtract equations to eliminate that variable entirely, clearing the path to victory!`;
      key_points = [
        "Step 1: Multiply equations so coefficients of one variable have the same or opposite magnitude.",
        "Step 2: Add equations if signs are opposite; subtract if signs are the same.",
        "Step 3: Solve the remaining variable and back-substitute.",
      ];
      examples = [
        { problem: "Solve 2x + y = 11 and 3x - y = 9 by elimination", solution: "Add the two equations to eliminate y: 5x = 20 => x = 4. Substitute: 2(4) + y = 11 => y = 3. Solution: (4, 3)." },
      ];
    } else {
      title = "Metropolis Real-World Dispatch";
      story = `🦸 Daily Planet Dispatch: Real-world crises in Metropolis—from rescue fleet speeds to solar alloy mixture percentages—translate directly into pairs of linear equations. Identify the unknown quantities, assign variables x and y, build the two equations, and solve step by step!`;
      key_points = [
        "Step 1: Identify unknown quantities and assign variables x and y.",
        "Step 2: Translate real-world relationships into two algebraic equations.",
        "Step 3: Solve using substitution or elimination and verify integer units.",
      ];
      examples = [
        { problem: "Clark buys 2 digital and 3 print passes for $35; 4 digital and 3 print for $49. Find 1 digital pass (x).", solution: "Subtract 1st from 2nd: 2x = 14 => x = $7." },
      ];
    }
  }

  return Promise.resolve({
    title,
    story,
    key_points,
    examples,
  });
}

export async function generateFeedback({ character, totalCorrect, totalQuestions, sectionTitle }) {
  const pct = Math.round((totalCorrect / Math.max(1, totalQuestions)) * 100);
  const charStr = (typeof character === "string" ? character : character?.name || character?.id || "Superman").toLowerCase();
  const isBatman = charStr.includes("batman");
  const isSupergirl = charStr.includes("supergirl");

  let message = "";
  let tip = "";

  if (isBatman) {
    if (pct >= 80) {
      message = `🦇 "Exceptional work, detective. Your precision in ${sectionTitle} has secured Gotham's grid. The Batcomputer registers your mastery." — Batman`;
      tip = "Your coordinate calculations are sharp. Maintain this standard when tackling the Arkham Mainframe!";
    } else if (pct >= 50) {
      message = `🦇 "Good operational effort. We held the perimeter, but review the linear formulas before our next encounter with the syndicate." — Batman`;
      tip = "Check your sign distributions and verify both sides of the equation after substituting.";
    } else {
      message = `🦇 "Regroup and recalibrate. Every detective encounters resistance. Review the fundamentals and take the challenge once more." — Batman`;
      tip = "Isolate one variable at a time. Work step-by-step to avoid arithmetic traps.";
    }
  } else if (isSupergirl) {
    if (pct >= 80) {
      message = `🦸 "Super sonic accuracy! You decrypted the coordinate vectors in ${sectionTitle} flawlessly. National City is safe!" — Supergirl`;
      tip = "Your calculations are flying at cosmic speeds. Keep this momentum going!";
    } else if (pct >= 50) {
      message = `🦸 "Great energy! We deflected the primary threat, but take a moment to review the algebraic steps for full mastery!" — Supergirl`;
      tip = "Remember to balance both sides when doing operations on linear terms.";
    } else {
      message = `🦸 "Shake it off! Even Kryptonians have to train. Focus on the core formulas, take a deep breath, and fly right back in!" — Supergirl`;
      tip = "Break equations down into smaller pieces before solving.";
    }
  } else {
    // Superman (Default)
    if (pct >= 80) {
      message = `🦸 "Outstanding calculations! You breached LexCorp's frequency matrix in ${sectionTitle} and kept Metropolis safe under the sun." — Superman`;
      tip = "Your mathematical focus is solar-charged! You're ready to breach Lex Luthor's final defense core.";
    } else if (pct >= 50) {
      message = `🦸 "Strong effort! The solar shield held the perimeter, but review the linear formulas before our next encounter with Lex Luthor." — Superman`;
      tip = "Double-check your coefficients and ensure opposite signs cancel during elimination.";
    } else {
      message = `🦸 "Regroup and recharge! Every hero faces resistance. Recalibrate your formulas, absorb the sun's rays, and try again!" — Superman`;
      tip = "Take your time with substitution—plug the full expression in carefully with parentheses.";
    }
  }

  return Promise.resolve({
    message,
    tip,
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
