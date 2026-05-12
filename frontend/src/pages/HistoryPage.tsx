import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { format, parseISO } from 'date-fns'
import { Play, CheckCircle2, Clock, ChevronLeft, ChevronRight } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { useSessionHistory } from '@/features/session/api'
import { cn } from '@/lib/utils'

const STEP_LABELS = ['Warmup', 'Listen', 'Phrases', 'Shadow', 'Retell', 'Speak', 'Review']

export default function HistoryPage() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const { data, isLoading } = useSessionHistory(page)

  return (
    <div className="space-y-4">
      <PageHeader title="History" description="Your past learning sessions" />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-16 border rounded-xl bg-muted animate-pulse" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center py-24 gap-3 text-muted-foreground">
          <Clock className="h-12 w-12 opacity-30" />
          <p className="text-base font-medium">No sessions yet</p>
          <p className="text-sm">Start a video to begin your learning journey</p>
          <Button variant="outline" onClick={() => navigate('/videos')}>Browse videos</Button>
        </div>
      ) : (
        <>
          <div className="border rounded-xl overflow-hidden divide-y">
            {data.content.map((session) => (
              <div
                key={session.id}
                className="flex items-center gap-4 px-4 py-3 hover:bg-accent/50 transition-colors cursor-pointer"
                onClick={() => navigate(`/session/${session.videoId}`)}
              >
                <div className="shrink-0">
                  {session.status === 'COMPLETED' ? (
                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                  ) : (
                    <Play className="h-5 w-5 text-blue-500" />
                  )}
                </div>

                {session.videoThumbnailUrl ? (
                  <img
                    src={session.videoThumbnailUrl}
                    alt=""
                    className="h-10 w-16 rounded object-cover shrink-0 hidden sm:block"
                  />
                ) : (
                  <div className="h-10 w-16 rounded bg-muted shrink-0 hidden sm:block" />
                )}

                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium line-clamp-1">{session.videoTitle}</p>
                  <div className="flex items-center gap-3 mt-0.5">
                    <span
                      className={cn(
                        'text-xs px-1.5 py-0.5 rounded font-medium',
                        session.status === 'COMPLETED'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                          : 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
                      )}
                    >
                      {session.status === 'COMPLETED' ? 'Completed' : `Step ${session.currentStep + 1}/7 · ${STEP_LABELS[session.currentStep] ?? ''}`}
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
            <div className="flex items-center justify-center gap-3">
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
      )}
    </div>
  )
}
