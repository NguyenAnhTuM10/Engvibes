import { cn } from '@/lib/utils'
import type { WordMatch } from '@/shared/types/api'

const statusClass: Record<string, string> = {
  MATCH: 'text-green-600 dark:text-green-400',
  MISSING: 'text-red-500 line-through opacity-60',
  EXTRA: 'text-gray-400 line-through',
  MISPRONOUNCED: 'text-orange-500 dark:text-orange-400 underline decoration-wavy',
}

export default function WordDiffDisplay({ matches }: { matches: WordMatch[] }) {
  return (
    <p className="text-sm leading-relaxed flex flex-wrap gap-x-1">
      {matches.map((m, i) => (
        <span key={i} className={cn('font-medium', statusClass[m.status])}>
          {m.word}
        </span>
      ))}
    </p>
  )
}
