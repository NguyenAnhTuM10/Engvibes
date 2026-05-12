import { useNavigate } from 'react-router-dom'
import { Flame, BookOpen, Play, Trophy } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { Session } from '@/shared/types/api'

interface Props {
  session: Session
}

export default function SessionCompleteScreen({ session }: Props) {
  const navigate = useNavigate()

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center space-y-8 max-w-sm mx-auto">
      <div className="space-y-2">
        <div className="text-6xl">🎉</div>
        <h1 className="text-2xl font-bold">Session complete!</h1>
        <p className="text-muted-foreground text-sm">Great work — you finished all 7 steps.</p>
      </div>

      <div className="grid grid-cols-2 gap-3 w-full">
        <StatCard
          icon={<Trophy className="h-5 w-5 text-yellow-500" />}
          label="XP earned"
          value={`+${session.totalXpEarned}`}
        />
        <StatCard
          icon={<Flame className="h-5 w-5 text-orange-500" />}
          label="Streak"
          value="Keep it up!"
        />
      </div>

      <div className="flex flex-col gap-3 w-full">
        <Button
          size="lg"
          onClick={() => navigate('/decks')}
          className="gap-2"
        >
          <BookOpen className="h-4 w-4" />
          Review flashcards
        </Button>
        <Button
          variant="outline"
          size="lg"
          onClick={() => navigate('/videos')}
          className="gap-2"
        >
          <Play className="h-4 w-4" />
          Next video
        </Button>
        <Button variant="ghost" size="sm" onClick={() => navigate('/')}>
          Back to home
        </Button>
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
    <div className="border rounded-xl p-4 text-left">
      <div className="flex items-center gap-2 mb-1">
        {icon}
        <span className="text-xs text-muted-foreground">{label}</span>
      </div>
      <p className="text-xl font-bold">{value}</p>
    </div>
  )
}
