import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { CheckCircle2, AlertCircle, Info } from 'lucide-react'
import type { PhonemeMatch } from '../types'

interface Props {
  phonemeMatches: PhonemeMatch[]
  transcript: string
}

export function FeedbackPanel({ phonemeMatches, transcript }: Props) {
  const errors = phonemeMatches.filter((m) => !m.matched && m.expected !== '' && m.tip)
  const correct = phonemeMatches.filter((m) => m.matched)

  return (
    <div className="space-y-4">
      {/* Transcript */}
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Info className="h-4 w-4 text-blue-500" />
            What we heard
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-base italic text-muted-foreground">
            "{transcript || '(no speech detected)'}"
          </p>
        </CardContent>
      </Card>

      {/* Errors với tips */}
      {errors.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-red-500" />
              Pronunciation Tips
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {errors.map((m, i) => (
              <div key={i} className="flex gap-3 items-start">
                {/* Phoneme badge */}
                <div className="flex gap-1 shrink-0 pt-0.5">
                  <Badge variant="outline" className="font-mono text-red-600 border-red-300">
                    /{m.expected}/
                  </Badge>
                  {m.actual && (
                    <>
                      <span className="text-xs text-muted-foreground self-center">→</span>
                      <Badge variant="outline" className="font-mono text-muted-foreground">
                        /{m.actual}/
                      </Badge>
                    </>
                  )}
                </div>
                {/* Tip text */}
                <p className="text-sm text-muted-foreground leading-relaxed">{m.tip}</p>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* All correct */}
      {errors.length === 0 && correct.length > 0 && (
        <div className="flex items-center gap-2 text-green-600 text-sm font-medium">
          <CheckCircle2 className="h-5 w-5" />
          Perfect pronunciation! All phonemes correct.
        </div>
      )}
    </div>
  )
}
