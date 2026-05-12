import { useEffect, useState } from 'react'
import { Search, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useDebounce } from '@/shared/hooks/useDebounce'
import type { CefrLevel } from '@/shared/types/api'

const CEFR_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const
const TOPICS = ['food', 'travel', 'technology', 'education', 'sport', 'health'] as const

interface Props {
  search: string
  onSearchChange: (v: string) => void
  cefrLevel: CefrLevel | null
  onCefrChange: (v: CefrLevel | null) => void
  topic: string | null
  onTopicChange: (v: string | null) => void
}

export default function VideoFilters({
  search,
  onSearchChange,
  cefrLevel,
  onCefrChange,
  topic,
  onTopicChange,
}: Props) {
  const [localSearch, setLocalSearch] = useState(search)
  const debounced = useDebounce(localSearch, 350)

  useEffect(() => {
    onSearchChange(debounced)
  }, [debounced]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="mb-6 space-y-3 sticky top-0 z-10 bg-background/95 backdrop-blur pb-3 pt-1">
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
        <input
          type="text"
          placeholder="Search videos..."
          value={localSearch}
          onChange={(e) => setLocalSearch(e.target.value)}
          className="w-full pl-9 pr-8 py-2 text-sm border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-ring"
        />
        {localSearch && (
          <button
            onClick={() => {
              setLocalSearch('')
              onSearchChange('')
            }}
            aria-label="Clear search"
            className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5"
          >
            <X className="h-4 w-4 text-muted-foreground" />
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {CEFR_LEVELS.map((level) => (
          <button
            key={level}
            onClick={() => onCefrChange(cefrLevel === level ? null : level)}
            className={cn(
              'px-3 py-1 text-xs font-semibold rounded-full border transition-colors',
              cefrLevel === level
                ? 'bg-primary text-primary-foreground border-primary'
                : 'border-border hover:bg-accent',
            )}
          >
            {level}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap gap-2">
        {TOPICS.map((t) => (
          <button
            key={t}
            onClick={() => onTopicChange(topic === t ? null : t)}
            className={cn(
              'px-3 py-1 text-xs rounded-full border capitalize transition-colors',
              topic === t
                ? 'bg-secondary text-secondary-foreground border-secondary-foreground/20'
                : 'border-border hover:bg-accent',
            )}
          >
            {t}
          </button>
        ))}
      </div>
    </div>
  )
}
