import { useEffect, useRef, useState } from "react";
import {
  startLearningSession,
  fetchNextAdaptiveQuestion,
  recordQuestionAttempt,
  completeLearningSession,
} from "../utils/api";

export default function TestPage({ section, character, onFinish, onBack }) {
  const TOTAL = 5; // 5 adaptive questions per mission quest
  const [qIndex, setQIndex] = useState(0);
  const [currentQ, setCurrentQ] = useState(null);
  const [shuffledOptions, setShuffledOptions] = useState([]);
  const [adaptiveInfo, setAdaptiveInfo] = useState(null);
  const [bktUpdate, setBktUpdate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null); // index in shuffledOptions
  const [locked, setLocked] = useState(false);
  const [showExplain, setShowExplain] = useState(false);

  // Session stats
  const [sessionId, setSessionId] = useState(null);
  const [totalCorrect, setTotalCorrect] = useState(0);
  const [streak, setStreak] = useState(0);
  const [xpGained, setXpGained] = useState(0);
  const [currentPKnow, setCurrentPKnow] = useState(0.20);

  // Typewriter
  const [typed, setTyped] = useState("");
  const typingRef = useRef(null);
  const startRef = useRef(Date.now());

  const startTypewriter = (text) => {
    if (typingRef.current) clearInterval(typingRef.current);
    setTyped("");
    let i = 0;
    typingRef.current = setInterval(() => {
      i += 2;
      setTyped(text.slice(0, i));
      if (i >= text.length) {
        clearInterval(typingRef.current);
        typingRef.current = null;
      }
    }, 12);
  };

  const displayQuestion = (q, adaptiveData) => {
    setCurrentQ(q);
    setSelected(null);
    setLocked(false);
    setShowExplain(false);
    setBktUpdate(null);

    // Shuffle options dynamically so option A is not always in the first button slot
    const raw = [
      { key: "A", text: q.optionA },
      { key: "B", text: q.optionB },
      { key: "C", text: q.optionC },
      { key: "D", text: q.optionD },
    ];
    // Fisher-Yates shuffle
    const shuffled = [...raw];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    setShuffledOptions(shuffled);

    if (adaptiveData?.pknow !== undefined) {
      setCurrentPKnow(adaptiveData.pknow);
    } else if (adaptiveData?.pKnow !== undefined) {
      setCurrentPKnow(adaptiveData.pKnow);
    }

    startRef.current = Date.now();
    startTypewriter(q.questionText || "");
  };

  // 1. Initialize session on mount
  useEffect(() => {
    let mounted = true;

    async function initSession() {
      setLoading(true);
      try {
        const sessionRes = await startLearningSession({ topic: section.id }).catch(() => null);
        const sId = sessionRes ? sessionRes.id : 1;
        if (mounted) setSessionId(sId);

        // Fetch first adaptive question
        const adaptiveRes = await fetchNextAdaptiveQuestion({ sessionId: sId, topic: section.id });
        if (mounted && adaptiveRes?.question) {
          setAdaptiveInfo(adaptiveRes);
          displayQuestion(adaptiveRes.question, adaptiveRes);
        } else if (mounted) {
          // Fallback
          const fallback = {
            id: 1,
            topic: section.id,
            difficulty: 1,
            questionText: "🦇 Batcomputer Diagnostic: Solve for x in 3x + 4 = 19.",
            optionA: "x = 5",
            optionB: "x = 6",
            optionC: "x = 4",
            optionD: "x = 7",
            correctAnswer: "A",
            explanation: "3x = 15 => x = 5.",
          };
          displayQuestion(fallback, null);
        }
      } catch (err) {
        console.error("Failed to init session:", err);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    initSession();

    return () => {
      mounted = false;
      if (typingRef.current) clearInterval(typingRef.current);
    };
  }, [section.id]);

  const submit = async () => {
    if (!currentQ || selected === null || locked || shuffledOptions.length === 0) return;

    const responseTimeMs = Date.now() - startRef.current;
    const chosenKey = shuffledOptions[selected]?.key;
    const correctKey = (currentQ.correctAnswer || "A").trim().toUpperCase();
    const isCorrect = chosenKey === correctKey;

    setLocked(true);
    setShowExplain(true);

    if (isCorrect) {
      setTotalCorrect((c) => c + 1);
      setStreak((s) => s + 1);
      setXpGained((x) => x + 15);
    } else {
      setStreak(0);
      setXpGained((x) => x + 5);
    }

    // Persist attempt to backend and capture Bayesian Knowledge Tracing update
    if (sessionId && currentQ.id) {
      try {
        const attemptRes = await recordQuestionAttempt({
          sessionId,
          questionId: currentQ.id,
          correct: isCorrect,
          responseTimeMs,
          attemptNumber: 1,
        });
        if (attemptRes) {
          setBktUpdate(attemptRes);
          if (attemptRes.pknow !== undefined) setCurrentPKnow(attemptRes.pknow);
          else if (attemptRes.pKnow !== undefined) setCurrentPKnow(attemptRes.pKnow);
        }
      } catch (err) {
        console.warn("Failed recording attempt:", err);
      }
    }
  };

  const next = async () => {
    const nextIdx = qIndex + 1;
    if (nextIdx >= TOTAL) {
      if (sessionId) {
        await completeLearningSession({ sessionId }).catch(() => {});
      }
      onFinish({
        totalCorrect: totalCorrect,
        totalQuestions: TOTAL,
      });
      return;
    }

    setLoading(true);
    setQIndex(nextIdx);

    try {
      // Pull next dynamically adapted question from Bayesian Adaptive Engine!
      const adaptiveRes = await fetchNextAdaptiveQuestion({ sessionId: sessionId || 1, topic: section.id });
      if (adaptiveRes?.question) {
        setAdaptiveInfo(adaptiveRes);
        displayQuestion(adaptiveRes.question, adaptiveRes);
      }
    } catch (err) {
      console.warn("Failed loading next question:", err);
    } finally {
      setLoading(false);
    }
  };

  const optionStyle = (idx) => {
    const chosenKey = shuffledOptions[idx]?.key;
    const correctKey = (currentQ?.correctAnswer || "A").trim().toUpperCase();

    if (!locked) {
      return idx === selected
        ? "border-[rgba(56,189,248,0.75)] bg-[rgba(56,189,248,0.15)] ring-1 ring-[rgb(56,189,248)]"
        : "border-[rgba(48,68,105,0.9)] bg-[rgba(10,20,44,0.65)] hover:bg-[rgba(13,26,58,0.85)]";
    }
    if (chosenKey === correctKey) {
      return "border-[rgba(52,211,153,0.85)] bg-[rgba(52,211,153,0.15)] ring-1 ring-[rgb(52,211,153)]";
    }
    if (idx === selected && chosenKey !== correctKey) {
      return "border-[rgba(251,113,133,0.85)] bg-[rgba(251,113,133,0.15)] ring-1 ring-[rgb(251,113,133)]";
    }
    return "border-[rgba(48,68,105,0.7)] bg-[rgba(10,20,44,0.50)] opacity-70";
  };

  const pKnowPercent = Math.round(currentPKnow * 100);
  const correctKey = (currentQ?.correctAnswer || "A").trim().toUpperCase();
  const selectedKey = selected !== null ? shuffledOptions[selected]?.key : null;

  return (
    <div className="min-h-screen text-slate-100">
      {/* Top bar */}
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-white/[0.06] bg-[rgba(15,23,42,0.85)]">
        <div className="max-w-4xl mx-auto px-6 h-16 flex items-center justify-between">
          <button
            onClick={onBack}
            className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] hover:bg-[rgba(13,26,58,0.85)] transition text-sm"
          >
            ← Roadmap
          </button>
          <div className="flex items-center gap-4">
            <div className="text-xs px-3 py-1 rounded-full border border-yellow-500/40 bg-yellow-500/10 text-yellow-300 font-semibold">
              +{xpGained} XP
            </div>
            <img src={character?.image} alt={character?.name} className="w-8 h-8 object-contain rounded-lg" />
            <span className="text-sm font-semibold">
              {Math.min(qIndex + 1, TOTAL)} / {TOTAL}
            </span>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Progress Bar */}
        <div className="h-2 w-full rounded-full bg-[rgba(255,255,255,0.08)] overflow-hidden">
          <div
            className="h-2 bg-gradient-to-r from-[rgb(56,189,248)] to-[rgb(94,234,212)] transition-all duration-300"
            style={{ width: `${((qIndex + (locked ? 1 : 0)) / TOTAL) * 100}%` }}
          />
        </div>

        {/* Bayesian Knowledge Tracing (BKT) HUD */}
        <div className="mt-4 p-4 rounded-2xl border border-[rgba(56,189,248,0.4)] bg-[rgba(15,23,42,0.85)] shadow-lg backdrop-blur-md">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div className="flex items-center gap-2">
              <span className="text-sm">🧠</span>
              <span className="text-xs font-bold tracking-wider text-[rgb(94,234,212)] uppercase pixel">
                Bayesian Knowledge Model
              </span>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-300">
                Latent Skill Mastery: <span className="font-bold text-[rgb(56,189,248)]">{pKnowPercent}%</span>
              </span>
              <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${
                pKnowPercent >= 85 ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40"
                : pKnowPercent >= 40 ? "bg-sky-500/20 text-sky-300 border border-sky-500/40"
                : "bg-slate-500/20 text-slate-300 border border-slate-500/40"
              }`}>
                {pKnowPercent >= 85 ? "Mastered 🏆" : pKnowPercent >= 40 ? "Application ⚙️" : "Foundation 🔬"}
              </span>
            </div>
          </div>

          {/* BKT Probability Meter Bar */}
          <div className="mt-2.5 h-2 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/50">
            <div
              className="h-full bg-gradient-to-r from-teal-400 via-sky-400 to-indigo-400 transition-all duration-500"
              style={{ width: `${Math.max(5, pKnowPercent)}%` }}
            />
          </div>

          {/* Explainable Decision string */}
          {adaptiveInfo?.adaptiveReason && (
            <div className="mt-2.5 text-xs text-slate-300/90 flex items-start gap-1.5 leading-relaxed">
              <span className="text-[rgb(94,234,212)] font-semibold shrink-0">Adaptive Rationale:</span>
              <span>{adaptiveInfo.adaptiveReason}</span>
            </div>
          )}
        </div>

        <div className="mt-6">
          <div className="rounded-3xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(10,20,44,0.75)] shadow-xl overflow-hidden backdrop-blur-md">
            {/* Quest Header */}
            <div className="p-6 bg-gradient-to-br from-[rgba(56,189,248,0.12)] to-transparent border-b border-[rgba(48,68,105,0.6)] flex items-center justify-between flex-wrap gap-2">
              <div>
                <div className="pixel-heading text-xl">{section.title}</div>
                <div className="mt-2 text-sm text-slate-300/80">
                  Question {qIndex + 1} of {TOTAL} • Difficulty Level {currentQ?.difficulty || 1}/3
                </div>
              </div>
              {streak >= 2 && (
                <div className="px-3 py-1 rounded-full border border-orange-500/50 bg-orange-500/15 text-orange-300 text-xs font-bold animate-pulse">
                  🔥 {streak} Streak!
                </div>
              )}
            </div>

            {/* Quest Body */}
            <div className="p-6 md:p-8">
              {loading ? (
                <div className="py-12 text-center">
                  <div className="pixel-heading text-lg text-[rgb(94,234,212)]">Bayesian Engine Calculating Next Challenge...</div>
                  <div className="mt-4 h-2 max-w-xs mx-auto rounded-full bg-[rgba(255,255,255,0.08)] overflow-hidden">
                    <div className="h-2 w-1/3 bg-[rgb(56,189,248)] animate-pulse" />
                  </div>
                </div>
              ) : (
                <>
                  {/* Question Text */}
                  <div className="text-xl md:text-2xl font-bold text-slate-100 leading-relaxed min-h-[4rem]">
                    {typed}
                    {typed?.length < (currentQ?.questionText?.length || 0) && (
                      <span className="inline-block w-[10px] ml-1 bg-[rgb(94,234,212)] animate-pulse">▍</span>
                    )}
                  </div>

                  {/* Shuffled Options */}
                  <div className="mt-6 grid grid-cols-1 gap-3">
                    {shuffledOptions.map((optObj, idx) => (
                      <button
                        key={idx}
                        onClick={() => !locked && setSelected(idx)}
                        className={`text-left rounded-2xl border-2 p-4 transition ${optionStyle(idx)}`}
                      >
                        <div className="flex items-start gap-4">
                          <div className="h-9 w-9 rounded-xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(13,26,58,0.55)] flex items-center justify-center text-sm font-bold shrink-0">
                            {String.fromCharCode(65 + idx)}
                          </div>
                          <div className="text-base text-slate-100 leading-relaxed pt-1">{optObj.text}</div>
                        </div>
                      </button>
                    ))}
                  </div>

                  {/* Action Buttons */}
                  <div className="mt-6 flex gap-3 flex-wrap items-center">
                    {!locked ? (
                      <button
                        className="btn-pixel px-7 py-3 rounded-xl font-bold text-sm disabled:opacity-40 disabled:cursor-not-allowed"
                        onClick={submit}
                        disabled={selected === null}
                      >
                        Submit Response →
                      </button>
                    ) : (
                      <button
                        className="btn-pixel px-7 py-3 rounded-xl font-bold text-sm"
                        onClick={next}
                      >
                        {qIndex + 1 >= TOTAL ? "Conclude Mission →" : "Next Challenge →"}
                      </button>
                    )}
                  </div>

                  {/* Bayesian Probability Shift Breakdown */}
                  {bktUpdate?.bktExplanation && (
                    <div className="mt-5 p-3.5 rounded-2xl border border-indigo-500/40 bg-indigo-950/30 text-indigo-200 text-xs">
                      <div className="font-bold text-indigo-300 mb-1 flex items-center gap-1.5">
                        <span>📊</span>
                        <span>Bayesian Inference Update:</span>
                      </div>
                      <div className="leading-relaxed">{bktUpdate.bktExplanation}</div>
                    </div>
                  )}

                  {/* Step-by-Step Explanation */}
                  {showExplain && (
                    <div className="mt-5 rounded-2xl border-2 border-[rgba(56,189,248,0.35)] bg-[rgba(56,189,248,0.08)] p-5 animate-fadeIn">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-base">
                          {selectedKey === correctKey
                            ? "✅ Correct! Batcomputer Confirmed."
                            : "❌ Incorrect Calculation."}
                        </span>
                        <span className="text-sm text-slate-300">
                          (Expected: {shuffledOptions.find(o => o.key === correctKey)?.text || correctKey})
                        </span>
                      </div>
                      <div className="mt-3 text-sm text-slate-200/90 leading-relaxed whitespace-pre-line border-t border-slate-700/50 pt-3">
                        {currentQ?.explanation || "No explanation provided."}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
