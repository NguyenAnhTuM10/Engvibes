import {
  useRef, useState, useEffect, useMemo, useCallback,
} from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft, BookmarkPlus, Volume2, VolumeX,
  RotateCcw, ChevronRight, ChevronLeft, CheckCircle2, Mic,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import CefrBadge from '@/components/ui/CefrBadge'
import { useVideo, useVideoSubtitles } from '@/features/videos/api'
import { useCreateOrGetSession, useVocabInfo, useAddVocabFromListen } from '@/features/session/api'
import type { SubtitleSegment, Vocab, WarmupWord } from '@/shared/types/api'
import { toast } from 'sonner'

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtMs(ms: number) {
  const s = Math.floor(ms / 1000)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

function calcScore(actual: string, typed: string): number {
  const actualW = actual.toLowerCase().replace(/[^a-z\s']/g, '').trim().split(/\s+/).filter(Boolean)
  const typedW = typed.toLowerCase().replace(/[^a-z\s']/g, '').trim().split(/\s+/).filter(Boolean)
  if (actualW.length === 0) return 100
  let correct = 0
  actualW.forEach((w, i) => { if (typedW[i] === w) correct++ })
  return Math.round((correct / actualW.length) * 100)
}

// ── Dictation word diff ───────────────────────────────────────────────────────

function WordDiff({ actual, typed }: { actual: string; typed: string }) {
  const displayWords = actual.trim().split(/\s+/).filter(Boolean)
  const typedClean = typed.toLowerCase().replace(/[^a-z\s']/g, '').trim().split(/\s+/).filter(Boolean)

  return (
    <p className="text-sm leading-loose">
      {displayWords.map((word, i) => {
        const clean = word.toLowerCase().replace(/[^a-z']/g, '')
        const correct = typedClean[i]?.replace(/[^a-z']/g, '') === clean
        return (
          <span key={i}
            className={cn('mr-1 px-0.5 rounded', correct
              ? 'text-green-600 dark:text-green-400'
              : 'bg-red-100 dark:bg-red-950/30 text-red-600 dark:text-red-400 line-through decoration-red-500')}>
            {word}
          </span>
        )
      })}
    </p>
  )
}

// ── Dictation panel ───────────────────────────────────────────────────────────

interface DictSegResult {
  idx: number
  typed: string
  score: number
}

interface DictState {
  currentIdx: number
  phase: 'play' | 'type' | 'check' | 'done'
  typed: string
  isPlaying: boolean
  results: DictSegResult[]
}

function DictationPanel({
  subtitles,
  videoRef,
  onExit,
}: {
  subtitles: SubtitleSegment[]
  videoRef: React.RefObject<HTMLVideoElement>
  onExit: () => void
}) {
  const [state, setState] = useState<DictState>({
    currentIdx: 0,
    phase: 'play',
    typed: '',
    isPlaying: false,
    results: [],
  })
  const inputRef = useRef<HTMLInputElement>(null)
  const stopHandlerRef = useRef<(() => void) | null>(null)

  const seg = subtitles[state.currentIdx]
  const totalSegs = subtitles.length
  const overallScore = state.results.length
    ? Math.round(state.results.reduce((sum, r) => sum + r.score, 0) / state.results.length)
    : 0

  // Clean up stop handler on unmount
  useEffect(() => {
    return () => {
      const video = videoRef.current
      if (video && stopHandlerRef.current) {
        video.removeEventListener('timeupdate', stopHandlerRef.current)
        video.pause()
      }
    }
  }, [videoRef])

  // Focus input when entering 'type' phase
  useEffect(() => {
    if (state.phase === 'type') {
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [state.phase, state.currentIdx])

  const playCurrent = useCallback(() => {
    const video = videoRef.current
    if (!video || !seg) return

    // Remove previous stop handler
    if (stopHandlerRef.current) {
      video.removeEventListener('timeupdate', stopHandlerRef.current)
      stopHandlerRef.current = null
    }

    video.currentTime = seg.startMs / 1000
    const endSec = seg.endMs / 1000

    const handler = () => {
      if (video.currentTime >= endSec) {
        video.pause()
        video.removeEventListener('timeupdate', handler)
        stopHandlerRef.current = null
        setState((s) => ({ ...s, isPlaying: false, phase: 'type' }))
      }
    }
    stopHandlerRef.current = handler
    video.addEventListener('timeupdate', handler)

    video.play().then(() => {
      setState((s) => ({ ...s, isPlaying: true, phase: 'play' }))
    }).catch(() => {
      setState((s) => ({ ...s, isPlaying: false, phase: 'type' }))
    })
  }, [videoRef, seg])

  const handleCheck = useCallback(() => {
    if (!seg) return
    const score = calcScore(seg.text, state.typed)
    setState((s) => ({
      ...s,
      phase: 'check',
      results: [...s.results, { idx: s.currentIdx, typed: s.typed, score }],
    }))
  }, [seg, state.typed])

  const handleNext = useCallback(() => {
    const nextIdx = state.currentIdx + 1
    if (nextIdx >= totalSegs) {
      setState((s) => ({ ...s, phase: 'done' }))
    } else {
      setState((s) => ({ ...s, currentIdx: nextIdx, phase: 'play', typed: '', isPlaying: false }))
    }
  }, [state.currentIdx, totalSegs])

  const handlePrev = useCallback(() => {
    if (state.currentIdx === 0) return
    // Remove last result
    setState((s) => ({
      ...s,
      currentIdx: s.currentIdx - 1,
      phase: 'play',
      typed: '',
      isPlaying: false,
      results: s.results.filter((r) => r.idx !== s.currentIdx - 1),
    }))
  }, [state.currentIdx])

  // ── Done screen ──────────────────────────────────────────────────────────
  if (state.phase === 'done') {
    const avg = state.results.length
      ? Math.round(state.results.reduce((s, r) => s + r.score, 0) / state.results.length)
      : 0

    return (
      <div className="flex flex-col items-center justify-center h-full px-8 py-12 space-y-8 max-w-lg mx-auto">
        <div>
          <CheckCircle2 className="h-14 w-14 text-green-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-center">Dictation Complete!</h2>
          <p className="text-muted-foreground text-center mt-1">
            {totalSegs} sentences — {state.results.length} completed
          </p>
        </div>

        <div className="w-full rounded-2xl border p-6 text-center space-y-1">
          <p className="text-6xl font-black tabular-nums" style={{
            color: avg >= 80 ? '#22c55e' : avg >= 60 ? '#3b82f6' : '#f97316',
          }}>{avg}%</p>
          <p className="text-sm text-muted-foreground font-medium">Overall accuracy</p>
        </div>

        <div className="w-full space-y-2">
          {state.results.map((r) => (
            <div key={r.idx} className="flex items-center justify-between text-xs border rounded-lg px-3 py-2">
              <span className="text-muted-foreground truncate flex-1 mr-3">
                {subtitles[r.idx]?.text}
              </span>
              <span className={cn('font-semibold shrink-0',
                r.score >= 80 ? 'text-green-600' : r.score >= 60 ? 'text-blue-600' : 'text-orange-500')}>
                {r.score}%
              </span>
            </div>
          ))}
        </div>

        <div className="flex gap-3 w-full">
          <Button variant="outline" className="flex-1" onClick={() => {
            setState({ currentIdx: 0, phase: 'play', typed: '', isPlaying: false, results: [] })
          }}>
            <RotateCcw className="h-3.5 w-3.5 mr-1.5" /> Retry
          </Button>
          <Button className="flex-1" onClick={onExit}>Back to video</Button>
        </div>
      </div>
    )
  }

  // ── Active segment ───────────────────────────────────────────────────────
  const progress = (state.currentIdx / totalSegs) * 100
  const currentResult = state.results.find((r) => r.idx === state.currentIdx)

  return (
    <div className="flex flex-col h-full">
      {/* Progress bar */}
      <div className="h-1 bg-muted shrink-0">
        <div className="h-full bg-primary transition-all duration-300" style={{ width: `${progress}%` }} />
      </div>

      {/* Top bar */}
      <div className="flex items-center justify-between px-6 py-3 border-b bg-muted/30 shrink-0">
        <Button variant="ghost" size="sm" onClick={onExit} className="gap-1">
          <ArrowLeft className="h-3.5 w-3.5" /> Exit
        </Button>
        <div className="text-center">
          <p className="text-sm font-semibold">Dictation Mode</p>
          <p className="text-xs text-muted-foreground">
            Sentence {state.currentIdx + 1} of {totalSegs}
          </p>
        </div>
        <div className="text-right min-w-[60px]">
          {state.results.length > 0 && (
            <p className={cn('text-sm font-bold',
              overallScore >= 80 ? 'text-green-600' : overallScore >= 60 ? 'text-blue-600' : 'text-orange-500')}>
              {overallScore}%
            </p>
          )}
        </div>
      </div>

      {/* Main area */}
      <div className="flex-1 overflow-y-auto flex flex-col items-center justify-center px-6 py-8">
        <div className="w-full max-w-xl space-y-6">
          {/* Timestamp & play controls */}
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono text-muted-foreground">
              {fmtMs(seg?.startMs ?? 0)} – {fmtMs(seg?.endMs ?? 0)}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={playCurrent}
                disabled={state.isPlaying}
                className="gap-1.5"
              >
                {state.isPlaying ? (
                  <VolumeX className="h-3.5 w-3.5 animate-pulse" />
                ) : (
                  <Volume2 className="h-3.5 w-3.5" />
                )}
                {state.phase === 'play' && !state.isPlaying ? 'Play' : state.isPlaying ? 'Playing…' : 'Replay'}
              </Button>
            </div>
          </div>

          {/* Instruction */}
          {state.phase === 'play' && !state.isPlaying && (
            <div className="rounded-xl border-2 border-dashed border-primary/30 p-6 text-center space-y-2">
              <Volume2 className="h-8 w-8 text-primary/40 mx-auto" />
              <p className="text-sm font-medium text-muted-foreground">
                Press <strong>Play</strong> to hear the sentence, then type what you hear
              </p>
            </div>
          )}

          {(state.phase === 'type' || state.phase === 'check') && (
            <>
              {/* Input */}
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Type what you heard
                </label>
                <input
                  ref={inputRef}
                  type="text"
                  value={state.typed}
                  onChange={(e) => setState((s) => ({ ...s, typed: e.target.value }))}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && state.phase === 'type' && state.typed.trim()) handleCheck()
                  }}
                  disabled={state.phase === 'check'}
                  placeholder="Type the sentence you heard…"
                  className="w-full rounded-xl border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-60 disabled:cursor-default"
                />
              </div>

              {/* Check button */}
              {state.phase === 'type' && (
                <Button
                  className="w-full"
                  onClick={handleCheck}
                  disabled={!state.typed.trim()}
                >
                  Check answer
                </Button>
              )}

              {/* Result */}
              {state.phase === 'check' && currentResult && (
                <div className="rounded-xl border p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Correct text</p>
                    <span className={cn('text-sm font-bold',
                      currentResult.score >= 80 ? 'text-green-600' : currentResult.score >= 60 ? 'text-blue-600' : 'text-orange-500')}>
                      {currentResult.score}%
                    </span>
                  </div>
                  <WordDiff actual={seg?.text ?? ''} typed={state.typed} />
                  <p className="text-xs text-muted-foreground pt-1 border-t">
                    Your answer:{' '}
                    <span className="text-foreground">{state.typed || '(empty)'}</span>
                  </p>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Bottom nav */}
      <div className="border-t px-6 py-3 flex items-center justify-between shrink-0 bg-background">
        <Button variant="ghost" size="sm" onClick={handlePrev} disabled={state.currentIdx === 0} className="gap-1">
          <ChevronLeft className="h-4 w-4" /> Prev
        </Button>
        {state.phase === 'check' && (
          <Button onClick={handleNext} className="gap-1.5">
            {state.currentIdx + 1 >= totalSegs ? 'Finish' : 'Next'}
            <ChevronRight className="h-4 w-4" />
          </Button>
        )}
        {state.phase !== 'check' && (
          <Button variant="ghost" size="sm" onClick={handleNext} className="gap-1 text-muted-foreground">
            Skip <ChevronRight className="h-4 w-4" />
          </Button>
        )}
      </div>
    </div>
  )
}

// ── Transcript segment text (clickable words) ─────────────────────────────────

function SegmentText({
  text,
  onWordClick,
}: {
  text: string
  onWordClick: (word: string, e: React.MouseEvent<HTMLSpanElement>) => void
}) {
  const parts = text.split(/(\s+)/)
  return (
    <>
      {parts.map((part, i) => {
        if (/^\s+$/.test(part)) return <span key={i}>{part}</span>
        const clean = part.replace(/^[^a-zA-Z']+|[^a-zA-Z']+$/g, '')
        if (!clean) return <span key={i}>{part}</span>
        return (
          <span key={i}
            onClick={(e) => onWordClick(clean.toLowerCase(), e)}
            className="cursor-pointer hover:bg-yellow-200/60 dark:hover:bg-yellow-600/25 rounded px-0.5 -mx-0.5 transition-colors">
            {part}
          </span>
        )
      })}
    </>
  )
}

// ── Vocab popover ─────────────────────────────────────────────────────────────

function VocabCard({
  word, warmupWord, vocab, isLoading, position, onSave, onClose, isSaving,
}: {
  word: string
  warmupWord: WarmupWord | undefined
  vocab: Vocab | undefined
  isLoading: boolean
  position: { x: number; y: number }
  onSave: (vocabId: string) => void
  onClose: () => void
  isSaving: boolean
}) {
  const def = vocab?.definition ?? warmupWord?.definition
  const ipa = vocab?.ipa ?? warmupWord?.ipa
  const pos = vocab?.partOfSpeech ?? warmupWord?.partOfSpeech

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!(e.target as Element).closest('[data-vocab-card]')) onClose()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onClose])

  return (
    <div data-vocab-card
      className="fixed z-50 w-64 bg-background border rounded-xl shadow-xl p-4 space-y-2"
      style={{
        left: Math.min(position.x, window.innerWidth - 280),
        top: Math.min(position.y + 8, window.innerHeight - 220),
      }}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold capitalize">{word}</p>
          {ipa && <p className="text-xs text-muted-foreground font-mono mt-0.5">{ipa}</p>}
          {pos && <p className="text-xs text-blue-600 dark:text-blue-400 capitalize">{pos}</p>}
        </div>
        <button onClick={onClose}
          className="text-muted-foreground hover:text-foreground text-xl leading-none shrink-0 -mt-0.5">×</button>
      </div>
      {isLoading ? (
        <div className="h-3 w-32 bg-muted rounded animate-pulse" />
      ) : def ? (
        <p className="text-sm text-muted-foreground leading-snug">{def}</p>
      ) : !warmupWord ? (
        <p className="text-xs text-muted-foreground italic">No definition found</p>
      ) : null}
      {vocab?.id && (
        <Button size="sm" className="w-full gap-1.5 mt-1"
          onClick={() => onSave(vocab.id)} disabled={isSaving}>
          <BookmarkPlus className="h-3.5 w-3.5" />
          {isSaving ? 'Saving…' : 'Save to Flashcards'}
        </Button>
      )}
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function VideoWatchPage() {
  const { videoId } = useParams<{ videoId: string }>()
  const navigate = useNavigate()

  const { data: video, isLoading: videoLoading } = useVideo(videoId!)
  const { data: subtitles } = useVideoSubtitles(videoId!)

  const videoRef = useRef<HTMLVideoElement>(null)
  const segmentRefs = useRef<(HTMLDivElement | null)[]>([])

  const [activeIdx, setActiveIdx] = useState(-1)
  const [dictationMode, setDictationMode] = useState(false)

  // Vocab popover
  const [selectedWord, setSelectedWord] = useState<string | null>(null)
  const [popoverPos, setPopoverPos] = useState<{ x: number; y: number } | null>(null)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const createSession = useCreateOrGetSession()
  const { data: vocabInfo, isLoading: vocabLoading } = useVocabInfo(sessionId ?? '', selectedWord)
  const addVocab = useAddVocabFromListen(sessionId ?? '')

  // Create session eagerly for vocab saving
  useEffect(() => {
    if (!videoId) return
    createSession.mutateAsync(videoId).then((s) => setSessionId(s.id)).catch(() => {})
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [videoId])

  // Track active subtitle on timeupdate
  useEffect(() => {
    const video = videoRef.current
    if (!video || !subtitles?.length) return
    const handler = () => {
      const ms = video.currentTime * 1000
      const idx = subtitles.findIndex((s) => ms >= s.startMs && ms < s.endMs)
      setActiveIdx((prev) => (prev !== idx ? idx : prev))
    }
    video.addEventListener('timeupdate', handler)
    return () => video.removeEventListener('timeupdate', handler)
  }, [subtitles])

  // Auto-scroll transcript
  useEffect(() => {
    if (activeIdx >= 0 && !dictationMode) {
      segmentRefs.current[activeIdx]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [activeIdx, dictationMode])

  const handleWordClick = useCallback((word: string, e: React.MouseEvent<HTMLSpanElement>) => {
    const rect = (e.target as HTMLElement).getBoundingClientRect()
    setPopoverPos({ x: rect.left, y: rect.bottom })
    setSelectedWord(word)
  }, [])

  const handleJumpTo = useCallback((startMs: number, e: React.MouseEvent) => {
    e.stopPropagation()
    if (videoRef.current) {
      videoRef.current.currentTime = startMs / 1000
      videoRef.current.play().catch(() => {})
    }
  }, [])

  const handleSaveVocab = useCallback((vocabId: string) => {
    if (!sessionId) { toast.error('Session not ready, try again.'); return }
    addVocab.mutate({ vocabId }, {
      onSuccess: () => {
        setSelectedWord(null)
        setPopoverPos(null)
        toast.success('Saved to flashcards!')
      },
    })
  }, [sessionId, addVocab])

  const warmupWord = useMemo(
    () => video?.warmupWords?.find((w) => w.word.toLowerCase() === selectedWord),
    [video?.warmupWords, selectedWord],
  )

  if (videoLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }
  if (!video) {
    return (
      <div className="flex h-screen items-center justify-center flex-col gap-3">
        <p className="text-muted-foreground">Video not found</p>
        <Button variant="outline" onClick={() => navigate('/videos')}>Back to Videos</Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-screen bg-background overflow-hidden">
      {/* Hidden video element — always in DOM for audio during dictation */}
      <video
        ref={videoRef}
        src={video.videoUrl ?? ''}
        preload="auto"
        className={cn('absolute inset-0 pointer-events-none opacity-0', !dictationMode && 'hidden')}
        aria-hidden
      />

      {/* ── Header ──────────────────────────────────────────────────────── */}
      <header className="flex items-center gap-3 px-4 h-14 border-b bg-background shrink-0 z-10">
        <Button variant="ghost" size="icon" onClick={() => navigate('/videos')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-sm truncate">{video.title}</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <CefrBadge level={video.cefrLevel} />
          {video.topic && (
            <span className="text-xs bg-secondary text-secondary-foreground px-2 py-0.5 rounded-full capitalize hidden sm:inline">
              {video.topic}
            </span>
          )}
          <Button
            variant={dictationMode ? 'default' : 'outline'}
            size="sm"
            onClick={() => setDictationMode((d) => !d)}
            className="gap-1.5"
          >
            {dictationMode ? <ArrowLeft className="h-3.5 w-3.5" /> : <Volume2 className="h-3.5 w-3.5" />}
            <span className="hidden sm:inline">{dictationMode ? 'Back to Video' : 'Dictation Mode'}</span>
          </Button>
          <Button variant="outline" size="sm" onClick={() => navigate('/speak')} className="gap-1.5 hidden sm:flex">
            <Mic className="h-3.5 w-3.5" /> Speaking
          </Button>
        </div>
      </header>

      {/* ── Dictation mode: full-width panel ────────────────────────────── */}
      {dictationMode && subtitles && subtitles.length > 0 && (
        <DictationPanel
          subtitles={subtitles}
          videoRef={videoRef as React.RefObject<HTMLVideoElement>}
          onExit={() => setDictationMode(false)}
        />
      )}

      {dictationMode && (!subtitles || subtitles.length === 0) && (
        <div className="flex-1 flex items-center justify-center flex-col gap-3 text-muted-foreground">
          <p>No transcript available for dictation.</p>
          <Button variant="outline" onClick={() => setDictationMode(false)}>Back to Video</Button>
        </div>
      )}

      {/* ── Normal watch mode: video + transcript ────────────────────────── */}
      {!dictationMode && (
        <div className="flex flex-1 min-h-0">
          {/* Video panel */}
          <div className="flex flex-col flex-[0_0_58%] bg-black min-h-0">
            <div className="flex-1 flex items-center justify-center min-h-0">
              {video.videoUrl ? (
                <video
                  ref={videoRef}
                  src={video.videoUrl}
                  controls
                  className="w-full h-full object-contain"
                  preload="metadata"
                />
              ) : (
                <div className="flex items-center justify-center text-white/40 text-sm">
                  Video not available
                </div>
              )}
            </div>
            {video.summary && (
              <div className="px-4 py-2.5 bg-background/95 border-t shrink-0">
                <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                  {video.summary}
                </p>
              </div>
            )}
          </div>

          {/* Transcript panel */}
          <div className="flex flex-col flex-[0_0_42%] border-l min-h-0 bg-background">
            <div className="flex items-center justify-between px-4 py-2.5 border-b bg-muted/30 shrink-0">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Transcript {subtitles && subtitles.length > 0 && `· ${subtitles.length} segments`}
              </span>
              <span className="text-xs text-muted-foreground hidden sm:inline">
                Click a word to save
              </span>
            </div>

            <div className="flex-1 overflow-y-auto">
              {!subtitles ? (
                <div className="flex items-center justify-center h-full gap-2 text-muted-foreground text-sm">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                  Loading…
                </div>
              ) : subtitles.length === 0 ? (
                <div className="flex items-center justify-center h-full text-muted-foreground text-sm">
                  No transcript available
                </div>
              ) : (
                <div className="py-2 px-2">
                  {subtitles.map((seg, i) => {
                    const isActive = i === activeIdx
                    return (
                      <div key={seg.id}
                        ref={(el) => { segmentRefs.current[i] = el }}
                        className={cn(
                          'flex gap-3 px-3 py-2.5 rounded-lg transition-all',
                          isActive ? 'bg-primary/10 border border-primary/25' : 'hover:bg-muted/40',
                        )}>
                        <span role="button" title="Jump to"
                          onClick={(e) => handleJumpTo(seg.startMs, e)}
                          className={cn(
                            'text-xs font-mono shrink-0 pt-0.5 w-9 text-right cursor-pointer hover:text-primary transition-colors',
                            isActive ? 'text-primary font-semibold' : 'text-muted-foreground',
                          )}>
                          {fmtMs(seg.startMs)}
                        </span>
                        <p className={cn('text-sm leading-relaxed flex-1',
                          isActive ? 'text-foreground font-medium' : 'text-muted-foreground')}>
                          <SegmentText text={seg.text} onWordClick={handleWordClick} />
                        </p>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Vocab popover */}
      {selectedWord && popoverPos && (
        <VocabCard
          word={selectedWord}
          warmupWord={warmupWord}
          vocab={vocabInfo}
          isLoading={vocabLoading}
          position={popoverPos}
          onSave={handleSaveVocab}
          onClose={() => { setSelectedWord(null); setPopoverPos(null) }}
          isSaving={addVocab.isPending}
        />
      )}
    </div>
  )
}
