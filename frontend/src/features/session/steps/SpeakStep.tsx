import { useState } from 'react'
import { ChevronDown, ChevronUp, ThumbsUp, Lightbulb } from 'lucide-react'
import { Button } from '@/components/ui/button'
import AudioRecorder from '@/shared/components/AudioRecorder'
import { useSpeakingQuestion, useSubmitSpeakAttempt } from '@/features/session/api'
import type { SpeakFeedback } from '@/shared/types/api'
import { cn } from '@/lib/utils'
import { useCountUp } from '@/shared/hooks/useCountUp'

interface Props {
  sessionId: string
  onComplete: () => void
}

function SpeakFeedbackPanel({
  feedback,
  onTryAgain,
  onContinue,
}: {
  feedback: SpeakFeedback
  onTryAgain: () => void
  onContinue: () => void
}) {
  const score = useCountUp(feedback.score)
  const [modelOpen, setModelOpen] = useState(false)

  function scoreBg(s: number) {
    if (s >= 80) return 'bg-green-50 border-green-200 dark:bg-green-950/20 dark:border-green-800'
    if (s >= 60) return 'bg-blue-50 border-blue-200 dark:bg-blue-950/20 dark:border-blue-800'
    return 'bg-orange-50 border-orange-200 dark:bg-orange-950/20 dark:border-orange-800'
  }
  function scoreColor(s: number) {
    if (s >= 80) return 'text-green-600'
    if (s >= 60) return 'text-blue-600'
    return 'text-orange-500'
  }
  function barColor(s: number) {
    if (s >= 80) return 'bg-green-500'
    if (s >= 60) return 'bg-blue-500'
    return 'bg-orange-400'
  }

  function SubBar({ label, value }: { label: string; value: number }) {
    return (
      <div className="space-y-1">
        <div className="flex justify-between text-xs font-medium">
          <span>{label}</span>
          <span>{Math.round(value)}%</span>
        </div>
        <div className="h-2 bg-muted rounded-full overflow-hidden">
          <div className={cn('h-full rounded-full transition-all duration-700', barColor(value))} style={{ width: `${value}%` }} />
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <div className={cn('border rounded-xl p-5', scoreBg(feedback.score))}>
        <div className="flex items-center gap-5">
          <span className={cn('text-5xl font-black tabular-nums', scoreColor(feedback.score))}>{score}</span>
          <div className="flex-1 space-y-2.5">
            <SubBar label="Fluency" value={feedback.fluencyScore} />
            <SubBar label="Grammar" value={feedback.grammarScore} />
            <SubBar label="Vocabulary variety" value={feedback.vocabVarietyScore} />
          </div>
        </div>
        {feedback.transcript && (
          <p className="text-xs text-muted-foreground mt-3 italic border-t pt-3">
            You said: "{feedback.transcript}"
          </p>
        )}
      </div>

      {feedback.vocabFromVideoUsed.length > 0 && (
        <div className="border rounded-xl p-4">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
            Video vocab you used
          </p>
          <div className="flex flex-wrap gap-1.5">
            {feedback.vocabFromVideoUsed.map((w) => (
              <span key={w} className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 font-medium">{w}</span>
            ))}
          </div>
        </div>
      )}

      {(feedback.positiveNotes.length > 0 || feedback.improvementTips.length > 0) && (
        <div className="grid grid-cols-2 gap-3">
          {feedback.positiveNotes.length > 0 && (
            <div className="border rounded-xl p-3">
              <p className="text-xs font-semibold text-green-700 dark:text-green-400 flex items-center gap-1 mb-2"><ThumbsUp className="h-3.5 w-3.5" /> What worked</p>
              {feedback.positiveNotes.map((n, i) => <p key={i} className="text-xs text-muted-foreground">• {n}</p>)}
            </div>
          )}
          {feedback.improvementTips.length > 0 && (
            <div className="border rounded-xl p-3">
              <p className="text-xs font-semibold text-blue-700 dark:text-blue-400 flex items-center gap-1 mb-2"><Lightbulb className="h-3.5 w-3.5" /> Tips</p>
              {feedback.improvementTips.map((t, i) => <p key={i} className="text-xs text-muted-foreground">• {t}</p>)}
            </div>
          )}
        </div>
      )}

      {feedback.modelAnswer && (
        <div className="border rounded-xl overflow-hidden">
          <button onClick={() => setModelOpen((o) => !o)} className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors">
            <span>See a model answer</span>
            {modelOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </button>
          {modelOpen && (
            <div className="px-4 pb-4 text-sm leading-relaxed text-muted-foreground border-t bg-muted/20">
              <p className="pt-3 whitespace-pre-wrap">{feedback.modelAnswer}</p>
            </div>
          )}
        </div>
      )}

      <div className="flex gap-3">
        <Button variant="outline" className="flex-1" onClick={onTryAgain}>Try again</Button>
        <Button className="flex-1" onClick={onContinue}>Continue to Review →</Button>
      </div>
    </div>
  )
}

export default function SpeakStep({ sessionId, onComplete }: Props) {
  const { data: question, isLoading } = useSpeakingQuestion(sessionId)
  const submitAttempt = useSubmitSpeakAttempt(sessionId)
  const [feedback, setFeedback] = useState<SpeakFeedback | null>(null)
  const [hintsOpen, setHintsOpen] = useState(true)

  const handleSubmit = (blob: Blob) => {
    submitAttempt.mutate(blob, {
      onSuccess: (data: SpeakFeedback) => setFeedback(data),
    })
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (feedback) {
    return (
      <div className="max-w-2xl mx-auto space-y-6">
        <SpeakFeedbackPanel
          feedback={feedback}
          onTryAgain={() => { setFeedback(null); submitAttempt.reset() }}
          onContinue={onComplete}
        />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold">Time to speak</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Answer the question in English — up to 90 seconds
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={onComplete}>Skip step</Button>
      </div>

      {question && (
        <>
          <div className="bg-primary/5 border border-primary/20 rounded-xl p-5">
            <p className="text-xs text-primary font-semibold uppercase tracking-wide mb-2">Question</p>
            <p className="text-lg font-medium leading-relaxed">{question.question}</p>
          </div>

          <div className="border rounded-xl overflow-hidden">
            <button
              onClick={() => setHintsOpen((o) => !o)}
              className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors"
            >
              <span>Hints & vocabulary</span>
              {hintsOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
            {hintsOpen && (
              <div className="px-4 pb-4 border-t space-y-3 pt-3">
                {question.suggestedVocab.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-muted-foreground mb-1.5">Suggested vocabulary</p>
                    <div className="flex flex-wrap gap-1.5">
                      {question.suggestedVocab.map((w) => (
                        <span key={w} className="text-xs px-2 py-0.5 bg-muted rounded-full">{w}</span>
                      ))}
                    </div>
                  </div>
                )}
                {question.collocations.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-muted-foreground mb-1.5">Useful phrases</p>
                    <div className="flex flex-wrap gap-1.5">
                      {question.collocations.map((c) => (
                        <span key={c} className="text-xs px-2 py-0.5 bg-blue-50 dark:bg-blue-950/20 text-blue-700 dark:text-blue-300 rounded-full">{c}</span>
                      ))}
                    </div>
                  </div>
                )}
                {question.sampleOpening && (
                  <p className="text-xs text-muted-foreground italic">
                    Opening: "<span className="text-foreground">{question.sampleOpening}</span>"
                  </p>
                )}
                {question.structureTips && question.structureTips.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-muted-foreground mb-1">Structure tips</p>
                    {question.structureTips.map((t, i) => (
                      <p key={i} className="text-xs text-muted-foreground">• {t}</p>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      )}

      <div className="border-t pt-5">
        <AudioRecorder
          maxDurationSec={90}
          isSubmitting={submitAttempt.isPending}
          onSubmit={handleSubmit}
        />
      </div>
    </div>
  )
}
