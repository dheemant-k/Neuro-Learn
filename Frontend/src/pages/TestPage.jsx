import { useEffect, useRef, useState } from "react";
import {
  startLearningSession,
  fetchNextAdaptiveQuestion,
  recordQuestionAttempt,
  completeLearningSession,
} from "../utils/api";
import { sound } from "../utils/soundEffects";
import CharacterReactionPopup from "../components/CharacterReactionPopup";

export default function TestPage({ section, character, onFinish, onBack }) {
  const TOTAL = 5;
  const [qIndex, setQIndex] = useState(0);
  const [currentQ, setCurrentQ] = useState(null);
  const [shuffledOptions, setShuffledOptions] = useState([]);
  const [eliminatedOptions, setEliminatedOptions] = useState([]); // from X-Ray Vision
  const [adaptiveInfo, setAdaptiveInfo] = useState(null);
  const [bktUpdate, setBktUpdate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [locked, setLocked] = useState(false);
  const [showExplain, setShowExplain] = useState(false);

  // Gamification & Duel Battle Stats
  const [solarCharge, setSolarCharge] = useState(0); // 0 to 100%
  const [lexFirewallHp, setLexFirewallHp] = useState(100); // 100% down to 0%
  const [streak, setStreak] = useState(0);
  const [totalCorrect, setTotalCorrect] = useState(0);
  const [xpGained, setXpGained] = useState(0);
  const [currentPKnow, setCurrentPKnow] = useState(0.20);
  const [floatingEffects, setFloatingEffects] = useState([]); // { id, text, type }
  const [cardShake, setCardShake] = useState(false);
  const [muted, setMuted] = useState(sound.isMuted());
  const [reactionState, setReactionState] = useState("idle"); // "idle" | "correct" | "wrong"

  // Tactical Power-Ups
  const [xRayAvailable, setXRayAvailable] = useState(true);
  const [showKryptonClue, setShowKryptonClue] = useState(false);
  const [showScratchpad, setShowScratchpad] = useState(false);

  // Dynamic Superhero Quotes
  const [heroQuote, setHeroQuote] = useState(
    "Metropolis defense matrix active. Calculate the solution coordinates to breach LexCorp's grid!"
  );
  const [lexQuote, setLexQuote] = useState(
    "LexCorp Quantum Encryption: 100% online. You won't crack these linear equations, Superman."
  );

  const startRef = useRef(Date.now());
  const canvasRef = useRef(null);
  const [isDrawing, setIsDrawing] = useState(false);

  // Toggle Mute
  const toggleMute = () => {
    const next = !muted;
    sound.setMuted(next);
    setMuted(next);
  };

  // Trigger floating visual effect
  const addFloatingEffect = (text, type = "xp") => {
    const id = Date.now() + Math.random();
    setFloatingEffects((prev) => [...prev, { id, text, type }]);
    setTimeout(() => {
      setFloatingEffects((prev) => prev.filter((item) => item.id !== id));
    }, 1500);
  };

  const displayQuestion = (q, adaptiveData) => {
    setCurrentQ(q);
    setSelected(null);
    setLocked(false);
    setShowExplain(false);
    setBktUpdate(null);
    setEliminatedOptions([]);
    setShowKryptonClue(false);
    setReactionState("idle");

    // Shuffle options dynamically
    const raw = [
      { key: "A", text: q.optionA },
      { key: "B", text: q.optionB },
      { key: "C", text: q.optionC },
      { key: "D", text: q.optionD },
    ];
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

        const adaptiveRes = await fetchNextAdaptiveQuestion({
          sessionId: sId,
          topic: section.id,
          character: character?.id || "superman",
        });
        if (mounted && adaptiveRes?.question) {
          setAdaptiveInfo(adaptiveRes);
          displayQuestion(adaptiveRes.question, adaptiveRes);
        } else if (mounted) {
          const fallback = {
            id: 1,
            topic: section.id,
            difficulty: 1,
            questionText: "⚡ Metropolis Solar Grid: Solve for x in the energy equation: 3x + 4 = 19.",
            optionA: "x = 5",
            optionB: "x = 6",
            optionC: "x = 4",
            optionD: "x = 7",
            correctAnswer: "A",
            explanation: "Subtract 4: 3x = 15. Divide by 3: x = 5.",
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
    };
  }, [section.id]);

  const [sessionId, setSessionId] = useState(null);

  // Tactical Assist 1: X-Ray Vision (50/50)
  const useXRayVision = () => {
    if (!xRayAvailable || locked || !currentQ) return;
    sound.playLaser();
    setXRayAvailable(false);

    const correctKey = (currentQ.correctAnswer || "A").trim().toUpperCase();
    const wrongKeys = shuffledOptions
      .map((opt) => opt.key)
      .filter((k) => k !== correctKey);

    // Randomly pick 2 wrong keys to eliminate
    const shuffledWrong = [...wrongKeys].sort(() => 0.5 - Math.random());
    const toEliminate = shuffledWrong.slice(0, 2);
    setEliminatedOptions(toEliminate);

    addFloatingEffect("👁️ X-RAY VISION ACTIVATED!", "laser");
    setHeroQuote("X-Ray Vision scanned the frequency! Two incorrect options eliminated.");
  };

  // Submit Answer
  const submit = async () => {
    if (!currentQ || selected === null || locked || shuffledOptions.length === 0) return;

    const chosenOption = shuffledOptions[selected];
    if (eliminatedOptions.includes(chosenOption?.key)) return;

    const responseTimeMs = Date.now() - startRef.current;
    const chosenKey = chosenOption?.key;
    const correctKey = (currentQ.correctAnswer || "A").trim().toUpperCase();
    const isCorrect = chosenKey === correctKey;

    setLocked(true);
    setShowExplain(true);

    if (isCorrect) {
      setReactionState("correct");
      sound.playCorrect();
      setTotalCorrect((c) => c + 1);
      const newStreak = streak + 1;
      setStreak(newStreak);

      const xpEarn = 15 + newStreak * 5;
      setXpGained((x) => x + xpEarn);
      addFloatingEffect(`+${xpEarn} XP!`, "xp");

      if (newStreak >= 2) {
        sound.playStreak();
        addFloatingEffect(`🔥 ${newStreak}x STREAK BONUS!`, "streak");
      }

      // Charge Solar Power & Damage LexCorp
      setSolarCharge((prev) => Math.min(100, prev + 20));
      setLexFirewallHp((prev) => Math.max(0, prev - 20));

      setHeroQuote("Direct hit! Solar blast neutralized this frequency node.");
      setLexQuote("Firewall compromised! Security node damaged.");
    } else {
      setReactionState("wrong");
      sound.playWrong();
      setStreak(0);
      setXpGained((x) => x + 5);
      addFloatingEffect("+5 XP (Effort)", "effort");

      setCardShake(true);
      setTimeout(() => setCardShake(false), 500);

      setHeroQuote("LexCorp frequency deflected the beam. Review the steps and re-calibrate!");
      setLexQuote("Haha! The firewall absorbed your calculation. Try harder, Man of Steel.");
    }

    // Persist attempt to Bayesian Knowledge Tracing engine
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
    sound.playClick();
    setReactionState("idle");
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
      const adaptiveRes = await fetchNextAdaptiveQuestion({
        sessionId: sessionId || 1,
        topic: section.id,
        character: character?.id || "superman",
      });
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

  // Keyboard navigation (1-4 or Enter)
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (showScratchpad) return;
      if (!locked) {
        if (["1", "2", "3", "4"].includes(e.key)) {
          const idx = parseInt(e.key, 10) - 1;
          if (idx < shuffledOptions.length && !eliminatedOptions.includes(shuffledOptions[idx].key)) {
            sound.playClick();
            setSelected(idx);
          }
        } else if (e.key === "Enter" && selected !== null) {
          submit();
        }
      } else if (e.key === "Enter") {
        next();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [locked, selected, shuffledOptions, eliminatedOptions, showScratchpad]);

  // Scratchpad Canvas drawing logic
  const startDrawing = (e) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const rect = canvas.getBoundingClientRect();
    ctx.beginPath();
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
    setIsDrawing(true);
  };

  const draw = (e) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const rect = canvas.getBoundingClientRect();
    ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    ctx.strokeStyle = "#38bdf8";
    ctx.lineWidth = 2.5;
    ctx.lineCap = "round";
    ctx.stroke();
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  // Concept Clues lookup
  const getConceptClue = () => {
    const tid = section.id;
    if (tid.includes("substitution")) {
      return "💡 Substitution Strategy: Express one variable in terms of the other (e.g., y = 12 - 3x), then plug that whole expression into the other equation.";
    }
    if (tid.includes("elimination")) {
      return "💡 Elimination Strategy: Multiply so the coefficients of x or y match. If the signs are opposite, ADD the equations; if same, SUBTRACT.";
    }
    if (tid.includes("graphing")) {
      return "💡 Graphing Strategy: Find intercepts: set x = 0 to get the y-intercept, set y = 0 to get the x-intercept. Plot the two points!";
    }
    return "💡 General Strategy: Keep equations balanced by doing the same arithmetic operation to both sides of the equal sign.";
  };

  // Helper to format equations with distinct high-contrast chips
  const formatTextWithEquations = (text) => {
    if (!text) return null;
    const parts = text.split(/(\b-?\d*x\s*[\+\-]\s*\d*y\s*=\s*-?\d+\b|\b[xy]\s*=\s*-?\d+\b|\b-?\d*x\s*[\+\-]\s*\d+\s*=\s*-?\d+\b|\b\([a-zA-Z0-9\-]+,\s*[a-zA-Z0-9\-]+\))/g);
    if (parts.length <= 1) return text;

    return parts.map((part, i) => {
      if (part.match(/(\b-?\d*x\s*[\+\-]\s*\d*y\s*=\s*-?\d+\b|\b[xy]\s*=\s*-?\d+\b|\b-?\d*x\s*[\+\-]\s*\d+\s*=\s*-?\d+\b|\b\([a-zA-Z0-9\-]+,\s*[a-zA-Z0-9\-]+\))/)) {
        return (
          <span
            key={i}
            className="mx-1 px-2.5 py-0.5 rounded-lg bg-slate-950/90 border border-sky-400/60 text-amber-300 font-mono font-bold text-[0.95em] shadow-sm tracking-wide inline-block"
          >
            {part}
          </span>
        );
      }
      return part;
    });
  };

  const correctKey = (currentQ?.correctAnswer || "A").trim().toUpperCase();

  return (
    <div className="min-h-screen text-slate-100 relative overflow-x-hidden select-none">
      {/* Floating Damage / XP Particles */}
      <div className="fixed inset-0 pointer-events-none z-50 overflow-hidden">
        {floatingEffects.map((f) => (
          <div
            key={f.id}
            className={`absolute top-1/3 left-1/2 -translate-x-1/2 text-2xl font-black pixel-heading animate-bounce drop-shadow-[0_4px_10px_rgba(0,0,0,0.8)] ${
              f.type === "streak"
                ? "text-amber-300 scale-125"
                : f.type === "laser"
                ? "text-sky-300"
                : f.type === "effort"
                ? "text-indigo-300"
                : "text-emerald-300"
            }`}
          >
            {f.text}
          </div>
        ))}
      </div>

      {/* Top Header Navbar */}
      <header className="sticky top-0 z-40 backdrop-blur-md border-b border-sky-500/20 bg-slate-950/85">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          <button
            onClick={onBack}
            className="px-3.5 py-1.5 rounded-xl border border-slate-700 bg-slate-900/60 hover:bg-slate-800 text-xs text-slate-300 transition"
          >
            ← Exit Mission
          </button>

          <div className="flex items-center gap-4">
            {/* Streak Multiplier */}
            {streak > 1 && (
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-full border border-amber-500/40 bg-amber-500/10 text-amber-300 text-xs font-bold animate-pulse">
                <span>🔥</span>
                <span>{streak}x COMBO</span>
              </div>
            )}

            {/* Total XP */}
            <div className="text-xs px-3 py-1 rounded-full border border-yellow-500/40 bg-yellow-500/10 text-yellow-300 font-semibold">
              +{xpGained} XP
            </div>

            {/* Sound Toggle */}
            <button
              onClick={toggleMute}
              className="p-2 rounded-xl border border-slate-700 hover:border-slate-500 text-sm transition"
              title={muted ? "Unmute audio" : "Mute audio"}
            >
              {muted ? "🔇" : "🔊"}
            </button>
          </div>
        </div>
      </header>

      {/* Main Duel Arena */}
      <main className="max-w-5xl mx-auto px-4 py-6">
        {/* SUPERMAN VS LEXCORP DUEL HUD */}
        <div className="rounded-3xl border-2 border-sky-500/30 bg-gradient-to-r from-slate-950 via-slate-900 to-indigo-950/70 p-5 shadow-2xl relative overflow-hidden">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center">
            {/* Player Side: Superman Solar Charge */}
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-600 via-blue-600 to-amber-400 p-0.5 shrink-0 shadow-lg flex items-center justify-center font-black text-white text-xl">
                S
              </div>
              <div className="flex-1">
                <div className="flex justify-between items-center text-xs font-semibold mb-1">
                  <span className="text-sky-300 flex items-center gap-1">
                    🦸 Superman • Solar Charge
                  </span>
                  <span className="text-amber-300 font-bold">{solarCharge}%</span>
                </div>
                <div className="h-3 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/60 p-0.5">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-amber-400 via-sky-400 to-blue-500 transition-all duration-500"
                    style={{ width: `${Math.max(5, solarCharge)}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Enemy Side: LexCorp Firewall */}
            <div className="flex items-center gap-3">
              <div className="flex-1">
                <div className="flex justify-between items-center text-xs font-semibold mb-1">
                  <span className="text-rose-400">🏢 LexCorp Quantum Firewall</span>
                  <span className="text-rose-300 font-bold">{lexFirewallHp}% HP</span>
                </div>
                <div className="h-3 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/60 p-0.5">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-rose-500 to-red-600 transition-all duration-500"
                    style={{ width: `${lexFirewallHp}%` }}
                  />
                </div>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-purple-700 to-rose-700 p-0.5 shrink-0 shadow-lg flex items-center justify-center font-black text-white text-sm">
                LEX
              </div>
            </div>
          </div>

          {/* Interactive Dialogue Banner */}
          <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-start gap-3 text-xs">
            <span className="px-2 py-0.5 rounded-md bg-sky-500/20 text-sky-300 font-bold shrink-0">
              COMM-LINK
            </span>
            <p className="text-slate-300 italic">{locked ? heroQuote : lexQuote}</p>
          </div>
        </div>

        {/* MISSION PROGRESS & POWER-UP BAR */}
        <div className="mt-5 flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
              Node Decryption:
            </span>
            <span className="text-sm font-bold text-sky-400">
              {qIndex + 1} / {TOTAL}
            </span>
          </div>

          {/* Power-Up Buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={useXRayVision}
              disabled={!xRayAvailable || locked}
              className="px-3 py-1.5 rounded-xl border border-sky-400/40 bg-sky-500/10 hover:bg-sky-500/25 text-sky-300 text-xs font-semibold transition disabled:opacity-30 disabled:cursor-not-allowed flex items-center gap-1.5 shadow-sm"
              title="Eliminate two incorrect choices"
            >
              <span>👁️</span>
              <span>X-Ray 50:50</span>
              {xRayAvailable && (
                <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-ping" />
              )}
            </button>

            <button
              onClick={() => setShowKryptonClue((prev) => !prev)}
              className="px-3 py-1.5 rounded-xl border border-indigo-400/40 bg-indigo-500/10 hover:bg-indigo-500/25 text-indigo-300 text-xs font-semibold transition flex items-center gap-1.5"
            >
              <span>💎</span>
              <span>Krypton Clue</span>
            </button>

            <button
              onClick={() => setShowScratchpad((prev) => !prev)}
              className={`px-3 py-1.5 rounded-xl border text-xs font-semibold transition flex items-center gap-1.5 ${
                showScratchpad
                  ? "border-emerald-400 bg-emerald-500/20 text-emerald-300"
                  : "border-slate-700 bg-slate-900/60 hover:bg-slate-800 text-slate-300"
              }`}
            >
              <span>📝</span>
              <span>Scratchpad</span>
            </button>
          </div>
        </div>

        {/* Kryptonian Clue Popout */}
        {showKryptonClue && (
          <div className="mt-3 p-3.5 rounded-2xl border border-indigo-500/40 bg-indigo-950/40 text-xs text-indigo-200 animate-fadeIn">
            {getConceptClue()}
          </div>
        )}

        {/* Scratchpad Whiteboard Drawer */}
        {showScratchpad && (
          <div className="mt-4 p-4 rounded-3xl border-2 border-emerald-500/40 bg-slate-950/95 shadow-2xl animate-fadeIn">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-emerald-300 flex items-center gap-1.5">
                <span>📝</span> Fortress Scratchpad • Draw or scribble formulas here
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={clearCanvas}
                  className="text-xs px-2.5 py-1 rounded-lg border border-slate-700 bg-slate-800 text-slate-300 hover:text-white"
                >
                  Clear
                </button>
                <button
                  onClick={() => setShowScratchpad(false)}
                  className="text-xs px-2.5 py-1 rounded-lg border border-slate-700 bg-slate-800 text-slate-300 hover:text-white"
                >
                  Close
                </button>
              </div>
            </div>
            <canvas
              ref={canvasRef}
              width={700}
              height={180}
              onMouseDown={startDrawing}
              onMouseMove={draw}
              onMouseUp={stopDrawing}
              onMouseLeave={stopDrawing}
              className="w-full bg-slate-900/90 rounded-2xl border border-slate-800 cursor-crosshair touch-none"
            />
          </div>
        )}

        {/* QUESTION CARD */}
        <div
          className={`mt-4 rounded-3xl border-2 border-slate-800/90 bg-gradient-to-b from-slate-900/95 via-slate-900/90 to-slate-950/95 p-6 md:p-8 shadow-2xl backdrop-blur-xl transition-all ${
            cardShake ? "animate-pulse ring-2 ring-rose-500" : ""
          }`}
        >
          {loading ? (
            <div className="py-20 text-center">
              <div className="inline-block p-4 rounded-3xl bg-sky-500/10 border border-sky-500/30 mb-4 animate-bounce">
                <span className="text-3xl">🛰️</span>
              </div>
              <div className="pixel-heading text-lg text-sky-400 animate-pulse">
                Accessing LexCorp Mainframe Coordinates...
              </div>
              <p className="text-xs text-slate-400 mt-2">Decryption algorithms running...</p>
            </div>
          ) : (
            <>
              {/* Question HUD Header Bar */}
              <div className="flex items-center justify-between flex-wrap gap-2 pb-4 mb-5 border-b border-slate-800/80 text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-slate-400 font-mono uppercase tracking-wider text-[11px] font-bold">
                    MISSION NODE {String(qIndex + 1).padStart(2, "0")} / {String(TOTAL).padStart(2, "0")}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-1 rounded-full bg-slate-800/90 border border-slate-700/80 text-slate-300 text-[11px] font-semibold flex items-center gap-1.5 shadow-sm">
                    <span>{currentQ?.difficulty === 3 ? "🔴" : currentQ?.difficulty === 2 ? "🟡" : "🟢"}</span>
                    <span>{currentQ?.difficulty === 3 ? "Tier 3 • Advanced" : currentQ?.difficulty === 2 ? "Tier 2 • Intermediate" : "Tier 1 • Foundational"}</span>
                  </span>
                  <span className="px-2.5 py-1 rounded-full bg-sky-500/15 border border-sky-400/40 text-sky-300 text-[11px] font-bold shadow-sm">
                    {currentQ?.characterTheme ? `⚡ ${currentQ.characterTheme.toUpperCase()}` : "⚡ LINEAR SYSTEMS"}
                  </span>
                </div>
              </div>

              {/* Question Narrative & Main Body */}
              <div className="space-y-4">
                {(() => {
                  const raw = currentQ?.questionText || "";
                  const colonIdx = raw.indexOf(":");
                  const hasPrefix = colonIdx > 0 && colonIdx < 50;
                  const prefix = hasPrefix ? raw.slice(0, colonIdx).trim() : null;
                  const body = hasPrefix ? raw.slice(colonIdx + 1).trim() : raw;

                  return (
                    <div className="space-y-3">
                      {prefix && (
                        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-gradient-to-r from-sky-500/20 to-indigo-500/20 border border-sky-400/40 text-sky-200 text-xs font-bold tracking-wide shadow-md">
                          <span className="text-base">{prefix.slice(0, 2)}</span>
                          <span>{prefix.slice(2).trim()}</span>
                        </div>
                      )}
                      <div className="text-base md:text-lg text-slate-100 font-sans leading-relaxed tracking-wide font-medium">
                        {formatTextWithEquations(body)}
                      </div>
                    </div>
                  );
                })()}
              </div>

              {/* Shuffled Options Grid */}
              <div className="mt-7 grid grid-cols-1 md:grid-cols-2 gap-3.5">
                {shuffledOptions.map((opt, idx) => {
                  const isEliminated = eliminatedOptions.includes(opt.key);
                  const isChosen = selected === idx;
                  const isTargetCorrect = opt.key === correctKey;

                  let cardClasses =
                    "group relative p-4 rounded-2xl border-2 text-left font-medium transition-all text-sm flex items-center justify-between shadow-sm ";

                  if (isEliminated) {
                    cardClasses +=
                      "border-slate-800/40 bg-slate-950/40 text-slate-600 line-through cursor-not-allowed opacity-35 ";
                  } else if (!locked) {
                    if (isChosen) {
                      cardClasses +=
                        "border-sky-400 bg-sky-500/20 text-sky-100 ring-2 ring-sky-400/50 shadow-lg shadow-sky-500/20 scale-[1.01] ";
                    } else {
                      cardClasses +=
                        "border-slate-800/90 bg-slate-900/70 hover:border-sky-500/60 hover:bg-slate-800/90 text-slate-200 cursor-pointer hover:-translate-y-0.5 hover:shadow-md ";
                    }
                  } else {
                    if (isTargetCorrect) {
                      cardClasses +=
                        "border-emerald-400 bg-emerald-500/20 text-emerald-100 ring-2 ring-emerald-400/60 shadow-lg shadow-emerald-500/20 ";
                    } else if (isChosen && !isTargetCorrect) {
                      cardClasses +=
                        "border-rose-500 bg-rose-500/20 text-rose-100 ring-2 ring-rose-500/60 shadow-lg shadow-rose-500/20 ";
                    } else {
                      cardClasses += "border-slate-800/60 bg-slate-900/30 text-slate-500 opacity-45 ";
                    }
                  }

                  return (
                    <button
                      key={opt.key}
                      disabled={locked || isEliminated}
                      onClick={() => {
                        sound.playClick();
                        setSelected(idx);
                      }}
                      className={cardClasses}
                    >
                      <div className="flex items-center gap-3.5">
                        <span
                          className={`w-8 h-8 rounded-xl flex items-center justify-center font-mono font-bold text-xs transition-all shadow-inner shrink-0 ${
                            isChosen && !locked
                              ? "bg-sky-400 text-slate-950 shadow-sky-400/50"
                              : "bg-slate-800/90 border border-slate-700/80 text-slate-300 group-hover:border-slate-500"
                          }`}
                        >
                          {idx + 1}
                        </span>
                        <span className="text-sm md:text-base font-medium tracking-wide">
                          {formatTextWithEquations(opt.text)}
                        </span>
                      </div>
                      {locked && isTargetCorrect && (
                        <span className="px-2 py-0.5 rounded-lg bg-emerald-500/20 border border-emerald-400/60 text-emerald-400 font-bold text-xs flex items-center gap-1 animate-bounce">
                          <span>✓</span> Correct
                        </span>
                      )}
                      {locked && isChosen && !isTargetCorrect && (
                        <span className="px-2 py-0.5 rounded-lg bg-rose-500/20 border border-rose-400/60 text-rose-400 font-bold text-xs flex items-center gap-1">
                          <span>✗</span> Miscalculated
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>

              {/* Action Buttons: Confirm or Next */}
              <div className="mt-7 pt-4 border-t border-slate-800/80 flex items-center justify-between flex-wrap gap-4">
                <div className="text-xs text-slate-400 flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-slate-800 font-mono text-[11px] text-slate-300">
                    HOTKEY
                  </span>
                  <span>{!locked ? "Press 1–4 to select, Enter to lock in" : "Press Enter for next frequency node"}</span>
                </div>

                {!locked ? (
                  <button
                    onClick={submit}
                    disabled={selected === null}
                    className="px-7 py-3 rounded-xl bg-gradient-to-r from-sky-400 to-blue-500 text-slate-950 font-bold hover:brightness-110 transition shadow-lg shadow-sky-500/25 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
                  >
                    <span>Lock In Coordinates</span>
                    <span>→</span>
                  </button>
                ) : (
                  <button
                    onClick={next}
                    className="px-7 py-3 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-500 text-slate-950 font-bold hover:brightness-110 transition shadow-lg shadow-emerald-500/25 flex items-center gap-2"
                  >
                    <span>{qIndex + 1 >= TOTAL ? "Complete Mission 🏆" : "Next Frequency Node"}</span>
                    <span>→</span>
                  </button>
                )}
              </div>

              {/* Step-by-Step Tactical Breakdown Banner */}
              {showExplain && currentQ?.explanation && (
                <div className="mt-5 p-5 rounded-2xl border border-sky-500/40 bg-gradient-to-r from-sky-950/40 to-slate-900/60 animate-fadeIn shadow-xl">
                  <div className="flex items-center gap-2 text-xs font-bold text-sky-400 uppercase tracking-wider mb-2">
                    <span>📖</span>
                    <span>Tactical Reconnaissance // Step-by-Step Breakdown</span>
                  </div>
                  <p className="text-sm text-slate-200 leading-relaxed font-sans">
                    {formatTextWithEquations(currentQ.explanation)}
                  </p>
                </div>
              )}
            </>
          )}
        </div>

        {/* Bayesian Knowledge Meter Footer */}
        <div className="mt-4 flex items-center justify-between px-3 text-xs text-slate-400">
          <span className="flex items-center gap-1.5">
            <span>🧠</span> Bayesian Knowledge State:{" "}
            <strong className="text-sky-400">{Math.round(currentPKnow * 100)}% P(L)</strong>
          </span>
          {bktUpdate && (
            <span className="text-emerald-400 animate-pulse">
              Adaptive model updated (+{Math.round((bktUpdate.pknow - bktUpdate.previousPknow) * 100)}%)
            </span>
          )}
        </div>
      </main>

      {/* Dynamic Animated Character Reaction Cut-in */}
      <CharacterReactionPopup
        hero={character}
        state={reactionState}
        streak={streak}
        selectedOption={selected}
      />
    </div>
  );
}
