import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import AudioRecorder from '@/shared/components/AudioRecorder'
import { PhonemeDisplay } from '@/features/pronunciation/components/PhonemeDisplay'
import { WordLevelDisplay } from '@/features/pronunciation/components/WordLevelDisplay'
import { ScorePanel } from '@/features/pronunciation/components/ScorePanel'
import { FeedbackPanel } from '@/features/pronunciation/components/FeedbackPanel'
import { usePronunciationWs } from '@/features/pronunciation/hooks/usePronunciationWs'
import {
  useCreateSession,
  useSubmitAttempt,
  useAttempts,
  useWords,
  useSentences,
} from '@/features/pronunciation/api'
import type {
  PronunciationSentence,
  PronunciationWord,
} from '@/features/pronunciation/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { RotateCcw, Trophy, Volume2, BookOpen, Type } from 'lucide-react'
import { cn } from '@/lib/utils'

const LEVEL_COLOR: Record<string, string> = {
  B1: 'bg-blue-100 text-blue-700 border-blue-200',
  B2: 'bg-purple-100 text-purple-700 border-purple-200',
  C1: 'bg-orange-100 text-orange-700 border-orange-200',
}

// Gom mảng theo key, giữ nguyên thứ tự xuất hiện đầu tiên của mỗi nhóm.
function groupBy<T>(items: T[], key: (item: T) => string): Record<string, T[]> {
  const out: Record<string, T[]> = {}
  for (const item of items) {
    const k = key(item)
    ;(out[k] ??= []).push(item)
  }
  return out
}

// ── Component ───────────────────────────────────────────────────────────────

type Mode = 'word' | 'sentence'

export default function PronunciationPage() {
  const qc = useQueryClient()

  const { data: words } = useWords()
  const { data: sentences } = useSentences()

  const wordGroups = useMemo(
    () => groupBy(words ?? [], (w) => w.group),
    [words],
  )
  const sentenceGroups = useMemo(
    () => groupBy(sentences ?? [], (s) => s.category),
    [sentences],
  )
  const wordGroupNames = Object.keys(wordGroups)
  const sentGroupNames = Object.keys(sentenceGroups)

  const [mode,           setMode]           = useState<Mode>('word')
  const [sessionId,      setSessionId]      = useState<string | null>(null)
  const [activeWord,     setActiveWord]     = useState<string | null>(null)
  const [activeSentence, setActiveSentence] = useState<string | null>(null)
  const [activeGroup,    setActiveGroup]    = useState<string | null>(null)
  const [activeSentGrp,  setActiveSentGrp]  = useState<string | null>(null)

  const createSession  = useCreateSession()
  const submitAttempt  = useSubmitAttempt(sessionId ?? '')
  const { data: attempts } = useAttempts(sessionId)
  const { status, progress, message, result, resetResult } = usePronunciationWs(sessionId)

  // Nhóm đang chọn — fallback về nhóm đầu tiên khi nội dung vừa load xong
  const currentGroup = activeGroup ?? wordGroupNames[0] ?? ''
  const currentSentGrp = activeSentGrp ?? sentGroupNames[0] ?? ''

  const activeText = mode === 'word' ? activeWord : activeSentence
  const isProcessing = status === 'processing' || status === 'transcribed'

  // Metadata của từ/câu đang chọn (IPA, câu ví dụ, target sound)
  const activeWordEntry: PronunciationWord | undefined =
    wordGroups[currentGroup]?.find((w) => w.text === activeWord)
  const activeSentEntry: PronunciationSentence | undefined =
    (sentences ?? []).find((s) => s.text === activeSentence)

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
              {wordGroupNames.map((g) => (
                <button
                  key={g}
                  onClick={() => setActiveGroup(g)}
                  className={cn(
                    'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
                    currentGroup === g
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
              {(wordGroups[currentGroup] ?? []).map((w) => (
                <button
                  key={w.text}
                  onClick={() => handleSelect(w.text, 'WORD')}
                  disabled={createSession.isPending}
                  title={w.exampleSentence}
                  className={cn(
                    'flex flex-col items-center px-4 py-2 rounded-lg border text-sm font-medium transition-all',
                    activeWord === w.text
                      ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                      : 'border-muted hover:border-primary/50 hover:bg-accent',
                  )}
                >
                  <span>{w.text}</span>
                  <span
                    className={cn(
                      'text-[11px] font-mono mt-0.5',
                      activeWord === w.text ? 'text-primary-foreground/80' : 'text-muted-foreground',
                    )}
                  >
                    /{w.ipa}/
                  </span>
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
              {sentGroupNames.map((g) => (
                <button
                  key={g}
                  onClick={() => setActiveSentGrp(g)}
                  className={cn(
                    'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
                    currentSentGrp === g
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
            {(sentenceGroups[currentSentGrp] ?? []).map(({ text, level, targetSound }) => (
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
                  <div className="flex shrink-0 items-center gap-1.5">
                    {targetSound && (
                      <Badge variant="outline" className="text-[10px] font-mono">
                        /{targetSound}/
                      </Badge>
                    )}
                    <Badge
                      variant="outline"
                      className={cn('text-[10px] border', LEVEL_COLOR[level])}
                    >
                      {level}
                    </Badge>
                  </div>
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

                {/* Phoneme / Word display — preview dùng IPA tĩnh có sẵn */}
                {mode === 'word' ? (
                  <PhonemeDisplay
                    targetIpa={result?.targetIpa ?? activeWordEntry?.ipa ?? null}
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

                {/* Chi tiết làm giàu cho từ: nghĩa, âm trọng tâm, cặp tương phản, lỗi, tip */}
                {mode === 'word' && activeWordEntry && (
                  <div className="space-y-1.5 pt-1 text-sm text-left max-w-md mx-auto">
                    <p className="text-center text-muted-foreground">
                      <span className="text-foreground font-medium">{activeWordEntry.vi}</span>
                      {' · '}Focus{' '}
                      <span className="font-mono text-foreground">/{activeWordEntry.targetSound}/</span>
                      {activeWordEntry.minimalPair && (
                        <>{' · '}≠{' '}
                          <span className="font-mono text-foreground">{activeWordEntry.minimalPair}</span>
                        </>
                      )}
                    </p>
                    <p className="italic text-muted-foreground text-center">
                      "{activeWordEntry.exampleSentence}"
                    </p>
                    <p className="text-amber-600 dark:text-amber-400">⚠ {activeWordEntry.commonError}</p>
                    <p className="text-muted-foreground">💡 {activeWordEntry.tip}</p>
                  </div>
                )}

                {/* Chi tiết cho câu: âm trọng tâm, bản dịch, tip */}
                {mode === 'sentence' && activeSentEntry && (
                  <div className="space-y-1 pt-1 text-sm max-w-md mx-auto">
                    {activeSentEntry.targetSound && (
                      <p className="text-center text-muted-foreground">
                        Focus{' '}
                        <span className="font-mono text-foreground">/{activeSentEntry.targetSound}/</span>
                      </p>
                    )}
                    <p className="italic text-muted-foreground text-center">{activeSentEntry.vi}</p>
                    <p className="text-muted-foreground">💡 {activeSentEntry.tip}</p>
                  </div>
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
              {/* Vòng nối SRS: báo từ vừa được thêm/cập nhật vào "Sounds to practice" */}
              {(result.soundCardChanges?.length ?? 0) > 0 && (
                <div className="rounded-lg border border-primary/30 bg-primary/5 px-4 py-3 text-sm space-y-1">
                  {result.soundCardChanges!.map((c) => (
                    <p key={c.cardId} className="flex items-center gap-1.5">
                      <BookOpen className="h-3.5 w-3.5 text-primary shrink-0" />
                      {c.action === 'ADDED' && <>Đã thêm <b>"{c.word}"</b> vào danh sách ôn “Sounds to practice”.</>}
                      {c.action === 'DEMOTED' && <>Vẫn cần luyện <b>"{c.word}"</b> — đã đẩy lên đầu hàng đợi ôn.</>}
                      {c.action === 'PROMOTED' && <>Phát âm tốt <b>"{c.word}"</b>! Giãn lịch ôn{c.intervalDays ? ` (+${c.intervalDays} ngày)` : ''}.</>}
                    </p>
                  ))}
                </div>
              )}
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
