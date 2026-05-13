import { CheckCircle, AlertTriangle, Star, Lightbulb, RotateCcw } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import type { ConversationEndResponse } from '../types'

interface Props {
  data: ConversationEndResponse
  onRestart: () => void
}

export default function SessionSummary({ data, onRestart }: Props) {
  const { totalTurns, xpEarned, summary } = data

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-4">
            <CheckCircle className="h-10 w-10 text-green-600" />
          </div>
        </div>
        <h2 className="text-2xl font-bold">Conversation Complete!</h2>
        <p className="text-muted-foreground">
          {totalTurns} turn{totalTurns !== 1 ? 's' : ''} completed
        </p>
        <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-2">
          <Star className="h-4 w-4 text-primary" />
          <span className="text-sm font-semibold text-primary">+{xpEarned} XP earned</span>
        </div>
      </div>

      {summary ? (
        <>
          {/* Encouragement */}
          {summary.encouragement && (
            <Card className="border-green-200 bg-green-50 dark:bg-green-950/20 dark:border-green-800">
              <CardContent className="pt-4">
                <p className="text-sm text-green-800 dark:text-green-300">
                  {summary.encouragement}
                </p>
              </CardContent>
            </Card>
          )}

          {/* Grammar Errors */}
          {summary.grammarErrors?.length > 0 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  Grammar to review
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {summary.grammarErrors.map((err, i) => (
                  <div key={i} className="text-sm">
                    <span className="line-through text-red-500 dark:text-red-400 mr-2">
                      {err.error}
                    </span>
                    <span className="text-muted-foreground mr-1">→</span>
                    <span className="font-medium text-green-700 dark:text-green-400">
                      {err.correction}
                    </span>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {/* Vocab Highlights */}
          {summary.vocabHighlights?.length > 0 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Star className="h-4 w-4 text-primary" />
                  Great vocabulary used
                </CardTitle>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                {summary.vocabHighlights.map((v, i) => (
                  <span
                    key={i}
                    className="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary"
                  >
                    {v}
                  </span>
                ))}
              </CardContent>
            </Card>
          )}

          {/* Top Tip */}
          {summary.topTip && (
            <Card className="border-blue-200 bg-blue-50 dark:bg-blue-950/20 dark:border-blue-800">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2 text-blue-700 dark:text-blue-300">
                  <Lightbulb className="h-4 w-4" />
                  Top tip for next time
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-blue-800 dark:text-blue-300">{summary.topTip}</p>
              </CardContent>
            </Card>
          )}
        </>
      ) : (
        <p className="text-center text-sm text-muted-foreground">
          Great job completing the practice session!
        </p>
      )}

      <Button onClick={onRestart} className="w-full" variant="outline">
        <RotateCcw className="h-4 w-4 mr-2" />
        Practice Another Scenario
      </Button>
    </div>
  )
}
