import { useState, useRef, useEffect, useCallback } from 'react'
import { Mic, MicOff, PhoneOff, Loader2, Volume2, ArrowLeft, RotateCcw, ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import PageHeader from '@/components/ui/PageHeader'
import { useScenarios, useConversationReview, type ConversationReviewResponse } from '@/features/conversation/api'
import type { ConversationScenario } from '@/features/conversation/types'
import { useAuthStore } from '@/features/auth/store'

// ── Types ─────────────────────────────────────────────────────────────────────

interface Message {
  role: 'user' | 'assistant'
  text: string
}

type Stage = 'pick' | 'connecting' | 'active' | 'reviewing' | 'result'

// ── Constants ─────────────────────────────────────────────────────────────────

const SAMPLE_RATE = 24000
// T3.1 — Voice + instructions do SERVER sở hữu (application.yml + proxy).
// Client KHÔNG set voice/instructions/model nữa; proxy strip nếu gửi.

// ── Audio helpers ─────────────────────────────────────────────────────────────

function float32ToPcm16Base64(float32: Float32Array): string {
  const pcm16 = new Int16Array(float32.length)
  for (let i = 0; i < float32.length; i++) {
    const s = Math.max(-1, Math.min(1, float32[i]))
    pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7fff
  }
  const bytes = new Uint8Array(pcm16.buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i])
  return btoa(binary)
}

/**
 * Streaming audio player.
 *
 * Thay vì gom hết chunk rồi mới phát (mất tính realtime + dễ tràn bộ nhớ),
 * mỗi chunk delta được decode và lên lịch phát ngay sau chunk trước đó.
 * Giữ tham chiếu tới các BufferSource đang phát để có thể dừng khi user ngắt lời.
 */
class StreamingAudioPlayer {
  private ctx: AudioContext
  private scheduledAt: number
  private sources: AudioBufferSourceNode[] = []

  constructor(ctx: AudioContext) {
    this.ctx = ctx
    this.scheduledAt = ctx.currentTime
  }

  /** Phát một chunk PCM16 base64 ngay khi nhận được. */
  enqueue(base64Chunk: string) {
    if (!base64Chunk) return
    const binary = atob(base64Chunk)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
    if (bytes.length === 0) return

    const int16 = new Int16Array(bytes.buffer, 0, Math.floor(bytes.length / 2))
    const float32 = new Float32Array(int16.length)
    for (let i = 0; i < int16.length; i++) float32[i] = int16[i] / 32768

    const buffer = this.ctx.createBuffer(1, float32.length, SAMPLE_RATE)
    buffer.copyToChannel(float32, 0)

    const src = this.ctx.createBufferSource()
    src.buffer = buffer
    src.connect(this.ctx.destination)

    const startAt = Math.max(this.ctx.currentTime, this.scheduledAt)
    src.start(startAt)
    this.scheduledAt = startAt + buffer.duration

    this.sources.push(src)
    src.onended = () => {
      this.sources = this.sources.filter((s) => s !== src)
    }
  }

  /** Dừng ngay mọi audio đang phát/đã lên lịch (khi user ngắt lời). */
  stopAll() {
    for (const src of this.sources) {
      try { src.stop() } catch { /* đã dừng */ }
    }
    this.sources = []
    this.scheduledAt = this.ctx.currentTime
  }
}

// ── Scenario Card ─────────────────────────────────────────────────────────────

function ScenarioCard({ scenario, onSelect }: { scenario: ConversationScenario; onSelect: () => void }) {
  const icons: Record<string, string> = {
    JOB_INTERVIEW: '💼',
    COFFEE_SHOP: '☕',
    HOTEL_CHECKIN: '🏨',
    DOCTOR_APPOINTMENT: '🏥',
    MAKING_PLANS: '📅',
  }
  return (
      <div
          role="button"
          tabIndex={0}
          onClick={onSelect}
          onKeyDown={(e) => e.key === 'Enter' && onSelect()}
          className="border rounded-xl p-4 cursor-pointer hover:shadow-md hover:border-primary/40 transition-all group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring space-y-2"
      >
        <div className="flex items-center gap-3">
          <span className="text-2xl">{icons[scenario.id] ?? '🗣️'}</span>
          <div>
            <p className="text-sm font-semibold group-hover:text-primary transition-colors">{scenario.displayName}</p>
            <p className="text-xs text-muted-foreground">{scenario.aiRole}</p>
          </div>
        </div>
        <p className="text-xs text-muted-foreground leading-relaxed">{scenario.description}</p>
        <p className="text-xs text-primary/80 font-medium">Goal: {scenario.userGoal}</p>
      </div>
  )
}

// ── Result Panel ──────────────────────────────────────────────────────────────

function ResultPanel({
                       feedback,
                       scenario,
                       onTryAgain,
                       onPickAnother,
                     }: {
  feedback: ConversationReviewResponse
  scenario: ConversationScenario
  onTryAgain: () => void
  onPickAnother: () => void
}) {
  function bandColor(s: number) {
    if (s >= 7) return 'text-green-600 dark:text-green-400'
    if (s >= 5) return 'text-blue-600 dark:text-blue-400'
    return 'text-orange-500'
  }
  function barColor(s: number) {
    if (s >= 7) return 'bg-green-500'
    if (s >= 5) return 'bg-blue-500'
    return 'bg-orange-400'
  }
  function BandBar({ label, value }: { label: string; value: number }) {
    return (
        <div className="space-y-1">
          <div className="flex justify-between text-xs font-medium">
            <span>{label}</span>
            <span className="font-mono">{value.toFixed(1)}</span>
          </div>
          <div className="h-2 bg-muted rounded-full overflow-hidden">
            <div className={cn('h-full rounded-full transition-all duration-700', barColor(value))}
                 style={{ width: `${(value / 9) * 100}%` }} />
          </div>
        </div>
    )
  }

  return (
      <div className="max-w-xl mx-auto space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground uppercase tracking-wide font-medium">Conversation Result</p>
            <h2 className="text-lg font-bold mt-0.5">{scenario.displayName}</h2>
          </div>
          <Button variant="ghost" size="sm" onClick={onPickAnother}>
            <ArrowLeft className="h-3.5 w-3.5 mr-1" /> Scenarios
          </Button>
        </div>

        <div className={cn('border rounded-xl p-5', feedback.overall >= 7
            ? 'bg-green-50 border-green-200 dark:bg-green-950/20 dark:border-green-800'
            : feedback.overall >= 5
                ? 'bg-blue-50 border-blue-200 dark:bg-blue-950/20 dark:border-blue-800'
                : 'bg-orange-50 border-orange-200 dark:bg-orange-950/20 dark:border-orange-800')}>
          <div className="flex items-center gap-5">
            <div className="text-center shrink-0">
              <p className="text-xs text-muted-foreground font-medium mb-1">Band Score</p>
              <span className={cn('text-5xl font-black tabular-nums', bandColor(feedback.overall))}>
              {feedback.overall.toFixed(1)}
            </span>
            </div>
            <div className="flex-1 space-y-2.5">
              <BandBar label="Fluency" value={feedback.fluency} />
              <BandBar label="Grammar" value={feedback.grammar} />
              <BandBar label="Vocabulary" value={feedback.vocabulary} />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="border rounded-xl p-4 space-y-1">
            <p className="text-xs font-semibold text-green-700 dark:text-green-400">What worked</p>
            <p className="text-xs text-muted-foreground leading-relaxed">{feedback.strengths}</p>
          </div>
          <div className="border rounded-xl p-4 space-y-1">
            <p className="text-xs font-semibold text-blue-700 dark:text-blue-400">How to improve</p>
            <p className="text-xs text-muted-foreground leading-relaxed">{feedback.improvements}</p>
          </div>
        </div>

        {feedback.grammarNotes?.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Grammar notes</p>
              {feedback.grammarNotes.map((n, i) => (
                  <div key={i} className="border-l-4 border-red-400 pl-3 py-1 space-y-0.5">
                    <p className="text-xs line-through text-red-500/75">{n.error}</p>
                    <p className="text-xs text-green-600 dark:text-green-400 font-medium">✓ {n.correction}</p>
                  </div>
              ))}
            </div>
        )}

        <p className="text-sm text-center text-muted-foreground italic">{feedback.encouragement}</p>

        <div className="flex gap-3 pt-1">
          <Button variant="outline" className="flex-1 gap-1.5" onClick={onTryAgain}>
            <RotateCcw className="h-3.5 w-3.5" /> Try again
          </Button>
          <Button className="flex-1 gap-1.5" onClick={onPickAnother}>
            <ArrowRight className="h-3.5 w-3.5" /> New scenario
          </Button>
        </div>
      </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function ConversationPage() {
  const [stage, setStage] = useState<Stage>('pick')
  const [sessionInfo, setSessionInfo] = useState<ConversationScenario | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [isAiSpeaking, setIsAiSpeaking] = useState(false)
  const [isMuted, setIsMuted] = useState(false)
  const [elapsedSec, setElapsedSec] = useState(0)
  const [feedback, setFeedback] = useState<ConversationReviewResponse | null>(null)

  const wsRef           = useRef<WebSocket | null>(null)
  const audioCtxRef     = useRef<AudioContext | null>(null)
  const streamRef       = useRef<MediaStream | null>(null)
  const processorRef    = useRef<ScriptProcessorNode | null>(null)
  const playerRef       = useRef<StreamingAudioPlayer | null>(null)
  const startTimeRef    = useRef<number>(0)
  const timerRef        = useRef<ReturnType<typeof setInterval> | null>(null)
  const messagesEndRef  = useRef<HTMLDivElement>(null)
  // ID của item AI đang phát — cần cho conversation.item.truncate khi bị ngắt lời.
  const activeResponseItemRef = useRef<string | null>(null)
  // T2 — DB session id do server tạo (gửi qua event app.session_created).
  const dbSessionIdRef = useRef<string | null>(null)

  const { data: scenarios } = useScenarios()
  const review = useConversationReview()

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    return () => { cleanup() }
  }, [])

  const cleanup = useCallback(() => {
    if (timerRef.current) clearInterval(timerRef.current)
    playerRef.current?.stopAll()
    processorRef.current?.disconnect()
    streamRef.current?.getTracks().forEach((t) => t.stop())
    audioCtxRef.current?.close()
    wsRef.current?.close()
    processorRef.current = null
    streamRef.current    = null
    audioCtxRef.current  = null
    wsRef.current        = null
    playerRef.current    = null
    activeResponseItemRef.current = null
  }, [])

  const startMicCapture = useCallback((audioCtx: AudioContext, ws: WebSocket, stream: MediaStream) => {
    const source    = audioCtx.createMediaStreamSource(stream)
    const processor = audioCtx.createScriptProcessor(4096, 1, 1)
    processorRef.current = processor
    source.connect(processor)
    processor.connect(audioCtx.destination)

    processor.onaudioprocess = (e) => {
      if (ws.readyState !== WebSocket.OPEN) return
      const data = e.inputBuffer.getChannelData(0)
      // Luôn append audio — semantic_vad phía server lo việc phát hiện end-of-turn.
      ws.send(JSON.stringify({ type: 'input_audio_buffer.append', audio: float32ToPcm16Base64(data) }))
    }

    startTimeRef.current = Date.now()
    timerRef.current = setInterval(() => {
      setElapsedSec(Math.floor((Date.now() - startTimeRef.current) / 1000))
    }, 1000)
  }, [])

  const startConversation = useCallback(async (scenario: ConversationScenario) => {
    setSessionInfo(scenario)
    setStage('connecting')
    setMessages([])
    setElapsedSec(0)

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    streamRef.current = stream

    const audioCtx = new AudioContext({ sampleRate: SAMPLE_RATE })
    audioCtxRef.current = audioCtx
    playerRef.current = new StreamingAudioPlayer(audioCtx)

    const token = useAuthStore.getState().token ?? ''
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const ws = new WebSocket(
        `${proto}://${window.location.host}/ws/conversation?token=${token}&scenario=${scenario.id}`,
    )
    wsRef.current = ws

    ws.onopen = () => {
      // Chờ session.created từ OpenAI trước khi cấu hình session.
    }

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data as string)
      switch (data.type) {
        // T2 — server báo DB sessionId, lưu lại để dùng khi review.
        case 'app.session_created':
          dbSessionIdRef.current = data.sessionId
          break

        case 'session.created':
          // T3.1 — Client CHỈ set audio format (cần cho mic PCM).
          // voice/instructions/model do server set qua proxy.sendServerSessionUpdate.
          ws.send(JSON.stringify({
            type: 'session.update',
            session: {
              type: 'realtime',
              output_modalities: ['audio'],
              audio: {
                input: {
                  format: { type: 'audio/pcm', rate: SAMPLE_RATE },
                  turn_detection: { type: 'semantic_vad' },
                  // BẬT transcription cho audio của user — nếu thiếu,
                  // event conversation.item.input_audio_transcription.completed
                  // sẽ không bao giờ được phát và tin nhắn user không hiện.
                  transcription: { model: 'gpt-realtime-whisper' },
                },
                output: {
                  // API yêu cầu 'rate' — thiếu sẽ lỗi missing_required_parameter.
                  format: { type: 'audio/pcm', rate: SAMPLE_RATE },
                },
              },
            },
          }))
          break

        case 'session.updated':
          if (!processorRef.current) {
            startMicCapture(audioCtx, ws, stream)
            setStage('active')
            // Kích AI nói câu mở đầu.
            ws.send(JSON.stringify({ type: 'response.create' }))
          }
          break

          // ── Audio output: phát từng delta ngay (streaming) ──────────────────
        case 'response.output_audio.delta':
          playerRef.current?.enqueue(data.delta)
          break

        case 'response.output_audio.done':
          // Audio đã được phát dần qua các delta — không cần làm gì với bytes ở đây.
          break

          // ── Transcript của AI ───────────────────────────────────────────────
          // Tên event đúng theo API mới là response.output_audio_transcript.done
          // (code cũ dùng response.audio_transcript.done — không tồn tại).
        case 'response.output_audio_transcript.done':
          if (data.transcript?.trim()) {
            setMessages((prev) => [...prev, { role: 'assistant', text: data.transcript }])
          }
          break

          // ── Lifecycle của response ──────────────────────────────────────────
        case 'response.created':
          setIsAiSpeaking(true)
          break

        case 'response.output_item.added':
          // Lưu item_id của message AI để có thể truncate khi user ngắt lời.
          if (data.item?.type === 'message' && data.item?.role === 'assistant') {
            activeResponseItemRef.current = data.item.id
          }
          break

        case 'response.done':
          setIsAiSpeaking(false)
          activeResponseItemRef.current = null
          break

          // ── User ngắt lời (barge-in) ─────────────────────────────────────────
          // Với WebSocket, client tự quản lý playback nên phải tự xử lý truncation.
        case 'input_audio_buffer.speech_started': {
          // Dừng ngay audio AI đang phát.
          playerRef.current?.stopAll()
          setIsAiSpeaking(false)
          // Báo server cắt phần audio chưa phát khỏi conversation,
          // để AI biết nó bị ngắt ở đâu.
          const itemId = activeResponseItemRef.current
          if (itemId) {
            ws.send(JSON.stringify({
              type: 'conversation.item.truncate',
              item_id: itemId,
              content_index: 0,
              audio_end_ms: 0,
            }))
            activeResponseItemRef.current = null
          }
          break
        }

          // ── Transcript của user ─────────────────────────────────────────────
        case 'conversation.item.input_audio_transcription.completed':
          if (data.transcript?.trim()) {
            setMessages((prev) => [...prev, { role: 'user', text: data.transcript }])
          }
          break

        case 'error':
          console.error('OpenAI Realtime error:', data.error)
          break
      }
    }

    ws.onclose = () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }

    ws.onerror = () => {
      setStage('pick')
      cleanup()
    }
  }, [cleanup, startMicCapture])

  const endConversation = useCallback(() => {
    cleanup()
    setStage('reviewing')

    const durationSec = Math.floor((Date.now() - startTimeRef.current) / 1000)
    const sessionId = dbSessionIdRef.current

    if (!sessionId) {
      // Không có session server → không thể chấm (transcript ở server)
      setStage('pick')
      return
    }

    // T2.2 — chỉ gửi sessionId + duration; server tự load transcript chính thức.
    review.mutate(
        { sessionId, durationSec },
        {
          onSuccess: (data) => {
            setFeedback(data)
            setStage('result')
          },
          onError: () => setStage('active'),
        },
    )
  }, [cleanup, review])

  const handlePickScenario = useCallback(
      (scenario: ConversationScenario) => {
        startConversation(scenario)
      },
      [startConversation],
  )

  const fmt = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`

  // ── Result ──────────────────────────────────────────────────────────────────
  if (stage === 'result' && feedback && sessionInfo) {
    return (
        <ResultPanel
            feedback={feedback}
            scenario={sessionInfo}
            onTryAgain={() => {
              setFeedback(null)
              handlePickScenario(sessionInfo)
            }}
            onPickAnother={() => {
              setFeedback(null)
              setSessionInfo(null)
              setStage('pick')
            }}
        />
    )
  }

  // ── Reviewing ───────────────────────────────────────────────────────────────
  if (stage === 'reviewing') {
    return (
        <div className="flex flex-col items-center justify-center py-32 gap-4 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin" />
          <p className="text-sm font-medium">Analyzing your conversation...</p>
        </div>
    )
  }

  // ── Active conversation ─────────────────────────────────────────────────────
  if (stage === 'active' && sessionInfo) {
    return (
        <div className="flex flex-col h-[calc(100vh-8rem)] max-w-2xl mx-auto">
          <div className="flex items-center justify-between pb-3 border-b shrink-0">
            <div>
              <p className="text-xs text-muted-foreground uppercase tracking-wide">Speaking with</p>
              <p className="text-sm font-semibold">{sessionInfo.aiRole}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs font-mono text-muted-foreground">{fmt(elapsedSec)}</span>
              {isAiSpeaking && (
                  <span className="flex items-center gap-1 text-xs text-primary">
                <Volume2 className="h-3.5 w-3.5 animate-pulse" /> Speaking...
              </span>
              )}
              <Button variant="destructive" size="sm" className="gap-1.5" onClick={endConversation}>
                <PhoneOff className="h-3.5 w-3.5" /> End
              </Button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto py-4 space-y-3">
            {messages.length === 0 && (
                <p className="text-center text-sm text-muted-foreground pt-8">
                  Conversation starting... speak when the AI finishes.
                </p>
            )}
            {messages.map((m, i) => (
                <div key={i} className={cn('flex', m.role === 'user' ? 'justify-end' : 'justify-start')}>
                  <div className={cn(
                      'max-w-[78%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed',
                      m.role === 'user'
                          ? 'bg-primary text-primary-foreground rounded-br-sm'
                          : 'bg-muted rounded-bl-sm',
                  )}>
                    {m.text}
                  </div>
                </div>
            ))}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t pt-3 shrink-0 flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              {isMuted ? 'Mic muted' : 'Mic active — speak naturally'}
            </p>
            <button
                onClick={() => {
                  const tracks = streamRef.current?.getAudioTracks()
                  if (tracks) {
                    const next = !isMuted
                    tracks.forEach((t) => { t.enabled = !next })
                    setIsMuted(next)
                  }
                }}
                className={cn(
                    'h-10 w-10 rounded-full flex items-center justify-center transition-colors',
                    isMuted ? 'bg-red-100 text-red-600' : 'bg-muted text-muted-foreground hover:bg-muted/80',
                )}
            >
              {isMuted ? <MicOff className="h-4 w-4" /> : <Mic className="h-4 w-4" />}
            </button>
          </div>
        </div>
    )
  }

  // ── Connecting ──────────────────────────────────────────────────────────────
  if (stage === 'connecting') {
    return (
        <div className="flex flex-col items-center justify-center py-32 gap-4 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin" />
          <p className="text-sm font-medium">Connecting to AI conversation partner...</p>
        </div>
    )
  }

  // ── Pick scenario ───────────────────────────────────────────────────────────
  return (
      <div>
        <PageHeader
            title="Conversation Practice"
            description="Real-time AI conversation — choose a scenario and speak naturally. Get IELTS feedback at the end."
        />

        {!scenarios ? (
            <div className="flex justify-center py-16">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
        ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {scenarios.map((s) => (
                  <ScenarioCard
                      key={s.id}
                      scenario={s}
                      onSelect={() => handlePickScenario(s)}
                  />
              ))}
            </div>
        )}
      </div>
  )
}