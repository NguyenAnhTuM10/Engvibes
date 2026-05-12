import { cn } from '@/lib/utils'
import type { CefrLevel } from '@/shared/types/api'

const colors: Record<CefrLevel, string> = {
  A1: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  A2: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300',
  B1: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  B2: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
  C1: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300',
  C2: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
}

export default function CefrBadge({
  level,
  className,
}: {
  level: CefrLevel
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-block text-xs font-semibold px-1.5 py-0.5 rounded',
        colors[level] ?? 'bg-muted text-muted-foreground',
        className,
      )}
    >
      {level}
    </span>
  )
}
