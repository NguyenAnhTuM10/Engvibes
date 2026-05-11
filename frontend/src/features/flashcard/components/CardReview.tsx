import { useState, useCallback } from 'react'
import { useKeyboard } from '@/shared/hooks/useKeyboard'
import { Badge } from '@/components/ui/badge'
import RatingButtons from './RatingButtons'
import type { Card } from '@/shared/types/api'

const CEFR_COLORS: Record<string, string> = {
  A1: 'bg-green-100 text-green-800',
  A2: 'bg-green-200 text-green-900',
  B1: 'bg-blue-100 text-blue-800',
  B2: 'bg-purple-100 text-purple-800',
  C1: 'bg-orange-100 text-orange-800',
  C2: 'bg-red-100 text-red-800',
}

interface CardReviewProps {
  card: Card
  onRate: (rating: 1 | 2 | 3 | 4) => void
  isSubmitting: boolean
}

export default function CardReview({ card, onRate, isSubmitting }: CardReviewProps) {
  const [flipped, setFlipped] = useState(false)

  const flip = useCallback(() => setFlipped(true), [])

  const handleRate = useCallback(
    (rating: 1 | 2 | 3 | 4) => {
      setFlipped(false)
      onRate(rating)
    },
    [onRate],
  )

  useKeyboard({
    ' ': () => !flipped && flip(),
    '1': () => flipped && handleRate(1),
    '2': () => flipped && handleRate(2),
    '3': () => flipped && handleRate(3),
    '4': () => flipped && handleRate(4),
  })

  const { vocab, contextSentence } = card

  return (
    <div className="flex flex-col items-center gap-6 w-full max-w-xl mx-auto">
      {/* Card */}
      <div
        className="perspective-1000 w-full cursor-pointer select-none"
        style={{ height: 280 }}
        onClick={() => !flipped && flip()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && !flipped && flip()}
        aria-label={flipped ? 'Card back' : 'Card front — click to reveal'}
      >
        <div
          className={`transform-style-3d relative w-full h-full transition-transform duration-500 ${
            flipped ? 'rotate-y-180' : ''
          }`}
        >
          {/* Front */}
          <div className="backface-hidden absolute inset-0 flex flex-col items-center justify-center rounded-xl border-2 border-border bg-card shadow-lg p-8">
            <p className="text-4xl font-bold text-foreground mb-3">{vocab.word}</p>
            {vocab.ipa && (
              <p className="text-lg text-muted-foreground mb-3">{vocab.ipa}</p>
            )}
            <span
              className={`text-xs px-2 py-0.5 rounded-full font-semibold ${CEFR_COLORS[vocab.cefrLevel] ?? ''}`}
            >
              {vocab.cefrLevel}
            </span>
            <p className="text-sm text-muted-foreground mt-6">Press Space or click to reveal</p>
          </div>

          {/* Back */}
          <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-start justify-center rounded-xl border-2 border-primary bg-card shadow-lg p-8 overflow-y-auto">
            <div className="w-full">
              <div className="flex items-baseline gap-3 flex-wrap mb-1">
                <p className="text-2xl font-bold">{vocab.word}</p>
                {vocab.ipa && (
                  <p className="text-sm text-muted-foreground">{vocab.ipa}</p>
                )}
                <Badge variant="outline" className="text-xs">{vocab.partOfSpeech}</Badge>
              </div>
              <span
                className={`text-xs px-2 py-0.5 rounded-full font-semibold ${CEFR_COLORS[vocab.cefrLevel] ?? ''}`}
              >
                {vocab.cefrLevel}
              </span>
              <p className="mt-4 text-base text-foreground leading-relaxed">{vocab.definition}</p>
              {contextSentence && (
                <p className="mt-3 text-sm text-muted-foreground italic border-l-2 border-border pl-3">
                  "{contextSentence}"
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Rating buttons — shown when flipped */}
      <div className={`w-full transition-opacity duration-300 ${flipped ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}>
        <RatingButtons onRate={handleRate} disabled={isSubmitting} />
      </div>

      {!flipped && (
        <p className="text-xs text-muted-foreground">Keyboard: Space to flip · 1-4 to rate after flip · Esc to exit</p>
      )}
    </div>
  )
}
