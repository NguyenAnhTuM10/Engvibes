import { cn } from '@/lib/utils'

interface Props {
  overall: number
  accuracy: number
  fluency: number
}

function scoreColor(n: number) {
  if (n >= 80) return 'bg-green-500'
  if (n >= 60) return 'bg-yellow-500'
  return 'bg-red-500'
}

function scoreLabel(n: number) {
  if (n >= 90) return 'Excellent!'
  if (n >= 75) return 'Good'
  if (n >= 60) return 'Fair'
  return 'Keep practicing'
}

export function ScorePanel({ overall, accuracy, fluency }: Props) {
  const bars = [
    { label: 'Overall',  value: overall,  desc: scoreLabel(overall) },
    { label: 'Accuracy', value: accuracy, desc: 'Phoneme correctness' },
    { label: 'Fluency',  value: fluency,  desc: 'Sound completeness' },
  ]

  return (
    <div className="space-y-4">
      {/* Big overall score */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-muted-foreground">Score</span>
        <div className="flex items-baseline gap-1">
          <span className={cn(
            'text-4xl font-bold tabular-nums',
            overall >= 80 ? 'text-green-600' : overall >= 60 ? 'text-yellow-600' : 'text-red-600'
          )}>
            {overall}
          </span>
          <span className="text-muted-foreground text-sm">/100</span>
        </div>
      </div>

      {/* Score bars */}
      <div className="space-y-3">
        {bars.map(({ label, value, desc }) => (
          <div key={label} className="space-y-1">
            <div className="flex justify-between text-sm">
              <span className="font-medium">{label}</span>
              <span className="text-muted-foreground">{value}/100</span>
            </div>
            <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
              <div
                className={cn('h-full rounded-full transition-all duration-700', scoreColor(value))}
                style={{ width: `${value}%` }}
              />
            </div>
            <p className="text-xs text-muted-foreground">{desc}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
