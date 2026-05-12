import { useState } from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { useWarmupWords, useMarkWarmup } from '@/features/session/api'
import type { WarmupWordResponse } from '@/shared/types/api'

interface Props {
  sessionId: string
  onComplete: () => void
}

export default function WarmupStep({ sessionId, onComplete }: Props) {
  const { data: words, isLoading } = useWarmupWords(sessionId)
  const markWarmup = useMarkWarmup(sessionId)
  const [marked, setMarked] = useState<Record<string, 'known' | 'new'>>({})

  const handleMark = (word: WarmupWordResponse, status: 'known' | 'new') => {
    markWarmup.mutate({ vocabId: word.vocabId, status })
    setMarked((prev) => ({ ...prev, [word.vocabId]: status }))
  }

  const markedCount = Object.keys(marked).length
  const allDone = words ? markedCount >= words.length : false

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="h-36 rounded-lg bg-muted animate-pulse" />
        ))}
      </div>
    )
  }

  if (!words || words.length === 0) {
    return (
      <div className="flex flex-col items-center py-24 gap-4 text-muted-foreground">
        <p>No warmup words for this video</p>
        <Button onClick={onComplete}>Continue to Listen</Button>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold">Let's preview some new words</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            {words.length} words — mark each one before continuing
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={onComplete}>
          Skip warmup
        </Button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {words.map((word) => {
          const status = marked[word.vocabId]
          return (
            <div
              key={word.vocabId}
              className={cn(
                'rounded-lg border p-4 flex flex-col gap-3 transition-all duration-200',
                status === 'known' && 'opacity-50 scale-[0.97]',
                status === 'new' &&
                  'border-blue-400 bg-blue-50/50 dark:bg-blue-950/20',
              )}
            >
              <div className="flex-1 space-y-1 min-h-0">
                <p className="font-bold text-base leading-tight">{word.word}</p>
                {word.ipa && (
                  <p className="text-xs text-muted-foreground font-mono">{word.ipa}</p>
                )}
                {word.partOfSpeech && (
                  <p className="text-xs italic text-muted-foreground">{word.partOfSpeech}</p>
                )}
                {word.definition && (
                  <p className="text-xs mt-1 line-clamp-3 leading-relaxed">{word.definition}</p>
                )}
              </div>

              {!status ? (
                <div className="flex gap-1.5">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 text-xs h-7"
                    onClick={() => handleMark(word, 'known')}
                  >
                    I know
                  </Button>
                  <Button
                    size="sm"
                    className="flex-1 text-xs h-7"
                    onClick={() => handleMark(word, 'new')}
                  >
                    New!
                  </Button>
                </div>
              ) : (
                <p className="text-xs text-center text-muted-foreground font-medium">
                  {status === 'known' ? '✓ Known' : '📖 Added to deck'}
                </p>
              )}
            </div>
          )
        })}
      </div>

      {allDone && (
        <div className="flex justify-center pt-2">
          <Button onClick={onComplete} size="lg">
            Continue to Listen →
          </Button>
        </div>
      )}
    </div>
  )
}
