import { format, parseISO } from 'date-fns'
import {
  BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { Flame, Trophy, BookOpen, Mic } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { useOverviewStats, useWeeklyActivity, usePhonemeStats, useVocabGrowth } from '@/features/stats/api'
import { cn } from '@/lib/utils'

const CEFR_COLORS: Record<string, string> = {
  A1: '#16a34a', A2: '#22c55e', B1: '#3b82f6', B2: '#8b5cf6', C1: '#f97316', C2: '#ef4444',
}
const ACTIVITY_COLORS: Record<string, string> = {
  listen: '#3b82f6', shadow: '#8b5cf6', retell: '#10b981', speak: '#f59e0b',
}

function StatCard({ icon, label, value, sub }: { icon: React.ReactNode; label: string; value: string | number; sub?: string }) {
  return (
    <div className="border rounded-xl p-4">
      <div className="flex items-center gap-2 mb-1">{icon}<span className="text-xs text-muted-foreground">{label}</span></div>
      <p className="text-2xl font-bold">{value}</p>
      {sub && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
    </div>
  )
}

export default function ProgressPage() {
  const { data: overview, isLoading: oLoading } = useOverviewStats()
  const { data: weeklyArr, isLoading: wLoading } = useWeeklyActivity()
  const { data: phonemes } = usePhonemeStats()
  const { data: vocabGrowthMap } = useVocabGrowth()

  // weekly: DailyActivity[] thẳng từ backend
  const weeklyData = (weeklyArr ?? []).map((d) => ({
    date: format(parseISO(d.date), 'EEE'),
    ...d.byActivity,
    total: d.totalMinutes,
  }))
  const activityKeys = weeklyArr?.length
    ? [...new Set(weeklyArr.flatMap((d) => Object.keys(d.byActivity)))]
    : []

  // vocab-growth: { "2026-05-09": { A1: 71, A2: 29 }, ... }
  const growthData = vocabGrowthMap
    ? Object.entries(vocabGrowthMap).map(([date, levels]) => ({
        date: format(parseISO(date), 'MM/dd'),
        ...levels,
      }))
    : []
  const cefrKeys = growthData.length
    ? [...new Set(growthData.flatMap((d) => Object.keys(d).filter((k) => k !== 'date')))]
    : []

  return (
    <div className="space-y-6">
      <PageHeader title="Progress" description="Track your learning journey" />

      {/* Overview */}
      {oLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="border rounded-xl p-4 h-24 animate-pulse bg-muted" />
          ))}
        </div>
      ) : overview ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard icon={<Flame className="h-4 w-4 text-orange-500" />} label="Day streak" value={overview.streakDays} />
          <StatCard icon={<Trophy className="h-4 w-4 text-yellow-500" />} label="Total XP" value={overview.totalXp.toLocaleString()} />
          <StatCard icon={<BookOpen className="h-4 w-4 text-blue-500" />} label="Videos completed" value={overview.videosCompleted} />
          <StatCard
            icon={<Mic className="h-4 w-4 text-green-500" />}
            label="Vocab mastered"
            value={overview.vocabMastered}
            sub={overview.avgRetellScore7d != null ? `Avg retell: ${Math.round(overview.avgRetellScore7d)}%` : undefined}
          />
        </div>
      ) : null}

      {/* Weekly activity */}
      <div className="border rounded-xl p-5">
        <h3 className="text-sm font-semibold mb-4">Weekly Activity (minutes)</h3>
        {wLoading ? (
          <div className="h-48 bg-muted rounded animate-pulse" />
        ) : weeklyData.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-12">No activity this week</p>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={weeklyData} barSize={20}>
              <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} formatter={(v: unknown) => [`${v} min`]} />
              {activityKeys.length > 0 ? (
                <>
                  <Legend iconSize={10} wrapperStyle={{ fontSize: 11 }} />
                  {activityKeys.map((key) => (
                    <Bar key={key} dataKey={key} stackId="a" fill={ACTIVITY_COLORS[key] ?? '#6b7280'} name={key.charAt(0).toUpperCase() + key.slice(1)} />
                  ))}
                </>
              ) : (
                <Bar dataKey="total" fill="#3b82f6" name="Minutes" />
              )}
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Phoneme weakness */}
      <div className="border rounded-xl p-5">
        <h3 className="text-sm font-semibold mb-4">Pronunciation Weak Spots</h3>
        {!phonemes || phonemes.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-8">
            Complete shadow exercises to see your phoneme analysis
          </p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {phonemes.sort((a, b) => b.errorRate - a.errorRate).map((p) => {
              const pct = Math.round(p.errorRate * 100)
              return (
                <div
                  key={p.phoneme}
                  title={`${pct}% error · ${p.totalAttempts} attempts`}
                  className={cn(
                    'px-3 py-2 rounded-lg font-mono text-sm cursor-default hover:scale-105 transition-transform',
                    pct >= 70 ? 'bg-red-500 text-white' : pct >= 50 ? 'bg-red-300 text-red-900' : pct >= 30 ? 'bg-orange-200 text-orange-900' : 'bg-yellow-100 text-yellow-800',
                  )}
                >
                  <div className="font-bold">/{p.phoneme}/</div>
                  <div className="text-xs opacity-80">{pct}%</div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Vocab growth */}
      <div className="border rounded-xl p-5">
        <h3 className="text-sm font-semibold mb-4">Vocabulary Growth</h3>
        {growthData.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-12">
            Add flashcards from videos to track vocabulary growth
          </p>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={growthData}>
              <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }} />
              <Legend iconSize={10} wrapperStyle={{ fontSize: 11 }} />
              {cefrKeys.map((level) => (
                <Line key={level} type="monotone" dataKey={level} stroke={CEFR_COLORS[level] ?? '#6b7280'} dot={false} strokeWidth={2} />
              ))}
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
