import { useNavigate } from 'react-router-dom'
import { Flame, BookOpen, Play, Trophy } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import PageHeader from '@/components/ui/PageHeader'
import { useCurrentUser } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'

export default function HomePage() {
  const { data: user } = useCurrentUser()
  const storeUser = useAuthStore((s) => s.user)
  const displayUser = user ?? storeUser
  const navigate = useNavigate()

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${displayUser?.username ?? '...'}!`}
        description="Keep up your learning streak"
      />

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <StatCard
          icon={<Flame className="h-5 w-5 text-orange-500" />}
          label="Streak"
          value={`${displayUser?.currentStreakDays ?? 0} days`}
        />
        <StatCard
          icon={<Trophy className="h-5 w-5 text-yellow-500" />}
          label="Total XP"
          value={displayUser?.totalXp ?? 0}
        />
        <StatCard
          icon={<BookOpen className="h-5 w-5 text-blue-500" />}
          label="CEFR Level"
          value={displayUser?.cefrLevel ?? '—'}
        />
        <StatCard
          icon={<Play className="h-5 w-5 text-green-500" />}
          label="Role"
          value={displayUser?.role ?? '—'}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <ActionCard
          title="Continue learning"
          description="Pick up where you left off"
          onClick={() => navigate('/videos')}
        />
        <ActionCard
          title="Review flashcards"
          description="Study your vocabulary decks"
          onClick={() => navigate('/decks')}
        />
        <ActionCard
          title="Today's challenge"
          description="Complete today's video lesson"
          onClick={() => navigate('/videos')}
        />
      </div>
    </div>
  )
}

function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string | number
}) {
  return (
    <Card>
      <CardContent className="pt-4">
        <div className="flex items-center gap-2 mb-1">
          {icon}
          <span className="text-xs text-muted-foreground">{label}</span>
        </div>
        <p className="text-xl font-bold">{value}</p>
      </CardContent>
    </Card>
  )
}

function ActionCard({
  title,
  description,
  onClick,
}: {
  title: string
  description: string
  onClick: () => void
}) {
  return (
    <Card
      className="hover:shadow-md transition-shadow cursor-pointer"
      onClick={onClick}
    >
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  )
}
