import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import bg from "../assets/bg.gif";

export default function AuthPage({ onLoginSuccess, onBack }) {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("STUDENT"); // STUDENT | TEACHER | PARENT

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        if (!name.trim()) throw new Error("Name is required");
        await register(name.trim(), email.trim(), password, role);
      } else {
        await login(email.trim(), password);
      }
      if (onLoginSuccess) {
        onLoginSuccess();
      }
    } catch (err) {
      setError(err.message || "Authentication failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = async (demoEmail) => {
    setError(null);
    setEmail(demoEmail);
    setPassword("password123");
    setIsRegister(false);
    setLoading(true);
    try {
      await login(demoEmail, "password123");
      if (onLoginSuccess) {
        onLoginSuccess();
      }
    } catch (err) {
      setError(err.message || "Demo login failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen text-slate-100 flex items-center justify-center p-4">
      {/* Background with theme overlay */}
      <div className="fixed inset-0 -z-20 hero-bg-cover">
        <img src={bg} alt="background" className="w-full h-full object-cover" />
      </div>
      <div className="fixed inset-0 -z-10 bg-[linear-gradient(180deg,rgba(15,23,42,0.85)_0%,rgba(30,41,59,0.92)_100%)] backdrop-blur-sm" />

      {/* Auth Card */}
      <div className="w-full max-w-md rounded-3xl border-2 border-[rgba(51,65,85,0.85)] bg-[rgba(15,23,42,0.88)] p-8 shadow-2xl backdrop-blur-md relative">
        {onBack && (
          <button
            onClick={onBack}
            className="absolute top-6 left-6 text-xs px-3 py-1.5 rounded-xl border border-slate-700 bg-slate-800/80 hover:bg-slate-700 text-slate-300 transition"
          >
            ← Home
          </button>
        )}
        <div className="text-center pt-2">
          <div className="text-xs tracking-[0.3em] text-[rgb(94,234,212)] font-semibold">
            NEUROLEARN SYSTEM
          </div>
          <h1 className="pixel-heading mt-3 text-2xl md:text-3xl text-slate-100">
            {isRegister ? "Create Hero" : "Hero Login"}
          </h1>
          <p className="mt-2 text-sm text-slate-400">
            {isRegister
              ? "Join the adaptive learning quest"
              : "Enter your credentials to continue your adventure"}
          </p>
        </div>

        {/* Quick Demo Access Bar */}
        <div className="mt-6 p-3.5 rounded-2xl border border-sky-500/30 bg-sky-950/30">
          <div className="flex items-center justify-between text-xs font-semibold text-sky-300 mb-2">
            <span>⚡ Quick Demo Access</span>
            <span className="text-[10px] text-slate-400 font-normal">1-click test</span>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickDemo("bruce@neurolearn.com")}
              className="py-1.5 px-1 rounded-xl border border-teal-500/40 bg-teal-500/10 hover:bg-teal-500/25 text-teal-300 text-xs font-semibold transition disabled:opacity-50 text-center"
            >
              🦸 Student
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickDemo("teacher@neurolearn.com")}
              className="py-1.5 px-1 rounded-xl border border-sky-500/40 bg-sky-500/10 hover:bg-sky-500/25 text-sky-300 text-xs font-semibold transition disabled:opacity-50 text-center"
            >
              🖥 Teacher
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickDemo("parent@neurolearn.com")}
              className="py-1.5 px-1 rounded-xl border border-indigo-500/40 bg-indigo-500/10 hover:bg-indigo-500/25 text-indigo-300 text-xs font-semibold transition disabled:opacity-50 text-center"
            >
              👨‍👧 Parent
            </button>
          </div>
        </div>

        {error && (
          <div className="mt-4 p-3 rounded-xl border border-red-500/40 bg-red-500/10 text-red-300 text-sm text-center">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {isRegister && (
            <div>
              <label className="block text-xs uppercase tracking-wider text-slate-300 font-semibold mb-1">
                Name / Gamer Tag
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. PixelKnight"
                className="w-full px-4 py-3 rounded-xl bg-slate-800/80 border border-slate-700 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[rgb(94,234,212)] focus:ring-1 focus:ring-[rgb(94,234,212)] transition"
              />
            </div>
          )}

          <div>
            <label className="block text-xs uppercase tracking-wider text-slate-300 font-semibold mb-1">
              Email
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="hero@neurolearn.com"
              className="w-full px-4 py-3 rounded-xl bg-slate-800/80 border border-slate-700 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[rgb(94,234,212)] focus:ring-1 focus:ring-[rgb(94,234,212)] transition"
            />
          </div>

          <div>
            <label className="block text-xs uppercase tracking-wider text-slate-300 font-semibold mb-1">
              Password
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-4 py-3 rounded-xl bg-slate-800/80 border border-slate-700 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[rgb(94,234,212)] focus:ring-1 focus:ring-[rgb(94,234,212)] transition"
            />
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs uppercase tracking-wider text-slate-300 font-semibold mb-2">
                Choose Your Role
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: "STUDENT", label: "Student", desc: "Hero" },
                  { id: "PARENT", label: "Parent", desc: "Guide" },
                  { id: "TEACHER", label: "Teacher", desc: "Mentor" },
                ].map((r) => (
                  <button
                    key={r.id}
                    type="button"
                    onClick={() => setRole(r.id)}
                    className={`py-2 px-1 text-center rounded-xl border transition text-xs font-semibold ${
                      role === r.id
                        ? "border-[rgb(94,234,212)] bg-[rgba(94,234,212,0.15)] text-[rgb(94,234,212)] ring-1 ring-[rgb(94,234,212)]"
                        : "border-slate-700 bg-slate-800/60 text-slate-400 hover:border-slate-600 hover:text-slate-200"
                    }`}
                  >
                    <div>{r.label}</div>
                    <div className="text-[10px] opacity-70 font-normal">{r.desc}</div>
                  </button>
                ))}
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-3 rounded-xl bg-[rgb(94,234,212)] text-slate-900 font-semibold hover:brightness-110 transition shadow-lg disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
          >
            {loading ? (
              <span className="inline-block animate-pulse">Communicating with Base...</span>
            ) : isRegister ? (
              "Initialize Character →"
            ) : (
              "Launch Quest →"
            )}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-400">
          {isRegister ? (
            <p>
              Already registered?{" "}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(false);
                  setError(null);
                }}
                className="text-[rgb(94,234,212)] hover:underline font-semibold"
              >
                Log in here
              </button>
            </p>
          ) : (
            <p>
              Need a new player profile?{" "}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(true);
                  setError(null);
                }}
                className="text-[rgb(94,234,212)] hover:underline font-semibold"
              >
                Create an account
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
