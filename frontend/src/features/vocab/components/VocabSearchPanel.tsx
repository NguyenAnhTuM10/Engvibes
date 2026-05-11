import { useState } from 'react'
import { Search, Plus } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useSearchVocab } from '@/features/vocab/api'
import { useDebounce } from '@/shared/hooks/useDebounce'
import AddToDeckDialog from '@/components/AddToDeckDialog'
import type { Vocab } from '@/shared/types/api'

const CEFR_COLORS: Record<string, string> = {
  A1: 'bg-green-100 text-green-800',
  A2: 'bg-green-200 text-green-900',
  B1: 'bg-blue-100 text-blue-800',
  B2: 'bg-purple-100 text-purple-800',
  C1: 'bg-orange-100 text-orange-800',
  C2: 'bg-red-100 text-red-800',
}

export default function VocabSearchPanel() {
  const [query, setQuery] = useState('')
  const [addTarget, setAddTarget] = useState<Vocab | null>(null)
  const debouncedQuery = useDebounce(query, 300)
  const { data, isFetching } = useSearchVocab(debouncedQuery)

  return (
    <div className="space-y-3">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search vocabulary..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="pl-9"
        />
      </div>

      {isFetching && (
        <p className="text-sm text-muted-foreground text-center py-2">Searching...</p>
      )}

      {data && data.content.length === 0 && debouncedQuery && !isFetching && (
        <p className="text-sm text-muted-foreground text-center py-4">
          No results for "{debouncedQuery}"
        </p>
      )}

      <div className="space-y-2 max-h-80 overflow-y-auto">
        {data?.content.map((vocab) => (
          <div
            key={vocab.id}
            className="flex items-start justify-between gap-3 p-3 rounded-md border bg-card hover:bg-accent/50 transition-colors"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-semibold text-sm">{vocab.word}</span>
                {vocab.ipa && (
                  <span className="text-xs text-muted-foreground">{vocab.ipa}</span>
                )}
                <span
                  className={`text-xs px-1.5 py-0.5 rounded font-medium ${CEFR_COLORS[vocab.cefrLevel] ?? ''}`}
                >
                  {vocab.cefrLevel}
                </span>
                <Badge variant="outline" className="text-xs">
                  {vocab.partOfSpeech}
                </Badge>
              </div>
              <p className="text-sm text-muted-foreground mt-0.5 line-clamp-1">
                {vocab.definition}
              </p>
            </div>
            <Button
              size="icon"
              variant="ghost"
              className="shrink-0"
              onClick={() => setAddTarget(vocab)}
              aria-label={`Add ${vocab.word} to deck`}
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>
        ))}
      </div>

      {addTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-background rounded-lg shadow-xl p-6 w-full max-w-sm mx-4">
            <AddToDeckDialog vocab={addTarget} onClose={() => setAddTarget(null)} />
          </div>
        </div>
      )}
    </div>
  )
}
