import { useState } from 'react'
import { Button } from '@/components/ui/button'
import AudioRecorder from '@/shared/components/AudioRecorder'
import ScaffoldSelector from '@/features/retell/components/ScaffoldSelector'
import RetellFeedbackPanel from '@/features/retell/components/RetellFeedbackPanel'
import { useStartRetell, useSubmitRetellAttempt } from '@/features/session/api'
import type { RetellFeedback, RetellScaffoldResponse } from '@/shared/types/api'

interface Props {
  sessionId: string
  onComplete: () => void
}

type RetellPhase = 'select' | 'record' | 'feedback'

function ScaffoldDisplay({ scaffold }: { scaffold: RetellScaffoldResponse }) {
  const level = scaffold.scaffoldLevel

  if (level === 1) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <p className="text-base font-medium">No aids — rely on your memory</p>
        <p className="text-sm mt-1">Record your retelling below</p>
      </div>
    )
  }

  if (level === 2 && scaffold.wordBank?.length) {
    return (
      <div className="space-y-2">
        <p className="text-sm font-medium text-muted-foreground">Key vocabulary to use:</p>
        <div className="flex flex-wrap gap-2">
          {scaffold.wordBank.map((w) => (
            <span
              key={w}
              className="px-3 py-1.5 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded-lg text-sm font-medium"
            >
              {w}
            </span>
          ))}
        </div>
      </div>
    )
  }

  if (level === 3 && scaffold.sentenceStarters?.length) {
    return (
      <div className="space-y-2">
        <p className="text-sm font-medium text-muted-foreground">Sentence starters:</p>
        <ol className="space-y-2">
          {scaffold.sentenceStarters.map((s, i) => (
            <li
              key={i}
              className="flex gap-3 p-3 bg-purple-50 dark:bg-purple-950/20 rounded-lg text-sm"
            >
              <span className="font-bold text-purple-600 shrink-0">{i + 1}.</span>
              <span className="italic text-muted-foreground">{s}</span>
            </li>
          ))}
        </ol>
      </div>
    )
  }

  if (level === 4 && scaffold.storyFrame) {
    return (
      <div className="space-y-2">
        <p className="text-sm font-medium text-muted-foreground">Story frame:</p>
        <div className="p-4 bg-green-50 dark:bg-green-950/20 border border-green-200 dark:border-green-800 rounded-xl">
          <pre className="text-sm font-sans whitespace-pre-wrap leading-relaxed text-foreground">
            {scaffold.storyFrame}
          </pre>
        </div>
      </div>
    )
  }

  return null
}

export default function RetellStep({ sessionId, onComplete }: Props) {
  const [phase, setPhase] = useState<RetellPhase>('select')
  const [selectedLevel, setSelectedLevel] = useState<number | null>(null)
  const [scaffold, setScaffold] = useState<RetellScaffoldResponse | null>(null)
  const [feedback, setFeedback] = useState<RetellFeedback | null>(null)

  const startRetell = useStartRetell(sessionId)
  const submitAttempt = useSubmitRetellAttempt(sessionId)

  const handleSelectLevel = (level: number) => {
    setSelectedLevel(level)
    startRetell.mutate(level, {
      onSuccess: (data) => {
        setScaffold(data)
        setPhase('record')
      },
    })
  }

  const handleSubmit = (blob: Blob) => {
    submitAttempt.mutate(blob, {
      onSuccess: (data: RetellFeedback) => {
        setFeedback(data)
        setPhase('feedback')
      },
    })
  }

  const handleTryAgain = () => {
    setFeedback(null)
    setScaffold(null)
    setSelectedLevel(null)
    setPhase('select')
    submitAttempt.reset()
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Skip always available */}
      {phase !== 'feedback' && (
        <div className="flex justify-end">
          <Button variant="ghost" size="sm" onClick={onComplete}>
            Skip step
          </Button>
        </div>
      )}

      {phase === 'select' && (
        <ScaffoldSelector
          selected={selectedLevel}
          onSelect={handleSelectLevel}
          loading={startRetell.isPending}
        />
      )}

      {phase === 'record' && scaffold && (
        <div className="space-y-5">
          <div>
            <h2 className="text-lg font-semibold">Retell the video in your own words</h2>
            <p className="text-sm text-muted-foreground mt-0.5">
              L{scaffold.scaffoldLevel} · Up to 90 seconds · Speak naturally
            </p>
          </div>

          <ScaffoldDisplay scaffold={scaffold} />

          <div className="border-t pt-5">
            <AudioRecorder
              maxDurationSec={90}
              isSubmitting={submitAttempt.isPending}
              onSubmit={handleSubmit}
            />
          </div>
        </div>
      )}

      {phase === 'feedback' && feedback && (
        <RetellFeedbackPanel
          feedback={feedback}
          onTryAgain={handleTryAgain}
          onContinue={onComplete}
        />
      )}
    </div>
  )
}
