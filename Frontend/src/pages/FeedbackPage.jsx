import { useEffect, useRef, useState } from "react";
import { generateFeedback } from "../utils/api";
import boxImg from "../assets/box.png";

// Character-aware theme config
function getCharacterTheme(charName = "") {
  const n = charName.toLowerCase();
  if (n.includes("batman"))
    return {
      accent: "rgb(56,189,248)",
      accentBorder: "rgba(56,189,248,0.45)",
      label: "Gotham Archives Decrypted",
      continueLabel: "Return to Gotham Roadmap \u2192",
    };
  if (n.includes("supergirl"))
    return {
      accent: "rgb(196,181,253)",
      accentBorder: "rgba(196,181,253,0.45)",
      label: "DEO Mission Debrief",
      continueLabel: "Back to National City Roadmap \u2192",
    };
  // Superman default
  return {
    accent: "rgb(94,234,212)",
    accentBorder: "rgba(94,234,212,0.45)",
    label: "LexCorp Signal Decrypted",
    continueLabel: "Return to Metropolis Roadmap \u2192",
  };
}

export default function FeedbackPage({ section, character, results, onContinue }) {
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState(null);
  const [typed, setTyped] = useState("");
  const [doneTyping, setDoneTyping] = useState(false);
  const [xpShown, setXpShown] = useState(0);
  const [showDetails, setShowDetails] = useState(false);
  const timerRef = useRef(null);

  const pct = Math.round((results.totalCorrect / Math.max(1, results.totalQuestions)) * 100);
  const theme = getCharacterTheme(character?.name || character?.id || "");

  const ratingLabel = pct >= 80 ? "excellent" : pct >= 50 ? "good" : "keep_trying";
  const ratingEmoji = ratingLabel === "excellent" ? "\uD83C\uDFC6" : ratingLabel === "good" ? "\u2B50" : "\uD83D\uDCAA";
  const ratingText =
    ratingLabel === "excellent"
      ? "MISSION COMPLETE"
      : ratingLabel === "good"
      ? "OBJECTIVE SECURED"
      : "REGROUP & RETRY";

  useEffect(() => {
    let cancelled = false;
    generateFeedback({
      character: character?.name,
      totalCorrect: results.totalCorrect,
      totalQuestions: results.totalQuestions,
      sectionTitle: section.title,
    })
      .then((data) => {
        if (!cancelled) {
          setFeedback(data);
          setLoading(false);
          startTypewriter(data.message || "Great job!");
          animateXp(data.xp_earned || results.totalCorrect * 20);
        }
      })
      .catch(() => {
        if (!cancelled) {
          const xp = results.totalCorrect * 20;
          const fallback = {
            message: `You scored ${results.totalCorrect}/${results.totalQuestions}! Keep going!`,
            tip: "Review the questions you got wrong.",
            xp_earned: xp,
          };
          setFeedback(fallback);
          setLoading(false);
          startTypewriter(fallback.message);
          animateXp(xp);
        }
      });

    return () => {
      cancelled = true;
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  const startTypewriter = (text) => {
    if (timerRef.current) clearInterval(timerRef.current);
    setTyped("");
    setDoneTyping(false);
    let i = 0;
    timerRef.current = setInterval(() => {
      i += 2;
      setTyped(text.slice(0, i));
      if (i >= text.length) {
        clearInterval(timerRef.current);
        timerRef.current = null;
        setDoneTyping(true);
        setTimeout(() => setShowDetails(true), 400);
      }
    }, 22);
  };

  const animateXp = (target) => {
    let current = 0;
    const step = Math.ceil(target / 30);
    const iv = setInterval(() => {
      current = Math.min(current + step, target);
      setXpShown(current);
      if (current >= target) clearInterval(iv);
    }, 40);
  };

  return (
    <div className="min-h-screen text-slate-100 flex items-center justify-center">
      <div className="max-w-2xl w-full mx-auto px-6 py-10">
        {loading ? (
          <div className="flex flex-col items-center py-20 gap-4">
            <img
              src={character?.image}
              alt={character?.name}
              className="w-20 h-20 object-contain opacity-80 animate-pulse"
            />
            <div className="pixel-heading text-lg text-slate-300">
              {character?.name} is analysing your performance&hellip;
            </div>
            <div className="h-1.5 w-56 rounded-full bg-white/10 overflow-hidden">
              <div
                className="h-full rounded-full animate-pulse"
                style={{ width: "45%", background: theme.accent }}
              />
            </div>
          </div>
        ) : (
          <div className="space-y-5">
            {/* ── Mission status card ── */}
            <div
              className="rounded-3xl border-2 bg-[rgba(10,20,44,0.82)] shadow-2xl overflow-hidden"
              style={{ borderColor: theme.accentBorder }}
            >
              {/* Accent top stripe */}
              <div
                className="h-1 w-full"
                style={{ background: `linear-gradient(to right, ${theme.accent}88, transparent)` }}
              />
              <div className="p-8 text-center">
                <span className="text-5xl">{ratingEmoji}</span>
                <div
                  className="text-[10px] tracking-[0.2em] font-bold uppercase mt-3"
                  style={{ color: theme.accent }}
                >
                  {theme.label}
                </div>
                <div className="pixel-heading text-2xl mt-1">{ratingText}</div>
                <div className="mt-1 text-slate-300/70 text-sm">{section.title}</div>

                {/* Stats grid */}
                <div className="mt-6 grid grid-cols-3 gap-3">
                  {[
                    { label: "Score", value: `${results.totalCorrect}/${results.totalQuestions}`, color: theme.accent },
                    { label: "XP Earned", value: `+${xpShown}`, color: "rgb(250,204,21)" },
                    { label: "Accuracy", value: `${pct}%`, color: "rgb(248,250,252)" },
                  ].map(({ label, value, color }) => (
                    <div
                      key={label}
                      className="rounded-2xl bg-white/5 border border-white/10 p-4"
                    >
                      <div className="text-2xl font-bold" style={{ color }}>{value}</div>
                      <div className="text-[10px] tracking-widest uppercase text-slate-400 mt-1">{label}</div>
                    </div>
                  ))}
                </div>

                {/* Accuracy bar */}
                <div className="mt-5">
                  <div className="flex justify-between text-[10px] text-slate-400 mb-1.5 uppercase tracking-widest">
                    <span>Mission Mastery</span><span>{pct}%</span>
                  </div>
                  <div className="h-2.5 w-full rounded-full bg-white/10 overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-1000"
                      style={{ width: `${pct}%`, background: `linear-gradient(to right, ${theme.accent}, ${theme.accent}99)` }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* ── Character speech bubble ── */}
            <div className="flex gap-4 items-start">
              <div className="shrink-0 hidden md:flex flex-col items-center gap-2">
                <img
                  src={character?.image}
                  alt={character?.name}
                  className="w-20 h-20 object-contain rounded-2xl ring-2"
                  style={{ boxShadow: `0 0 18px ${theme.accent}55`, ringColor: theme.accent }}
                />
                <span
                  className="text-[10px] uppercase tracking-widest font-semibold"
                  style={{ color: theme.accent }}
                >
                  {character?.name}
                </span>
              </div>
              <div
                className="flex-1 relative rounded-2xl border-2 overflow-hidden"
                style={{
                  borderColor: theme.accentBorder,
                  backgroundImage: `url(${boxImg})`,
                  backgroundSize: "cover",
                  backgroundPosition: "center",
                }}
              >
                <div className="absolute inset-0 bg-[rgba(10,20,44,0.88)]" />
                {/* Speech pointer */}
                <div
                  className="absolute -left-3 top-5 w-0 h-0 hidden md:block"
                  style={{
                    borderTop: "8px solid transparent",
                    borderBottom: "8px solid transparent",
                    borderRight: `12px solid ${theme.accentBorder}`,
                  }}
                />
                <div className="relative p-5">
                  {/* Mobile header */}
                  <div className="md:hidden flex items-center gap-3 mb-3">
                    <img src={character?.image} alt={character?.name} className="w-9 h-9 object-contain rounded-xl" />
                    <span className="font-semibold text-sm" style={{ color: theme.accent }}>
                      {character?.name}
                    </span>
                  </div>
                  <div className="text-sm text-slate-100/95 leading-relaxed font-medium">
                    {typed}
                    {!doneTyping && (
                      <span
                        className="inline-block w-0.5 h-4 ml-0.5 animate-pulse rounded-sm align-middle"
                        style={{ background: theme.accent }}
                      />
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* ── Tactical tip (shows after typewriter) ── */}
            {showDetails && feedback?.tip && (
              <div
                className="rounded-2xl border-2 p-5"
                style={{ borderColor: "rgba(250,204,21,0.35)", background: "rgba(250,204,21,0.04)" }}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-lg">\uD83D\uDCA1</span>
                  <span className="font-bold text-yellow-300 text-sm uppercase tracking-widest">Tactical Tip</span>
                </div>
                <div className="text-slate-200/90 text-sm leading-relaxed">{feedback.tip}</div>
              </div>
            )}

            {/* ── Key formula chips ── */}
            {showDetails && (
              <div
                className="rounded-2xl border p-4 flex flex-wrap gap-2 items-center"
                style={{ borderColor: "rgba(148,163,184,0.15)", background: "rgba(15,23,42,0.6)" }}
              >
                <span className="text-[10px] uppercase tracking-widest text-slate-400 mr-1">Key Formulae</span>
                {["ax + by = c", "y=0 \u2192 x-int", "x=0 \u2192 y-int", "elimination", "substitution"].map((f) => (
                  <span
                    key={f}
                    className="mx-1 px-2.5 py-0.5 rounded-lg text-xs font-mono"
                    style={{
                      background: "rgba(2,6,23,0.9)",
                      border: `1px solid ${theme.accentBorder}`,
                      color: "rgb(252,211,77)",
                    }}
                  >
                    {f}
                  </span>
                ))}
              </div>
            )}

            {/* ── Continue CTA ── */}
            {showDetails && (
              <div className="text-center pt-1">
                <button
                  onClick={onContinue}
                  className="px-10 py-3.5 rounded-2xl font-bold text-slate-950 shadow-xl hover:brightness-110 hover:scale-105 transition-all duration-200"
                  style={{ background: `linear-gradient(135deg, ${theme.accent}, ${theme.accent}cc)` }}
                >
                  {theme.continueLabel}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
