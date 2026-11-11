import { useEffect, useState } from "react";
import {
  generateQuestions,
  fetchTeacherQuestions,
  updateTeacherQuestion,
  approveTeacherQuestion,
  rejectTeacherQuestion,
} from "../utils/api";
import { chapters } from "../data/chapters";

// Every section (and each chapter's boss) a teacher can generate questions for.
const SECTION_OPTIONS = chapters.flatMap((ch) => [
  ...ch.sections.map((s) => ({ id: s.id, label: `${ch.title} — ${s.title}` })),
  ...(ch.finalBoss ? [{ id: ch.finalBoss.id, label: `${ch.title} — Final Boss` }] : []),
]);
const SECTION_TITLES = Object.fromEntries(SECTION_OPTIONS.map((s) => [s.id, s.label]));
const HEROES = ["batman", "superman", "supergirl"];
const KEYS = ["A", "B", "C", "D"];
const STATUSES = ["PENDING", "APPROVED", "REJECTED"];

const inputClass =
  "px-3 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500/60";
const panelClass = "rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5";

/** Teacher tool: generate questions with the LLM, then review (edit / approve / reject) them. */
export default function QuestionBankPanel() {
  const [form, setForm] = useState({ topic: SECTION_OPTIONS[0]?.id || "intro", difficulty: 1, characterTheme: "batman", count: 3 });
  const [generating, setGenerating] = useState(false);
  const [lastResult, setLastResult] = useState(null); // { created, rejected, model } | { error }
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetchTeacherQuestions(statusFilter)
      .then((data) => { if (!cancelled) setQuestions(data || []); })
      .catch(() => { if (!cancelled) setQuestions([]); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [statusFilter, reloadKey]);

  const refresh = () => setReloadKey((k) => k + 1);

  const changeFilter = (s) => {
    if (s === statusFilter) return;
    setLoading(true);
    setStatusFilter(s);
  };

  const submit = async (e) => {
    e.preventDefault();
    setGenerating(true);
    setLastResult(null);
    try {
      const res = await generateQuestions({ ...form, difficulty: Number(form.difficulty), count: Number(form.count) });
      setLastResult(res);
      if (statusFilter !== "PENDING") setLoading(true);
      setStatusFilter("PENDING");
      refresh();
    } catch (err) {
      setLastResult({ error: err.message });
    } finally {
      setGenerating(false);
    }
  };

  const review = async (action, q) => {
    try {
      if (action === "approve") await approveTeacherQuestion(q.id);
      else await rejectTeacherQuestion(q.id);
      refresh();
    } catch (err) {
      window.alert(err.message);
    }
  };

  return (
    <div className="space-y-8">
      {/* Generate */}
      <div className={panelClass}>
        <div className="pixel-heading text-xl">Generate Questions</div>
        <p className="mt-2 text-sm text-slate-400">
          AI drafts multiple-choice questions for a section. Each is checked automatically (format, duplicates, and an
          independent re-solve), then waits here for your review. Students only ever see questions you approve.
        </p>
        <form onSubmit={submit} className="mt-4 grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
          <label className="md:col-span-2 text-xs text-slate-400">
            Section
            <select className={`${inputClass} mt-1 w-full`} value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })}>
              {SECTION_OPTIONS.map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
            </select>
          </label>
          <label className="text-xs text-slate-400">
            Difficulty
            <select className={`${inputClass} mt-1 w-full`} value={form.difficulty} onChange={(e) => setForm({ ...form, difficulty: e.target.value })}>
              <option value={1}>1 — Basic</option>
              <option value={2}>2 — Application</option>
              <option value={3}>3 — Multi-step</option>
            </select>
          </label>
          <label className="text-xs text-slate-400">
            Hero
            <select className={`${inputClass} mt-1 w-full`} value={form.characterTheme} onChange={(e) => setForm({ ...form, characterTheme: e.target.value })}>
              {HEROES.map((h) => <option key={h} value={h}>{h[0].toUpperCase() + h.slice(1)}</option>)}
            </select>
          </label>
          <div className="flex gap-2">
            <label className="text-xs text-slate-400 w-20">
              Count
              <select className={`${inputClass} mt-1 w-full`} value={form.count} onChange={(e) => setForm({ ...form, count: e.target.value })}>
                {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
            </label>
            <button
              type="submit"
              disabled={generating}
              className="flex-1 self-end px-4 py-2 rounded-xl bg-[rgb(94,234,212)] text-slate-900 text-sm font-semibold hover:brightness-110 transition disabled:opacity-50"
            >
              {generating ? "Generating…" : "Generate"}
            </button>
          </div>
        </form>

        {lastResult?.error && <p className="mt-3 text-sm text-rose-300">{lastResult.error}</p>}
        {lastResult && !lastResult.error && (
          <div className="mt-3 text-sm">
            <p className="text-emerald-300">
              {lastResult.created.length} question{lastResult.created.length === 1 ? "" : "s"} ready for review
              <span className="text-slate-500"> · model: {lastResult.model}</span>
            </p>
            {lastResult.rejected.length > 0 && (
              <ul className="mt-1 text-amber-300/90 list-disc pl-5">
                {lastResult.rejected.map((r, i) => (
                  <li key={i}>Dropped: {r.reason}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* Review */}
      <div>
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="pixel-heading text-xl">My Generated Questions</div>
          <div className="flex gap-2">
            {STATUSES.map((s) => (
              <button
                key={s}
                onClick={() => changeFilter(s)}
                className={`px-3 py-1.5 rounded-xl border text-xs font-semibold transition ${
                  statusFilter === s
                    ? "border-sky-400/80 bg-sky-500/20 text-sky-200"
                    : "border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] text-slate-300 hover:bg-[rgba(13,26,58,0.85)]"
                }`}
              >
                {s[0] + s.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>

        {statusFilter === "PENDING" && questions.length > 0 && (
          <p className="mt-2 text-xs text-amber-300/80">AI-generated — check the maths and wording before approving.</p>
        )}

        <div className="mt-4 space-y-4">
          {loading ? (
            <p className="text-slate-400 text-sm">Loading…</p>
          ) : questions.length === 0 ? (
            <p className="text-slate-500 text-sm">No {statusFilter.toLowerCase()} questions.</p>
          ) : (
            questions.map((q) => (
              <QuestionCard key={q.id} q={q} onReview={review} onSaved={refresh} />
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function QuestionCard({ q, onReview, onSaved }) {
  const [editing, setEditing] = useState(false);
  const pending = q.status === "PENDING";

  if (editing) {
    return <QuestionEditor q={q} onCancel={() => setEditing(false)} onSaved={() => { setEditing(false); onSaved(); }} />;
  }

  return (
    <div className={panelClass}>
      <div className="flex flex-wrap items-center gap-2 text-xs">
        <span className="px-2 py-0.5 rounded-full bg-sky-500/15 text-sky-300 border border-sky-500/30">{SECTION_TITLES[q.topic] || q.topic}</span>
        <span className="px-2 py-0.5 rounded-full bg-slate-700/40 text-slate-300">Difficulty {q.difficulty}</span>
        <span className="px-2 py-0.5 rounded-full bg-slate-700/40 text-slate-300 capitalize">{q.characterTheme}</span>
        {q.generatorModel && <span className="text-slate-500">· {q.generatorModel}</span>}
      </div>
      <p className="mt-3 text-slate-100">{q.questionText}</p>
      <ul className="mt-3 space-y-1.5 text-sm">
        {KEYS.map((k) => {
          const correct = q.correctAnswer === k;
          return (
            <li key={k} className={correct ? "text-emerald-300" : "text-slate-300"}>
              <span className="font-mono">{k})</span> {q[`option${k}`]} {correct && <span className="font-semibold">✓ correct</span>}
              {!correct && q.misconceptions?.[k] && (
                <div className="ml-6 text-xs text-slate-500">Mistake: {q.misconceptions[k]}</div>
              )}
            </li>
          );
        })}
      </ul>
      <p className="mt-3 text-sm text-slate-400"><span className="text-slate-500">Explanation:</span> {q.explanation}</p>
      {pending && (
        <div className="mt-4 flex gap-2">
          <button onClick={() => onReview("approve", q)} className="px-3 py-1.5 rounded-lg border border-emerald-500/40 bg-emerald-500/10 hover:bg-emerald-500/25 text-emerald-300 text-xs font-semibold transition">
            Approve
          </button>
          <button onClick={() => setEditing(true)} className="px-3 py-1.5 rounded-lg border border-sky-500/40 bg-sky-500/10 hover:bg-sky-500/25 text-sky-300 text-xs font-semibold transition">
            Edit
          </button>
          <button onClick={() => onReview("reject", q)} className="px-3 py-1.5 rounded-lg border border-rose-500/40 bg-rose-500/10 hover:bg-rose-500/25 text-rose-300 text-xs font-semibold transition">
            Reject
          </button>
        </div>
      )}
    </div>
  );
}

function QuestionEditor({ q, onCancel, onSaved }) {
  const [draft, setDraft] = useState({
    questionText: q.questionText,
    optionA: q.optionA,
    optionB: q.optionB,
    optionC: q.optionC,
    optionD: q.optionD,
    correctAnswer: q.correctAnswer,
    explanation: q.explanation,
    difficulty: q.difficulty,
    misconceptions: { A: "", B: "", C: "", D: "", ...(q.misconceptions || {}) },
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await updateTeacherQuestion(q.id, { ...draft, difficulty: Number(draft.difficulty) });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={`${panelClass} space-y-3`}>
      <label className="block text-xs text-slate-400">
        Question
        <textarea className={`${inputClass} mt-1 w-full`} rows={3} value={draft.questionText}
          onChange={(e) => setDraft({ ...draft, questionText: e.target.value })} />
      </label>
      {KEYS.map((k) => (
        <div key={k} className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <label className="text-xs text-slate-400">
            Option {k}
            <input className={`${inputClass} mt-1 w-full`} value={draft[`option${k}`]}
              onChange={(e) => setDraft({ ...draft, [`option${k}`]: e.target.value })} />
          </label>
          <label className="text-xs text-slate-400">
            {draft.correctAnswer === k ? "Correct option (no mistake needed)" : `Mistake behind ${k}`}
            <input className={`${inputClass} mt-1 w-full`} disabled={draft.correctAnswer === k} value={draft.misconceptions[k] || ""}
              onChange={(e) => setDraft({ ...draft, misconceptions: { ...draft.misconceptions, [k]: e.target.value } })} />
          </label>
        </div>
      ))}
      <div className="grid grid-cols-2 gap-2">
        <label className="text-xs text-slate-400">
          Correct answer
          <select className={`${inputClass} mt-1 w-full`} value={draft.correctAnswer}
            onChange={(e) => setDraft({ ...draft, correctAnswer: e.target.value })}>
            {KEYS.map((k) => <option key={k} value={k}>{k}</option>)}
          </select>
        </label>
        <label className="text-xs text-slate-400">
          Difficulty
          <select className={`${inputClass} mt-1 w-full`} value={draft.difficulty}
            onChange={(e) => setDraft({ ...draft, difficulty: e.target.value })}>
            {[1, 2, 3].map((d) => <option key={d} value={d}>{d}</option>)}
          </select>
        </label>
      </div>
      <label className="block text-xs text-slate-400">
        Explanation
        <textarea className={`${inputClass} mt-1 w-full`} rows={3} value={draft.explanation}
          onChange={(e) => setDraft({ ...draft, explanation: e.target.value })} />
      </label>
      {error && <p className="text-sm text-rose-300">{error}</p>}
      <div className="flex gap-2">
        <button onClick={save} disabled={saving} className="px-4 py-2 rounded-xl bg-[rgb(94,234,212)] text-slate-900 text-sm font-semibold hover:brightness-110 transition disabled:opacity-50">
          {saving ? "Saving…" : "Save"}
        </button>
        <button onClick={onCancel} className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] text-slate-300 text-sm hover:bg-[rgba(13,26,58,0.85)] transition">
          Cancel
        </button>
      </div>
    </div>
  );
}
