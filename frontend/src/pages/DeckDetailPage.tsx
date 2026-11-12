import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, BookOpen, ChevronLeft, ChevronRight, Gamepad2, Play, Trash2 } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import ImportDialog from '@/features/flashcard/components/ImportDialog'
import { useDecks, useDeckCards, useDeleteCard } from '@/features/flashcard/api'
import type { Card as FlashCard } from '@/shared/types/api'

const STATE_COLORS: Record<string, string> = {
  NEW: 'bg-blue-100 text-blue-800',
  LEARNING: 'bg-yellow-100 text-yellow-800',
  REVIEW: 'bg-green-100 text-green-800',
  RELEARNING: 'bg-red-100 text-red-800',
}

const CEFR_COLORS: Record<string, string> = {
  A1: 'bg-green-100 text-green-800', A2: 'bg-green-200 text-green-900',
  B1: 'bg-blue-100 text-blue-800', B2: 'bg-purple-100 text-purple-800',
  C1: 'bg-orange-100 text-orange-800', C2: 'bg-red-100 text-red-800',
}

export default function DeckDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)

  const { data: decks = [] } = useDecks()
  const deck = decks.find((d) => d.id === id)
  const { data: cardsPage, isLoading } = useDeckCards(id!, page)

  if (!id) return null

  const dueCount = deck?.dueCount ?? 0
  const cardCount = deck?.cardCount ?? 0

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
        description={`${cardCount} cards · ${dueCount} due`}
        action={
          <div className="flex flex-wrap gap-2">
            <ImportDialog deckId={id} />
            <Button
              variant="outline"
              className="gap-2"
              disabled={cardCount === 0}
              onClick={() => navigate(`/decks/${id}/games`)}
            >
              <Gamepad2 className="h-4 w-4" /> Games
            </Button>
            <Button className="gap-2" onClick={() => navigate(`/decks/${id}/review`)} disabled={dueCount === 0}>
              <Play className="h-4 w-4" />
              Review {dueCount > 0 && `(${dueCount})`}
            </Button>
          </div>
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
          <p className="text-sm">Import a word list, save words from videos, or search the dictionary</p>
          <div className="flex gap-2">
            <ImportDialog deckId={id} />
            <Button variant="outline" onClick={() => navigate('/decks')}>
              Search vocabulary
            </Button>
          </div>
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
                aria-label="Previous page"
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
                aria-label="Next page"
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
  const nextReview = new Date(card.nextReview).toLocaleDateString()

  return (
    <Card>
      <CardContent className="py-3 px-4 flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="font-semibold">{card.front}</span>
            {card.ipa && <span className="text-xs text-muted-foreground">{card.ipa}</span>}
            {card.vocab ? (
              <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${CEFR_COLORS[card.vocab.cefrLevel] ?? ''}`}>
                {card.vocab.cefrLevel}
              </span>
            ) : (
              <span className="text-xs px-1.5 py-0.5 rounded font-medium bg-muted text-muted-foreground">
                Custom
              </span>
            )}
            <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${STATE_COLORS[card.state] ?? ''}`}>
              {card.state}
            </span>
          </div>
          <p className="text-sm text-muted-foreground line-clamp-1">{card.back}</p>
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
            if (confirm(`Remove "${card.front}" from this deck?`)) {
              deleteCard.mutate(card.id)
            }
          }}
          aria-label={`Remove ${card.front}`}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </CardContent>
    </Card>
  )
}
