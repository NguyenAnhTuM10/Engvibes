import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import AudioRecorder from '@/shared/components/AudioRecorder'
import { PhonemeDisplay } from '@/features/pronunciation/components/PhonemeDisplay'
import { WordLevelDisplay } from '@/features/pronunciation/components/WordLevelDisplay'
import { ScorePanel } from '@/features/pronunciation/components/ScorePanel'
import { FeedbackPanel } from '@/features/pronunciation/components/FeedbackPanel'
import { usePronunciationWs } from '@/features/pronunciation/hooks/usePronunciationWs'
import { useCreateSession, useSubmitAttempt, useAttempts } from '@/features/pronunciation/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { RotateCcw, Trophy, Volume2, BookOpen, Type } from 'lucide-react'
import { cn } from '@/lib/utils'

// ── Vocabulary ──────────────────────────────────────────────────────────────

const WORD_GROUPS: Record<string, string[]> = {
  'TH Sounds':  ['think', 'through', 'three', 'this', 'the', 'there', 'weather', 'brother'],
  'R / L':      ['right', 'light', 'river', 'liver', 'world', 'really', 'clearly'],
  'V / W':      ['very', 'well', 'valley', 'village', 'voice', 'wine'],
  'Vowels':     ['bit', 'beat', 'cat', 'cut', 'caught', 'coat', 'book', 'boot'],
  'Clusters':   ['strength', 'months', 'twelfth', 'shoulder', 'children', 'clothes'],
}

interface SentenceItem {
  text: string
  level: 'B1' | 'B2' | 'C1'
}

const SENTENCE_GROUPS: Record<string, SentenceItem[]> = {
  'Daily Life': [
    { text: 'Could you tell me how to get to the nearest station?', level: 'B1' },
    { text: 'I would like to make a reservation for two people.', level: 'B1' },
    { text: 'The weather has been quite unpredictable lately.', level: 'B1' },
    { text: 'Would you mind turning down the volume a little?', level: 'B1' },
    { text: 'I am really looking forward to the weekend.', level: 'B1' },
  ],
  'Business': [
    { text: 'Could you please elaborate on that point?', level: 'B2' },
    { text: 'We need to finalize the quarterly report by Thursday.', level: 'B2' },
    { text: 'I would like to schedule a meeting with your team.', level: 'B2' },
    { text: 'The presentation was thoroughly prepared and well-structured.', level: 'C1' },
    { text: 'We sincerely appreciate your contribution to this project.', level: 'B2' },
  ],
  'IELTS / Academic': [
    { text: 'There are several factors that contribute to this phenomenon.', level: 'C1' },
    { text: 'The evidence strongly suggests that this trend will continue.', level: 'C1' },
    { text: 'The advantages of this approach outweigh the disadvantages.', level: 'B2' },
    { text: 'This issue has been widely debated among researchers worldwide.', level: 'C1' },
    { text: 'It is worth noting that the results vary significantly.', level: 'B2' },
  ],
  'TH Drills': [
    { text: 'This is the third time I have thought about this theory.', level: 'B1' },
    { text: 'The weather there seemed rather threatening and thick with clouds.', level: 'B2' },
    { text: 'Both of them breathed through their teeth without thinking.', level: 'B2' },
    { text: 'The author thought thoroughly about the theme of the thesis.', level: 'C1' },
    { text: 'Three thousand people gathered on the path beneath the bridge.', level: 'B2' },
  ],
}

const LEVEL_COLOR: Record<string, string> = {
  B1: 'bg-blue-100 text-blue-700 border-blue-200',
  B2: 'bg-purple-100 text-purple-700 border-purple-200',
  C1: 'bg-orange-100 text-orange-700 border-orange-200',
}

// ── Component ───────────────────────────────────────────────────────────────

type Mode = 'word' | 'sentence'

export default function PronunciationPage() {
  const qc = useQueryClient()

  const [mode,          setMode]          = useState<Mode>('word')
  const [sessionId,     setSessionId]     = useState<string | null>(null)
  const [activeWord,    setActiveWord]    = useState<string | null>(null)
  const [activeSentence, setActiveSentence] = useState<string | null>(null)
  const [activeGroup,   setActiveGroup]   = useState<string>('TH Sounds')
  const [activeSentGrp, setActiveSentGrp] = useState<string>('Daily Life')

  const createSession  = useCreateSession()
  const submitAttempt  = useSubmitAttempt(sessionId ?? '')
  const { data: attempts } = useAttempts(sessionId)
  const { status, progress, message, result, resetResult } = usePronunciationWs(sessionId)

  const activeText = mode === 'word' ? activeWord : activeSentence
  const isProcessing = status === 'processing' || status === 'transcribed'

  // ── Handlers ───────────────────────────────────────────────────────────

  const handleSelect = async (text: string, type: 'WORD' | 'SENTENCE') => {
    if (text === activeText && sessionId) return
    if (type === 'WORD') setActiveWord(text)
    else setActiveSentence(text)
    setSessionId(null)
    resetResult()
    const session = await createSession.mutateAsync({ targetText: text, sessionType: type })
    setSessionId(session.id)
  }

  const handleSubmit = async (blob: Blob) => {
    if (!sessionId) return
    resetResult()
    await submitAttempt.mutateAsync(blob)
    qc.invalidateQueries({ queryKey: ['pronunciation-attempts', sessionId] })
    qc.invalidateQueries({ queryKey: ['pronunciation-session',  sessionId] })
  }

  const handleRetry = () => resetResult()

  const switchMode = (m: Mode) => {
    setMode(m)
    setSessionId(null)
    setActiveWord(null)
    setActiveSentence(null)
    resetResult()
    if (m === 'word') setActiveGroup('TH Sounds')
    else setActiveSentGrp('Daily Life')
  }

  const bestScore = attempts?.length
    ? Math.max(...attempts.map((a) => a.overallScore))
    : null

  return (
    <div className="max-w-2xl mx-auto py-6 space-y-6 px-4">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Pronunciation Practice</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Record your voice and get instant phoneme-level feedback.
        </p>
      </div>

      {/* Mode tabs */}
      <div className="flex gap-2 p-1 bg-muted rounded-lg w-fit">
        <button
          onClick={() => switchMode('word')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors',
            mode === 'word'
              ? 'bg-background shadow-sm text-foreground'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <Type className="h-4 w-4" />
          Words
        </button>
        <button
          onClick={() => switchMode('sentence')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors',
            mode === 'sentence'
              ? 'bg-background shadow-sm text-foreground'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <BookOpen className="h-4 w-4" />
          Sentences
        </button>
      </div>

      {/* ── Words selector ── */}
      {mode === 'word' && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">Choose a word</CardTitle>
            <div className="flex flex-wrap gap-1.5 mt-2">
              {Object.keys(WORD_GROUPS).map((g) => (
                <button
                  key={g}
                  onClick={() => setActiveGroup(g)}
                  className={cn(
                    'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
                    activeGroup === g
                      ? 'bg-primary text-primary-foreground border-primary'
                      : 'border-muted hover:border-primary/50 text-muted-foreground',
                  )}
                >
                  {g}
                </button>
              ))}
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {WORD_GROUPS[activeGroup].map((word) => (
                <button
                  key={word}
                  onClick={() => handleSelect(word, 'WORD')}
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
      )}

      {/* ── Sentences selector ── */}
      {mode === 'sentence' && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">Choose a sentence</CardTitle>
            <div className="flex flex-wrap gap-1.5 mt-2">
              {Object.keys(SENTENCE_GROUPS).map((g) => (
                <button
                  key={g}
                  onClick={() => setActiveSentGrp(g)}
                  className={cn(
                    'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
                    activeSentGrp === g
                      ? 'bg-primary text-primary-foreground border-primary'
                      : 'border-muted hover:border-primary/50 text-muted-foreground',
                  )}
                >
                  {g}
                </button>
              ))}
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            {SENTENCE_GROUPS[activeSentGrp].map(({ text, level }) => (
              <button
                key={text}
                onClick={() => handleSelect(text, 'SENTENCE')}
                disabled={createSession.isPending}
                className={cn(
                  'w-full text-left px-4 py-3 rounded-lg border text-sm transition-all',
                  activeSentence === text
                    ? 'bg-primary/5 border-primary shadow-sm'
                    : 'border-muted hover:border-primary/40 hover:bg-accent',
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <span className="leading-relaxed">{text}</span>
                  <Badge
                    variant="outline"
                    className={cn('shrink-0 text-[10px] border', LEVEL_COLOR[level])}
                  >
                    {level}
                  </Badge>
                </div>
              </button>
            ))}
          </CardContent>
        </Card>
      )}

      {/* ── Practice area ── */}
      {activeText && sessionId && (
        <>
          <Card>
            <CardContent className="pt-6 space-y-5">
              {/* Target text */}
              <div className="space-y-3 text-center">
                {mode === 'word' ? (
                  <div className="flex items-center justify-center gap-3">
                    <h2 className="text-5xl font-bold tracking-wide">{activeText}</h2>
                    <button
                      onClick={() => window.speechSynthesis.speak(
                        Object.assign(new SpeechSynthesisUtterance(activeText), { lang: 'en-US', rate: 0.8 })
                      )}
                      className="p-2 rounded-full hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
                      title="Hear native pronunciation"
                    >
                      <Volume2 className="h-5 w-5" />
                    </button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <div className="flex items-start justify-center gap-2">
                      <p className="text-xl font-semibold leading-relaxed text-center">
                        "{activeText}"
                      </p>
                      <button
                        onClick={() => window.speechSynthesis.speak(
                          Object.assign(new SpeechSynthesisUtterance(activeText), { lang: 'en-US', rate: 0.75 })
                        )}
                        className="p-1.5 rounded-full hover:bg-accent transition-colors text-muted-foreground hover:text-foreground shrink-0 mt-0.5"
                        title="Hear native pronunciation"
                      >
                        <Volume2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                )}

                {/* Phoneme / Word display */}
                {mode === 'word' ? (
                  <PhonemeDisplay
                    targetIpa={result?.targetIpa ?? null}
                    phonemeMatches={result?.phonemeMatches}
                    mode={result ? 'result' : 'preview'}
                  />
                ) : (
                  <WordLevelDisplay
                    sentence={activeText}
                    wordAnalyses={result?.wordAnalyses ?? null}
                    mode={result ? 'result' : 'preview'}
                  />
                )}
              </div>

              {/* Best score */}
              {bestScore !== null && (
                <div className="flex justify-center">
                  <Badge variant="outline" className="gap-1.5">
                    <Trophy className="h-3.5 w-3.5 text-yellow-500" />
                    Best: {bestScore}/100
                  </Badge>
                </div>
              )}

              {/* Processing progress */}
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

              {status === 'transcribed' && (
                <p className="text-sm text-center text-muted-foreground italic">
                  Heard: "{message}"
                </p>
              )}

              {status === 'error' && (
                <p className="text-sm text-center text-destructive">{message}</p>
              )}
            </CardContent>
          </Card>

          {/* Recorder / Retry */}
          {!result ? (
            <AudioRecorder
              maxDurationSec={mode === 'sentence' ? 30 : 15}
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

          {/* History */}
          {(attempts?.length ?? 0) > 1 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Attempt History</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-1.5">
                  {[...(attempts ?? [])].reverse().map((a) => (
                    <div key={a.attemptId} className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">#{a.attemptNumber}</span>
                      <span className="font-mono text-xs text-muted-foreground italic truncate max-w-[60%] mx-2">
                        "{a.transcript}"
                      </span>
                      <Badge
                        variant="outline"
                        className={cn(
                          'font-mono shrink-0',
                          a.overallScore >= 80 ? 'text-green-600 border-green-300' :
                          a.overallScore >= 60 ? 'text-yellow-600 border-yellow-300' :
                                                 'text-red-600 border-red-300',
                        )}
                      >
                        {a.overallScore}/100
                      </Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}

      {/* Empty state */}
      {!activeText && (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">
            {mode === 'word' ? 'Select a word above to start' : 'Select a sentence above to start'}
          </p>
          <p className="text-sm mt-1">Record your voice and get instant phoneme feedback</p>
        </div>
      )}
    </div>
  )
}
