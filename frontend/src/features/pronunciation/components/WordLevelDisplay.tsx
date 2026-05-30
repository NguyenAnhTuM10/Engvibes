import { cn } from '@/lib/utils'
import type { WordAnalysis } from '../types'

interface Props {
  sentence: string
  wordAnalyses: WordAnalysis[] | null
  mode: 'preview' | 'result'
}

function wordChipClass(score: number, heard: string | null) {
  if (heard === null) return 'bg-gray-100 text-gray-400 border-gray-300 dark:bg-gray-800 dark:text-gray-500'
  if (score >= 80)   return 'bg-green-100 text-green-800 border-green-400 dark:bg-green-900/30 dark:text-green-400'
  if (score >= 60)   return 'bg-yellow-100 text-yellow-800 border-yellow-400 dark:bg-yellow-900/30 dark:text-yellow-400'
  return 'bg-red-100 text-red-800 border-red-400 dark:bg-red-900/30 dark:text-red-400'
}

export function WordLevelDisplay({ sentence, wordAnalyses, mode }: Props) {
  if (mode === 'preview' || !wordAnalyses?.length) {
    return (
      <div className="flex flex-wrap justify-center gap-2 py-2">
        {sentence.split(' ').map((word, i) => (
          <span
            key={i}
            className="px-3 py-1.5 rounded-lg border-2 border-muted text-lg font-semibold text-muted-foreground"
          >
            {word}
          </span>
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap justify-center gap-2 py-2">
        {wordAnalyses.map((w, i) => (
          <div key={i} className="flex flex-col items-center gap-0.5">
            <span
              className={cn(
                'px-3 py-1.5 rounded-lg border-2 text-lg font-semibold transition-colors',
                wordChipClass(w.score, w.heard),
              )}
              title={w.heard ? `Heard: "${w.heard}" — ${w.score}/100` : 'Not detected'}
            >
              {w.word}
            </span>
            <span className="text-[10px] font-mono text-muted-foreground">
              {w.heard === null ? '—' : `${w.score}`}
            </span>
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="flex justify-center gap-4 text-xs text-muted-foreground">
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-sm bg-green-400 inline-block" /> ≥80
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-sm bg-yellow-400 inline-block" /> 60–79
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-sm bg-red-400 inline-block" /> &lt;60
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-sm bg-gray-300 inline-block" /> missed
        </span>
      </div>
    </div>
  )
}
