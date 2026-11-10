# Neuro-Learn — AI Integration & Improvement Scope

> Written 2026-11-09 against branch `feat/phase-3-classes` (Phases 0–3 done).
> Builds on [`docs/history/future_scope.md`](history/future_scope.md) and
> [`docs/history/research_and_aiml_scope.md`](history/research_and_aiml_scope.md), checks their
> claims against the code, and adds current research. Sources are listed at the end.

---

## 1. TL;DR — what to do, in order

1. **Fix the data first.** The app logs *whether* an answer was right but not *which* option was
   picked, which hints were used, or why the engine chose the next question. Almost every useful AI
   feature needs those. This is cheap (one migration + a few fields) and blocks everything else.
2. **Grade answers on the server.** Today the browser receives `correctAnswer` and reports
   `correct` itself, so scores, BKT mastery and any model trained on them can be faked.
3. **LLM question generation with teacher review** (README's promised AI deliverable). Research
   says LLMs write valid questions but weak distractors, so: verify the maths in code, tag each
   distractor with the misconception it targets, and keep a human approval step.
4. **Pre-generated, guarded hints** per question *and per wrong option*. Same evidence base: LLM hints
   can match human-written ones, but an unguarded chatbot that gives answers away *lowers* exam
   scores. Generating offline keeps cost near zero and lets teachers review them.
5. **Fit BKT parameters from real data** and add an offline evaluation harness before reaching
   for deep models. Deep knowledge tracing beats simpler models by small margins on benchmarks.
6. **No per-child ADHD/"engagement flag".** RT variability doesn't single out ADHD, and India's
   DPDP rules prohibit behavioural monitoring of children. **Decided: option A** (§6).

---

## 2. What "AI" exists in the code today

| Area | Reality (checked in code) |
|---|---|
| Student model | Classic BKT per topic (`AdaptiveEngineService`), fixed parameters for every topic: P(L0)=0.20, P(T)=0.18, P(G)=0.25, P(S)=0.10. Per-topic parameters are *supported* (`BktParameters`) but never fitted. |
| Adaptive policy | Rule-based INCREASE / MAINTAIN / DECREASE with a human-readable `reason` string — returned to the client but **not stored**. |
| Questions | 3 seeded banks (V2 Batman, V6 Superman, V7 Quadratics), hand-written, difficulty 1–3 hand-labelled. No IRT columns. |
| "AI" text in UI | Story briefings and feedback in `Frontend/src/utils/api.js` are **hard-coded templates**, not model output. |
| LLM | None in the live app. All Gemini code was client-side and removed in Phase 0. |
| `legacy-python/` | ~2,000 lines of unused ML: webcam attention model, eye tracker (MediaPipe), LSTM mastery, NLP answer matcher, Gemini agents. Not wired to anything. |

### Data captured vs. what AI features need

| Signal | Stored now? | Needed for |
|---|---|---|
| correct / incorrect | ✅ `question_attempts.correct` (but client-reported) | everything |
| response time | ✅ `response_time_ms` | pacing, IRT speed models, hint timing |
| **option selected** | ❌ | misconception detection, distractor-specific hints, distractor quality, IRT diagnostics |
| hints / X-ray / clue used | ❌ (UI has them, nothing logged) | fair mastery updates (hint = partial credit), hint effectiveness |
| adaptive decision + reason | ❌ | explainability in reports, offline policy evaluation |
| question/prompt version | ❌ | comparing generated vs authored items |
| skill tags finer than `topic` | ❌ | multi-skill knowledge tracing, targeted generation |

### Corrections to the older docs

- `research_and_aiml_scope.md` says *"store in `questions.irt_beta` (schema already has this column)"* — **it doesn't**; there is no IRT column (V1–V8 checked).
- That file also has headline numbers I couldn't verify and that look overstated — e.g. game-based learning "+14% retention, larger for ADHD students" (attributed to Mayer 2019), "immediate feedback is the single most effective ADHD accommodation" (attributed to Cibrian et al. 2020, which is a smartwatch self-regulation study), "+43% transfer" (Kornell & Bjork 2008), and company revenue/acquisition figures. **Don't cite these in the report without checking the original papers.** The BKT citations (Corbett & Anderson 1995; VanLehn 2011's d≈0.76 for ITS) are sound.
- `future_scope.md` still says only one chapter exists and numbers the analytics migration V7; Chapter 2 and V7–V8 are now taken. Next free migration is **V9**.

---

## 3. What the research says (2023–2025)

| Question | Finding | Implication for Neuro-Learn |
|---|---|---|
| Can an AI tutor actually teach? | Harvard physics RCT (Kestin et al., Sci. Reports 2025, ~194 students): a tutor built around the course's own problems and pedagogy produced roughly **double** the learning gains of an active-learning class. Small, one course, elite university. | Works when tightly scoped to *your* curriculum and pedagogy — not a generic chatbot. |
| Can it hurt? | Bastani et al., PNAS 2025 (~1,000 high-schoolers, maths): unrestricted GPT-4 raised practice scores but **lowered exam scores** once removed (≈17% in the preprint); a guarded "tutor" version that withheld answers avoided the harm. | Never let the model hand out answers. Hints only, graduated, with guardrails. |
| Are LLM hints as good as human ones? | Pardos & Bhandari, PLOS ONE 2024 (N=274, algebra and more): ChatGPT hints gave learning gains **not significantly different** from human-tutor hints; 32% failed quality checks overall, cut to ~0% for algebra with self-consistency. | Generate hints offline, check them (self-consistency + teacher review), then serve from the DB. |
| AI helping humans instead of students? | Tutor CoPilot RCT (Stanford, 900 tutors / 1,800 students): +4 pp mastery overall, up to +9 pp for weaker tutors. | A teacher-facing "how to help this student" assistant is a valid, lower-risk angle. |
| LLM-generated MCQs? | Feng et al. NAACL 2024 & follow-ups: LLM distractors are mathematically valid but **don't match real student misconceptions**; few-shot examples help; teachers found ~70% of generated questions usable after edits. | Teacher-in-the-loop is required. Tag distractors with misconceptions; learn from which wrong options students actually pick. |
| Deep knowledge tracing vs simpler models? | pyKT benchmarks: modern DLKT models are within ~0.5–1% AUC of each other; reported baselines vary widely between papers. | Fitted BKT + good data beats an under-trained LSTM. DKT is a research comparison, not a product need. |
| IRT calibration data needs? | Common guideline ≈ **500 respondents** for a stable 2PL; hierarchical Bayesian methods can go lower. | Don't run IRT on demo data. Plan it for after a real pilot; until then keep hand-labelled difficulty. |
| Spaced review? | FSRS beats SM-2 in ~97–99% of collections on the open srs-benchmark (log-loss). | If you add "review due" missions, use FSRS (open source) rather than inventing a schedule. |
| RT variability as an ADHD signal? | Meta-analyses: large group differences vs typical peers, but only **g≈0.25 vs other clinical groups** — it's trans-diagnostic. | Can't flag individual children as ADHD-like from it. See §6. |
| Children's data law (India) | DPDP Act §9(3) + 2025 Rules: verifiable parental consent for under-18s; **tracking, behavioural monitoring and targeted ads for children are prohibited** (consent doesn't lift it); narrow exemption for educational institutions' educational/safety purposes. Most obligations phase in ~18 months after Nov 2025. | Adaptive learning for educational purposes is defensible; a behavioural "risk flag" on individual children is not obviously so. Get proper advice before launch. |

---

## 4. Opportunities, by tier

### Tier 0 — Data foundation (do first, ~1–2 days)

> **Status:** done in Phase 4a (V9) — selected option, hints, adaptive decision and difficulty are logged; grading is server-side; 50:50 is server-decided and down-weights BKT evidence. Still open: `question_distractors` misconception tags, skill tags, anonymised export.

| Item | Why |
|---|---|
| **V9 migration:** `question_attempts.selected_option`, `hints_used`, `adaptive_action`, `adaptive_reason`, `difficulty_served` | Unlocks misconception analysis, distractor hints, policy evaluation, explainable reports. |
| **Server-side grading:** client sends `selectedOption`; server computes `correct`; stop sending `correctAnswer` until after submission | Integrity of every downstream number (BKT, XP, teacher analytics, any trained model). Also closes the cheating gap noted in Phase 1. |
| `question_distractors` (question_id, option, misconception_tag, explanation) | Lets hints and teacher analytics talk about *why* a student is wrong ("added instead of subtracted when eliminating"). |
| Optional `skills` + `question_skills` tags | Needed later for multi-skill knowledge tracing and targeted generation. |
| Export script → anonymised CSV of attempts | Offline research (BKT fitting, IRT, DKT) without touching prod data or names. |

### Tier 1 — LLM features with low risk and clear value

All generation happens **server-side, offline or on teacher request**, never in the student's answer loop (consistent with README §8 and §19).

**1a. Teacher-reviewed question generation (README's AI deliverable — Phase 4)**

> **Status:** built in Phase 4b with Google Gemini (structural validation, duplicate check, independent self-check solve, teacher review). Not yet: a symbolic (SymPy) maths check — the self-check + teacher review cover correctness for now.
- Teacher picks chapter/section, difficulty, theme, count → backend calls the LLM with few-shot examples from the existing bank and a strict JSON schema.
- Validate before showing: 4 unique options, exactly one correct; **re-solve the maths in code** (e.g. SymPy in a small Python helper, or a Java expression evaluator for linear/quadratic items) and reject mismatches; run self-consistency (ask twice, keep agreeing items); dedupe against the bank.
- Each distractor carries a misconception tag + one-line explanation.
- Saved as `PENDING`; teacher edits/approves; only `APPROVED` questions are served.
- Track provenance (`source = LLM`, model, prompt version) so Phase 5 evaluation can compare generated vs authored items.

**1b. Guarded hint ladder (pre-generated)**
- For each question: 3 graduated hints (nudge → strategy → worked step), plus one targeted hint **per wrong option** using its misconception tag.
- Generated offline in batch, quality-checked, teacher-reviewable, stored in a `question_hints` table. Serving is a DB read: no latency, no per-student cost, no child data sent to a model.
- Log hint use; treat a hinted correct answer as weaker evidence in BKT (or skip the "learned" transition) so mastery isn't inflated.
- Replaces the current templated "Krypton Clue" / X-ray behaviour with real, question-specific help.

**1c. Narrative summaries for parents and teachers**
- The backend computes the facts (mastery per section, trends, common misconceptions); the LLM only turns them into a short paragraph. Numbers are inserted from data, never generated.
- Send no names or emails — use "the student" and fill the name in afterwards.
- Cache per student per day. Fallback to the existing rule-based `parentingTips` if the call fails.

**1d. Teacher "next step" assistant (Tutor-CoPilot-style)**
- On the drill-down modal: "Jason keeps choosing the option that adds the equations instead of subtracting — try asking him to …". Built from Tier 0 misconception data + a prompt. Low risk because a teacher reads it, not a child.

### Tier 2 — Better models (classic, explainable ML)

> **Status (Phase 5):** simulator, evaluation harness and BKT fitter are built — see [evaluation.md](evaluation.md).
> Key result: the current engine's "Mastered" claim is almost never true because BKT ignores item difficulty;
> a difficulty-aware (Elo/IRT-style) estimate fixes it in simulation. Real-data fitting awaits a pilot.

| Item | Data needed | Notes |
|---|---|---|
| **Fit BKT per topic** (EM via pyBKT, or a small Java fit) | A few hundred attempts per topic | Replaces the single default parameter set. Keeps the explainable model the report is built on. |
| **Evaluation harness** | Exported attempts + the simulator | Replay logs, predict next-answer correctness, report AUC/RMSE for: default BKT, fitted BKT, (later) DKT/simpleKT via pyKT. Feeds Phase 5's fixed-vs-adaptive study. |
| **Student simulator** | none | Synthetic learners with known skill curves — tests the engine and the evaluation pipeline. Results on simulated data must be labelled as such. |
| **IRT 2PL difficulty** | ~500 respondents (or hierarchical Bayesian with fewer) | After a real pilot. Gives empirical difficulty and flags bad items (low discrimination, distractor nobody picks). |
| **FSRS review scheduling** | Attempt timestamps | "Review due" missions on the roadmap for topics that are decaying. Open-source implementations exist. |
| **Item quality analytics** | `selected_option` | Per-question: p-value, discrimination, distractor pick rates → auto-flag questions for teacher review. No ML needed. |

### Tier 3 — Research / stretch

- **Deep knowledge tracing** (DKT / simpleKT via pyKT) as an optional Python service, compared side by side with BKT in the evaluation harness. Expect small gains; worth it only as a research result.
- **Learned difficulty policy** (contextual bandit / RL) — only with substantial real data and offline policy evaluation from logged `adaptive_action`s.
- **Curriculum-grounded "Why am I learning this?" chat** (RAG over chapter content with pgvector). Must follow the Bastani guardrails: explains concepts and applications, never solves the current question.
- **Teacher natural-language questions over class data** — safer as a fixed set of LLM-chosen *predefined* queries than free NL-to-SQL.
- **Multilingual content** (e.g. Hindi) generated + reviewed like 1a.
- **Free-response answers** graded by symbolic check first, LLM rubric second.

---

## 5. Cross-cutting engineering for any LLM feature

- **One backend gateway** (`ai/LlmClient` interface) so the provider (Gemini, Claude, OpenAI, a local model) is a config choice. API keys only on the server.
- **Structured output + validation** on every call; reject and retry rather than trusting free text.
- **Prompt versioning and a small eval set** (20–50 known questions/hints with expected properties) run in CI with a mocked client, and manually against the real model before changing prompts.
- **Cost & safety controls:** per-teacher rate limits, request/token logging, timeouts, a global kill switch (feature flag) and graceful fallback to existing content.
- **Child-data minimisation:** prompts contain curriculum content and anonymised aggregates only — no names, emails or free text typed by children. Document this in the privacy notice.
- **Human in the loop** for anything a child will see that was generated (questions, hints) until there is evidence it's reliable.

---

## 6. The ADHD / engagement-analytics question

The earlier plan (`next_steps_plan.md`, Phases A–D) proposes an individual "Engagement Pattern Flag" (🟢/🟡/🔴) built from response-time variability, oscillating right/wrong patterns and session abandonment.

**Concerns found in this review:**
1. **Science:** RT variability separates ADHD groups from typical peers, but barely from other clinical groups (g≈0.25). Oscillation and abandonment are even less specific (tiredness, a hard topic, a noisy room). An individual flag will mostly be noise.
2. **Law (India):** DPDP §9(3) prohibits behavioural monitoring of children regardless of consent; the education exemption covers educational activities, and a quasi-clinical attention flag is arguably outside that. This is a reading of commentary, not legal advice — but it's a real risk.
3. **Ethics:** A teacher seeing 🔴 next to a child's name can stick even with a disclaimer.

> **Decision (2026-11-09): Option A.** No individual flag in the product. Engagement data is used
> for the student's own pacing feedback and class-level aggregates only; the ADHD story rests on the
> app's design. `next_steps_plan.md` Phase C (Engagement Pattern Flag) is cancelled.

**Options that were considered:**
- **A (chosen):** Drop the individual flag. Keep the useful parts as *learning* signals: session completion and pacing shown to the student themself ("you're usually faster after a short break"), class-level aggregates for teachers, and the ADHD-friendly *design* (short sessions, immediate feedback, calm mode) as the accommodation story.
- **B:** Keep it as an offline research analysis on simulated or explicitly consented data, reported in the project write-up, not shown in the product.
- **C:** Build it as planned — then it needs the wording, consent flow and legal review sorted first.

---

## 7. How this maps onto FUTURE_WORKFLOW phases

| Phase | Proposed content |
|---|---|
| **4 (next)** | Tier 0 data foundation + server-side grading, then 1a teacher-reviewed generation. |
| 4b | 1b pre-generated hint ladder (reuses the 1a pipeline). |
| **5** | Simulator + evaluation harness + fixed-vs-adaptive study + BKT fitting. |
| 6 | Deployment (unchanged); LLM keys as env vars. |
| 7 | Learning-pace insights (option A): session completion, student-facing pacing tips, class-level aggregates. Plus 1c/1d summaries. |
| 8–9 | FSRS review, item analytics, IRT after a pilot, DKT/RAG research. |

## 8. Decisions needed

1. ~~ADHD/engagement direction~~ — **decided: option A** (§6).
2. LLM provider and budget (free tier vs paid; any data-residency requirement)?
3. Is a real pilot (a class using the app for some weeks) possible? IRT, BKT fitting and any credible ML claim depend on it.
4. Who reviews generated content — the team, or teachers in the app?

---

## Sources

- Kestin et al., *AI tutoring outperforms in-class active learning* — [preprint PDF](https://biblioteca.unisced.edu.mz/bitstream/123456789/3656/1/AI_Tutoring_Outperforms_Active_Learning.pdf); [Harvard Gazette](https://news.harvard.edu/gazette/story/2024/09/professor-tailored-ai-tutor-to-physics-course-engagement-doubled/)
- Bastani et al., *Generative AI without guardrails can harm learning*, PNAS 122(26) 2025 — [SSRN](https://papers.ssrn.com/abstract=4895486); [Wharton summary](https://knowledge.wharton.upenn.edu/article/without-guardrails-generative-ai-can-harm-education)
- Pardos & Bhandari, *ChatGPT-generated help produces learning gains equivalent to human tutor-authored help* (PLOS ONE 2024) — [Stanford SCALE](https://scale.stanford.edu/publications/chatgpt-generated-help-produces-learning-gains-equivalent-human-tutor-authored-help); [2023 preprint](https://arxiv.org/abs/2302.06871)
- Tutor CoPilot RCT — [Stanford NSSA](https://nssa.stanford.edu/news/study-ai-assisted-tutoring-boosts-students-math-skills); [The 74](https://www.the74million.org/article/study-ai-assisted-tutoring-boosts-students-math-skills/)
- Feng et al., *Exploring Automated Distractor Generation for Math MCQs via LLMs* (NAACL Findings 2024) — [arXiv](https://arxiv.org/html/2404.02124v3); [BEA 2024 follow-up](https://aclanthology.org/2024.bea-1.19.pdf); [D-GEN 2025](https://arxiv.org/pdf/2504.13439v2.pdf)
- pyKT benchmark — [arXiv 2206.11460](https://arxiv.org/pdf/2206.11460); simpleKT — [arXiv 2302.06881](https://arxiv.org/pdf/2302.06881)
- 2PL sample size — [Frankfurt (APM) PDF](https://publikationen.ub.uni-frankfurt.de/files/54836/0146621619893786.pdf); [ETS RM-23-03](https://ets.org/Media/Research/pdf/RM-23-03.pdf)
- FSRS vs SM-2 — [Anki docs](https://docs.ankiweb.net/faqs/what-spaced-repetition-algorithm); open-spaced-repetition srs-benchmark (GitHub)
- RT variability in ADHD — [Kofler et al. 2013 meta-analysis](https://sciences.ucf.edu/psychology/childrenslearningclinic/wp-content/uploads/sites/24/2013/05/Kofler-et-al.-2013-RT-Variability-Meta-analysis.pdf); [Karalunas et al. review (PMC4267725)](https://pmc.ncbi.nlm.nih.gov/articles/PMC4267725)
- India DPDP children's data — [LiveLaw](https://www.livelaw.in/articles/dpdp-child-data-rules-demand-edtech-platforms-555317); [ORF](https://www.orfonline.org/expert-speak/dpdp-rules-and-the-future-of-child-data-safety); [Biometric Update (Rules notified Nov 2025)](https://www.biometricupdate.com/202511/india-notifies-its-sweeping-digital-personal-data-protection-rules)
