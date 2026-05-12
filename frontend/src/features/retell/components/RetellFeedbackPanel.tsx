import { useState } from 'react'
import { ThumbsUp, Lightbulb, ChevronDown, ChevronUp, BookPlus, Check, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useCountUp } from '@/shared/hooks/useCountUp'
import type { RetellFeedback } from '@/shared/types/api'

function ScoreBar({ label, value }: { label: string; value: number }) {
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs font-medium">
        <span>{label}</span>
        <span>{Math.round(value)}%</span>
      </div>
      <div className="h-2 bg-muted rounded-full overflow-hidden">
        <div
          className={cn(
            'h-full rounded-full transition-all duration-700',
            value >= 80 ? 'bg-green-500' : value >= 60 ? 'bg-blue-500' : 'bg-orange-400',
          )}
          style={{ width: `${value}%` }}
        />
      </div>
    </div>
  )
}

function scoreColor(s: number) {
  if (s >= 80) return 'text-green-600 dark:text-green-400'
  if (s >= 60) return 'text-blue-600 dark:text-blue-400'
  if (s >= 40) return 'text-orange-500'
  return 'text-red-500'
}

function scoreBg(s: number) {
  if (s >= 80) return 'bg-green-50 border-green-200 dark:bg-green-950/20 dark:border-green-800'
  if (s >= 60) return 'bg-blue-50 border-blue-200 dark:bg-blue-950/20 dark:border-blue-800'
  if (s >= 40) return 'bg-orange-50 border-orange-200 dark:bg-orange-950/20 dark:border-orange-800'
  return 'bg-red-50 border-red-200 dark:bg-red-950/20 dark:border-red-800'
}

interface Props {
  feedback: RetellFeedback
  onTryAgain: () => void
  onContinue: () => void
}

export default function RetellFeedbackPanel({ feedback, onTryAgain, onContinue }: Props) {
  const score = useCountUp(feedback.score)
  const [modelOpen, setModelOpen] = useState(false)

  return (
    <div className="space-y-6 max-w-2xl mx-auto pb-6">
      {/* Score banner */}
      <div className={cn('border rounded-xl p-6', scoreBg(feedback.score))}>
        <div className="flex items-center gap-6">
          <div className={cn('text-6xl font-black tabular-nums shrink-0', scoreColor(feedback.score))}>
            {score}
          </div>
          <div className="flex-1 space-y-3">
            <ScoreBar label="Coverage" value={feedback.coverageScore} />
            <ScoreBar label="Vocabulary" value={feedback.vocabularyScore} />
            <ScoreBar label="Grammar" value={feedback.grammarScore} />
          </div>
        </div>
        {feedback.transcript && (
          <p className="text-xs text-muted-foreground mt-4 italic border-t pt-3">
            You said: "{feedback.transcript}"
          </p>
        )}
      </div>

      {/* Coverage */}
      {(feedback.coveredPoints.length > 0 || feedback.missedPoints.length > 0) && (
        <div className="grid grid-cols-2 gap-4">
          {feedback.coveredPoints.length > 0 && (
            <div className="border rounded-xl p-4 bg-green-50/50 dark:bg-green-950/10 border-green-200 dark:border-green-800">
              <p className="text-xs font-semibold text-green-700 dark:text-green-400 mb-2 uppercase tracking-wide">
                ✓ Covered
              </p>
              <ul className="space-y-1.5">
                {feedback.coveredPoints.map((p, i) => (
                  <li key={i} className="flex items-start gap-1.5 text-xs">
                    <Check className="h-3.5 w-3.5 text-green-600 shrink-0 mt-0.5" />
                    <span>{p}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {feedback.missedPoints.length > 0 && (
            <div className="border rounded-xl p-4 bg-red-50/50 dark:bg-red-950/10 border-red-200 dark:border-red-800">
              <p className="text-xs font-semibold text-red-700 dark:text-red-400 mb-2 uppercase tracking-wide">
                ✗ Missed
              </p>
              <ul className="space-y-1.5">
                {feedback.missedPoints.map((p, i) => (
                  <li key={i} className="flex items-start gap-1.5 text-xs text-muted-foreground">
                    <X className="h-3.5 w-3.5 text-red-400 shrink-0 mt-0.5" />
                    <span>{p}</span>
                  </li>
                ))}
              </ul>
              <p className="text-xs text-muted-foreground mt-2 italic">Try mentioning these next time</p>
            </div>
          )}
        </div>
      )}

      {/* Vocabulary */}
      {(feedback.usedVocab.length > 0 || feedback.missedVocab.length > 0) && (
        <div className="border rounded-xl p-4 space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Vocabulary highlights
          </p>
          {feedback.usedVocab.length > 0 && (
            <div>
              <p className="text-xs text-muted-foreground mb-1.5">Words you used</p>
              <div className="flex flex-wrap gap-1.5">
                {feedback.usedVocab.map((w) => (
                  <span
                    key={w}
                    className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 font-medium"
                  >
                    {w}
                  </span>
                ))}
              </div>
            </div>
          )}
          {feedback.missedVocab.length > 0 && (
            <div>
              <p className="text-xs text-muted-foreground mb-1.5">Vocab you missed</p>
              <div className="flex flex-wrap gap-1.5">
                {feedback.missedVocab.map((w) => (
                  <span
                    key={w}
                    className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground font-medium flex items-center gap-1"
                  >
                    {w}
                    <BookPlus className="h-3 w-3" />
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Grammar */}
      {feedback.grammarIssues.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Grammar suggestions
          </p>
          {feedback.grammarIssues.slice(0, 3).map((issue, i) => (
            <div key={i} className="border-l-4 border-red-400 pl-4 py-2 space-y-1">
              <p className="text-sm line-through text-red-500 opacity-75">{issue.errorQuote}</p>
              <p className="text-sm text-green-600 dark:text-green-400 font-medium">✓ {issue.correction}</p>
              <p className="text-xs text-muted-foreground italic">{issue.explanation}</p>
            </div>
          ))}
        </div>
      )}

      {/* Encouragement */}
      {(feedback.positiveNotes.length > 0 || feedback.improvementTips.length > 0) && (
        <div className="grid grid-cols-2 gap-4">
          {feedback.positiveNotes.length > 0 && (
            <div className="border rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-green-700 dark:text-green-400 flex items-center gap-1.5">
                <ThumbsUp className="h-3.5 w-3.5" /> What you did well
              </p>
              <ul className="space-y-1.5">
                {feedback.positiveNotes.map((n, i) => (
                  <li key={i} className="text-xs text-muted-foreground">• {n}</li>
                ))}
              </ul>
            </div>
          )}
          {feedback.improvementTips.length > 0 && (
            <div className="border rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-blue-700 dark:text-blue-400 flex items-center gap-1.5">
                <Lightbulb className="h-3.5 w-3.5" /> Tips to improve
              </p>
              <ul className="space-y-1.5">
                {feedback.improvementTips.map((t, i) => (
                  <li key={i} className="text-xs text-muted-foreground">• {t}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Model answer */}
      {feedback.modelAnswer && (
        <div className="border rounded-xl overflow-hidden">
          <button
            onClick={() => setModelOpen((o) => !o)}
            className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors"
          >
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

      {/* Actions */}
      <div className="flex gap-3">
        <Button variant="outline" className="flex-1" onClick={onTryAgain}>
          Try again
        </Button>
        <Button className="flex-1" onClick={onContinue}>
          Continue to Speaking →
        </Button>
      </div>
    </div>
  )
}
