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
  const [adaptiveInfo, setAdaptiveInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null); // 0, 1, 2, 3
  const [locked, setLocked] = useState(false);
  const [showExplain, setShowExplain] = useState(false);

  // Session stats
  const [sessionId, setSessionId] = useState(null);
  const [totalCorrect, setTotalCorrect] = useState(0);
  const [streak, setStreak] = useState(0);
  const [xpGained, setXpGained] = useState(0);

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
          displayQuestion(adaptiveRes.question);
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
          displayQuestion(fallback);
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

  const displayQuestion = (q) => {
    setCurrentQ(q);
    setSelected(null);
    setLocked(false);
    setShowExplain(false);
    startRef.current = Date.now();
    startTypewriter(q.questionText || "");
  };

  const getCorrectIndex = (letter) => {
    if (!letter) return 0;
    const clean = letter.trim().toUpperCase();
    if (clean === "B") return 1;
    if (clean === "C") return 2;
    if (clean === "D") return 3;
    return 0;
  };

  const submit = async () => {
    if (!currentQ || selected === null || locked) return;

    const responseTimeMs = Date.now() - startRef.current;
    const correctIdx = getCorrectIndex(currentQ.correctAnswer);
    const isCorrect = selected === correctIdx;

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

    // Persist attempt to backend
    if (sessionId && currentQ.id) {
      recordQuestionAttempt({
        sessionId,
        questionId: currentQ.id,
        correct: isCorrect,
        responseTimeMs,
        attemptNumber: 1,
      }).catch((err) => console.warn("Failed recording attempt:", err));
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
      // Pull next dynamically adapted question from the Adaptive Engine!
      const adaptiveRes = await fetchNextAdaptiveQuestion({ sessionId: sessionId || 1, topic: section.id });
      if (adaptiveRes?.question) {
        setAdaptiveInfo(adaptiveRes);
        displayQuestion(adaptiveRes.question);
      }
    } catch (err) {
      console.warn("Failed loading next question:", err);
    } finally {
      setLoading(false);
    }
  };

  const optionStyle = (idx) => {
    const correctIdx = getCorrectIndex(currentQ?.correctAnswer);
    if (!locked) {
      return idx === selected
        ? "border-[rgba(56,189,248,0.75)] bg-[rgba(56,189,248,0.15)] ring-1 ring-[rgb(56,189,248)]"
        : "border-[rgba(48,68,105,0.9)] bg-[rgba(10,20,44,0.65)] hover:bg-[rgba(13,26,58,0.85)]";
    }
    if (idx === correctIdx) return "border-[rgba(52,211,153,0.85)] bg-[rgba(52,211,153,0.15)] ring-1 ring-[rgb(52,211,153)]";
    if (idx === selected && selected !== correctIdx) return "border-[rgba(251,113,133,0.85)] bg-[rgba(251,113,133,0.15)] ring-1 ring-[rgb(251,113,133)]";
    return "border-[rgba(48,68,105,0.7)] bg-[rgba(10,20,44,0.50)] opacity-70";
  };

  const options = currentQ
    ? [currentQ.optionA, currentQ.optionB, currentQ.optionC, currentQ.optionD]
    : [];

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

        {/* Adaptive Engine Decision Badge */}
        {adaptiveInfo?.adaptiveReason && (
          <div className="mt-4 p-3 rounded-2xl border border-[rgb(94,234,212)]/40 bg-[rgb(94,234,212)]/10 text-[rgb(94,234,212)] text-xs flex items-center gap-2">
            <span className="font-bold">🧠 AI Adaptive Engine:</span>
            <span>{adaptiveInfo.adaptiveReason}</span>
          </div>
        )}

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
                  <div className="pixel-heading text-lg text-[rgb(94,234,212)]">Adaptive Engine Analyzing Performance...</div>
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

                  {/* Options */}
                  <div className="mt-6 grid grid-cols-1 gap-3">
                    {options.map((opt, idx) => (
                      <button
                        key={idx}
                        onClick={() => !locked && setSelected(idx)}
                        className={`text-left rounded-2xl border-2 p-4 transition ${optionStyle(idx)}`}
                      >
                        <div className="flex items-start gap-4">
                          <div className="h-9 w-9 rounded-xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(13,26,58,0.55)] flex items-center justify-center text-sm font-bold shrink-0">
                            {String.fromCharCode(65 + idx)}
                          </div>
                          <div className="text-base text-slate-100 leading-relaxed pt-1">{opt}</div>
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

                  {/* Step-by-Step Explanation */}
                  {showExplain && (
                    <div className="mt-6 rounded-2xl border-2 border-[rgba(56,189,248,0.35)] bg-[rgba(56,189,248,0.08)] p-5 animate-fadeIn">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-base">
                          {selected === getCorrectIndex(currentQ?.correctAnswer)
                            ? "✅ Correct! Batcomputer Confirmed."
                            : "❌ Incorrect Calculation."}
                        </span>
                        <span className="text-sm text-slate-300">
                          (Correct Answer: Option {currentQ?.correctAnswer})
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
