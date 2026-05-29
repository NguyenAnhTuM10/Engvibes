import { cn } from '@/lib/utils'
import type { PhonemeMatch } from '../types'

interface Props {
  targetIpa: string | null
  phonemeMatches?: PhonemeMatch[]
  /** 'preview' = chỉ hiện IPA expected; 'result' = color-coded so sánh */
  mode: 'preview' | 'result'
}

/**
 * Hiển thị phoneme tiles.
 * Preview: hiển thị IPA của từ đúng để user biết cần phát âm gì.
 * Result: mỗi tile xanh = đúng, đỏ = sai/thiếu + hiện actual bên dưới.
 */
export function PhonemeDisplay({ targetIpa, phonemeMatches, mode }: Props) {
  if (mode === 'preview' || !phonemeMatches?.length) {
    return (
      <div className="text-center">
        <span className="text-3xl font-mono tracking-widest text-muted-foreground">
          {targetIpa ? `/${targetIpa}/` : '…'}
        </span>
      </div>
    )
  }

  // Lọc bỏ các phoneme thừa của user (expected = '') khỏi display chính
  const displayMatches = phonemeMatches.filter((m) => m.expected !== '')

  return (
    <div className="flex flex-wrap justify-center gap-2">
      {displayMatches.map((m, i) => (
        <div key={i} className="flex flex-col items-center gap-1">
          {/* Tile phoneme expected */}
          <div
            className={cn(
              'px-3 py-2 rounded-lg border-2 font-mono text-lg min-w-[2.5rem] text-center transition-colors',
              m.matched
                ? 'bg-green-500/10 border-green-500 text-green-700 dark:text-green-400'
                : 'bg-red-500/10 border-red-500 text-red-700 dark:text-red-400',
            )}
            title={m.tip ?? undefined}
          >
            {m.expected}
          </div>

          {/* Phoneme user thực sự nói (chỉ hiện khi sai) */}
          {!m.matched && (
            <span className="text-xs font-mono text-muted-foreground">
              {m.actual ? `/${m.actual}/` : '—'}
            </span>
          )}
        </div>
      ))}
    </div>
  )
}
