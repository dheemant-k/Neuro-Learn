import { useEffect, useRef, useState } from "react";
import jokerImg from "../assets/joker.png";
import batmanImg from "../assets/batman.png";
import {
  startLearningSession,
  fetchQuestions,
  recordQuestionAttempt,
  completeLearningSession,
  claimBossVictory,
} from "../utils/api";

export default function BossFightPage({ character, onVictory, onDefeat, onBack }) {
  const [questions, setQuestions] = useState([]);
  const [qIndex, setQIndex] = useState(0);
  const [currentQ, setCurrentQ] = useState(null);
  const [shuffledOptions, setShuffledOptions] = useState([]);
  const [selected, setSelected] = useState(null);
  const [locked, setLocked] = useState(false);
  const [loading, setLoading] = useState(true);

  // Boss Battle Stats
  const MAX_BOSS_HP = 500;
  const [bossHp, setBossHp] = useState(500);
  const MAX_SHIELDS = 3;
  const [playerShields, setPlayerShields] = useState(3);
  const [battleState, setBattleState] = useState("fighting"); // "fighting" | "victory" | "defeat"
  const [floatingDmg, setFloatingDmg] = useState(null);
  const [shake, setShake] = useState(false);

  // Session stats
  const [sessionId, setSessionId] = useState(null);
  const [totalXpAwarded, setTotalXpAwarded] = useState(0);
  const [battleLog, setBattleLog] = useState(
    "🦇 Batcomputer Alert: The Riddler has deployed an encryption matrix. Solve the equations to dismantle the firewall!"
  );

  const startRef = useRef(Date.now());

  // 1. Fetch Final Boss questions & start session
  useEffect(() => {
    let mounted = true;

    async function initBossBattle() {
      setLoading(true);
      try {
        const sessionRes = await startLearningSession({ topic: "final_boss" }).catch(() => null);
        if (mounted && sessionRes) setSessionId(sessionRes.id);

        const qList = await fetchQuestions({ topic: "final_boss" });
        if (mounted && qList && qList.length > 0) {
          setQuestions(qList);
          displayBossQuestion(qList[0]);
        }
      } catch (err) {
        console.warn("Error initiating boss fight:", err);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    initBossBattle();

    return () => {
      mounted = false;
    };
  }, []);

  const displayBossQuestion = (q) => {
    setCurrentQ(q);
    setSelected(null);
    setLocked(false);
    startRef.current = Date.now();

    // Fisher-Yates shuffle
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
  };

  const triggerShake = () => {
    setShake(true);
    setTimeout(() => setShake(false), 500);
  };

  const submitAttack = async () => {
    if (!currentQ || selected === null || locked || shuffledOptions.length === 0) return;

    const responseTimeMs = Date.now() - startRef.current;
    const chosenKey = shuffledOptions[selected]?.key;
    const correctKey = (currentQ.correctAnswer || "A").trim().toUpperCase();
    const isCorrect = chosenKey === correctKey;

    setLocked(true);

    if (sessionId && currentQ.id) {
      recordQuestionAttempt({
        sessionId,
        questionId: currentQ.id,
        correct: isCorrect,
        responseTimeMs,
        attemptNumber: 1,
      }).catch(() => {});
    }

    if (isCorrect) {
      // Deal 100 DMG to Boss
      const newHp = Math.max(0, bossHp - 100);
      setBossHp(newHp);
      setFloatingDmg("-100 HP CRITICAL!");
      setTotalXpAwarded((x) => x + 25);
      setBattleLog("💥 DIRECT HIT! Batarang calculation locked! 100 DMG dealt to the Arkham mainframe.");
      triggerShake();

      setTimeout(() => setFloatingDmg(null), 1200);

      if (newHp <= 0) {
        // Victory!
        handleBossVictory();
        return;
      }
    } else {
      // Player takes damage
      const newShields = playerShields - 1;
      setPlayerShields(newShields);
      setFloatingDmg("⚡ SHIELD DAMAGED!");
      setBattleLog("⚠️ FIREWALL RETALIATION! Bat-Shield compromised. Recalibrate coordinate offsets!");
      triggerShake();

      setTimeout(() => setFloatingDmg(null), 1200);

      if (newShields <= 0) {
        // Defeat
        setBattleState("defeat");
        if (sessionId) completeLearningSession({ sessionId }).catch(() => {});
        return;
      }
    }
  };

  const handleBossVictory = async () => {
    setBattleState("victory");
    try {
      await claimBossVictory();
      setTotalXpAwarded((x) => x + 100); // 100 Boss bonus
      if (sessionId) await completeLearningSession({ sessionId }).catch(() => {});
    } catch (err) {
      console.warn("Failed claiming victory bonus:", err);
    }
  };

  const nextRound = () => {
    const nextIdx = qIndex + 1;
    if (nextIdx >= questions.length) {
      // If we went through all 5 questions and boss isn't dead
      if (bossHp <= 100) {
        handleBossVictory();
      } else {
        setBattleState("defeat");
      }
      return;
    }

    setQIndex(nextIdx);
    displayBossQuestion(questions[nextIdx]);
  };

  const resetBattle = () => {
    setBossHp(500);
    setPlayerShields(3);
    setBattleState("fighting");
    setQIndex(0);
    setTotalXpAwarded(0);
    setBattleLog("🦇 Batcomputer Alert: Initiating emergency reboot. Arkham encryption re-engaged!");
    if (questions.length > 0) {
      displayBossQuestion(questions[0]);
    }
  };

  const correctKey = (currentQ?.correctAnswer || "A").trim().toUpperCase();

  return (
    <div className={`min-h-screen text-slate-100 transition-all ${shake ? "translate-x-1 translate-y-1" : ""}`}>
      {/* Top Header */}
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-red-500/20 bg-[rgba(10,15,30,0.92)]">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <button
            onClick={onBack}
            className="px-4 py-2 rounded-xl border border-red-500/40 bg-red-950/30 hover:bg-red-900/50 text-red-200 text-sm font-semibold transition"
          >
            ← Retreat to Roadmap
          </button>
          <div className="flex items-center gap-3">
            <span className="text-xs px-3 py-1 rounded-full border border-yellow-500/40 bg-yellow-500/10 text-yellow-300 font-bold uppercase tracking-wider pixel">
              BOSS COMBAT
            </span>
            <span className="text-xs text-slate-300">
              Round {qIndex + 1} / {questions.length || 5}
            </span>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Boss Arena HUD */}
        <div className="relative rounded-3xl border-2 border-red-500/40 bg-gradient-to-b from-red-950/30 via-slate-900/90 to-slate-950 p-6 md:p-8 shadow-2xl overflow-hidden backdrop-blur-md">
          {/* Subtle Cyberpunk background glow */}
          <div className="absolute top-0 right-0 w-80 h-80 bg-red-600/10 rounded-full blur-3xl -z-10 pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-80 h-80 bg-sky-600/10 rounded-full blur-3xl -z-10 pointer-events-none" />

          {/* Boss Profile & HP */}
          <div className="flex items-center justify-between flex-wrap gap-4 border-b border-slate-700/60 pb-6">
            <div className="flex items-center gap-4">
              <div className="relative">
                <img
                  src={jokerImg}
                  alt="Boss"
                  className="w-20 h-20 md:w-24 md:h-24 object-contain rounded-2xl border-2 border-red-500/60 bg-red-950/40 shadow-lg"
                />
                <span className="absolute -bottom-2 -right-2 text-xs px-2 py-0.5 rounded-full bg-red-600 text-white font-bold uppercase pixel">
                  BOSS
                </span>
              </div>
              <div>
                <div className="text-xs text-red-400 font-bold uppercase tracking-widest pixel">
                  ARKHAM GATEKEEPER
                </div>
                <div className="pixel-heading text-2xl text-red-200">The Riddler & Syndicate</div>
                <div className="text-xs text-slate-400 mt-1">Dual-Variable Frequency Barrier</div>
              </div>
            </div>

            {/* Boss HP Bar */}
            <div className="w-full md:w-64 text-right">
              <div className="flex justify-between items-center text-xs font-bold mb-1.5">
                <span className="text-red-400">MAINFRAME HEALTH</span>
                <span className="text-red-300 font-mono text-sm">{bossHp} / {MAX_BOSS_HP} HP</span>
              </div>
              <div className="h-4 w-full rounded-full bg-slate-950 overflow-hidden border border-red-500/50 p-0.5">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-red-600 via-rose-500 to-amber-500 transition-all duration-500 shadow-lg shadow-red-500/50"
                  style={{ width: `${(bossHp / MAX_BOSS_HP) * 100}%` }}
                />
              </div>
            </div>
          </div>

          {/* Player Shield & Tactical Integrity */}
          <div className="mt-4 flex items-center justify-between flex-wrap gap-3 py-2 text-xs">
            <div className="flex items-center gap-3">
              <img src={batmanImg} alt="Hero" className="w-8 h-8 object-contain rounded-lg border border-sky-400/50" />
              <span className="text-slate-300 font-semibold">Bat-Shield Integrity:</span>
              <div className="flex gap-1.5">
                {[...Array(MAX_SHIELDS)].map((_, i) => (
                  <div
                    key={i}
                    className={`w-4 h-4 rounded-md border flex items-center justify-center transition-all ${
                      i < playerShields
                        ? "bg-sky-400 border-sky-300 shadow-md shadow-sky-500/50 text-slate-950 text-[10px] font-bold"
                        : "bg-slate-800 border-slate-700 opacity-30 text-transparent"
                    }`}
                  >
                    ⚡
                  </div>
                ))}
              </div>
            </div>

            <div className="text-yellow-400 font-semibold">
              Boss XP Pool: +{totalXpAwarded} XP
            </div>
          </div>

          {/* Tactical Battle Log */}
          <div className="mt-4 p-3 rounded-2xl border border-slate-700/70 bg-slate-950/70 text-xs flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <span className="text-amber-400 text-sm">📡</span>
              <span className="text-slate-300 font-mono leading-relaxed">{battleLog}</span>
            </div>
            {floatingDmg && (
              <span className="px-2.5 py-1 rounded-lg bg-red-500/20 text-red-300 border border-red-500/40 font-bold uppercase animate-bounce text-[11px] shrink-0">
                {floatingDmg}
              </span>
            )}
          </div>

          {/* Boss Encounter Question Area */}
          {battleState === "fighting" && (
            <div className="mt-6 pt-6 border-t border-slate-800">
              {loading ? (
                <div className="py-12 text-center">
                  <div className="pixel-heading text-lg text-red-400">Locking Targeting Coordinates...</div>
                  <div className="mt-4 h-2 max-w-xs mx-auto rounded-full bg-slate-800 overflow-hidden">
                    <div className="h-2 w-1/3 bg-red-500 animate-pulse" />
                  </div>
                </div>
              ) : (
                <>
                  <div className="text-lg md:text-xl font-bold text-slate-100 leading-relaxed">
                    {currentQ?.questionText}
                  </div>

                  {/* Shuffled Action Choices */}
                  <div className="mt-6 grid grid-cols-1 gap-3">
                    {shuffledOptions.map((optObj, idx) => {
                      const isChosen = idx === selected;
                      const isTargetCorrect = optObj.key === correctKey;

                      let style = "border-[rgba(48,68,105,0.9)] bg-[rgba(10,20,44,0.65)] hover:bg-[rgba(13,26,58,0.85)]";
                      if (locked) {
                        if (isTargetCorrect) {
                          style = "border-emerald-400 bg-emerald-950/40 ring-1 ring-emerald-400 text-emerald-200";
                        } else if (isChosen && !isTargetCorrect) {
                          style = "border-red-500 bg-red-950/40 ring-1 ring-red-400 text-red-200";
                        } else {
                          style = "border-slate-800 bg-slate-900/40 opacity-50";
                        }
                      } else if (isChosen) {
                        style = "border-amber-400 bg-amber-500/15 ring-1 ring-amber-400 text-amber-200";
                      }

                      return (
                        <button
                          key={idx}
                          onClick={() => !locked && setSelected(idx)}
                          className={`text-left rounded-2xl border-2 p-4 transition ${style}`}
                        >
                          <div className="flex items-start gap-4">
                            <div className="h-9 w-9 rounded-xl border-2 border-slate-700 bg-slate-900 flex items-center justify-center text-sm font-bold shrink-0 font-mono">
                              {String.fromCharCode(65 + idx)}
                            </div>
                            <div className="text-base text-slate-100 leading-relaxed pt-1">
                              {optObj.text}
                            </div>
                          </div>
                        </button>
                      );
                    })}
                  </div>

                  {/* Attack / Next Button */}
                  <div className="mt-6 flex items-center justify-between flex-wrap gap-4">
                    {!locked ? (
                      <button
                        className="btn-pixel px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-red-600 to-amber-500 text-slate-950 shadow-lg shadow-red-500/30 disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110"
                        onClick={submitAttack}
                        disabled={selected === null}
                      >
                        Fire EMP Calculation Attack (100 DMG) ⚡
                      </button>
                    ) : (
                      <button
                        className="btn-pixel px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-sky-500 to-teal-400 text-slate-950 shadow-lg hover:brightness-110"
                        onClick={nextRound}
                      >
                        Engage Next Vector →
                      </button>
                    )}
                  </div>

                  {/* Solution Diagnostic */}
                  {locked && (
                    <div className="mt-5 rounded-2xl border-2 border-slate-700/80 bg-slate-900/60 p-5 text-sm leading-relaxed">
                      <div className="font-bold text-slate-200 mb-1">
                        Batcomputer Mathematical Decryption:
                      </div>
                      <div className="text-slate-300 whitespace-pre-line">
                        {currentQ?.explanation}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {/* Victory Screen */}
          {battleState === "victory" && (
            <div className="mt-8 py-10 px-6 text-center animate-fadeIn">
              <div className="text-6xl mb-3 animate-bounce">🏆</div>
              <div className="pixel-heading text-3xl md:text-4xl text-amber-300">
                ARKHAM GATES OVERRIDDEN!
              </div>
              <div className="mt-2 text-lg text-emerald-400 font-bold">
                The Riddler's Encryption Has Been Decimated!
              </div>
              <p className="mt-3 text-slate-300 max-w-lg mx-auto leading-relaxed">
                Your mastery of linear equation systems has broken the syndicate's defense grid. Gotham City is safe under your watch.
              </p>

              <div className="mt-6 inline-flex items-center gap-3 px-6 py-3 rounded-2xl border-2 border-amber-400/60 bg-amber-500/10 text-amber-200">
                <span className="text-2xl">🎖️</span>
                <div className="text-left">
                  <div className="text-xs uppercase tracking-wider font-bold">Achievement Unlocked</div>
                  <div className="font-bold text-base">Arkham Cryptographer Master (+100 XP)</div>
                </div>
              </div>

              <div className="mt-8">
                <button
                  onClick={onVictory}
                  className="px-8 py-4 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 text-slate-950 font-bold text-base shadow-xl hover:brightness-110 transition"
                >
                  Return to Roadmap with Glory →
                </button>
              </div>
            </div>
          )}

          {/* Defeat Screen */}
          {battleState === "defeat" && (
            <div className="mt-8 py-10 px-6 text-center animate-fadeIn">
              <div className="text-6xl mb-3">💥</div>
              <div className="pixel-heading text-3xl text-red-400">
                BAT-SHIELD BREACHED!
              </div>
              <div className="mt-2 text-base text-slate-300 font-medium">
                The Riddler's firewall retaliated with overwhelming feedback.
              </div>
              <p className="mt-3 text-slate-400 text-sm max-w-md mx-auto">
                Do not despair, detective. Review linear systems, recalibrate your formulas, and breach the gates once more!
              </p>

              <div className="mt-8 flex items-center justify-center gap-4 flex-wrap">
                <button
                  onClick={resetBattle}
                  className="px-6 py-3 rounded-xl bg-gradient-to-r from-red-600 to-amber-500 text-slate-950 font-bold text-sm shadow-lg hover:brightness-110 transition"
                >
                  🔄 Retry Boss Battle
                </button>
                <button
                  onClick={onDefeat}
                  className="px-6 py-3 rounded-xl border border-slate-700 bg-slate-800 text-slate-200 font-semibold text-sm hover:bg-slate-700 transition"
                >
                  ← Return to Roadmap
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
