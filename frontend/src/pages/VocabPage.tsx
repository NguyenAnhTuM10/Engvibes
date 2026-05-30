import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useVocabDecks, useCreateVocabDeck } from '@/features/vocab/sm2api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Plus, BookOpen, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'

export default function VocabPage() {
  const navigate = useNavigate()
  const { data: decks = [], isLoading } = useVocabDecks()
  const createDeck = useCreateVocabDeck()

  const [showCreate, setShowCreate] = useState(false)
  const [name, setName]             = useState('')
  const [desc, setDesc]             = useState('')

  const handleCreate = async () => {
    if (!name.trim()) return
    await createDeck.mutateAsync({ name: name.trim(), description: desc.trim() || undefined })
    toast.success('Deck created')
    setName(''); setDesc(''); setShowCreate(false)
  }

  return (
    <div className="max-w-2xl mx-auto py-6 px-4 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Vocabulary</h1>
          <p className="text-muted-foreground text-sm mt-1">
            SM-2 spaced repetition — review at the right time, every time.
          </p>
        </div>
        <Button onClick={() => setShowCreate(v => !v)} size="sm" className="gap-2">
          <Plus className="h-4 w-4" /> New Deck
        </Button>
      </div>

      {/* Create deck form */}
      {showCreate && (
        <Card>
          <CardContent className="pt-4 space-y-3">
            <Input
              placeholder="Deck name"
              value={name}
              onChange={e => setName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreate()}
              autoFocus
            />
            <Input
              placeholder="Description (optional)"
              value={desc}
              onChange={e => setDesc(e.target.value)}
            />
            <div className="flex gap-2 justify-end">
              <Button variant="ghost" size="sm" onClick={() => setShowCreate(false)}>Cancel</Button>
              <Button size="sm" disabled={!name.trim() || createDeck.isPending} onClick={handleCreate}>
                Create
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Deck list */}
      {isLoading ? (
        <div className="text-center py-12 text-muted-foreground">Loading…</div>
      ) : decks.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <BookOpen className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p>No decks yet. Create one to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {decks.map(deck => (
            <Card
              key={deck.id}
              className="cursor-pointer hover:shadow-sm transition-shadow"
              onClick={() => navigate(`/vocab/${deck.id}`)}
            >
              <CardHeader className="pb-2 pt-4 px-5">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base">{deck.name}</CardTitle>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </div>
              </CardHeader>
              {deck.description && (
                <CardContent className="pb-4 px-5 pt-0">
                  <p className="text-sm text-muted-foreground">{deck.description}</p>
                </CardContent>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
