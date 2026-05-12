import { CheckCircle2, Circle, Clock } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ShadowAttemptResult, SubtitleSegment } from '@/shared/types/api'

interface Props {
  segments: SubtitleSegment[]
  results: Record<number, ShadowAttemptResult>
  currentIdx: number
  onSelect: (idx: number) => void
}

export default function SegmentList({ segments, results, currentIdx, onSelect }: Props) {
  return (
    <div className="flex flex-col gap-1 overflow-y-auto">
      {segments.map((seg, i) => {
        const result = results[seg.orderIndex]
        const isCurrent = i === currentIdx

        return (
          <button
            key={seg.id}
            onClick={() => onSelect(i)}
            className={cn(
              'flex items-start gap-2.5 text-left px-3 py-2.5 rounded-lg text-sm transition-colors',
              isCurrent
                ? 'bg-primary/10 border border-primary/30'
                : 'hover:bg-accent',
            )}
          >
            <div className="shrink-0 mt-0.5">
              {result ? (
                <CheckCircle2 className="h-4 w-4 text-green-500" />
              ) : isCurrent ? (
                <Clock className="h-4 w-4 text-primary" />
              ) : (
                <Circle className="h-4 w-4 text-muted-foreground" />
              )}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-medium">{seg.text}</p>
              {result && (
                <p
                  className={cn(
                    'text-xs mt-0.5',
                    result.score >= 80
                      ? 'text-green-600'
                      : result.score >= 60
                      ? 'text-blue-600'
                      : 'text-orange-500',
                  )}
                >
                  {Math.round(result.score)}%
                </p>
              )}
            </div>
          </button>
        )
      })}
    </div>
  )
}
