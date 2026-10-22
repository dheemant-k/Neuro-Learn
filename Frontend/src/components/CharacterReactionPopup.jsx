import { useEffect, useRef, useState } from "react";
import lexluthorImg from "../assets/lexluthor.png";

// Typewriter hook
function useTypewriter(text, speed = 28) {
  const [displayed, setDisplayed] = useState("");
  const indexRef = useRef(0);
  const timerRef = useRef(null);

  useEffect(() => {
    setDisplayed("");
    indexRef.current = 0;
    clearInterval(timerRef.current);
    if (!text) return;
    timerRef.current = setInterval(() => {
      indexRef.current += 1;
      setDisplayed(text.slice(0, indexRef.current));
      if (indexRef.current >= text.length) clearInterval(timerRef.current);
    }, speed);
    return () => clearInterval(timerRef.current);
  }, [text, speed]);

  return displayed;
}

// Particle burst component
function ParticleBurst({ active, color }) {
  const particles = Array.from({ length: 12 }, (_, i) => i);
  if (!active) return null;
  return (
    <div className="absolute inset-0 pointer-events-none overflow-hidden rounded-3xl">
      {particles.map((i) => {
        const angle = (i / particles.length) * 360;
        const dist = 40 + Math.random() * 30;
        return (
          <div
            key={i}
            className="absolute w-1.5 h-1.5 rounded-full"
            style={{
              background: color,
              top: "50%",
              left: "50%",
              animation: `particleFly${i % 4} 0.6s ease-out forwards`,
              transform: `rotate(${angle}deg) translateX(${dist}px)`,
              opacity: 0,
            }}
          />
        );
      })}
    </div>
  );
}

export default function CharacterReactionPopup({
  hero,
  state, // "idle" | "correct" | "wrong"
  streak = 0,
}) {
  const [bubbleText, setBubbleText] = useState("");
  const [activeSide, setActiveSide] = useState(null); // "hero" | "villain" | null
  const [burstHero, setBurstHero] = useState(false);
  const [burstVillain, setBurstVillain] = useState(false);
  const dismissRef = useRef(null);

  const heroName = hero?.name || "Superman";
  const heroImg = hero?.image;

  const heroCorrectQuotes = [
    "Direct hit! That coordinate vector balanced perfectly!",
    "Solar Core surging! You decrypted the variable!",
    "Linear equation conquered! That's true hero math!",
    "Flawless! LexCorp's scrambler is destabilized!",
    "Exactly right! Keep this speed and focus alive!",
  ];
  const heroStreakQuotes = [
    `UNSTOPPABLE! ${streak}x Combo! Metropolis is rallying!`,
    `DOUBLE IMPACT! ${streak} consecutive breaches!`,
    `SUPER SONIC ACCURACY! ${streak} in a row! The city is safe!`,
  ];
  const lexTaunts = [
    "Did you drop a minus sign? My firewall absorbed that!",
    "Miscalculated! Linear systems need precision!",
    "Did you rush the substitution? LexCorp Grid: 1, Hero: 0!",
    "Mathematically inaccurate! Check your coefficients!",
  ];
  const lexIdleQuotes = [
    "Your calculations won't breach my quantum grid...",
    "LexCorp's firewall stands at 100%. Try your best.",
    "Every equation you miss feeds my power supply.",
  ];
  const heroIdleQuotes = [
    "Focus. Metropolis is counting on you.",
    "Take your time — solve it step by step.",
    "I believe in you. Break through.",
  ];

  // Auto-dismiss bubble after 4s
  useEffect(() => {
    clearTimeout(dismissRef.current);
    if (state === "correct") {
      setActiveSide("hero");
      setBurstHero(true);
      setTimeout(() => setBurstHero(false), 700);
      const quotes = streak >= 2 ? heroStreakQuotes : heroCorrectQuotes;
      setBubbleText(quotes[Math.floor(Math.random() * quotes.length)]);
      dismissRef.current = setTimeout(() => {
        setActiveSide(null);
        setBubbleText("");
      }, 4500);
    } else if (state === "wrong") {
      setActiveSide("villain");
      setBurstVillain(true);
      setTimeout(() => setBurstVillain(false), 700);
      setBubbleText(lexTaunts[Math.floor(Math.random() * lexTaunts.length)]);
      dismissRef.current = setTimeout(() => {
        setActiveSide(null);
        setBubbleText("");
      }, 4500);
    } else {
      setActiveSide(null);
      setBubbleText("");
    }
    return () => clearTimeout(dismissRef.current);
  }, [state, streak]);

  const heroIdle = heroIdleQuotes[Math.floor(Math.random() * heroIdleQuotes.length)];
  const lexIdle = lexIdleQuotes[Math.floor(Math.random() * lexIdleQuotes.length)];

  const displayHero = activeSide === "hero" ? bubbleText : heroIdle;
  const displayLex = activeSide === "villain" ? bubbleText : lexIdle;

  const typedHero = useTypewriter(displayHero, 22);
  const typedLex = useTypewriter(displayLex, 22);

  return (
    <>
      {/* LEFT: Superman Character Panel */}
      <aside
        className="character-panel character-panel-left"
        aria-label="Superman panel"
        data-active={activeSide === "hero"}
      >
        {/* Glow ring */}
        <div className={`char-avatar-ring ${activeSide === "hero" ? "ring-active-hero" : ""}`}>
          <div className="char-burst-container">
            <ParticleBurst active={burstHero} color="#38bdf8" />
            {heroImg ? (
              <img
                src={heroImg}
                alt={heroName}
                className="char-avatar-img"
              />
            ) : (
              <div className="char-avatar-fallback char-avatar-fallback-hero">S</div>
            )}
            {activeSide === "hero" && streak >= 2 && (
              <div className="streak-badge animate-bounce">
                🔥 {streak}x
              </div>
            )}
          </div>
        </div>

        {/* Name tag */}
        <div className={`char-name-tag ${activeSide === "hero" ? "char-name-tag-hero-active" : "char-name-tag-hero"}`}>
          <span className="char-name-icon">{activeSide === "hero" && streak >= 2 ? "⚡" : "🦸"}</span>
          <span>{heroName}</span>
        </div>

        {/* Speech bubble */}
        <div className={`speech-bubble speech-bubble-hero ${activeSide === "hero" ? "speech-bubble-active-hero" : ""}`}>
          <div className="speech-bubble-tail speech-bubble-tail-left" />
          <p className="speech-bubble-text">
            {typedHero}
            {typedHero.length < displayHero.length && (
              <span className="tw-caret" />
            )}
          </p>
          {activeSide === "hero" && (
            <div className="speech-bubble-footer speech-bubble-footer-hero">
              {streak >= 2 ? "🔥 Combo Strike!" : "✓ Correct!"}
            </div>
          )}
        </div>
      </aside>

      {/* RIGHT: Lex Luthor Character Panel */}
      <aside
        className="character-panel character-panel-right"
        aria-label="Lex Luthor panel"
        data-active={activeSide === "villain"}
      >
        {/* Speech bubble (on right, tails point right) */}
        <div className={`speech-bubble speech-bubble-villain ${activeSide === "villain" ? "speech-bubble-active-villain" : ""}`}>
          <div className="speech-bubble-tail speech-bubble-tail-right" />
          <p className="speech-bubble-text">
            {typedLex}
            {typedLex.length < displayLex.length && (
              <span className="tw-caret" style={{ background: "#f87171" }} />
            )}
          </p>
          {activeSide === "villain" && (
            <div className="speech-bubble-footer speech-bubble-footer-villain">
              ✗ Deflected!
            </div>
          )}
          {activeSide !== "villain" && (
            <div className="speech-bubble-footer speech-bubble-footer-villain" style={{ opacity: 0.5 }}>
              🔒 Firewall Active
            </div>
          )}
        </div>

        {/* Name tag */}
        <div className={`char-name-tag ${activeSide === "villain" ? "char-name-tag-villain-active" : "char-name-tag-villain"}`}>
          <span className="char-name-icon">🏢</span>
          <span>Lex Luthor</span>
        </div>

        {/* Glow ring */}
        <div className={`char-avatar-ring ${activeSide === "villain" ? "ring-active-villain" : ""}`}>
          <div className="char-burst-container">
            <ParticleBurst active={burstVillain} color="#f87171" />
            <img
              src={lexluthorImg}
              alt="Lex Luthor"
              className="char-avatar-img"
            />
            {activeSide === "villain" && (
              <div className="streak-badge streak-badge-villain animate-bounce">
                💢
              </div>
            )}
          </div>
        </div>
      </aside>
    </>
  );
}
