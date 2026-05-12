import { useState } from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import AudioRecorder from '@/shared/components/AudioRecorder'
import WordDiffDisplay from '@/features/session/components/WordDiffDisplay'
import SegmentList from '@/features/session/components/SegmentList'
import { useListenSubtitles, useSubmitShadowAttempt } from '@/features/session/api'
import type { ShadowAttemptResult } from '@/shared/types/api'

interface Props {
  sessionId: string
  onComplete: () => void
}

export default function ShadowStep({ sessionId, onComplete }: Props) {
  const { data: segments, isLoading } = useListenSubtitles(sessionId)
  const [currentIdx, setCurrentIdx] = useState(0)
  const [results, setResults] = useState<Record<number, ShadowAttemptResult>>({})
  const [attempts, setAttempts] = useState<Record<number, number>>({})
  const submitAttempt = useSubmitShadowAttempt(sessionId)

  const MAX_ATTEMPTS = 3
  const seg = segments?.[currentIdx]
  const currentResult = seg ? results[seg.orderIndex] : undefined
  const currentAttempts = seg ? (attempts[seg.orderIndex] ?? 0) : 0
  const exhausted = currentAttempts >= MAX_ATTEMPTS

  const handleSubmit = (blob: Blob) => {
    if (!seg) return
    submitAttempt.mutate(
      { segmentIdx: seg.orderIndex, audio: blob },
      {
        onSuccess: (data: ShadowAttemptResult) => {
          setResults((prev) => ({ ...prev, [seg.orderIndex]: data }))
          setAttempts((prev) => ({
            ...prev,
            [seg.orderIndex]: (prev[seg.orderIndex] ?? 0) + 1,
          }))
        },
      },
    )
  }

  const handleNext = () => {
    if (!segments) return
    if (currentIdx < segments.length - 1) setCurrentIdx(currentIdx + 1)
    else onComplete()
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!segments || segments.length === 0) {
    return (
      <div className="flex flex-col items-center py-24 gap-4 text-muted-foreground">
        <p>No segments available</p>
        <Button onClick={onComplete}>Continue</Button>
      </div>
    )
  }

  const doneCount = Object.keys(results).length

  return (
    <div className="flex gap-6 max-w-4xl mx-auto h-full">
      {/* Sidebar */}
      <aside className="hidden md:flex flex-col w-52 shrink-0 border rounded-xl p-2 max-h-[calc(100vh-12rem)] overflow-hidden">
        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide px-2 py-1.5 mb-1">
          {doneCount}/{segments.length} done
        </p>
        <SegmentList
          segments={segments}
          results={results}
          currentIdx={currentIdx}
          onSelect={setCurrentIdx}
        />
      </aside>

      {/* Main panel */}
      <div className="flex-1 min-w-0 space-y-5">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-semibold">Shadow the speaker</h2>
            <p className="text-sm text-muted-foreground mt-0.5">
              Listen then repeat — match pronunciation as closely as possible
            </p>
          </div>
          <Button variant="ghost" size="sm" onClick={onComplete}>
            Skip step
          </Button>
        </div>

        {seg && (
          <div className="border rounded-xl p-6 space-y-5">
            {/* Segment counter (mobile) */}
            <div className="flex items-center justify-between md:hidden">
              <span className="text-xs text-muted-foreground">
                {currentIdx + 1} / {segments.length}
              </span>
              <span className="text-xs text-muted-foreground">{doneCount} done</span>
            </div>

            {/* Phrase to shadow */}
            <div className="bg-muted/50 rounded-lg px-5 py-4">
              <p className="text-xl font-medium leading-relaxed text-center">{seg.text}</p>
            </div>

            {/* Result */}
            {currentResult && (
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <span
                    className={cn(
                      'text-3xl font-bold tabular-nums',
                      currentResult.score >= 80
                        ? 'text-green-600'
                        : currentResult.score >= 60
                        ? 'text-blue-600'
                        : 'text-orange-500',
                    )}
                  >
                    {Math.round(currentResult.score)}%
                  </span>
                  <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                    <div
                      className={cn(
                        'h-full rounded-full',
                        currentResult.score >= 80
                          ? 'bg-green-500'
                          : currentResult.score >= 60
                          ? 'bg-blue-500'
                          : 'bg-orange-400',
                      )}
                      style={{ width: `${currentResult.score}%` }}
                    />
                  </div>
                </div>

                <div>
                  <p className="text-xs text-muted-foreground mb-1">You said:</p>
                  <p className="text-sm italic text-muted-foreground">
                    "{currentResult.transcript}"
                  </p>
                </div>

                <WordDiffDisplay matches={currentResult.wordMatches} />

                {currentResult.weakPhonemes.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    <span className="text-xs text-muted-foreground">Weak phonemes:</span>
                    {currentResult.weakPhonemes.map((p) => (
                      <span
                        key={p}
                        className="text-xs bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300 px-2 py-0.5 rounded-full font-mono"
                      >
                        /{p}/
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Recorder */}
            <div className="border-t pt-4">
              <AudioRecorder
                maxDurationSec={20}
                isSubmitting={submitAttempt.isPending}
                onSubmit={handleSubmit}
                disabled={exhausted}
              />
              {exhausted && (
                <p className="text-xs text-muted-foreground text-center mt-2">
                  Max 3 attempts reached
                </p>
              )}
            </div>
          </div>
        )}

        {/* Nav buttons */}
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={currentIdx === 0}
            onClick={() => setCurrentIdx((i) => i - 1)}
          >
            ← Previous
          </Button>

          {currentIdx < segments.length - 1 ? (
            <Button size="sm" onClick={handleNext}>
              Next segment →
            </Button>
          ) : (
            <Button size="sm" onClick={onComplete}>
              Continue to Retell →
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
