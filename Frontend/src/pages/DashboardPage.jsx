import { useEffect, useState } from "react";
import { getDashboard, fetchTeacherAnalytics, fetchTeacherStudentDashboard, linkChild, fetchParentChildren, fetchChildReportCard } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import { chapters } from "../data/chapters";

export default function DashboardPage({ onBack }) {
  const { user } = useAuth();
  const role = user?.role || "STUDENT";

  return (
    <div className="min-h-screen text-slate-100">
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-white/[0.06] bg-[rgba(15,23,42,0.82)]">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <button onClick={onBack} className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] hover:bg-[rgba(13,26,58,0.85)] transition text-sm">
            ← Home
          </button>
          <div className="pixel-heading text-base">
            {role === "TEACHER" ? "🖥 Teacher Command Center" : role === "PARENT" ? "👨‍👧 Parent Portal" : "📊 Student Dashboard"}
          </div>
          <div className="text-xs text-slate-400">{user?.name}</div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-10">
        {role === "TEACHER" && <TeacherDashboard />}
        {role === "PARENT" && <ParentDashboard />}
        {role === "STUDENT" && <StudentDashboard userEmail={user?.email} />}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────
// STUDENT VIEW
// ─────────────────────────────────────────
function StudentDashboard({ userEmail }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const sectionTitleMap = {};
  chapters.forEach((ch) => ch.sections.forEach((s) => { sectionTitleMap[s.id] = s.title; }));

  useEffect(() => {
    getDashboard().then(setData).catch(() => setData(null)).finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner label="Loading your stats..." />;
  if (!data || data.total_answered === 0) return (
    <div className="text-center py-20">
      <div className="text-4xl mb-4">📊</div>
      <div className="pixel-heading text-xl">No Data Yet</div>
      <p className="mt-2 text-slate-300/80">Complete your first quiz to see your stats here.</p>
    </div>
  );

  return (
    <>
      <div className="pixel-heading text-2xl">Your Learning Overview</div>
      <div className="mt-6 grid grid-cols-2 md:grid-cols-5 gap-4">
        <StatCard label="Total XP" value={data.total_xp} color="text-[rgb(250,204,21)]" />
        <StatCard label="Level" value={data.level} color="text-[rgb(56,189,248)]" />
        <StatCard label="Questions" value={`${data.total_correct}/${data.total_answered}`} color="text-[rgb(94,234,212)]" />
        <StatCard label="Accuracy" value={`${data.overall_accuracy}%`} color="text-white" />
        <StatCard label="Bayesian P(L)" value={`${Math.round((data.overall_p_know ?? 0.20) * 100)}%`} color="text-indigo-400" />
      </div>

      <div className="mt-10">
        <div className="pixel-heading text-xl">Bayesian Section Mastery</div>
        <div className="mt-4 space-y-4">
          {data.sections.map((sec) => {
            const bktPct = Math.round((sec.p_know ?? 0.20) * 100);
            const isMastered = sec.is_mastered || bktPct >= 85;
            return (
              <div key={sec.section_id} className="rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-lg">{sectionTitleMap[sec.section_id] || sec.section_id}</span>
                      {isMastered && <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">Mastered 🏆</span>}
                    </div>
                    <div className="text-sm text-slate-300/80 mt-1">{sec.correct}/{sec.answered} correct • {sec.accuracy}% raw accuracy</div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-slate-400">Bayesian Knowledge State</div>
                    <div className="text-2xl font-bold text-sky-400">{bktPct}% <span className="text-xs font-normal text-slate-400">P(L)</span></div>
                  </div>
                </div>
                <div className="mt-3">
                  <div className="h-2.5 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/50">
                    <div className="h-full bg-gradient-to-r from-teal-400 via-sky-400 to-indigo-500 transition-all duration-500" style={{ width: `${Math.max(5, bktPct)}%` }} />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {data.recent_history?.length > 0 && (
        <div className="mt-10">
          <div className="pixel-heading text-xl">Recent Activity</div>
          <div className="mt-4 rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5">
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {data.recent_history.slice().reverse().map((h, i) => (
                <div key={i} className="flex items-center gap-3 text-sm">
                  <span className={`w-2.5 h-2.5 rounded-full shrink-0 ${h.correct ? "bg-emerald-400" : "bg-rose-400"}`} />
                  <span className="text-slate-300/80">{sectionTitleMap[h.section_id] || h.section_id}</span>
                  <span className="text-slate-400/60">•</span>
                  <span className="text-slate-400/70">{h.rt?.toFixed(1)}s</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ─────────────────────────────────────────
// TEACHER VIEW
// ─────────────────────────────────────────
function TeacherDashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [inspecting, setInspecting] = useState(null); // studentProfileId being inspected
  const [studentDash, setStudentDash] = useState(null);
  const [studentName, setStudentName] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [loadingStudent, setLoadingStudent] = useState(false);

  const sectionTitleMap = {};
  chapters.forEach((ch) => ch.sections.forEach((s) => { sectionTitleMap[s.id] = s.title; }));

  useEffect(() => {
    fetchTeacherAnalytics().then(setAnalytics).catch(() => setAnalytics(null)).finally(() => setLoading(false));
  }, []);

  const handleInspect = (student) => {
    setInspecting(student.studentProfileId);
    setStudentName(student.name);
    setStudentDash(null);
    setLoadingStudent(true);
    fetchTeacherStudentDashboard(student.studentProfileId)
      .then(setStudentDash)
      .catch(() => setStudentDash(null))
      .finally(() => setLoadingStudent(false));
  };

  if (loading) return <LoadingSpinner label="Loading class analytics..." />;
  if (!analytics) return <div className="text-center py-20 text-rose-400">Failed to load analytics.</div>;

  const filteredRoster = (analytics.studentRoster || []).filter(s =>
    s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <>
      {/* Class overview cards */}
      <div className="pixel-heading text-2xl">Class Overview</div>
      <div className="mt-6 grid grid-cols-2 md:grid-cols-5 gap-4">
        <StatCard label="Total Students" value={analytics.totalStudents} color="text-sky-400" />
        <StatCard label="Avg XP" value={Math.round(analytics.classAvgXp)} color="text-yellow-400" />
        <StatCard label="Avg Accuracy" value={`${analytics.classAvgAccuracy}%`} color="text-white" />
        <StatCard label="Class P(L)" value={`${Math.round((analytics.classAvgPKnow ?? 0.20) * 100)}%`} color="text-indigo-400" />
        <StatCard label="Total Attempts" value={analytics.totalQuestionsAttempted} color="text-teal-400" />
      </div>

      {/* Curriculum Bottlenecks */}
      <div className="mt-10">
        <div className="pixel-heading text-xl">Curriculum Bottleneck Analysis</div>
        <div className="mt-4 space-y-3">
          {(analytics.topicBottlenecks || []).map((tb, i) => (
            <div key={i} className={`rounded-2xl border-2 p-4 ${tb.bottleneckAlert ? "border-rose-500/60 bg-rose-950/30" : "border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)]"}`}>
              <div className="flex items-center justify-between flex-wrap gap-2">
                <div className="flex items-center gap-2">
                  {tb.bottleneckAlert && <span className="text-rose-400 text-lg">⚠️</span>}
                  <span className="font-semibold">{sectionTitleMap[tb.topic] || tb.topic}</span>
                </div>
                <div className="flex gap-4 text-sm">
                  <span className="text-slate-400">Class P(L): <span className="text-sky-400 font-bold">{Math.round((tb.avgPKnow ?? 0.20) * 100)}%</span></span>
                  <span className="text-slate-400">Accuracy: <span className="text-white font-bold">{tb.avgAccuracy?.toFixed(1)}%</span></span>
                  <span className="text-emerald-400">✓ {tb.studentsMastered} mastered</span>
                  <span className="text-rose-400">✗ {tb.studentsStruggling} struggling</span>
                </div>
              </div>
              <p className="mt-2 text-sm text-slate-300/80">{tb.recommendation}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Student Roster */}
      <div className="mt-10">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="pixel-heading text-xl">Student Roster</div>
          <input
            type="text"
            placeholder="Search by name or email..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="px-3 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500/60 w-56"
          />
        </div>
        <div className="mt-4 rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-700/50 text-slate-400 text-xs uppercase">
                  <th className="px-4 py-3 text-left">Student</th>
                  <th className="px-4 py-3 text-center">Level</th>
                  <th className="px-4 py-3 text-center">XP</th>
                  <th className="px-4 py-3 text-center">Accuracy</th>
                  <th className="px-4 py-3 text-center">P(L)</th>
                  <th className="px-4 py-3 text-center">Attempts</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-4 py-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredRoster.length === 0 ? (
                  <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-500">No students found.</td></tr>
                ) : filteredRoster.map((s) => (
                  <tr key={s.studentProfileId} className="border-b border-slate-800/50 hover:bg-slate-800/30 transition">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-200">{s.name}</div>
                      <div className="text-xs text-slate-500">{s.email}</div>
                    </td>
                    <td className="px-4 py-3 text-center text-sky-400 font-bold">{s.level}</td>
                    <td className="px-4 py-3 text-center text-yellow-400 font-bold">{s.totalXp}</td>
                    <td className="px-4 py-3 text-center text-white">{s.accuracy?.toFixed(1)}%</td>
                    <td className="px-4 py-3 text-center text-indigo-400 font-bold">{Math.round((s.pKnow ?? 0.20) * 100)}%</td>
                    <td className="px-4 py-3 text-center text-slate-300">{s.questionsAttempted}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-bold uppercase ${
                        s.status === "Mastered" ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40" :
                        s.status === "On Track" ? "bg-sky-500/20 text-sky-300 border border-sky-500/40" :
                        "bg-rose-500/20 text-rose-300 border border-rose-500/40"
                      }`}>{s.status}</span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => handleInspect(s)}
                        className="px-3 py-1 rounded-lg border border-sky-500/40 bg-sky-500/10 hover:bg-sky-500/25 text-sky-300 text-xs transition"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Student Drill-Down Modal */}
      {inspecting && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm px-4">
          <div className="w-full max-w-2xl rounded-2xl border-2 border-sky-500/40 bg-[rgba(13,26,58,0.97)] p-6 max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <div className="pixel-heading text-lg">🔍 Inspecting: {studentName}</div>
              <button onClick={() => { setInspecting(null); setStudentDash(null); }} className="text-slate-400 hover:text-white text-xl">✕</button>
            </div>
            {loadingStudent ? <LoadingSpinner label="Loading student data..." /> : studentDash ? (
              <StudentDashInspector data={studentDash} />
            ) : (
              <div className="text-rose-400 text-center py-8">No data available for this student.</div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function StudentDashInspector({ data }) {
  const sectionTitleMap = {};
  chapters.forEach((ch) => ch.sections.forEach((s) => { sectionTitleMap[s.id] = s.title; }));

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="XP" value={data.total_xp} color="text-yellow-400" />
        <StatCard label="Level" value={data.level} color="text-sky-400" />
        <StatCard label="Accuracy" value={`${data.overall_accuracy}%`} color="text-white" />
        <StatCard label="P(L)" value={`${Math.round((data.overall_p_know ?? 0.20) * 100)}%`} color="text-indigo-400" />
      </div>
      {data.sections?.length > 0 && (
        <div className="space-y-2">
          <div className="text-sm font-semibold text-slate-400 uppercase tracking-wide">Section Mastery</div>
          {data.sections.map((sec) => {
            const pct = Math.round((sec.p_know ?? 0.20) * 100);
            return (
              <div key={sec.section_id} className="flex items-center gap-3">
                <span className="text-xs text-slate-300 w-40 truncate">{sectionTitleMap[sec.section_id] || sec.section_id}</span>
                <div className="flex-1 h-2 rounded-full bg-slate-800 overflow-hidden">
                  <div className={`h-full transition-all ${pct >= 85 ? "bg-emerald-400" : pct >= 60 ? "bg-sky-400" : "bg-rose-400"}`} style={{ width: `${Math.max(3, pct)}%` }} />
                </div>
                <span className="text-xs text-slate-400 w-10 text-right">{pct}%</span>
              </div>
            );
          })}
        </div>
      )}
      {data.total_answered === 0 && <div className="text-slate-400 text-center py-4">This student hasn't attempted any questions yet.</div>}
    </div>
  );
}

// ─────────────────────────────────────────
// PARENT VIEW
// ─────────────────────────────────────────
function ParentDashboard() {
  const [children, setChildren] = useState([]);
  const [loading, setLoading] = useState(true);
  const [linkEmail, setLinkEmail] = useState("");
  const [linkStatus, setLinkStatus] = useState(null); // { type: "success"|"error", msg }
  const [linking, setLinking] = useState(false);
  const [selectedChild, setSelectedChild] = useState(null);
  const [reportCard, setReportCard] = useState(null);
  const [loadingCard, setLoadingCard] = useState(false);

  const sectionTitleMap = {};
  chapters.forEach((ch) => ch.sections.forEach((s) => { sectionTitleMap[s.id] = s.title; }));

  const loadChildren = () => {
    setLoading(true);
    fetchParentChildren().then(data => { setChildren(data || []); }).catch(() => setChildren([])).finally(() => setLoading(false));
  };

  useEffect(() => { loadChildren(); }, []);

  const handleLink = async () => {
    if (!linkEmail.trim()) return;
    setLinking(true);
    setLinkStatus(null);
    try {
      const res = await linkChild(linkEmail.trim());
      setLinkStatus({ type: "success", msg: res.message });
      setLinkEmail("");
      loadChildren();
    } catch (e) {
      setLinkStatus({ type: "error", msg: e.message });
    } finally {
      setLinking(false);
    }
  };

  const handleViewReport = (child) => {
    if (!child.studentProfileId) return;
    setSelectedChild(child);
    setReportCard(null);
    setLoadingCard(true);
    fetchChildReportCard(child.studentProfileId)
      .then(setReportCard)
      .catch(() => setReportCard(null))
      .finally(() => setLoadingCard(false));
  };

  return (
    <>
      <div className="pixel-heading text-2xl">Parent Portal</div>

      {/* Link child section */}
      <div className="mt-6 rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-6">
        <div className="text-base font-semibold text-slate-200 mb-3">🔗 Link a Child Account</div>
        <p className="text-sm text-slate-400 mb-4">Enter your child's registered email address to link their Neuro-Learn account.</p>
        <div className="flex gap-3 flex-wrap">
          <input
            type="email"
            placeholder="child@example.com"
            value={linkEmail}
            onChange={e => setLinkEmail(e.target.value)}
            onKeyDown={e => e.key === "Enter" && handleLink()}
            className="flex-1 min-w-48 px-3 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500/60"
          />
          <button
            onClick={handleLink}
            disabled={linking || !linkEmail.trim()}
            className="px-5 py-2 rounded-xl border border-teal-500/50 bg-teal-500/15 hover:bg-teal-500/30 text-teal-300 text-sm font-semibold transition disabled:opacity-40"
          >
            {linking ? "Linking..." : "Link Child"}
          </button>
        </div>
        {linkStatus && (
          <div className={`mt-3 text-sm px-3 py-2 rounded-xl ${linkStatus.type === "success" ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/30" : "bg-rose-500/10 text-rose-300 border border-rose-500/30"}`}>
            {linkStatus.msg}
          </div>
        )}
      </div>

      {/* Linked children */}
      <div className="mt-8">
        <div className="pixel-heading text-xl">Linked Children</div>
        {loading ? <LoadingSpinner label="Loading children..." /> : children.length === 0 ? (
          <div className="mt-4 text-center py-12 text-slate-500 rounded-2xl border-2 border-dashed border-slate-700/40">
            No children linked yet. Use the form above to link your child's account.
          </div>
        ) : (
          <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
            {children.map((child) => (
              <div key={child.userId} className="rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-lg font-semibold text-slate-100">{child.name}</div>
                    <div className="text-xs text-slate-500">{child.email}</div>
                  </div>
                  <button
                    onClick={() => handleViewReport(child)}
                    className="px-3 py-1 rounded-lg border border-indigo-500/40 bg-indigo-500/10 hover:bg-indigo-500/25 text-indigo-300 text-xs transition"
                  >
                    Report Card
                  </button>
                </div>
                <div className="mt-4 grid grid-cols-4 gap-2 text-center">
                  <div><div className="text-xs text-slate-500">Level</div><div className="text-lg font-bold text-sky-400">{child.level}</div></div>
                  <div><div className="text-xs text-slate-500">XP</div><div className="text-lg font-bold text-yellow-400">{child.totalXp}</div></div>
                  <div><div className="text-xs text-slate-500">Accuracy</div><div className="text-lg font-bold text-white">{child.accuracy?.toFixed(1)}%</div></div>
                  <div><div className="text-xs text-slate-500">P(L)</div><div className="text-lg font-bold text-indigo-400">{Math.round((child.pKnow ?? 0.20) * 100)}%</div></div>
                </div>
                {/* BKT progress bar */}
                <div className="mt-3">
                  <div className="flex justify-between text-[11px] text-slate-500 mb-1"><span>Overall Bayesian Mastery</span><span>{Math.round((child.pKnow ?? 0.20) * 100)}%</span></div>
                  <div className="h-2 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/50">
                    <div className="h-full bg-gradient-to-r from-teal-400 via-sky-400 to-indigo-500 transition-all" style={{ width: `${Math.max(5, Math.round((child.pKnow ?? 0.20) * 100))}%` }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Report Card Modal */}
      {selectedChild && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm px-4">
          <div className="w-full max-w-2xl rounded-2xl border-2 border-indigo-500/40 bg-[rgba(13,26,58,0.97)] p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-5">
              <div className="pixel-heading text-lg">📋 Report Card: {selectedChild.name}</div>
              <button onClick={() => { setSelectedChild(null); setReportCard(null); }} className="text-slate-400 hover:text-white text-xl">✕</button>
            </div>
            {loadingCard ? <LoadingSpinner label="Building report card..." /> : reportCard ? (
              <ReportCardView card={reportCard} sectionTitleMap={sectionTitleMap} />
            ) : (
              <div className="text-rose-400 text-center py-8">Could not load report card.</div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function ReportCardView({ card, sectionTitleMap }) {
  const d = card.dashboard;
  return (
    <div className="space-y-5">
      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="XP" value={d?.total_xp ?? 0} color="text-yellow-400" />
        <StatCard label="Level" value={d?.level ?? 1} color="text-sky-400" />
        <StatCard label="Accuracy" value={`${d?.overall_accuracy ?? 0}%`} color="text-white" />
        <StatCard label="P(L)" value={`${Math.round((d?.overall_p_know ?? 0.20) * 100)}%`} color="text-indigo-400" />
      </div>

      {/* Strengths */}
      <div className="rounded-xl border border-emerald-500/30 bg-emerald-950/20 p-4">
        <div className="text-sm font-semibold text-emerald-300 mb-2">💪 Strengths</div>
        <ul className="space-y-1">
          {card.strengths?.map((s, i) => <li key={i} className="text-sm text-slate-300">• {s}</li>)}
        </ul>
      </div>

      {/* Areas for Growth */}
      <div className="rounded-xl border border-amber-500/30 bg-amber-950/20 p-4">
        <div className="text-sm font-semibold text-amber-300 mb-2">📈 Areas for Growth</div>
        <ul className="space-y-1">
          {card.areasForGrowth?.map((s, i) => <li key={i} className="text-sm text-slate-300">• {s}</li>)}
        </ul>
      </div>

      {/* Section mastery bars */}
      {d?.sections?.length > 0 && (
        <div>
          <div className="text-sm font-semibold text-slate-400 uppercase tracking-wide mb-2">Section Breakdown</div>
          <div className="space-y-2">
            {d.sections.map((sec) => {
              const pct = Math.round((sec.p_know ?? 0.20) * 100);
              return (
                <div key={sec.section_id} className="flex items-center gap-3">
                  <span className="text-xs text-slate-300 w-36 truncate">{sectionTitleMap[sec.section_id] || sec.section_id}</span>
                  <div className="flex-1 h-2 rounded-full bg-slate-800 overflow-hidden">
                    <div className={`h-full transition-all ${pct >= 85 ? "bg-emerald-400" : pct >= 60 ? "bg-sky-400" : "bg-rose-400"}`} style={{ width: `${Math.max(3, pct)}%` }} />
                  </div>
                  <span className="text-xs text-slate-400 w-10 text-right">{pct}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Parenting Tips */}
      <div className="rounded-xl border border-sky-500/30 bg-sky-950/20 p-4">
        <div className="text-sm font-semibold text-sky-300 mb-2">💡 Guidance for Parents</div>
        <ul className="space-y-2">
          {card.parentingTips?.map((tip, i) => <li key={i} className="text-sm text-slate-300">{tip}</li>)}
        </ul>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────
function StatCard({ label, value, color }) {
  return (
    <div className="rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-4">
      <div className="text-xs text-slate-300/70">{label}</div>
      <div className={`mt-1 text-2xl font-bold ${color}`}>{value}</div>
    </div>
  );
}

function LoadingSpinner({ label }) {
  return (
    <div className="flex flex-col items-center py-16">
      <div className="pixel-heading text-lg">{label}</div>
      <div className="mt-4 h-2 w-48 rounded-full bg-[rgba(255,255,255,0.08)] overflow-hidden">
        <div className="h-2 w-1/3 bg-[rgb(56,189,248)] animate-pulse" />
      </div>
    </div>
  );
}
