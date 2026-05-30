import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { format, parseISO } from 'date-fns'
import { Play, CheckCircle2, Clock, ChevronLeft, ChevronRight, MessageSquare } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useSessionHistory } from '@/features/session/api'
import { useConversationHistory, type ConversationSessionRow } from '@/features/conversation/api'
import { cn } from '@/lib/utils'

const STEP_LABELS = ['Warmup', 'Listen', 'Phrases', 'Shadow', 'Retell', 'Speak', 'Review']

const SCENARIO_META: Record<string, { label: string; icon: string }> = {
  JOB_INTERVIEW:      { label: 'Job Interview',          icon: '💼' },
  COFFEE_SHOP:        { label: 'Coffee Shop',            icon: '☕' },
  HOTEL_CHECKIN:      { label: 'Hotel Check-in',         icon: '🏨' },
  DOCTOR_APPOINTMENT: { label: "Doctor's Appointment",   icon: '🏥' },
  MAKING_PLANS:       { label: 'Making Weekend Plans',   icon: '📅' },
}

export default function HistoryPage() {
  return (
    <div className="space-y-4">
      <PageHeader title="History" description="Your past learning sessions and conversations" />
      <Tabs defaultValue="sessions">
        <TabsList>
          <TabsTrigger value="sessions">Learning</TabsTrigger>
          <TabsTrigger value="conversations">Conversations</TabsTrigger>
        </TabsList>
        <TabsContent value="sessions" className="mt-4">
          <SessionHistory />
        </TabsContent>
        <TabsContent value="conversations" className="mt-4">
          <ConversationHistory />
        </TabsContent>
      </Tabs>
    </div>
  )
}

// ── Learning sessions (video) ───────────────────────────────────────────────────

function SessionHistory() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const { data, isLoading } = useSessionHistory(page)

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-16 border rounded-xl bg-muted animate-pulse" />
        ))}
      </div>
    )
  }
  if (!data || data.content.length === 0) {
    return (
      <div className="flex flex-col items-center py-24 gap-3 text-muted-foreground">
        <Clock className="h-12 w-12 opacity-30" />
        <p className="text-base font-medium">No sessions yet</p>
        <p className="text-sm">Start a video to begin your learning journey</p>
        <Button variant="outline" onClick={() => navigate('/videos')}>Browse videos</Button>
      </div>
    )
  }

  return (
    <>
      <div className="border rounded-xl overflow-hidden divide-y">
        {data.content.map((session) => (
          <div
            key={session.id}
            className="flex items-center gap-4 px-4 py-3 hover:bg-accent/50 transition-colors cursor-pointer"
            onClick={() => navigate(`/session/${session.videoId}`)}
          >
            <div className="shrink-0">
              {session.status === 'COMPLETED'
                ? <CheckCircle2 className="h-5 w-5 text-green-500" />
                : <Play className="h-5 w-5 text-blue-500" />}
            </div>

            {session.videoThumbnailUrl ? (
              <img src={session.videoThumbnailUrl} alt=""
                   className="h-10 w-16 rounded object-cover shrink-0 hidden sm:block" />
            ) : (
              <div className="h-10 w-16 rounded bg-muted shrink-0 hidden sm:block" />
            )}

            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium line-clamp-1">{session.videoTitle}</p>
              <div className="flex items-center gap-3 mt-0.5">
                <span className={cn(
                  'text-xs px-1.5 py-0.5 rounded font-medium',
                  session.status === 'COMPLETED'
                    ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                    : 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
                )}>
                  {session.status === 'COMPLETED'
                    ? 'Completed'
                    : `Step ${session.currentStep + 1}/7 · ${STEP_LABELS[session.currentStep] ?? ''}`}
                </span>
                {session.totalXpEarned > 0 && (
                  <span className="text-xs text-muted-foreground">+{session.totalXpEarned} XP</span>
                )}
              </div>
            </div>

            <div className="text-right shrink-0 hidden sm:block">
              <p className="text-xs text-muted-foreground">
                {format(parseISO(session.startedAt ?? session.updatedAt ?? session.createdAt ?? ''), 'MMM d, yyyy')}
              </p>
              {session.status !== 'COMPLETED' && (
                <Button size="sm" variant="outline" className="mt-1 h-6 text-xs">Resume</Button>
              )}
            </div>
          </div>
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-4">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm text-muted-foreground">{page + 1} / {data.totalPages}</span>
          <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </>
  )
}

// ── Conversation history (realtime) ─────────────────────────────────────────────

function parseOverall(summary: string | null): number | null {
  if (!summary) return null
  try {
    const v = JSON.parse(summary)
    return typeof v.overall === 'number' ? v.overall : null
  } catch {
    return null
  }
}

function ConversationHistory() {
  const navigate = useNavigate()
  const { data, isLoading } = useConversationHistory()

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-16 border rounded-xl bg-muted animate-pulse" />
        ))}
      </div>
    )
  }
  if (!data || data.length === 0) {
    return (
      <div className="flex flex-col items-center py-24 gap-3 text-muted-foreground">
        <MessageSquare className="h-12 w-12 opacity-30" />
        <p className="text-base font-medium">No conversations yet</p>
        <p className="text-sm">Practice speaking with an AI roleplay partner</p>
        <Button variant="outline" onClick={() => navigate('/conversation')}>Start a conversation</Button>
      </div>
    )
  }

  return (
    <div className="border rounded-xl overflow-hidden divide-y">
      {data.map((c: ConversationSessionRow) => {
        const meta = SCENARIO_META[c.scenarioId] ?? { label: c.scenarioId, icon: '🗣️' }
        const overall = parseOverall(c.summary)
        return (
          <div key={c.id} className="flex items-center gap-4 px-4 py-3 hover:bg-accent/50 transition-colors">
            <span className="text-2xl shrink-0">{meta.icon}</span>

            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium line-clamp-1">{meta.label}</p>
              <div className="flex items-center gap-3 mt-0.5">
                <span className={cn(
                  'text-xs px-1.5 py-0.5 rounded font-medium',
                  c.status === 'COMPLETED'
                    ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                    : 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
                )}>
                  {c.status === 'COMPLETED' ? 'Completed' : c.status}
                </span>
                <span className="text-xs text-muted-foreground">{c.totalTurns} turns</span>
                {c.xpEarned > 0 && <span className="text-xs text-muted-foreground">+{c.xpEarned} XP</span>}
              </div>
            </div>

            <div className="text-right shrink-0 flex items-center gap-3">
              {overall !== null && (
                <span className={cn(
                  'text-sm font-bold tabular-nums px-2 py-0.5 rounded',
                  overall >= 7 ? 'text-green-600' : overall >= 5 ? 'text-blue-600' : 'text-orange-500',
                )}>
                  {overall.toFixed(1)}
                </span>
              )}
              <p className="text-xs text-muted-foreground hidden sm:block">
                {format(parseISO(c.createdAt), 'MMM d, yyyy')}
              </p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
