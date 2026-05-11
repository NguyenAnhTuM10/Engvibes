import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { useReviewCard } from '@/features/flashcard/api'
import type { Card } from '@/shared/types/api'

interface QuickReviewModeProps {
  cards: Card[]
  onDone: () => void
}

export default function QuickReviewMode({ cards, onDone }: QuickReviewModeProps) {
  const [flipped, setFlipped] = useState<Record<string, boolean>>({})
  const reviewCard = useReviewCard()

  const handleRate = (id: string, rating: 3 | 4) => {
    reviewCard.mutate({ id, rating })
    setFlipped((prev) => ({ ...prev, [id]: false }))
  }

  if (cards.length === 0) {
    return (
      <div className="text-center py-6 text-muted-foreground">
        <p>No cards added in this session</p>
        <Button className="mt-3" onClick={onDone}>Continue</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">{cards.length} cards added this session</p>
      <div className="flex gap-3 overflow-x-auto pb-2">
        {cards.map((card) => {
          const isFlipped = flipped[card.id]
          return (
            <div
              key={card.id}
              className="shrink-0 w-44 border rounded-lg bg-card cursor-pointer select-none"
              style={{ minHeight: 120 }}
              onClick={() => !isFlipped && setFlipped((p) => ({ ...p, [card.id]: true }))}
            >
              {!isFlipped ? (
                <div className="flex flex-col items-center justify-center h-full p-4 text-center">
                  <p className="font-bold text-base">{card.vocab.word}</p>
                  {card.vocab.ipa && (
                    <p className="text-xs text-muted-foreground mt-1">{card.vocab.ipa}</p>
                  )}
                  <p className="text-[11px] text-muted-foreground mt-3">tap to reveal</p>
                </div>
              ) : (
                <div className="flex flex-col p-3 h-full">
                  <p className="font-semibold text-sm mb-1">{card.vocab.word}</p>
                  <p className="text-xs text-muted-foreground flex-1 line-clamp-3">
                    {card.vocab.definition}
                  </p>
                  <div className="flex gap-1 mt-2">
                    <Button
                      size="sm"
                      variant="outline"
                      className="flex-1 text-xs h-7 text-orange-600 border-orange-300 hover:bg-orange-50"
                      onClick={(e) => { e.stopPropagation(); handleRate(card.id, 3) }}
                    >
                      Hard
                    </Button>
                    <Button
                      size="sm"
                      className="flex-1 text-xs h-7 bg-green-600 hover:bg-green-700"
                      onClick={(e) => { e.stopPropagation(); handleRate(card.id, 4) }}
                    >
                      Easy
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )
        })}
      </div>
      <Button onClick={onDone} className="w-full">Done</Button>
    </div>
  )
}
