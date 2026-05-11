import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BookOpen, MoreHorizontal, Plus, Search } from 'lucide-react'
import { toast } from 'sonner'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import VocabSearchPanel from '@/features/vocab/components/VocabSearchPanel'
import { useDecks, useCreateDeck, useDeleteDeck, useUpdateDeck } from '@/features/flashcard/api'
import type { Deck } from '@/shared/types/api'

const COLORS = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
  '#8b5cf6', '#06b6d4', '#f97316', '#6b7280',
]

export default function DecksPage() {
  const { data: decks = [], isLoading } = useDecks()
  const [showCreate, setShowCreate] = useState(false)
  const [showSearch, setShowSearch] = useState(false)
  const navigate = useNavigate()

  return (
    <div>
      <PageHeader
        title="Flashcard Decks"
        description="Manage and review your vocabulary decks"
        action={
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setShowSearch(!showSearch)}>
              <Search className="h-4 w-4 mr-1" />
              Search vocab
            </Button>
            <Button size="sm" onClick={() => setShowCreate(true)}>
              <Plus className="h-4 w-4 mr-1" />
              New deck
            </Button>
          </div>
        }
      />

      {showSearch && (
        <div className="mb-6 p-4 border rounded-lg bg-card">
          <h3 className="text-sm font-semibold mb-3">Search & add vocabulary</h3>
          <VocabSearchPanel />
        </div>
      )}

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-36 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : decks.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
          <BookOpen className="h-12 w-12 opacity-30" />
          <p className="text-lg font-medium">No decks yet</p>
          <p className="text-sm">Create your first deck to start studying</p>
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="h-4 w-4 mr-1" />
            New deck
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {decks.map((deck) => (
            <DeckCard key={deck.id} deck={deck} onOpen={() => navigate(`/decks/${deck.id}`)} />
          ))}
        </div>
      )}

      {showCreate && <CreateDeckDialog onClose={() => setShowCreate(false)} />}
    </div>
  )
}

function DeckCard({ deck, onOpen }: { deck: Deck; onOpen: () => void }) {
  const deleteDeck = useDeleteDeck()
  const updateDeck = useUpdateDeck()
  const [editingName, setEditingName] = useState(false)
  const [newName, setNewName] = useState(deck.name)
  const navigate = useNavigate()

  const handleRename = () => {
    if (!newName.trim() || newName === deck.name) { setEditingName(false); return }
    updateDeck.mutate({ id: deck.id, name: newName.trim() }, { onSuccess: () => setEditingName(false) })
  }

  return (
    <Card className="hover:shadow-md transition-shadow overflow-hidden">
      <div className="h-1.5" style={{ backgroundColor: deck.color ?? '#6b7280' }} />
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-2">
          {editingName ? (
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onBlur={handleRename}
              onKeyDown={(e) => { if (e.key === 'Enter') handleRename(); if (e.key === 'Escape') setEditingName(false) }}
              className="h-7 text-sm font-semibold"
              autoFocus
            />
          ) : (
            <CardTitle className="text-base leading-tight">{deck.name}</CardTitle>
          )}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-7 w-7 shrink-0">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => { setNewName(deck.name); setEditingName(true) }}>
                Rename
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              {!deck.isDefault && (
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={() => {
                    if (confirm(`Delete "${deck.name}"? This will remove all cards in it.`)) {
                      deleteDeck.mutate(deck.id)
                    }
                  }}
                >
                  Delete
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <p className="text-xs text-muted-foreground">
          {deck.cardCount} cards · {deck.dueCount} due
        </p>
      </CardHeader>
      <CardContent className="flex gap-2">
        <Button
          size="sm"
          variant="outline"
          className="flex-1"
          onClick={onOpen}
        >
          Browse
        </Button>
        <Button
          size="sm"
          className="flex-1"
          disabled={deck.dueCount === 0}
          onClick={() => navigate(`/decks/${deck.id}/review`)}
          title={deck.dueCount === 0 ? 'No cards due' : undefined}
        >
          Review {deck.dueCount > 0 && `(${deck.dueCount})`}
        </Button>
      </CardContent>
    </Card>
  )
}

function CreateDeckDialog({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState('')
  const [color, setColor] = useState(COLORS[0])
  const createDeck = useCreateDeck()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) { toast.error('Deck name is required'); return }
    createDeck.mutate({ name: name.trim(), color }, { onSuccess: onClose })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-lg shadow-xl p-6 w-full max-w-sm mx-4">
        <h2 className="text-lg font-semibold mb-4">New flashcard deck</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <Label htmlFor="deck-name">Name</Label>
            <Input
              id="deck-name"
              placeholder="e.g. B2 Business Vocab"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
            />
          </div>
          <div className="space-y-2">
            <Label>Color</Label>
            <div className="flex gap-2 flex-wrap">
              {COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColor(c)}
                  className={`h-7 w-7 rounded-full transition-transform ${color === c ? 'scale-125 ring-2 ring-offset-2 ring-foreground' : ''}`}
                  style={{ backgroundColor: c }}
                  aria-label={`Color ${c}`}
                />
              ))}
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="ghost" type="button" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={createDeck.isPending}>
              {createDeck.isPending ? 'Creating...' : 'Create'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
