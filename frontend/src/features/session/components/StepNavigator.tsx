import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'

export const STEP_LABELS = [
  { label: 'Warmup', emoji: '📚' },
  { label: 'Listen', emoji: '🎧' },
  { label: 'Phrases', emoji: '💬' },
  { label: 'Shadow', emoji: '🎙️' },
  { label: 'Retell', emoji: '⭐' },
  { label: 'Speak', emoji: '🗣️' },
  { label: 'Review', emoji: '🃏' },
]

interface Props {
  currentStep: number
  onStepClick?: (step: number) => void
}

export default function StepNavigator({ currentStep, onStepClick }: Props) {
  return (
    <div className="flex items-center gap-1 overflow-x-auto">
      {STEP_LABELS.map((step, i) => {
        const done = i < currentStep
        const active = i === currentStep

        return (
          <div key={i} className="flex items-center gap-1 shrink-0">
            <button
              onClick={() => done && onStepClick?.(i)}
              disabled={!done && !active}
              title={step.label}
              className={cn(
                'flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium transition-colors whitespace-nowrap',
                done && 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 cursor-pointer hover:bg-green-200',
                active && 'bg-primary text-primary-foreground',
                !done && !active && 'text-muted-foreground opacity-50 cursor-not-allowed',
              )}
            >
              {done ? <Check className="h-3 w-3 shrink-0" /> : <span>{step.emoji}</span>}
              <span className="hidden sm:inline">{step.label}</span>
            </button>
            {i < STEP_LABELS.length - 1 && (
              <div
                className={cn('h-px w-3 shrink-0', done ? 'bg-green-400' : 'bg-border')}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}
