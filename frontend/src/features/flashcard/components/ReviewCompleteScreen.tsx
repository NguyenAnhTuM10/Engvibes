import { useNavigate } from 'react-router-dom'
import { CheckCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

interface ReviewStats {
  total: number
  again: number
  hard: number
  good: number
  easy: number
}

interface ReviewCompleteScreenProps {
  deckId: string
  stats: ReviewStats
}

export default function ReviewCompleteScreen({ deckId, stats }: ReviewCompleteScreenProps) {
  const navigate = useNavigate()

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-6 max-w-sm mx-auto">
      <CheckCircle className="h-16 w-16 text-green-500" />
      <div className="text-center">
        <h2 className="text-2xl font-bold mb-1">Session complete!</h2>
        <p className="text-muted-foreground">You reviewed {stats.total} cards</p>
      </div>

      <Card className="w-full">
        <CardContent className="pt-4">
          <div className="grid grid-cols-4 gap-2 text-center">
            <div>
              <p className="text-xl font-bold text-red-500">{stats.again}</p>
              <p className="text-xs text-muted-foreground">Again</p>
            </div>
            <div>
              <p className="text-xl font-bold text-orange-500">{stats.hard}</p>
              <p className="text-xs text-muted-foreground">Hard</p>
            </div>
            <div>
              <p className="text-xl font-bold text-green-500">{stats.good}</p>
              <p className="text-xs text-muted-foreground">Good</p>
            </div>
            <div>
              <p className="text-xl font-bold text-blue-500">{stats.easy}</p>
              <p className="text-xs text-muted-foreground">Easy</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-col gap-2 w-full">
        <Button onClick={() => navigate(`/decks/${deckId}`)}>Back to deck</Button>
        <Button variant="outline" onClick={() => navigate('/decks')}>All decks</Button>
      </div>
    </div>
  )
}
