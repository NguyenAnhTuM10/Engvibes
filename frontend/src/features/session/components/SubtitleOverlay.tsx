import { useEffect, useRef } from 'react'
import { cn } from '@/lib/utils'
import type { SubtitleSegment } from '@/shared/types/api'

interface Props {
  segments: SubtitleSegment[]
  currentSec: number
  onWordClick: (word: string, segmentId: string) => void
}

export default function SubtitleOverlay({ segments, currentSec, onWordClick }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const activeRef = useRef<HTMLDivElement>(null)

  const currentMs = currentSec * 1000

  const activeIdx = segments.findLastIndex(
    (seg) => seg.startMs <= currentMs && currentMs <= seg.endMs,
  )

  useEffect(() => {
    if (activeRef.current && containerRef.current) {
      activeRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, [activeIdx])

  return (
    <div ref={containerRef} className="space-y-3 overflow-y-auto max-h-full pr-1">
      {segments.map((seg, i) => {
        const isActive = i === activeIdx
        const words = seg.words && seg.words.length > 0 ? seg.words : null

        return (
          <div
            key={seg.id}
            ref={isActive ? activeRef : undefined}
            className={cn(
              'rounded-lg px-4 py-3 text-sm leading-relaxed transition-all duration-200',
              isActive
                ? 'bg-primary/10 border border-primary/30 text-foreground'
                : 'text-muted-foreground hover:text-foreground/80',
            )}
          >
            {words ? (
              <span className="flex flex-wrap gap-x-1">
                {words.map((w, wi) => {
                  const isWordActive =
                    isActive && w.startMs <= currentMs && currentMs <= w.endMs
                  return (
                    <ClickableWord
                      key={wi}
                      word={w.word}
                      isActive={isWordActive}
                      onClick={() => onWordClick(w.word, seg.id)}
                    />
                  )
                })}
              </span>
            ) : (
              <span className="flex flex-wrap gap-x-1">
                {seg.text.split(/\s+/).map((w, wi) => (
                  <ClickableWord
                    key={wi}
                    word={w}
                    isActive={false}
                    onClick={() => onWordClick(w.replace(/[^a-zA-Z'-]/g, ''), seg.id)}
                  />
                ))}
              </span>
            )}
          </div>
        )
      })}
    </div>
  )
}

function ClickableWord({
  word,
  isActive,
  onClick,
}: {
  word: string
  isActive: boolean
  onClick: () => void
}) {
  const clean = word.replace(/[^a-zA-Z'-]/g, '')
  if (!clean) return <span>{word} </span>

  return (
    <button
      onClick={onClick}
      className={cn(
        'inline hover:underline underline-offset-2 decoration-dotted transition-colors cursor-pointer bg-transparent border-0 p-0',
        isActive && 'font-semibold text-primary',
      )}
    >
      {word}
    </button>
  )
}
