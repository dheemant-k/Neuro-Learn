import { useEffect, useRef, useState } from "react";
import supermanImg from "../assets/superman.png";
import supergirlImg from "../assets/supergirl.png";
import batmanImg from "../assets/batman.png";
import lexluthorImg from "../assets/lexluthor.png";
import jokerImg from "../assets/joker.png";
import { sound } from "../utils/soundEffects";
import {
  startLearningSession,
  fetchQuestions,
  recordQuestionAttempt,
  completeLearningSession,
  claimBossVictory,
} from "../utils/api";

// Helper to format equations with distinct high-contrast chips
function formatTextWithEquations(text) {
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
}

export default function BossFightPage({ character, onVictory, onDefeat, onBack }) {
  const heroId = character?.id || "superman";
  const isBatman = heroId === "batman";
  const isSupergirl = heroId === "supergirl";

  const heroName = character?.name || (isSupergirl ? "Supergirl" : isBatman ? "Batman" : "Superman");
  const heroImg = isBatman ? batmanImg : isSupergirl ? supergirlImg : supermanImg;
  const bossName = isBatman ? "The Joker & Arkham Syndicate" : "Lex Luthor & Quantum Warsuit";
  const bossTitle = isBatman ? "ARKHAM GATEKEEPER" : "LEXCORP APEX WARSUT";
  const bossImg = isBatman ? jokerImg : lexluthorImg;
  const shieldLabel = isBatman ? "Bat-Shield Matrix" : "Solar Shield Matrix";

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
    isBatman
      ? "🦇 Batcomputer Alert: The Joker has deployed an encryption matrix. Solve the equations to dismantle the firewall!"
      : "🛰️ Daily Planet Comm-Link: Lex Luthor has engaged his LexCorp Quantum Warsuit. Solve the dual equations to collapse his anti-solar firewall!"
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

        const qList = await fetchQuestions({ topic: "final_boss", character: heroId });
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
  }, [heroId]);

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
      sound.playLaser();
      sound.playStreak();
      // Deal 100 DMG to Boss
      const newHp = Math.max(0, bossHp - 100);
      setBossHp(newHp);
      setFloatingDmg("-100 HP CRITICAL!");
      setTotalXpAwarded((x) => x + 25);
      setBattleLog(
        isBatman
          ? "💥 DIRECT HIT! Batarang coordinate calculation locked! 100 DMG dealt to the Arkham mainframe."
          : "💥 DIRECT HIT! Solar heat vision focused! 100 DMG dealt to LexCorp's quantum shield core."
      );
      triggerShake();

      setTimeout(() => setFloatingDmg(null), 1200);

      if (newHp <= 0) {
        handleBossVictory();
        return;
      }
    } else {
      sound.playWrong();
      // Player takes damage
      const newShields = playerShields - 1;
      setPlayerShields(newShields);
      setFloatingDmg("⚡ SHIELD DAMAGED!");
      setBattleLog(
        isBatman
          ? "⚠️ FIREWALL RETALIATION! Bat-Shield compromised. Recalibrate coordinate offsets!"
          : "⚠️ FIREWALL RETALIATION! LexCorp radiation beam hit your Solar Shields. Recalibrate coordinates!"
      );
      triggerShake();

      setTimeout(() => setFloatingDmg(null), 1200);

      if (newShields <= 0) {
        setBattleState("defeat");
        if (sessionId) completeLearningSession({ sessionId }).catch(() => {});
        return;
      }
    }
  };

  const handleBossVictory = async () => {
    sound.playVictory();
    setBattleState("victory");
    try {
      await claimBossVictory();
      setTotalXpAwarded((x) => x + 100);
      if (sessionId) await completeLearningSession({ sessionId }).catch(() => {});
    } catch (err) {
      console.warn("Failed claiming victory bonus:", err);
    }
  };

  const nextRound = () => {
    sound.playClick();
    const nextIdx = qIndex + 1;
    if (nextIdx >= questions.length) {
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
    sound.playClick();
    setBossHp(500);
    setPlayerShields(3);
    setBattleState("fighting");
    setQIndex(0);
    setTotalXpAwarded(0);
    setBattleLog(
      isBatman
        ? "🦇 Batcomputer Alert: Initiating emergency reboot. Arkham encryption re-engaged!"
        : "🛰️ Daily Planet Comm-Link: Solar reboot initiated! LexCorp Quantum Matrix re-engaged!"
    );
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
            className="px-4 py-2 rounded-xl border border-red-500/40 bg-red-950/30 hover:bg-red-900/50 text-red-200 text-sm font-semibold transition flex items-center gap-2"
          >
            <span>←</span>
            <span>Retreat to Roadmap</span>
          </button>
          <div className="flex items-center gap-3">
            <span className="text-xs px-3 py-1 rounded-full border border-yellow-500/40 bg-yellow-500/10 text-yellow-300 font-bold uppercase tracking-wider pixel">
              FINAL BOSS DUEL
            </span>
            <span className="text-xs text-slate-300 font-mono">
              Phase {qIndex + 1} / {questions.length || 5}
            </span>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Boss Arena HUD */}
        <div className="relative rounded-3xl border-2 border-red-500/40 bg-gradient-to-b from-red-950/30 via-slate-900/90 to-slate-950 p-6 md:p-8 shadow-2xl overflow-hidden backdrop-blur-md">
          {/* Cyberpunk background glows */}
          <div className="absolute top-0 right-0 w-80 h-80 bg-red-600/15 rounded-full blur-3xl -z-10 pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-80 h-80 bg-sky-600/15 rounded-full blur-3xl -z-10 pointer-events-none" />

          {/* Boss Profile & HP Header */}
          <div className="flex items-center justify-between flex-wrap gap-4 border-b border-slate-700/60 pb-6">
            <div className="flex items-center gap-4">
              <div className="relative">
                <img
                  src={bossImg}
                  alt={bossName}
                  className="w-20 h-20 md:w-24 md:h-24 object-contain rounded-2xl border-2 border-red-500/60 bg-red-950/40 shadow-xl p-1"
                />
                <span className="absolute -bottom-2 -right-2 text-[10px] px-2 py-0.5 rounded-full bg-red-600 text-white font-black uppercase pixel shadow-md animate-pulse">
                  APEX BOSS
                </span>
              </div>
              <div>
                <div className="text-xs text-red-400 font-bold uppercase tracking-widest pixel">
                  {bossTitle}
                </div>
                <div className="pixel-heading text-xl md:text-2xl text-red-100 mt-1">
                  {bossName}
                </div>
                <div className="text-xs text-slate-400 mt-1 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping" />
                  <span>Dual-Variable Quantum Encryption Grid</span>
                </div>
              </div>
            </div>

            {/* Boss HP Bar */}
            <div className="w-full md:w-72 text-right">
              <div className="flex justify-between items-center text-xs font-bold mb-1.5">
                <span className="text-red-400 uppercase tracking-wider">Warsuit Integrity</span>
                <span className="text-red-300 font-mono text-sm font-bold">{bossHp} / {MAX_BOSS_HP} HP</span>
              </div>
              <div className="h-4 w-full rounded-full bg-slate-950 overflow-hidden border border-red-500/50 p-0.5 shadow-inner">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-red-600 via-rose-500 to-amber-500 transition-all duration-500 shadow-lg shadow-red-500/50"
                  style={{ width: `${(bossHp / MAX_BOSS_HP) * 100}%` }}
                />
              </div>
            </div>
          </div>

          {/* Player Shield & XP Status */}
          <div className="mt-4 flex items-center justify-between flex-wrap gap-3 py-2 text-xs">
            <div className="flex items-center gap-3">
              <img src={heroImg} alt={heroName} className="w-8 h-8 object-contain rounded-xl border border-sky-400/50 bg-slate-900 p-0.5 shadow-md" />
              <span className="text-slate-300 font-semibold">{shieldLabel}:</span>
              <div className="flex gap-1.5">
                {[...Array(MAX_SHIELDS)].map((_, i) => (
                  <div
                    key={i}
                    className={`w-6 h-6 rounded-lg border flex items-center justify-center transition-all ${
                      i < playerShields
                        ? "bg-gradient-to-tr from-sky-500 to-cyan-400 border-sky-300 shadow-md shadow-sky-500/50 text-slate-950 text-xs font-black"
                        : "bg-slate-900 border-slate-800 opacity-25 text-transparent"
                    }`}
                  >
                    🛡️
                  </div>
                ))}
              </div>
            </div>

            <div className="text-yellow-400 font-semibold flex items-center gap-1.5 px-3 py-1 rounded-full bg-yellow-500/10 border border-yellow-500/30">
              <span>⭐</span>
              <span>Encounter XP Pool: +{totalXpAwarded} XP</span>
            </div>
          </div>

          {/* Tactical Battle Log */}
          <div className="mt-4 p-3.5 rounded-2xl border border-slate-700/70 bg-slate-950/80 text-xs flex items-center justify-between gap-3 shadow-inner">
            <div className="flex items-center gap-2 overflow-hidden">
              <span className="text-amber-400 text-sm shrink-0">📡</span>
              <span className="text-slate-200 font-mono leading-relaxed truncate">{battleLog}</span>
            </div>
            {floatingDmg && (
              <span className="px-3 py-1 rounded-xl bg-red-500/25 text-red-300 border border-red-500/50 font-black uppercase animate-bounce text-xs shrink-0 shadow-lg">
                {floatingDmg}
              </span>
            )}
          </div>

          {/* Boss Encounter Question Area */}
          {battleState === "fighting" && (
            <div className="mt-6 pt-6 border-t border-slate-800">
              {loading ? (
                <div className="py-16 text-center">
                  <div className="pixel-heading text-lg text-red-400 animate-pulse">
                    Locking Targeting Coordinates...
                  </div>
                  <div className="mt-4 h-2 max-w-xs mx-auto rounded-full bg-slate-800 overflow-hidden">
                    <div className="h-2 w-1/3 bg-red-500 animate-pulse" />
                  </div>
                </div>
              ) : (
                <>
                  {/* Upgraded Question Display */}
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
                            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-red-500/20 border border-red-500/40 text-red-200 text-xs font-bold tracking-wide shadow-md">
                              <span className="text-base">{prefix.slice(0, 2)}</span>
                              <span>{prefix.slice(2).trim()}</span>
                            </div>
                          )}
                          <div className="text-base md:text-xl font-bold text-slate-100 leading-relaxed font-sans">
                            {formatTextWithEquations(body)}
                          </div>
                        </div>
                      );
                    })()}
                  </div>

                  {/* Shuffled Action Choices */}
                  <div className="mt-7 grid grid-cols-1 md:grid-cols-2 gap-3.5">
                    {shuffledOptions.map((optObj, idx) => {
                      const isChosen = idx === selected;
                      const isTargetCorrect = optObj.key === correctKey;

                      let style = "border-slate-800 bg-slate-900/70 hover:border-red-500/60 hover:bg-slate-800/80 text-slate-200";
                      if (locked) {
                        if (isTargetCorrect) {
                          style = "border-emerald-400 bg-emerald-950/40 ring-2 ring-emerald-400/60 text-emerald-200 shadow-lg shadow-emerald-500/20";
                        } else if (isChosen && !isTargetCorrect) {
                          style = "border-red-500 bg-red-950/40 ring-2 ring-red-400/60 text-red-200 shadow-lg shadow-red-500/20";
                        } else {
                          style = "border-slate-800/60 bg-slate-900/30 opacity-40";
                        }
                      } else if (isChosen) {
                        style = "border-amber-400 bg-amber-500/15 ring-2 ring-amber-400/60 text-amber-200 shadow-lg shadow-amber-500/20 scale-[1.01]";
                      }

                      return (
                        <button
                          key={idx}
                          disabled={locked}
                          onClick={() => {
                            sound.playClick();
                            setSelected(idx);
                          }}
                          className={`group text-left rounded-2xl border-2 p-4 transition-all duration-200 flex items-center justify-between ${style}`}
                        >
                          <div className="flex items-center gap-3.5">
                            <div className="h-8 w-8 rounded-xl border border-slate-700 bg-slate-800/90 flex items-center justify-center text-xs font-bold shrink-0 font-mono text-slate-300 group-hover:border-slate-500 shadow-inner">
                              {String.fromCharCode(65 + idx)}
                            </div>
                            <div className="text-sm md:text-base text-slate-100 font-medium tracking-wide">
                              {formatTextWithEquations(optObj.text)}
                            </div>
                          </div>
                          {locked && isTargetCorrect && (
                            <span className="text-emerald-400 font-black text-sm animate-bounce">✓</span>
                          )}
                          {locked && isChosen && !isTargetCorrect && (
                            <span className="text-rose-400 font-black text-sm">✗</span>
                          )}
                        </button>
                      );
                    })}
                  </div>

                  {/* Attack / Next Button */}
                  <div className="mt-7 pt-4 border-t border-slate-800 flex items-center justify-between flex-wrap gap-4">
                    <div className="text-xs text-slate-400">
                      {!locked ? "Select your calculation attack, then fire EMP" : "Coordinates confirmed — engage next vector"}
                    </div>

                    {!locked ? (
                      <button
                        className="px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-red-600 via-rose-600 to-amber-500 text-slate-950 shadow-xl shadow-red-500/30 disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition flex items-center gap-2"
                        onClick={submitAttack}
                        disabled={selected === null}
                      >
                        <span>{isBatman ? "Fire EMP Batarang Strike (100 DMG)" : "Fire Solar Beam EMP Blast (100 DMG)"}</span>
                        <span>⚡</span>
                      </button>
                    ) : (
                      <button
                        className="px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-sky-400 to-teal-400 text-slate-950 shadow-xl hover:brightness-110 transition flex items-center gap-2"
                        onClick={nextRound}
                      >
                        <span>Engage Next Frequency Vector</span>
                        <span>→</span>
                      </button>
                    )}
                  </div>

                  {/* Solution Diagnostic Breakdown */}
                  {locked && (
                    <div className="mt-5 rounded-2xl border border-red-500/30 bg-gradient-to-r from-red-950/30 to-slate-900/60 p-5 text-sm leading-relaxed shadow-xl animate-fadeIn">
                      <div className="font-bold text-red-300 mb-2 flex items-center gap-2 text-xs uppercase tracking-wider">
                        <span>📖</span>
                        <span>Tactical Decryption Analysis:</span>
                      </div>
                      <div className="text-slate-200 whitespace-pre-line font-sans text-sm">
                        {formatTextWithEquations(currentQ?.explanation)}
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
              <div className="text-6xl mb-4 animate-bounce">🏆</div>
              <div className="pixel-heading text-3xl md:text-4xl text-amber-300">
                {isBatman ? "ARKHAM GATES OVERRIDDEN!" : "LEXCORP MAINFRAME DESTABILIZED!"}
              </div>
              <div className="mt-2 text-lg text-emerald-400 font-bold">
                {isBatman ? "The Joker's Encryption Has Been Decimated!" : "Lex Luthor's Quantum Warsuit Has Been Powered Down!"}
              </div>
              <p className="mt-3 text-slate-300 max-w-lg mx-auto leading-relaxed">
                {isBatman
                  ? "Your mastery of linear equation systems has broken the syndicate's defense grid. Gotham City is safe under your watch."
                  : "Your mastery of linear equation systems has broken LexCorp's grip on Metropolis. The city is safe under the sun."}
              </p>

              <div className="mt-6 inline-flex items-center gap-3 px-6 py-3 rounded-2xl border-2 border-amber-400/60 bg-amber-500/10 text-amber-200 shadow-xl">
                <span className="text-2xl">🎖️</span>
                <div className="text-left">
                  <div className="text-xs uppercase tracking-wider font-bold">Achievement Unlocked</div>
                  <div className="font-bold text-base">
                    {isBatman ? "Arkham Cryptographer Master (+100 XP)" : "Metropolis Quantum Defender (+100 XP)"}
                  </div>
                </div>
              </div>

              <div className="mt-8">
                <button
                  onClick={onVictory}
                  className="px-8 py-4 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-400 text-slate-950 font-bold text-base shadow-xl hover:brightness-110 transition"
                >
                  Return to Roadmap with Glory →
                </button>
              </div>
            </div>
          )}

          {/* Defeat Screen */}
          {battleState === "defeat" && (
            <div className="mt-8 py-10 px-6 text-center animate-fadeIn">
              <div className="text-6xl mb-4">💥</div>
              <div className="pixel-heading text-3xl text-red-400">
                {isBatman ? "BAT-SHIELD BREACHED!" : "SOLAR SHIELDS DEPLETED!"}
              </div>
              <div className="mt-2 text-base text-slate-300 font-medium">
                {isBatman
                  ? "The Joker's firewall retaliated with overwhelming feedback."
                  : "Lex Luthor's quantum firewall deflected your calculations."}
              </div>
              <p className="mt-3 text-slate-400 text-sm max-w-md mx-auto">
                Do not despair, hero. Review your linear equation principles, recalibrate your formulas, and breach the core once more!
              </p>

              <div className="mt-8 flex items-center justify-center gap-4 flex-wrap">
                <button
                  onClick={resetBattle}
                  className="px-7 py-3.5 rounded-xl bg-gradient-to-r from-red-600 to-amber-500 text-slate-950 font-bold text-sm shadow-lg hover:brightness-110 transition"
                >
                  🔄 Retry Boss Battle
                </button>
                <button
                  onClick={onDefeat}
                  className="px-7 py-3.5 rounded-xl border border-slate-700 bg-slate-800 text-slate-200 font-semibold text-sm hover:bg-slate-700 transition"
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
