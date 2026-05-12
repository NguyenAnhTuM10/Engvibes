import { useNavigate } from 'react-router-dom'
import { Flame, BookOpen, Trophy, ArrowRight, Zap } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import PageHeader from '@/components/ui/PageHeader'
import VideoCard from '@/features/videos/components/VideoCard'
import { useCurrentUser } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'
import { useRecommendedVideos, useDailyChallenge } from '@/features/recommend/api'
import { useSessionHistory } from '@/features/session/api'

export default function HomePage() {
  const { data: user } = useCurrentUser()
  const storeUser = useAuthStore((s) => s.user)
  const displayUser = user ?? storeUser
  const navigate = useNavigate()

  const { data: recommended } = useRecommendedVideos(5)
  const { data: challenge } = useDailyChallenge()
  const { data: history } = useSessionHistory()

  const inProgress = history?.content.filter((s) => s.status === 'IN_PROGRESS') ?? []

  return (
    <div className="space-y-8">
      <PageHeader
        title={`Welcome back, ${displayUser?.username ?? '...'}!`}
        description="Keep up your learning streak"
      />

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard icon={<Flame className="h-4 w-4 text-orange-500" />} label="Streak" value={`${displayUser?.currentStreakDays ?? 0} days`} />
        <StatCard icon={<Trophy className="h-4 w-4 text-yellow-500" />} label="Total XP" value={(displayUser?.totalXp ?? 0).toLocaleString()} />
        <StatCard icon={<BookOpen className="h-4 w-4 text-blue-500" />} label="CEFR Level" value={displayUser?.cefrLevel ?? '—'} />
        <StatCard icon={<Zap className="h-4 w-4 text-purple-500" />} label="Role" value={displayUser?.role ?? '—'} />
      </div>

      {/* Daily challenge */}
      {challenge && (
        <div className="border-2 border-primary/20 bg-primary/5 rounded-xl p-5">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold text-primary uppercase tracking-wide">Today's challenge</p>
              <p className="font-semibold">{challenge.videoTitle}</p>
              {challenge.vocabToReview > 0 && (
                <p className="text-sm text-muted-foreground">{challenge.vocabToReview} vocab cards to review</p>
              )}
              {challenge.randomPhrase && (
                <p className="text-xs text-muted-foreground italic">Practice: "{challenge.randomPhrase}"</p>
              )}
            </div>
            <Button onClick={() => navigate(`/session/${challenge.videoId}`)} className="gap-1.5 shrink-0">
              Start <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Continue sessions */}
      {inProgress.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-3">
            Continue learning
          </h2>
          <div className="space-y-2">
            {inProgress.slice(0, 3).map((s) => (
              <div
                key={s.id}
                className="flex items-center justify-between border rounded-lg px-4 py-3 hover:bg-accent transition-colors cursor-pointer"
                onClick={() => navigate(`/session/${s.videoId}`)}
              >
                <div>
                  <p className="text-sm font-medium line-clamp-1">{s.videoTitle}</p>
                  <p className="text-xs text-muted-foreground">Step {s.currentStep + 1} of 7</p>
                </div>
                <Button size="sm" variant="outline">Resume</Button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Recommended videos */}
      {recommended && recommended.length > 0 && (
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
              Recommended for you
            </h2>
            <Button variant="ghost" size="sm" onClick={() => navigate('/videos')}>
              View all
            </Button>
          </div>
          <div className="flex gap-4 overflow-x-auto pb-2 -mx-1 px-1">
            {recommended.map((v) => (
              <div key={v.id} className="min-w-[200px] max-w-[200px]">
                <VideoCard video={v} />
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Quick actions */}
      {!challenge && !recommended?.length && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <ActionCard title="Browse videos" description="Explore the video library" onClick={() => navigate('/videos')} />
          <ActionCard title="Review flashcards" description="Study your vocabulary decks" onClick={() => navigate('/decks')} />
          <ActionCard title="View progress" description="See your learning analytics" onClick={() => navigate('/progress')} />
        </div>
      )}
    </div>
  )
}

function StatCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="pt-4">
        <div className="flex items-center gap-2 mb-1">{icon}<span className="text-xs text-muted-foreground">{label}</span></div>
        <p className="text-xl font-bold">{value}</p>
      </CardContent>
    </Card>
  )
}

function ActionCard({ title, description, onClick }: { title: string; description: string; onClick: () => void }) {
  return (
    <Card className="hover:shadow-md transition-shadow cursor-pointer" onClick={onClick}>
      <CardHeader><CardTitle className="text-base">{title}</CardTitle></CardHeader>
      <CardContent><p className="text-sm text-muted-foreground">{description}</p></CardContent>
    </Card>
  )
}
