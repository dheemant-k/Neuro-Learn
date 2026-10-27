import { useEffect, useState } from "react";
import { fetchProgress } from "../utils/api";

export default function RoadmapPage({ chapter, character, onLearn, onTest, onFinalBoss, onBack }) {
  const [modal, setModal] = useState(null); // section object or null
  const [progressData, setProgressData] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadProgress = async () => {
    try {
      const data = await fetchProgress();
      if (data) setProgressData(data);
    } catch (err) {
      console.warn("Failed fetching roadmap progress:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProgress();
  }, []);

  const progressMap = {};
  if (progressData?.sections) {
    progressData.sections.forEach((s) => {
      progressMap[s.sectionId] = s;
    });
  }

  const isSectionDone = (secId) => {
    const p = progressMap[secId];
    return p ? p.isCompleted : false;
  };

  const isSectionUnlocked = (idx, secId) => {
    if (idx === 0) return true;
    const p = progressMap[secId];
    if (p && p.isUnlocked !== undefined) return p.isUnlocked;
    const prevSec = chapter.sections[idx - 1];
    return isSectionDone(prevSec.id);
  };

  const bossUnlocked = progressData?.bossUnlocked || chapter.sections.every((s) => isSectionDone(s.id));

  return (
    <div className="min-h-screen text-slate-100">
      {/* Top bar */}
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-white/[0.06] bg-[rgba(15,23,42,0.85)]">
        <div className="max-w-4xl mx-auto px-6 h-16 flex items-center justify-between">
          <button
            onClick={onBack}
            className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] hover:bg-[rgba(13,26,58,0.85)] transition text-sm"
          >
            ← Home
          </button>
          <div className="flex items-center gap-4">
            {progressData?.totalXp !== undefined && (
              <span className="text-xs px-3 py-1 rounded-full border border-yellow-500/40 bg-yellow-500/10 text-yellow-300 font-semibold">
                {progressData.totalXp} XP • Level {progressData.level || 1}
              </span>
            )}
            <div className="flex items-center gap-2">
              <img src={character.image} alt={character.name} className="w-8 h-8 object-contain rounded-lg" />
              <span className="text-sm font-semibold">{character.name}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <div className="pixel-heading text-2xl md:text-3xl">{chapter.title}</div>
            <p className="mt-2 text-slate-300/80">Follow the path to defeat the Final Boss and protect the city!</p>
          </div>
          <button
            onClick={loadProgress}
            className="text-xs px-3 py-1.5 rounded-xl border border-slate-700 bg-slate-800/80 text-slate-300 hover:text-white hover:bg-slate-700 transition"
          >
            🔄 Sync Progress
          </button>
        </div>

        {/* Roadmap Tree */}
        <div className="mt-10 relative">
          {/* Vertical progress line */}
          <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-[rgba(51,65,85,0.6)]" />

          {chapter.sections.map((sec, idx) => {
            const secProgress = progressMap[sec.id];
            const done = isSectionDone(sec.id);
            const unlocked = isSectionUnlocked(idx, sec.id);
            const pKnowPct = Math.round((secProgress?.pKnow || 0.20) * 100);
            const attempts = secProgress?.attempts || 0;
            const correct = secProgress?.correctCount || 0;
            const isMastered = secProgress?.isMastered || pKnowPct >= 85;

            return (
              <div key={sec.id} className="relative pl-16 pb-10">
                {/* Node Status Indicator */}
                <div
                  className={`absolute left-4 w-5 h-5 rounded-full border-2 top-1 transition-all ${
                    done
                      ? "bg-[rgb(52,211,153)] border-[rgb(52,211,153)] shadow-lg shadow-emerald-500/30"
                      : unlocked
                      ? "bg-[rgb(56,189,248)] border-[rgb(56,189,248)] animate-pulse shadow-lg shadow-sky-500/30"
                      : "bg-[rgba(51,65,85,0.6)] border-[rgba(51,65,85,0.6)]"
                  }`}
                />

                <button
                  onClick={() => unlocked && setModal(sec)}
                  disabled={!unlocked}
                  className={`w-full text-left rounded-2xl border-2 p-5 transition backdrop-blur-sm ${
                    !unlocked
                      ? "border-[rgba(51,65,85,0.5)] bg-[rgba(15,23,42,0.4)] opacity-60 cursor-not-allowed"
                      : done
                      ? "border-[rgba(52,211,153,0.5)] bg-[rgba(52,211,153,0.06)] hover:bg-[rgba(52,211,153,0.12)] shadow-md"
                      : "border-[rgba(56,189,248,0.5)] bg-[rgba(15,23,42,0.75)] hover:bg-[rgba(30,41,59,0.85)] shadow-md"
                  }`}
                >
                  <div className="flex items-center justify-between gap-3 flex-wrap">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-slate-400 tracking-wider font-semibold">
                          MISSION {idx + 1}
                        </span>
                        {isMastered && (
                          <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                            Mastered 🏆
                          </span>
                        )}
                      </div>
                      <div className="pixel-heading text-base mt-1">{sec.title}</div>
                      <div className="mt-1 text-sm text-slate-300/80">{sec.description}</div>
                    </div>

                    <div className="flex items-center gap-2">
                      {done && (
                        <span className="text-xs px-3 py-1 rounded-full border border-[rgba(52,211,153,0.55)] bg-[rgba(52,211,153,0.12)] text-emerald-300 font-semibold">
                          Completed ✓
                        </span>
                      )}
                      {!unlocked && <span className="text-2xl">🔒</span>}
                      {unlocked && !done && (
                        <span className="text-xs px-3 py-1 rounded-full border border-[rgba(56,189,248,0.55)] bg-[rgba(56,189,248,0.12)] text-sky-300 font-semibold">
                          Active Quest
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Bayesian Knowledge Mastery Bar */}
                  {unlocked && (
                    <div className="mt-4 pt-3 border-t border-slate-700/50">
                      <div className="flex items-center justify-between text-xs text-slate-300/80">
                        <span>
                          Bayesian Skill Mastery: <strong className="text-sky-300">{pKnowPct}%</strong>
                        </span>
                        <span>
                          {attempts > 0 ? `${correct}/${attempts} correct` : "Unattempted"}
                        </span>
                      </div>
                      <div className="mt-1.5 h-2 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/40">
                        <div
                          className="h-full bg-gradient-to-r from-teal-400 via-sky-400 to-indigo-400 transition-all duration-500"
                          style={{ width: `${Math.max(5, pKnowPct)}%` }}
                        />
                      </div>
                    </div>
                  )}
                </button>
              </div>
            );
          })}

          {/* Final Boss Node */}
          <div className="relative pl-16 pb-4">
            <div
              className={`absolute left-4 w-5 h-5 rounded-full border-2 top-1 transition-all ${
                bossUnlocked
                  ? "bg-amber-400 border-amber-400 animate-pulse shadow-lg shadow-amber-500/50"
                  : "bg-[rgba(51,65,85,0.6)] border-[rgba(51,65,85,0.6)]"
              }`}
            />
            <button
              onClick={() => bossUnlocked && onFinalBoss()}
              disabled={!bossUnlocked}
              className={`w-full text-left rounded-3xl border-2 p-6 transition backdrop-blur-md ${
                bossUnlocked
                  ? "border-amber-400/80 bg-gradient-to-r from-amber-500/10 via-red-500/10 to-transparent hover:brightness-110 shadow-xl cursor-pointer ring-1 ring-amber-400/40"
                  : "border-[rgba(51,65,85,0.5)] bg-[rgba(15,23,42,0.4)] opacity-60 cursor-not-allowed"
              }`}
            >
              <div className="flex items-center justify-between gap-4 flex-wrap">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs tracking-widest uppercase font-bold text-amber-300 pixel">
                      CLIMAX BOSS ARENA
                    </span>
                    <span className="text-xs px-2 py-0.5 rounded-full border border-amber-500/50 bg-amber-500/20 text-amber-300 font-semibold">
                      +100 Bonus XP
                    </span>
                  </div>
                  <div className="pixel-heading text-xl text-amber-200 mt-1">
                    {chapter.finalBoss.title}
                  </div>
                  <div className="mt-1 text-sm text-slate-300/80">
                    {bossUnlocked
                      ? character && (character.id || character.name || "").toLowerCase().includes("batman") ? "Arkham Gatekeeper detected! Breach the mainframe using system equations." : character && (character.id || character.name || "").toLowerCase().includes("supergirl") ? "Brainiac Signal Array active! Bust through the frequency matrix with coordinate equations." : "LexCorp Quantum Warsuit Core online! Break Lex Luthor\u0027s defense matrix with algebraic precision!"
                      : "Complete prerequisite missions to crack the security barrier and confront the Boss!"}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  {bossUnlocked ? (
                    <div className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-red-500 text-slate-950 font-bold text-sm shadow-lg hover:scale-105 transition">
                      <span>ENTER ARENA</span>
                      <span className="text-lg">⚔️</span>
                    </div>
                  ) : (
                    <span className="text-3xl opacity-70">🔒</span>
                  )}
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      {/* Learn / Test Mission Modal */}
      {modal && (
        <div className="fixed inset-0 z-50">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setModal(null)} />
          <div className="absolute inset-0 flex items-center justify-center p-4">
            <div className="w-full max-w-md rounded-3xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgb(10,20,44)] shadow-2xl overflow-hidden animate-fadeIn">
              <div className="p-6 border-b border-[rgba(48,68,105,0.6)] bg-gradient-to-r from-[rgba(13,26,58,0.8)] to-transparent">
                <div className="text-xs text-sky-400 uppercase tracking-widest font-semibold pixel">
                  MISSION BRIEFING
                </div>
                <div className="pixel-heading text-lg mt-1">{modal.title}</div>
                <div className="mt-2 text-sm text-slate-300/80">{modal.description}</div>
              </div>
              <div className="p-6 space-y-3">
                <button
                  onClick={() => {
                    setModal(null);
                    onLearn(modal);
                  }}
                  className="w-full px-5 py-4 rounded-2xl border-2 border-[rgba(94,234,212,0.5)] bg-[rgba(94,234,212,0.08)] hover:bg-[rgba(94,234,212,0.16)] transition text-left group"
                >
                  <div className="flex items-center gap-4">
                    <span className="text-3xl group-hover:scale-110 transition">📖</span>
                    <div>
                      <div className="font-bold text-base text-slate-100">Learn Mission</div>
                      <div className="text-xs text-slate-300/80 mt-0.5">
                        {character.name} briefs you on concepts & formulas
                      </div>
                    </div>
                  </div>
                </button>
                <button
                  onClick={() => {
                    setModal(null);
                    onTest(modal);
                  }}
                  className="w-full px-5 py-4 rounded-2xl border-2 border-[rgba(56,189,248,0.5)] bg-[rgba(56,189,248,0.08)] hover:bg-[rgba(56,189,248,0.16)] transition text-left group"
                >
                  <div className="flex items-center gap-4">
                    <span className="text-3xl group-hover:scale-110 transition">⚔️</span>
                    <div>
                      <div className="font-bold text-base text-slate-100">Adaptive Quest</div>
                      <div className="text-xs text-slate-300/80 mt-0.5">
                        5 AI-adapted challenges powered by BKT
                      </div>
                    </div>
                  </div>
                </button>
              </div>
              <div className="p-4 border-t border-[rgba(48,68,105,0.6)] flex justify-end bg-slate-900/40">
                <button
                  onClick={() => setModal(null)}
                  className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] hover:bg-[rgba(13,26,58,0.85)] text-sm"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
