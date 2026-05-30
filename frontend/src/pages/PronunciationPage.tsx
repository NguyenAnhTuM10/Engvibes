import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import AudioRecorder from '@/shared/components/AudioRecorder'
import { PhonemeDisplay } from '@/features/pronunciation/components/PhonemeDisplay'
import { ScorePanel } from '@/features/pronunciation/components/ScorePanel'
import { FeedbackPanel } from '@/features/pronunciation/components/FeedbackPanel'
import { usePronunciationWs } from '@/features/pronunciation/hooks/usePronunciationWs'
import { useCreateSession, useSubmitAttempt, useAttempts } from '@/features/pronunciation/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { RotateCcw, Trophy, Volume2 } from 'lucide-react'
import { cn } from '@/lib/utils'

// Từ mẫu chia theo độ khó — có thể load từ API vocab deck trong tương lai
const WORD_GROUPS = {
  'TH sounds': ['think', 'through', 'three', 'this', 'the', 'there', 'weather', 'brother'],
  'R / L':     ['right', 'light', 'river', 'liver', 'world', 'really', 'clearly'],
  'V / W':     ['very', 'well', 'valley', 'village', 'voice', 'wine'],
  'Vowels':    ['bit', 'beat', 'cat', 'cut', 'caught', 'coat', 'book', 'boot'],
  'Clusters':  ['strength', 'months', 'twelfth', 'shoulder', 'children', 'clothes'],
}

export default function PronunciationPage() {
  const qc = useQueryClient()

  const [sessionId,    setSessionId]    = useState<string | null>(null)
  const [activeWord,   setActiveWord]   = useState<string | null>(null)
  const [activeGroup,  setActiveGroup]  = useState<string>('TH sounds')
  const createSession = useCreateSession()
  const { data: attempts } = useAttempts(sessionId)

  // Async submit — trả về attemptId ngay, result đến qua WS
  const submitAttempt = useSubmitAttempt(sessionId ?? '')

  // WebSocket listener — update UI realtime
  const { status, progress, message, result, resetResult } = usePronunciationWs(sessionId)

  // ── Chọn từ → tạo session mới ─────────────────────────────────────────
  const handleSelectWord = async (word: string) => {
    if (word === activeWord && sessionId) return  // không tạo lại nếu cùng từ

    setActiveWord(word)
    setSessionId(null)
    resetResult()

    const session = await createSession.mutateAsync({ targetText: word, sessionType: 'WORD' })
    setSessionId(session.id)
  }

  // ── Submit audio ──────────────────────────────────────────────────────
  const handleSubmit = async (blob: Blob) => {
    if (!sessionId) return
    resetResult()
    await submitAttempt.mutateAsync(blob)
    // Sau khi COMPLETED: invalidate attempts list để history cập nhật
    qc.invalidateQueries({ queryKey: ['pronunciation-attempts', sessionId] })
    qc.invalidateQueries({ queryKey: ['pronunciation-session', sessionId] })
  }

  // ── Try again ─────────────────────────────────────────────────────────
  const handleRetry = () => {
    resetResult()
  }

  const isProcessing = status === 'processing' || status === 'transcribed'
  const bestScore = attempts?.length
    ? Math.max(...attempts.map((a) => a.overallScore))
    : null

  return (
    <div className="max-w-2xl mx-auto py-6 space-y-6 px-4">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Pronunciation Practice</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Select a word, record your voice, and see phoneme-level feedback.
        </p>
      </div>

      {/* Word selector grouped by phoneme category */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium">Choose a word to practice</CardTitle>
          {/* Category tabs */}
          <div className="flex flex-wrap gap-1.5 mt-2">
            {Object.keys(WORD_GROUPS).map((group) => (
              <button
                key={group}
                onClick={() => setActiveGroup(group)}
                className={cn(
                  'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
                  activeGroup === group
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'border-muted hover:border-primary/50 text-muted-foreground',
                )}
              >
                {group}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            {WORD_GROUPS[activeGroup as keyof typeof WORD_GROUPS].map((word) => (
              <button
                key={word}
                onClick={() => handleSelectWord(word)}
                disabled={createSession.isPending}
                className={cn(
                  'px-4 py-2 rounded-lg border text-sm font-medium transition-all',
                  activeWord === word
                    ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                    : 'border-muted hover:border-primary/50 hover:bg-accent',
                )}
              >
                {word}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Practice area — só hiện khi đã chọn từ */}
      {activeWord && sessionId && (
        <>
          {/* Word card */}
          <Card>
            <CardContent className="pt-6 space-y-5">
              {/* Target word */}
              <div className="text-center space-y-2">
                <div className="flex items-center justify-center gap-3">
                  <h2 className="text-5xl font-bold tracking-wide">{activeWord}</h2>
                  {/* Nút phát âm mẫu dùng browser TTS */}
                  <button
                    onClick={() => window.speechSynthesis.speak(
                      Object.assign(new SpeechSynthesisUtterance(activeWord), { lang: 'en-US', rate: 0.8 })
                    )}
                    className="p-2 rounded-full hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
                    title="Hear native pronunciation"
                  >
                    <Volume2 className="h-5 w-5" />
                  </button>
                </div>

                {/* Phoneme display — preview trước khi submit, result sau */}
                <PhonemeDisplay
                  targetIpa={result?.targetIpa ?? null}
                  phonemeMatches={result?.phonemeMatches}
                  mode={result ? 'result' : 'preview'}
                />
              </div>

              {/* Best score badge */}
              {bestScore !== null && (
                <div className="flex justify-center">
                  <Badge variant="outline" className="gap-1.5">
                    <Trophy className="h-3.5 w-3.5 text-yellow-500" />
                    Best: {bestScore}/100
                  </Badge>
                </div>
              )}

              {/* Progress status */}
              {isProcessing && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span className="h-1.5 w-1.5 rounded-full bg-primary animate-pulse shrink-0" />
                    {message}
                  </div>
                  <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                    <div
                      className="h-full bg-primary rounded-full transition-all duration-500"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              )}

              {/* Transcript preview khi TRANSCRIBED */}
              {status === 'transcribed' && (
                <p className="text-sm text-center text-muted-foreground italic">
                  Heard: "{message}"
                </p>
              )}

              {/* Error state */}
              {status === 'error' && (
                <p className="text-sm text-center text-destructive">{message}</p>
              )}
            </CardContent>
          </Card>

          {/* Audio recorder + retry */}
          {!result ? (
            <AudioRecorder
              maxDurationSec={15}
              isSubmitting={isProcessing || submitAttempt.isPending}
              onSubmit={handleSubmit}
              disabled={createSession.isPending}
            />
          ) : (
            <div className="flex justify-center">
              <Button variant="outline" onClick={handleRetry} className="gap-2">
                <RotateCcw className="h-4 w-4" />
                Try again
              </Button>
            </div>
          )}

          {/* Results */}
          {status === 'completed' && result && (
            <div className="space-y-4">
              <ScorePanel
                overall={result.overallScore}
                accuracy={result.accuracyScore}
                fluency={result.fluencyScore}
              />
              <FeedbackPanel
                phonemeMatches={result.phonemeMatches}
                transcript={result.transcript}
              />
            </div>
          )}

          {/* Attempt history (chỉ hiện khi có nhiều hơn 1 lần thử) */}
          {(attempts?.length ?? 0) > 1 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Attempt History</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-1.5">
                  {[...(attempts ?? [])].reverse().map((a) => (
                    <div key={a.attemptId} className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">Attempt #{a.attemptNumber}</span>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs text-muted-foreground italic">
                          "{a.transcript}"
                        </span>
                        <Badge
                          variant="outline"
                          className={cn(
                            'font-mono',
                            a.overallScore >= 80 ? 'text-green-600 border-green-300' :
                            a.overallScore >= 60 ? 'text-yellow-600 border-yellow-300' :
                                                   'text-red-600 border-red-300',
                          )}
                        >
                          {a.overallScore}/100
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}

      {/* Empty state */}
      {!activeWord && (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">Select a word above to start practicing</p>
          <p className="text-sm mt-1">Record your voice and get instant phoneme feedback</p>
        </div>
      )}
    </div>
  )
}
