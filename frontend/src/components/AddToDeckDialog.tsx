import { useState } from 'react'
import { toast } from 'sonner'
import { useDecks, useAddCard } from '@/features/flashcard/api'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import type { CardSource, Vocab } from '@/shared/types/api'

interface AddToDeckDialogProps {
  vocab: Vocab
  contextSentence?: string
  sourceVideoId?: string
  sourceType?: CardSource
  onClose: () => void
}

export default function AddToDeckDialog({
  vocab,
  contextSentence,
  sourceVideoId,
  sourceType = 'MANUAL',
  onClose,
}: AddToDeckDialogProps) {
  const { data: decks = [] } = useDecks()
  const addCard = useAddCard()
  const defaultDeck = decks.find((d) => d.isDefault)
  const [deckId, setDeckId] = useState<string>(defaultDeck?.id ?? '')

  const handleSubmit = () => {
    if (!deckId) return
    addCard.mutate(
      { vocabId: vocab.id, deckId, contextSentence, sourceVideoId, sourceType },
      {
        onSuccess: () => {
          const deck = decks.find((d) => d.id === deckId)
          toast.success(`Added "${vocab.word}" to ${deck?.name ?? 'deck'}`)
          onClose()
        },
      },
    )
  }

  return (
    <div className="space-y-4">
      <div>
        <p className="font-semibold text-lg">{vocab.word}</p>
        <p className="text-sm text-muted-foreground">{vocab.definition}</p>
      </div>
      <div className="space-y-1">
        <Label>Add to deck</Label>
        <Select value={deckId} onValueChange={setDeckId}>
          <SelectTrigger>
            <SelectValue placeholder="Select deck..." />
          </SelectTrigger>
          <SelectContent>
            {decks.map((d) => (
              <SelectItem key={d.id} value={d.id}>
                {d.name}
                {d.isDefault && ' (default)'}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} disabled={!deckId || addCard.isPending}>
          {addCard.isPending ? 'Adding...' : 'Add to deck'}
        </Button>
      </div>
    </div>
  )
}
