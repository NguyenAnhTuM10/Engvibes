import { LogOut, Flame, BookOpen, Play, Trophy } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useCurrentUser, useLogout } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'

export default function HomePage() {
  const { data: user } = useCurrentUser()
  const storeUser = useAuthStore((s) => s.user)
  const logout = useLogout()
  const displayUser = user ?? storeUser

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-gray-900">English Learning Platform</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-600">{displayUser?.username}</span>
          <Button variant="ghost" size="sm" onClick={logout}>
            <LogOut className="h-4 w-4 mr-1" />
            Logout
          </Button>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-1">
          Welcome back, {displayUser?.username ?? '...'}!
        </h2>
        <p className="text-gray-500 mb-8">Keep up your learning streak</p>

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
            href="/videos"
          />
          <ActionCard
            title="Review flashcards"
            description="Study your vocabulary decks"
            href="/decks"
          />
          <ActionCard
            title="Today's challenge"
            description="Complete today's video lesson"
            href="/videos"
          />
        </div>
      </main>
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
  href,
}: {
  title: string
  description: string
  href: string
}) {
  return (
    <a href={href}>
      <Card className="hover:shadow-md transition-shadow cursor-pointer">
        <CardHeader>
          <CardTitle className="text-base">{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">{description}</p>
        </CardContent>
      </Card>
    </a>
  )
}
