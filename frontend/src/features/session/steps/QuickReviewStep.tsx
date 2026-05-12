import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { useQuickReviewCards, useQuickReviewCard } from '@/features/session/api'
import type { Card } from '@/shared/types/api'
import { cn } from '@/lib/utils'

interface Props {
  sessionId: string
  onComplete: () => void
}

function QuickCard({
  card,
  sessionId,
}: {
  card: Card
  sessionId: string
}) {
  const [flipped, setFlipped] = useState(false)
  const [rated, setRated] = useState(false)
  const reviewCard = useQuickReviewCard(sessionId)

  const handleRate = (rating: 3 | 4) => {
    reviewCard.mutate({ cardId: card.id, rating })
    setRated(true)
  }

  return (
    <div
      className={cn(
        'shrink-0 w-44 border rounded-xl bg-card transition-all duration-200',
        rated && 'opacity-60 scale-95',
      )}
      style={{ minHeight: 128 }}
    >
      {!flipped ? (
        <button
          className="w-full h-full flex flex-col items-center justify-center p-4 text-center"
          onClick={() => setFlipped(true)}
        >
          <p className="font-bold text-base">{card.vocab.word}</p>
          {card.vocab.ipa && (
            <p className="text-xs text-muted-foreground font-mono mt-1">{card.vocab.ipa}</p>
          )}
          <p className="text-[11px] text-muted-foreground mt-3 opacity-60">tap to reveal</p>
        </button>
      ) : (
        <div className="flex flex-col p-3 h-full">
          <p className="font-semibold text-sm mb-1">{card.vocab.word}</p>
          <p className="text-xs text-muted-foreground flex-1 line-clamp-4">
            {card.vocab.definition}
          </p>
          {!rated ? (
            <div className="flex gap-1 mt-2">
              <Button
                size="sm"
                variant="outline"
                className="flex-1 text-xs h-7 text-orange-600 border-orange-300 hover:bg-orange-50"
                onClick={() => handleRate(3)}
              >
                Hard
              </Button>
              <Button
                size="sm"
                className="flex-1 text-xs h-7 bg-green-600 hover:bg-green-700"
                onClick={() => handleRate(4)}
              >
                Easy
              </Button>
            </div>
          ) : (
            <p className="text-xs text-center text-muted-foreground mt-2">✓ Rated</p>
          )}
        </div>
      )}
    </div>
  )
}

export default function QuickReviewStep({ sessionId, onComplete }: Props) {
  const { data: cards, isLoading } = useQuickReviewCards(sessionId)

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Quick review</h2>
        <p className="text-sm text-muted-foreground mt-0.5">
          Review the words you added during this session
        </p>
      </div>

      {!cards || cards.length === 0 ? (
        <div className="flex flex-col items-center py-16 gap-4 text-muted-foreground">
          <p>No new vocabulary added this session</p>
          <Button onClick={onComplete}>Finish session</Button>
        </div>
      ) : (
        <>
          <p className="text-sm text-muted-foreground">{cards.length} cards to review</p>
          <div className="flex gap-3 overflow-x-auto pb-3 -mx-1 px-1">
            {cards.map((card) => (
              <QuickCard key={card.id} card={card} sessionId={sessionId} />
            ))}
          </div>
          <Button onClick={onComplete} className="w-full" size="lg">
            Finish session 🎉
          </Button>
        </>
      )}
    </div>
  )
}
