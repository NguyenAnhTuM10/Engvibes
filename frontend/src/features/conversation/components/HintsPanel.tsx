import { useState } from 'react'
import { Lightbulb, ChevronDown, ChevronUp } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { HintResponse } from '../types'

interface Props {
  hints: HintResponse | null
}

export default function HintsPanel({ hints }: Props) {
  const [open, setOpen] = useState(false)

  if (!hints || (hints.keywords.length === 0 && !hints.exampleSentence)) return null

  return (
    <div className="rounded-lg border bg-amber-50 dark:bg-amber-950/20 border-amber-200 dark:border-amber-800">
      <button
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-4 py-3 text-sm font-medium text-amber-800 dark:text-amber-300"
      >
        <span className="flex items-center gap-2">
          <Lightbulb className="h-4 w-4" />
          Need ideas? Tap for hints
        </span>
        {open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>

      <div
        className={cn(
          'overflow-hidden transition-all duration-200',
          open ? 'max-h-40' : 'max-h-0',
        )}
      >
        <div className="border-t border-amber-200 dark:border-amber-800 px-4 pb-4 pt-3 space-y-3">
          {hints.keywords.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {hints.keywords.map((kw) => (
                <span
                  key={kw}
                  className="rounded-full bg-amber-100 dark:bg-amber-900/40 px-3 py-1 text-xs font-medium text-amber-800 dark:text-amber-200"
                >
                  {kw}
                </span>
              ))}
            </div>
          )}
          {hints.exampleSentence && (
            <p className="text-xs text-amber-700 dark:text-amber-300 italic">
              e.g. "{hints.exampleSentence}"
            </p>
          )}
        </div>
      </div>
    </div>
  )
}
