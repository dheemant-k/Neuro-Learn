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
export async function fetchProgress(chapterId = "linear_eq_2var") {
  const query = chapterId ? `?chapterId=${encodeURIComponent(chapterId)}` : "";
  return get(`/progress${query}`);
}

export async function claimBossVictory(bossTopic = "final_boss", chapterId = "linear_eq_2var") {
  return post("/progress/boss-victory", { bossTopic, chapterId });
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
  const topicLower = (sectionTopic || "").toLowerCase();

  let title = `${sectionTitle} Briefing`;
  let story = "";
  let key_points = [];
  let examples = [];

  const isQuad = topicLower.includes("quad") || titleLower.includes("quad") || titleLower.includes("discriminant") || titleLower.includes("parabola") || titleLower.includes("factoring");

  if (isQuad) {
    if (isBatman) {
      if (titleLower.includes("intro")) {
        title = "Gotham Ballistics Quadratic Scan";
        story = `🦇 Batcomputer Mission Briefing: Quadratic equations follow standard form ax^2 + bx + c = 0 (a != 0). Joker is launching parabolic toxin capsules across Gotham. To find their landing roots, we master second-degree polynomials.`;
        key_points = [
          "Standard quadratic equation: ax^2 + bx + c = 0, where a != 0.",
          "Degree is 2; the equation has at most two real roots (solutions).",
          "Root x satisfies the equation when substituted.",
        ];
        examples = [
          { problem: "Write 3x = 2x^2 - 5 in standard form with a > 0", solution: "Subtract 3x: 2x^2 - 3x - 5 = 0. Coefficients: a = 2, b = -3, c = -5." },
        ];
      } else if (titleLower.includes("factor")) {
        title = "Arkham Security Decryption via Factoring";
        story = `🦇 Batcave Forensic Protocol: When ax^2 + bx + c = 0 can be factored into (x - p)(x - q) = 0, the zero-product property guarantees that either x - p = 0 or x - q = 0, revealing both roots instantly.`;
        key_points = [
          "Step 1: Write equation with 0 on one side.",
          "Step 2: Factor into two binomials (x - r1)(x - r2) = 0.",
          "Step 3: Set each factor to zero to solve for roots.",
        ];
        examples = [
          { problem: "Solve x^2 - 7x + 12 = 0 by factoring", solution: "Find factors of 12 summing to -7: -3 and -4. (x - 3)(x - 4) = 0 => x = 3 or x = 4." },
        ];
      } else if (titleLower.includes("formula")) {
        title = "Batmobile Universal Quadratic Resolver";
        story = `🦇 Tactical Math Briefing: When factoring fails, the universal Quadratic Formula x = (-b ± √(b^2 - 4ac)) / (2a) solves any quadratic equation with exact precision.`;
        key_points = [
          "Quadratic formula: x = (-b ± √(b^2 - 4ac)) / (2a).",
          "Carefully preserve negative signs on b and c.",
          "Simplify the radical before dividing by 2a.",
        ];
        examples = [
          { problem: "Solve x^2 - 4x + 1 = 0 using the quadratic formula", solution: "x = (4 ± √(16 - 4)) / 2 = (4 ± 2√3) / 2 = 2 ± √3." },
        ];
      } else if (titleLower.includes("discriminant")) {
        title = "Firewall Discriminant Delta Analysis";
        story = `🦇 Batcomputer Delta Matrix: The discriminant Δ = b^2 - 4ac predicts the number of solutions before solving: Δ > 0 gives two real roots, Δ = 0 gives one repeated root, and Δ < 0 gives complex roots.`;
        key_points = [
          "Δ > 0: Two distinct real solutions.",
          "Δ = 0: Exactly one real (repeated) solution.",
          "Δ < 0: No real solutions (complex conjugate roots).",
        ];
        examples = [
          { problem: "Determine nature of roots for 2x^2 - 4x + 2 = 0", solution: "Δ = (-4)^2 - 4(2)(2) = 16 - 16 = 0. Exactly one repeated real root." },
        ];
      } else if (titleLower.includes("graph")) {
        title = "Batarang Trajectory Parabola Apex";
        story = `🦇 Flight Dynamics Analysis: Every quadratic graph y = ax^2 + bx + c is a symmetrical parabola. The apex or lowest point is the vertex (h, k), with axis of symmetry x = -b / (2a).`;
        key_points = [
          "Axis of symmetry equation: x = -b / (2a).",
          "If a > 0, parabola opens upward (minimum vertex); if a < 0, opens downward (maximum apex).",
          "y-intercept occurs where x = 0 (coordinate (0, c)).",
        ];
        examples = [
          { problem: "Find vertex axis of symmetry for y = -x^2 + 6x - 5", solution: "x = -6 / (2 * -1) = 3. Apex occurs at x = 3." },
        ];
      } else {
        title = "Gotham Kinematic & Projectile Briefing";
        story = `🦇 Ballistics Dispatch: Free-falling objects and rocket paths follow quadratic height equations h(t) = -5t^2 + v0*t + h0. Setting h = 0 reveals impact time.`;
        key_points = [
          "Identify independent variable t (time) and dependent variable h (height).",
          "Set height to zero to determine ground impact.",
          "Vertex provides the maximum altitude reached.",
        ];
        examples = [
          { problem: "A grappling hook reaches h(t) = -5t^2 + 20t. Find flight time until return", solution: "-5t(t - 4) = 0 => t = 0 (launch) or t = 4 (impact). Flight time = 4s." },
        ];
      }
    } else if (isSupergirl) {
      if (titleLower.includes("intro")) {
        title = "DEO Orbital Quadratic Scanner";
        story = `🦸 DEO Tactical Briefing: Supergirl here! Orbital re-entry vectors follow second-degree polynomials ax^2 + bx + c = 0. We need to isolate quadratic coefficients to ensure stable atmospheric entry into National City!`;
        key_points = [
          "Standard quadratic equation: ax^2 + bx + c = 0 (a != 0).",
          "Degree is 2; the curve is a symmetrical parabola.",
        ];
        examples = [
          { problem: "Identify quadratic form for orbital curve 5x^2 - 20 = 0", solution: "Degree 2 with a = 5, b = 0, c = -20. Pure quadratic equation!" },
        ];
      } else if (titleLower.includes("factor")) {
        title = "DEO Drone Intercept by Factoring";
        story = `🦸 National City Flight Control: Brainiac's swarm coordinates factor into simple linear binomials. By applying the zero-product rule, we intercept the drones at both root points!`;
        key_points = [
          "If (x - a)(x - b) = 0, then x = a or x = b.",
          "Always test roots back in original equation.",
        ];
        examples = [
          { problem: "Factor x^2 - 11x + 30 = 0", solution: "(x - 5)(x - 6) = 0 => Roots at x = 5 and x = 6." },
        ];
      } else if (titleLower.includes("formula")) {
        title = "DEO Tachyon Quadratic Formula";
        story = `🦸 Cosmic Speed Calibration: When Brainiac's shields produce irrational roots, Supergirl uses the Quadratic Formula x = (-b ± √(b^2 - 4ac)) / (2a) to compute exact coordinates.`;
        key_points = [
          "Universal formula applies to every quadratic equation.",
          "Root nature is determined by discriminant under the square root.",
        ];
        examples = [
          { problem: "Solve x^2 - 6x + 7 = 0", solution: "x = (6 ± √(36 - 28)) / 2 = (6 ± √8) / 2 = 3 ± √2." },
        ];
      } else if (titleLower.includes("discriminant")) {
        title = "S.T.A.R. Labs Tachyon Delta Field";
        story = `🦸 Harmonic Resonance Check: Calculating Δ = b^2 - 4ac reveals whether Brainiac's force field has two physical rupture points (Δ > 0), a single resonance node (Δ = 0), or is impenetrable (Δ < 0).`;
        key_points = [
          "Δ > 0: Two distinct real roots.",
          "Δ = 0: One real repeated root.",
          "Δ < 0: No real roots.",
        ];
        examples = [
          { problem: "Evaluate Δ for x^2 - 10x + 25 = 0", solution: "Δ = 100 - 4(1)(25) = 0. One repeated root at x = 5." },
        ];
      } else if (titleLower.includes("graph")) {
        title = "Supersonic Parabolic Trajectory";
        story = `🦸 High-Altitude Vector Mapping: Supergirl's parabolic jump reaches its vertex at x = -b / (2a). Plotting the parabola ensures zero collateral impact on National City!`;
        key_points = [
          "Vertex coordinates (h, k) give trajectory apex.",
          "Axis of symmetry passes vertically through vertex.",
        ];
        examples = [
          { problem: "Find apex height of y = -(x - 5)^2 + 25", solution: "Vertex is (5, 25). Max altitude is 25 meters." },
        ];
      } else {
        title = "DEO Projectile Defense Dispatch";
        story = `🦸 National City Emergency: Alien artillery shells follow vertical projectile trajectories h = -5t^2 + v0*t. Solve the quadratic equation to calculate impact time!`;
        key_points = [
          "Set height h = 0 to find total airtime.",
          "Vertex time t = -b / (2a) gives peak altitude time.",
        ];
        examples = [
          { problem: "Probe height h = -5t^2 + 30t. Find time at peak altitude", solution: "t = -30 / (2 * -5) = 3 seconds." },
        ];
      }
    } else {
      // Superman (Default)
      if (titleLower.includes("intro")) {
        title = "Metropolis Gravitational Quadratic Grid";
        story = `🦸 Metropolis Solar Grid Briefing: Superman here! LexCorp and General Zod are deploying gravitational disruptors that warp space according to quadratic equations ax^2 + bx + c = 0 (a != 0). Each root of the equation represents an anchor node. Let's master quadratics to balance the city's gravitational grid!`;
        key_points = [
          "Standard quadratic equation: ax^2 + bx + c = 0, where a != 0.",
          "Degree is 2; the equation has up to two real roots.",
          "Substituting a valid root balances the equation to zero.",
        ];
        examples = [
          { problem: "Verify if x = 3 is a root to x^2 - 5x + 6 = 0", solution: "3^2 - 5(3) + 6 = 9 - 15 + 6 = 0. Root verified!" },
        ];
      } else if (titleLower.includes("factor")) {
        title = "Fortress Crystal Factoring Protocol";
        story = `🦸 Fortress of Solitude Crystal Archive: When an energy equation factors into (x - p)(x - q) = 0, the zero-product rule allows Superman to shatter the security barrier by solving each factor individually!`;
        key_points = [
          "Find two numbers that multiply to c and add to b.",
          "Set each binomial factor equal to zero.",
          "Works for difference of squares: a^2 - b^2 = (a - b)(a + b).",
        ];
        examples = [
          { problem: "Solve x^2 - 9 = 0", solution: "(x - 3)(x + 3) = 0 => x = 3 or x = -3." },
        ];
      } else if (titleLower.includes("formula")) {
        title = "Universal Kryptonian Quadratic Formula";
        story = `🦸 Daily Planet Comm-Link: When LexCorp creates quantum polynomials that cannot be factored by eye, Superman relies on the universal formula: x = (-b ± √(b^2 - 4ac)) / (2a). It unlocks the roots of every quadratic in existence!`;
        key_points = [
          "Quadratic formula: x = (-b ± √(b^2 - 4ac)) / (2a).",
          "Identify a, b, c from standard form ax^2 + bx + c = 0.",
          "Simplify the radical term before final division.",
        ];
        examples = [
          { problem: "Solve x^2 - 2x - 1 = 0", solution: "x = (2 ± √(4 + 4)) / 2 = (2 ± 2√2) / 2 = 1 ± √2." },
        ];
      } else if (titleLower.includes("discriminant")) {
        title = "LexCorp Core Discriminant Analysis";
        story = `🦸 Cadmus Defense Matrix: The discriminant Δ = b^2 - 4ac reveals the exact nature of LexCorp's energy pulse before we even solve it: two distinct real roots (Δ > 0), a single resonance node (Δ = 0), or non-real complex waves (Δ < 0)!`;
        key_points = [
          "Δ > 0: Two distinct real roots.",
          "Δ = 0: One real repeated root (perfect square).",
          "Δ < 0: No real roots (complex roots).",
        ];
        examples = [
          { problem: "Calculate Δ for 2x^2 - 4x + 2 = 0", solution: "Δ = 16 - 16 = 0. Exactly one repeated root at x = 1." },
        ];
      } else if (titleLower.includes("graph")) {
        title = "Solar Flight Parabolic Trajectory";
        story = `🦸 Daily Planet Skyway Briefing: Parabolic flight paths form symmetric curves with vertex (h, k). Finding the axis of symmetry x = -b / (2a) lets Superman calculate the exact apex of any rocket or trajectory!`;
        key_points = [
          "Axis of symmetry: x = -b / (2a).",
          "Vertex is the maximum (if a < 0) or minimum (if a > 0).",
          "y-intercept is (0, c).",
        ];
        examples = [
          { problem: "Find axis of symmetry for y = 2x^2 - 8x + 5", solution: "x = -(-8) / (2 * 2) = 8 / 4 = 2." },
        ];
      } else {
        title = "Metropolis Kinematics & Ballistics";
        story = `🦸 Daily Planet Dispatch: Real-world rocket launches and projectile arcs in Metropolis follow quadratic height functions h(t) = -5t^2 + v0*t. Solve for time t to protect citizens from falling debris!`;
        key_points = [
          "Set height h = 0 to calculate time of ground impact.",
          "Vertex time gives maximum trajectory height.",
        ];
        examples = [
          { problem: "Missile height h = -5t^2 + 20t. Find ground impact time", solution: "-5t(t - 4) = 0 => impact at t = 4 seconds." },
        ];
      }
    }
  } else {
    // Chapter 1: Linear Equations
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
      tip = "Your calculations are sharp. Maintain this standard when tackling the Climax Mainframe!";
    } else if (pct >= 50) {
      message = `🦇 "Good operational effort. We held the perimeter, but review the algebraic formulas before our next encounter with the syndicate." — Batman`;
      tip = "Check your sign distributions and verify both sides of the equation after substituting.";
    } else {
      message = `🦇 "Regroup and recalibrate. Every detective encounters resistance. Review the fundamentals and take the challenge once more." — Batman`;
      tip = "Isolate one term at a time. Work step-by-step to avoid arithmetic traps.";
    }
  } else if (isSupergirl) {
    if (pct >= 80) {
      message = `🦸 "Super sonic accuracy! You decrypted the coordinate vectors in ${sectionTitle} flawlessly. National City is safe!" — Supergirl`;
      tip = "Your calculations are flying at cosmic speeds. Keep this momentum going!";
    } else if (pct >= 50) {
      message = `🦸 "Great energy! We deflected the primary threat, but take a moment to review the algebraic steps for full mastery!" — Supergirl`;
      tip = "Remember to balance both sides when doing operations on polynomial terms.";
    } else {
      message = `🦸 "Shake it off! Even Kryptonians have to train. Focus on the core formulas, take a deep breath, and fly right back in!" — Supergirl`;
      tip = "Break equations down into smaller pieces before solving.";
    }
  } else {
    // Superman (Default)
    if (pct >= 80) {
      message = `🦸 "Outstanding calculations! You breached the enemy matrix in ${sectionTitle} and kept Metropolis safe under the sun." — Superman`;
      tip = "Your mathematical focus is solar-charged! You're ready to breach the final boss core.";
    } else if (pct >= 50) {
      message = `🦸 "Strong effort! The solar shield held the perimeter, but review the formulas before our next climax encounter." — Superman`;
      tip = "Double-check your coefficients and discriminant values before computing roots.";
    } else {
      message = `🦸 "Regroup and recharge! Every hero faces resistance. Recalibrate your formulas, absorb the sun's rays, and try again!" — Superman`;
      tip = "Take your time with algebraic simplification—plug values into the formula carefully with parentheses.";
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
