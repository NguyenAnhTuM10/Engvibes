import { useState } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import AudioRecorder from '@/shared/components/AudioRecorder'
import WordDiffDisplay from '@/features/session/components/WordDiffDisplay'
import { useListenSubtitles, useSubmitPhraseAttempt } from '@/features/session/api'
import type { PhraseAttemptResult, SubtitleSegment } from '@/shared/types/api'
import { cn } from '@/lib/utils'

interface PhraseItemProps {
  segment: SubtitleSegment
  sessionId: string
  index: number
}

function PhraseItem({ segment, sessionId, index }: PhraseItemProps) {
  const [result, setResult] = useState<PhraseAttemptResult | null>(null)
  const [attempts, setAttempts] = useState(0)
  const submitAttempt = useSubmitPhraseAttempt(sessionId)

  const MAX_ATTEMPTS = 3
  const exhausted = attempts >= MAX_ATTEMPTS

  const handleSubmit = (blob: Blob) => {
    submitAttempt.mutate(
      { segmentIdx: segment.orderIndex, audio: blob },
      {
        onSuccess: (data: PhraseAttemptResult) => {
          setResult(data)
          setAttempts((n) => n + 1)
        },
      },
    )
  }

  return (
    <div className="border rounded-xl p-5 space-y-4">
      <div className="flex items-start gap-3">
        <span className="text-xs font-mono text-muted-foreground mt-0.5 shrink-0">
          #{index + 1}
        </span>
        <p className="text-base font-medium leading-relaxed">{segment.text}</p>
      </div>

      {!result ? (
        <div className="pl-6">
          <AudioRecorder
            maxDurationSec={30}
            isSubmitting={submitAttempt.isPending}
            onSubmit={handleSubmit}
            disabled={exhausted}
          />
          {exhausted && (
            <p className="text-xs text-muted-foreground mt-2 text-center">
              Max attempts reached
            </p>
          )}
        </div>
      ) : (
        <div className="pl-6 space-y-3">
          <div className="flex items-center gap-2">
            <span
              className={cn(
                'text-2xl font-bold tabular-nums',
                result.score >= 80
                  ? 'text-green-600'
                  : result.score >= 60
                  ? 'text-blue-600'
                  : 'text-orange-500',
              )}
            >
              {Math.round(result.score)}%
            </span>
            <div className="h-2 flex-1 bg-muted rounded-full overflow-hidden">
              <div
                className={cn(
                  'h-full rounded-full transition-all',
                  result.score >= 80
                    ? 'bg-green-500'
                    : result.score >= 60
                    ? 'bg-blue-500'
                    : 'bg-orange-400',
                )}
                style={{ width: `${result.score}%` }}
              />
            </div>
          </div>

          <div className="space-y-1">
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-wide">
              You said:
            </p>
            <p className="text-sm text-muted-foreground italic">"{result.transcript}"</p>
          </div>

          <WordDiffDisplay matches={result.wordMatches} />

          <div className="text-xs text-muted-foreground flex gap-3">
            <span className="text-green-600">■ Match</span>
            <span className="text-orange-500">■ Mispronounced</span>
            <span className="text-red-500">■ Missing</span>
          </div>

          {!exhausted && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setResult(null)}
              className="text-xs"
            >
              Try again ({MAX_ATTEMPTS - attempts} left)
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

interface Props {
  sessionId: string
  onComplete: () => void
}

export default function PhraseStep({ sessionId, onComplete }: Props) {
  const { data: segments, isLoading } = useListenSubtitles(sessionId)
  const phrases = segments?.slice(0, 10) ?? []

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-2xl mx-auto">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="border rounded-xl p-5 h-28 bg-muted animate-pulse" />
        ))}
      </div>
    )
  }

  if (phrases.length === 0) {
    return (
      <div className="flex flex-col items-center py-24 gap-4 text-muted-foreground">
        <p>No phrases available</p>
        <Button onClick={onComplete}>Continue</Button>
      </div>
    )
  }

  return (
    <div className="space-y-5 max-w-2xl mx-auto">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold">Practice these phrases</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Record yourself saying each phrase — up to 3 attempts each
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={onComplete}>
          Skip step
        </Button>
      </div>

      <div className="space-y-4">
        {phrases.map((seg, i) => (
          <PhraseItem key={seg.id} segment={seg} sessionId={sessionId} index={i} />
        ))}
      </div>

      <div className="flex justify-end pt-2">
        <Button onClick={onComplete} className="gap-2">
          <CheckCircle2 className="h-4 w-4" />
          Continue to Shadow
        </Button>
      </div>
    </div>
  )
}
