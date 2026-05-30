import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useReviewQueue, useSubmitReview } from '@/features/vocab/sm2api'
import type { QueueItem } from '@/features/vocab/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ArrowLeft, RotateCcw, Volume2 } from 'lucide-react'
import { cn } from '@/lib/utils'

// SM-2 quality buttons: Again=1, Hard=3, Good=4, Easy=5
const RATINGS = [
  { quality: 1, label: 'Again', color: 'bg-red-100 hover:bg-red-200 text-red-800 border-red-300' },
  { quality: 3, label: 'Hard',  color: 'bg-orange-100 hover:bg-orange-200 text-orange-800 border-orange-300' },
  { quality: 4, label: 'Good',  color: 'bg-green-100 hover:bg-green-200 text-green-800 border-green-300' },
  { quality: 5, label: 'Easy',  color: 'bg-blue-100 hover:bg-blue-200 text-blue-800 border-blue-300' },
]

export default function VocabReviewPage() {
  const { id }  = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: queue = [], isLoading } = useReviewQueue(id)
  const submitReview = useSubmitReview()

  const [idx, setIdx]       = useState(0)
  const [flipped, setFlip]  = useState(false)
  const [done, setDone]     = useState(0)

  const card: QueueItem | undefined = queue[idx]

  const handleRate = async (quality: number) => {
    if (!card) return
    await submitReview.mutateAsync({ cardId: card.cardId, quality })
    setDone(d => d + 1)
    setFlip(false)
    if (idx + 1 >= queue.length) {
      setIdx(queue.length)   // trigger "all done"
    } else {
      setIdx(i => i + 1)
    }
  }

  const speak = (text: string) =>
    window.speechSynthesis.speak(
      Object.assign(new SpeechSynthesisUtterance(text), { lang: 'en-US', rate: 0.85 })
    )

  if (isLoading) {
    return <div className="flex items-center justify-center min-h-[60vh] text-muted-foreground">Loading…</div>
  }

  const allDone = !isLoading && (queue.length === 0 || idx >= queue.length)

  // ── All done ──
  if (allDone) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-6 px-4 text-center">
        <div className="text-6xl">🎉</div>
        <div>
          <h2 className="text-2xl font-bold">All caught up!</h2>
          <p className="text-muted-foreground mt-1">
            Reviewed {done} card{done !== 1 ? 's' : ''} this session.
          </p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline" onClick={() => navigate(`/vocab/${id}`)}>
            <ArrowLeft className="h-4 w-4 mr-2" /> Back to deck
          </Button>
          {done > 0 && (
            <Button onClick={() => { setIdx(0); setDone(0); setFlip(false) }}>
              <RotateCcw className="h-4 w-4 mr-2" /> Restart
            </Button>
          )}
        </div>
      </div>
    )
  }

  const progress = ((idx) / queue.length) * 100

  return (
    <div className="max-w-lg mx-auto py-6 px-4 space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(`/vocab/${id}`)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1">
          <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
        <span className="text-sm text-muted-foreground tabular-nums whitespace-nowrap">
          {idx + 1} / {queue.length}
        </span>
      </div>

      {/* Flashcard */}
      <div
        className="min-h-[260px] rounded-xl border-2 bg-card flex flex-col items-center justify-center p-8 cursor-pointer select-none transition-colors hover:bg-accent/30"
        onClick={() => !flipped && setFlip(true)}
      >
        {/* Front */}
        <div className="text-center space-y-2">
          <h2 className="text-4xl font-bold tracking-wide">{card?.front}</h2>
          {card?.ipa && (
            <p className="text-base font-mono text-muted-foreground">/{card.ipa}/</p>
          )}
          <button
            onClick={e => { e.stopPropagation(); card && speak(card.front) }}
            className="text-muted-foreground hover:text-foreground transition-colors p-1 rounded"
          >
            <Volume2 className="h-4 w-4" />
          </button>
        </div>

        {/* Back (revealed on flip) */}
        {flipped && (
          <div className="mt-6 pt-6 border-t w-full text-center space-y-2">
            <p className="text-xl font-medium text-foreground">{card?.back}</p>
            {card?.exampleSentence && (
              <p className="text-sm text-muted-foreground italic">
                "{card.exampleSentence}"
              </p>
            )}
            {card?.isNew && (
              <Badge variant="outline" className="text-xs">New card</Badge>
            )}
            {!card?.isNew && card?.intervalDays != null && (
              <Badge variant="outline" className="text-xs text-muted-foreground">
                Last interval: {card.intervalDays}d
              </Badge>
            )}
          </div>
        )}

        {/* Tap hint */}
        {!flipped && (
          <p className="mt-6 text-sm text-muted-foreground">Tap to reveal</p>
        )}
      </div>

      {/* Rating buttons (only after flip) */}
      {flipped && (
        <div className="grid grid-cols-4 gap-2">
          {RATINGS.map(r => (
            <button
              key={r.quality}
              disabled={submitReview.isPending}
              onClick={() => handleRate(r.quality)}
              className={cn(
                'rounded-lg border-2 py-3 text-sm font-semibold transition-colors disabled:opacity-50',
                r.color
              )}
            >
              {r.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
