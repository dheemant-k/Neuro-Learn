import { useEffect, useState } from "react";
import { getDashboard } from "../utils/api";
import { chapters } from "../data/chapters";

export default function DashboardPage({ onBack }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDashboard("default")
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  const sectionTitleMap = {};
  chapters.forEach((ch) =>
    ch.sections.forEach((s) => {
      sectionTitleMap[s.id] = s.title;
    })
  );

  return (
    <div className="min-h-screen text-slate-100">
      {/* Top bar */}
      <div className="sticky top-0 z-50 backdrop-blur-md border-b border-white/[0.06] bg-[rgba(15,23,42,0.82)]">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <button onClick={onBack} className="px-4 py-2 rounded-xl border border-[rgba(48,68,105,0.8)] bg-[rgba(13,26,58,0.55)] hover:bg-[rgba(13,26,58,0.85)] transition text-sm">
            ← Home
          </button>
          <div className="pixel-heading text-base">Parent / Teacher Dashboard</div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-10">
        {loading ? (
          <div className="flex flex-col items-center py-20">
            <div className="pixel-heading text-xl">Loading dashboard...</div>
            <div className="mt-4 h-2 w-48 rounded-full bg-[rgba(255,255,255,0.08)] overflow-hidden">
              <div className="h-2 w-1/3 bg-[rgb(56,189,248)] animate-pulse" />
            </div>
          </div>
        ) : !data || data.total_answered === 0 ? (
          <div className="text-center py-20">
            <div className="text-4xl mb-4">📊</div>
            <div className="pixel-heading text-xl">No Data Yet</div>
            <p className="mt-2 text-slate-300/80">The student hasn't completed any tests yet. Data will appear here after taking quizzes.</p>
          </div>
        ) : (
          <>
            {/* Overview cards */}
            <div className="pixel-heading text-2xl">Student Overview</div>
            <div className="mt-6 grid grid-cols-2 md:grid-cols-5 gap-4">
              <StatCard label="Total XP" value={data.total_xp} color="text-[rgb(250,204,21)]" />
              <StatCard label="Level" value={data.level} color="text-[rgb(56,189,248)]" />
              <StatCard label="Questions" value={`${data.total_correct}/${data.total_answered}`} color="text-[rgb(94,234,212)]" />
              <StatCard label="Raw Accuracy" value={`${data.overall_accuracy}%`} color="text-white" />
              <StatCard 
                label="Bayesian P(L)" 
                value={`${Math.round((data.overall_p_know !== undefined ? data.overall_p_know : 0.20) * 100)}%`} 
                color="text-indigo-400" 
              />
            </div>

            {/* Section breakdown */}
            <div className="mt-10">
              <div className="pixel-heading text-xl">Bayesian Section Mastery Breakdown</div>
              <div className="mt-4 space-y-4">
                {data.sections.map((sec) => {
                  const bktPct = Math.round((sec.p_know !== undefined ? sec.p_know : 0.20) * 100);
                  const isMastered = sec.is_mastered || bktPct >= 85;

                  return (
                    <div key={sec.section_id} className="rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-lg">{sectionTitleMap[sec.section_id] || sec.section_id}</span>
                            {isMastered && (
                              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                                Mastered 🏆
                              </span>
                            )}
                          </div>
                          <div className="text-sm text-slate-300/80 mt-1">
                            {sec.correct}/{sec.answered} correct • Raw Accuracy: {sec.accuracy}%
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-xs text-slate-400">Bayesian Knowledge State</div>
                          <div className="text-2xl font-bold text-sky-400">{bktPct}% <span className="text-xs font-normal text-slate-400">P(L)</span></div>
                        </div>
                      </div>

                      {/* Bayesian Knowledge Bar */}
                      <div className="mt-3">
                        <div className="flex justify-between text-[11px] text-slate-400 mb-1">
                          <span>Latent Skill Probability</span>
                          <span>{bktPct}% / 100%</span>
                        </div>
                        <div className="h-2.5 w-full rounded-full bg-slate-800/80 overflow-hidden border border-slate-700/50">
                          <div 
                            className="h-full bg-gradient-to-r from-teal-400 via-sky-400 to-indigo-500 transition-all duration-500" 
                            style={{ width: `${Math.max(5, bktPct)}%` }} 
                          />
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Recent Activity */}
            {data.recent_history?.length > 0 && (
              <div className="mt-10">
                <div className="pixel-heading text-xl">Recent Activity</div>
                <div className="mt-4 rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-5">
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {data.recent_history.slice().reverse().map((h, i) => (
                      <div key={i} className="flex items-center gap-3 text-sm">
                        <span className={`w-2.5 h-2.5 rounded-full shrink-0 ${h.correct ? "bg-[rgb(52,211,153)]" : "bg-[rgb(251,113,133)]"}`} />
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
        )}
      </div>
    </div>
  );
}

function StatCard({ label, value, color }) {
  return (
    <div className="rounded-2xl border-2 border-[rgba(48,68,105,0.9)] bg-[rgba(15,23,42,0.72)] p-4">
      <div className="text-xs text-slate-300/70">{label}</div>
      <div className={`mt-1 text-2xl font-bold ${color}`}>{value}</div>
    </div>
  );
}
