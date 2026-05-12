import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, BookOpen, ChevronLeft, ChevronRight, Trash2 } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useDecks, useDeckCards, useDeleteCard } from '@/features/flashcard/api'
import type { Card as FlashCard } from '@/shared/types/api'

const STATE_COLORS: Record<string, string> = {
  NEW: 'bg-blue-100 text-blue-800',
  LEARNING: 'bg-yellow-100 text-yellow-800',
  REVIEW: 'bg-green-100 text-green-800',
  RELEARNING: 'bg-red-100 text-red-800',
}

export default function DeckDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)

  const { data: decks = [] } = useDecks()
  const deck = decks.find((d) => d.id === id)
  const { data: cardsPage, isLoading } = useDeckCards(id!, page)

  if (!id) return null

  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate('/decks')}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Decks
        </Button>
      </div>

      <PageHeader
        title={deck?.name ?? 'Deck'}
        description={`${deck?.cardCount ?? 0} cards · ${deck?.dueCount ?? 0} due`}
        action={
          <Button onClick={() => navigate(`/decks/${id}/review`)} disabled={deck?.dueCount === 0}>
            Review {(deck?.dueCount ?? 0) > 0 && `(${deck!.dueCount})`}
          </Button>
        }
      />

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-16 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : !cardsPage || cardsPage.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
          <BookOpen className="h-12 w-12 opacity-30" />
          <p className="text-lg font-medium">No cards yet</p>
          <p className="text-sm">Add some from videos or search vocabulary</p>
          <Button variant="outline" onClick={() => navigate('/decks')}>
            Search vocabulary
          </Button>
        </div>
      ) : (
        <>
          <div className="space-y-2">
            {cardsPage.content.map((card) => (
              <CardItem key={card.id} card={card} />
            ))}
          </div>

          {cardsPage.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 mt-6">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {page + 1} of {cardsPage.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={cardsPage.last}
                onClick={() => setPage((p) => p + 1)}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function CardItem({ card }: { card: FlashCard }) {
  const deleteCard = useDeleteCard()

  const nextReview = card.nextReview
    ? new Date(card.nextReview).toLocaleDateString()
    : '—'

  return (
    <Card>
      <CardContent className="py-3 px-4 flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="font-semibold">{card.vocab.word}</span>
            {card.vocab.ipa && (
              <span className="text-xs text-muted-foreground">{card.vocab.ipa}</span>
            )}
            <span
              className={`text-xs px-1.5 py-0.5 rounded font-medium ${
                { A1: 'bg-green-100 text-green-800', A2: 'bg-green-200 text-green-900',
                  B1: 'bg-blue-100 text-blue-800', B2: 'bg-purple-100 text-purple-800',
                  C1: 'bg-orange-100 text-orange-800', C2: 'bg-red-100 text-red-800' }[card.vocab.cefrLevel] ?? ''
              }`}
            >
              {card.vocab.cefrLevel}
            </span>
            <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${STATE_COLORS[card.state] ?? ''}`}>
              {card.state}
            </span>
          </div>
          <p className="text-sm text-muted-foreground line-clamp-1">{card.vocab.definition}</p>
          {card.contextSentence && (
            <p className="text-xs text-muted-foreground mt-0.5 italic line-clamp-1">
              "{card.contextSentence}"
            </p>
          )}
          <p className="text-xs text-muted-foreground mt-0.5">Next review: {nextReview}</p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="shrink-0 text-muted-foreground hover:text-destructive"
          onClick={() => {
            if (confirm(`Remove "${card.vocab.word}" from this deck?`)) {
              deleteCard.mutate(card.id)
            }
          }}
          aria-label={`Remove ${card.vocab.word}`}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </CardContent>
    </Card>
  )
}
