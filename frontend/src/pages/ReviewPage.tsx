import { useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import CardReview from '@/features/flashcard/components/CardReview'
import ReviewCompleteScreen from '@/features/flashcard/components/ReviewCompleteScreen'
import { useDecks, useDueCards, useReviewCard } from '@/features/flashcard/api'
import { useKeyboard } from '@/shared/hooks/useKeyboard'

interface ReviewStats {
  total: number
  again: number
  hard: number
  good: number
  easy: number
}

const EMPTY_STATS: ReviewStats = { total: 0, again: 0, hard: 0, good: 0, easy: 0 }

export default function ReviewPage() {
  const { id: deckId } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: decks = [] } = useDecks()
  const { data: dueCards = [], isLoading } = useDueCards(deckId!)
  const reviewCard = useReviewCard()

  const [currentIndex, setCurrentIndex] = useState(0)
  const [stats, setStats] = useState<ReviewStats>(EMPTY_STATS)
  const [done, setDone] = useState(false)
  const [confirmExit, setConfirmExit] = useState(false)

  const deck = decks.find((d) => d.id === deckId)
  const total = dueCards.length
  const currentCard = dueCards[currentIndex]

  const handleRate = useCallback(
    (rating: 1 | 2 | 3 | 4) => {
      if (!currentCard) return
      reviewCard.mutate({ id: currentCard.id, rating })
      setStats((prev) => ({
        total: prev.total + 1,
        again: rating === 1 ? prev.again + 1 : prev.again,
        hard:  rating === 2 ? prev.hard  + 1 : prev.hard,
        good:  rating === 3 ? prev.good  + 1 : prev.good,
        easy:  rating === 4 ? prev.easy  + 1 : prev.easy,
      }))
      if (currentIndex + 1 >= total) {
        setDone(true)
      } else {
        setCurrentIndex((i) => i + 1)
      }
    },
    [currentCard, currentIndex, total, reviewCard],
  )

  useKeyboard({
    Escape: () => {
      if (confirmExit) navigate(`/decks/${deckId}`)
      else setConfirmExit(true)
    },
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isLoading && dueCards.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <p className="text-xl font-semibold">No cards due</p>
        <p className="text-muted-foreground">Great job! Come back later for more reviews.</p>
        <Button onClick={() => navigate(`/decks/${deckId}`)}>Back to deck</Button>
      </div>
    )
  }

  if (done) {
    return <ReviewCompleteScreen deckId={deckId!} stats={{ ...stats, total }} />
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <p className="text-sm text-muted-foreground">{deck?.name ?? 'Review'}</p>
          <p className="text-xs text-muted-foreground">
            {currentIndex + 1} / {total}
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setConfirmExit(true)}
          aria-label="Exit review"
        >
          <X className="h-5 w-5" />
        </Button>
      </div>

      {/* Progress bar */}
      <div className="w-full h-1.5 bg-muted rounded-full mb-8">
        <div
          className="h-full bg-primary rounded-full transition-all duration-300"
          style={{ width: `${(currentIndex / total) * 100}%` }}
        />
      </div>

      {/* Card */}
      {currentCard && (
        <CardReview
          key={currentCard.id}
          card={currentCard}
          onRate={handleRate}
          isSubmitting={reviewCard.isPending}
        />
      )}

      {/* Exit confirm */}
      {confirmExit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-background rounded-lg shadow-xl p-6 w-full max-w-xs mx-4 text-center">
            <p className="font-semibold mb-2">Exit review?</p>
            <p className="text-sm text-muted-foreground mb-4">
              Progress will be saved for cards already reviewed.
            </p>
            <div className="flex gap-2 justify-center">
              <Button variant="outline" onClick={() => setConfirmExit(false)}>Keep going</Button>
              <Button variant="destructive" onClick={() => navigate(`/decks/${deckId}`)}>Exit</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
