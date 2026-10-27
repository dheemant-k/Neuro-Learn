import { useEffect, useRef, useState } from "react";
import { generateStory } from "../utils/api";
import { sound } from "../utils/soundEffects";
import boxImg from "../assets/box.png";

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

export default function LearnPage({ section, character, onGoTest, onBack }) {
  const [loading, setLoading] = useState(true);
  const [story, setStory] = useState(null);
  const [typed, setTyped] = useState("");
  const [doneTyping, setDoneTyping] = useState(false);
  const [speaking, setSpeaking] = useState(false);
  const timerRef = useRef(null);

  const heroName = character?.name || "Superman";
  const heroImg = character?.image;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setTyped("");
    setDoneTyping(false);

    generateStory({
      character: character,
      sectionTitle: section.title,
      sectionTopic: section.topic,
    })
      .then((data) => {
        if (!cancelled) {
          setStory(data);
          setLoading(false);
          startTypewriter(data.story || "");
        }
      })
      .catch(() => {
        if (!cancelled) {
          setStory({
            title: `${heroName}'s Quest`,
            story: `${heroName} is ready to guide you through ${section.title}! Let's master the coordinates.`,
            key_points: ["Practice makes perfect in linear equation systems."],
            examples: [],
          });
          setLoading(false);
          setDoneTyping(true);
          setTyped(`${heroName} is ready to guide you through ${section.title}! Let's master the coordinates.`);
        }
      });

    return () => {
      cancelled = true;
      if (timerRef.current) clearInterval(timerRef.current);
      speechSynthesis.cancel();
    };
  }, [section.id, character]);

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
      }
    }, 18);
  };

  const skipTypewriter = () => {
    sound.playClick();
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = null;
    setTyped(story?.story || "");
    setDoneTyping(true);
  };

  const handleSpeak = () => {
    sound.playClick();
    if (speaking) {
      speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }
    const text = story?.story || "";
    if (!text) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 0.95;
    utterance.onend = () => setSpeaking(false);
    utterance.onerror = () => setSpeaking(false);
    speechSynthesis.speak(utterance);
    setSpeaking(true);
  };

  return (
    <div className="min-h-screen text-slate-100">
      {/* Top Header */}
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-sky-500/20 bg-slate-950/85">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <button
            onClick={() => {
              sound.playClick();
              onBack();
            }}
            className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-900/60 hover:bg-slate-800 transition text-sm flex items-center gap-2"
          >
            <span>←</span>
            <span>Roadmap</span>
          </button>
          <div className="flex items-center gap-3">
            <span className="text-xs px-3 py-1 rounded-full border border-sky-500/30 bg-sky-500/10 text-sky-300 font-bold uppercase tracking-wider pixel">
              MISSION BRIEFING
            </span>
            <span className="text-sm font-semibold text-slate-300">{section.title}</span>
          </div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-10">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="pixel-heading text-xl text-sky-400 animate-pulse">
              {heroName} is decrypting mission coordinates...
            </div>
            <div className="mt-4 h-2 w-48 rounded-full bg-slate-800 overflow-hidden">
              <div className="h-2 w-1/3 bg-sky-400 animate-pulse" />
            </div>
          </div>
        ) : (
          <>
            {/* Mission Title Header */}
            <div className="flex items-center justify-between flex-wrap gap-4 border-b border-slate-800 pb-5">
              <div>
                <div className="text-xs text-sky-400 uppercase tracking-widest font-bold pixel">
                  TACTICAL ORIENTATION
                </div>
                <div className="pixel-heading text-2xl md:text-3xl text-slate-100 mt-1">
                  {story?.title || section.title}
                </div>
              </div>
              <span className="px-3 py-1 rounded-full bg-sky-500/15 border border-sky-400/40 text-sky-300 text-xs font-bold shadow-sm">
                🦸 {heroName.toUpperCase()} PROTOCOL
              </span>
            </div>

            {/* Hero + Speech Console */}
            <div className="mt-8 flex gap-5 items-start">
              {/* Character Avatar */}
              <div className="shrink-0 hidden md:flex flex-col items-center">
                <div className="w-24 h-24 rounded-2xl border-2 border-sky-400/40 bg-slate-900/90 p-1 shadow-xl relative group">
                  <img src={heroImg} alt={heroName} className="w-full h-full object-contain rounded-xl" />
                  <span className="absolute -bottom-1 -right-1 w-3 h-3 rounded-full bg-emerald-400 border-2 border-slate-950 animate-pulse" />
                </div>
                <div className="mt-2 text-center text-xs text-sky-300 font-bold">{heroName}</div>
                <div className="text-[10px] text-slate-400 font-mono">Mission Guide</div>
              </div>

              {/* Speech box console */}
              <div className="flex-1 relative min-h-[200px]">
                <div
                  className="relative rounded-3xl border-2 border-sky-500/30 overflow-hidden shadow-2xl bg-gradient-to-b from-slate-900/95 to-slate-950/95 backdrop-blur-xl"
                >
                  <div className="p-6 md:p-8">
                    {/* Mobile Character Header */}
                    <div className="md:hidden flex items-center gap-3 mb-4 pb-3 border-b border-slate-800">
                      <img src={heroImg} alt={heroName} className="w-12 h-12 object-contain rounded-xl border border-sky-400/30 bg-slate-900 p-0.5" />
                      <div>
                        <div className="font-bold text-sm text-sky-300">{heroName}</div>
                        <div className="text-xs text-slate-400 font-mono">Mission Briefing</div>
                      </div>
                    </div>

                    <div
                      className="text-base md:text-lg text-slate-100 leading-relaxed font-sans"
                      onClick={!doneTyping ? skipTypewriter : undefined}
                    >
                      {formatTextWithEquations(typed)}
                      {!doneTyping && <span className="tw-caret" />}
                    </div>

                    {!doneTyping && (
                      <div className="mt-4 text-xs text-slate-400/80 flex items-center gap-1.5 cursor-pointer hover:text-sky-300 transition" onClick={skipTypewriter}>
                        <span>⚡</span>
                        <span>Click console to skip typing animation</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Key Concept Points */}
            {story?.key_points?.length > 0 && doneTyping && (
              <div className="mt-8 rounded-3xl border-2 border-sky-500/30 bg-gradient-to-r from-sky-950/30 to-indigo-950/30 p-6 md:p-7 shadow-xl animate-fadeIn">
                <div className="flex items-center gap-2 text-xs font-bold text-sky-400 uppercase tracking-widest pixel mb-3">
                  <span>💎</span>
                  <span>Core Mathematical Foundations</span>
                </div>
                <ul className="space-y-2.5">
                  {story.key_points.map((p, i) => (
                    <li key={i} className="flex items-start gap-3 text-slate-200 text-sm md:text-base leading-relaxed">
                      <span className="w-2 h-2 rounded-full bg-sky-400 mt-2 shrink-0 shadow-sm" />
                      <span>{formatTextWithEquations(p)}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Tactical Examples Pod */}
            {story?.examples?.length > 0 && doneTyping && (
              <div className="mt-6 space-y-4 animate-fadeIn">
                <div className="text-xs font-bold text-amber-400 uppercase tracking-widest pixel px-1">
                  Tactical Verification Examples
                </div>
                {story.examples.map((ex, i) => (
                  <div key={i} className="rounded-2xl border-2 border-slate-800 bg-slate-900/80 p-5 shadow-lg">
                    <div className="font-bold text-amber-300 text-xs uppercase tracking-wider flex items-center gap-1.5">
                      <span>🎯</span>
                      <span>Example 0{i + 1}</span>
                    </div>
                    <div className="mt-2 text-slate-100 font-medium text-sm md:text-base">
                      {formatTextWithEquations(ex.problem)}
                    </div>
                    <div className="mt-2.5 pt-2.5 border-t border-slate-800/80 text-sm text-sky-300/90 font-mono">
                      {formatTextWithEquations(ex.solution)}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Action Bar */}
            <div className="mt-9 flex items-center justify-between flex-wrap gap-4 pt-6 border-t border-slate-800">
              <button
                onClick={handleSpeak}
                className="px-5 py-3 rounded-xl border border-slate-700 bg-slate-900/60 hover:bg-slate-800 text-slate-300 transition text-sm flex items-center gap-2"
              >
                <span>{speaking ? "🔇 Stop Speech" : "🔊 Audio Readout"}</span>
              </button>

              <button
                onClick={() => {
                  sound.playClick();
                  onGoTest();
                }}
                className="px-8 py-3.5 rounded-xl bg-gradient-to-r from-sky-400 to-blue-500 text-slate-950 font-bold hover:brightness-110 transition shadow-xl shadow-sky-500/20 flex items-center gap-2 text-sm"
              >
                <span>Engage Adaptive Test</span>
                <span>→</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
