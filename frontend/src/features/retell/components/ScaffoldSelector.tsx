import { Brain, BookOpen, ListOrdered, LayoutTemplate } from 'lucide-react'
import { cn } from '@/lib/utils'

const LEVELS = [
  {
    level: 1,
    label: 'No help',
    icon: Brain,
    tagline: 'Just me and the mic',
    desc: 'Most challenging — retell completely from memory',
    color: 'border-slate-300 hover:border-slate-400',
    activeColor: 'border-slate-700 bg-slate-50 dark:bg-slate-900/30',
  },
  {
    level: 2,
    label: 'Word bank',
    icon: BookOpen,
    tagline: 'Show me key words',
    desc: 'Get a list of important vocabulary to use',
    color: 'border-blue-200 hover:border-blue-400',
    activeColor: 'border-blue-500 bg-blue-50 dark:bg-blue-950/20',
  },
  {
    level: 3,
    label: 'Sentence starters',
    icon: ListOrdered,
    tagline: 'Give me sentence beginnings',
    desc: 'Provide opening phrases for each main idea',
    color: 'border-purple-200 hover:border-purple-400',
    activeColor: 'border-purple-500 bg-purple-50 dark:bg-purple-950/20',
  },
  {
    level: 4,
    label: 'Story frame',
    icon: LayoutTemplate,
    tagline: 'Full template',
    desc: 'Most guided — fill in a complete structure',
    color: 'border-green-200 hover:border-green-400',
    activeColor: 'border-green-500 bg-green-50 dark:bg-green-950/20',
  },
]

interface Props {
  selected: number | null
  onSelect: (level: number) => void
  loading?: boolean
}

export default function ScaffoldSelector({ selected, onSelect, loading }: Props) {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold">How much help do you want?</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Choose your scaffolding level — you can try again with a different level
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {LEVELS.map(({ level, label, icon: Icon, tagline, desc, color, activeColor }) => {
          const isActive = selected === level
          return (
            <button
              key={level}
              onClick={() => !loading && onSelect(level)}
              disabled={loading}
              className={cn(
                'flex items-start gap-4 p-5 rounded-xl border-2 text-left transition-all',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                isActive ? activeColor : cn('bg-card', color),
                loading && 'opacity-60 cursor-not-allowed',
              )}
            >
              <div
                className={cn(
                  'p-2.5 rounded-lg shrink-0 mt-0.5',
                  isActive ? 'bg-background shadow-sm' : 'bg-muted',
                )}
              >
                <Icon className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-baseline gap-2">
                  <span className="font-semibold text-sm">L{level}:</span>
                  <span className="font-semibold text-sm">{label}</span>
                </div>
                <p className="text-xs text-muted-foreground italic mt-0.5">"{tagline}"</p>
                <p className="text-xs text-muted-foreground mt-1">{desc}</p>
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}
