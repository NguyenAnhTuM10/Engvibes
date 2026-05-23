import { useState, useMemo } from 'react'
import { ChevronDown, ChevronUp, ArrowRight, ArrowLeft, Mic, RotateCcw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import PageHeader from '@/components/ui/PageHeader'
import AudioRecorder from '@/shared/components/AudioRecorder'
import { useAssessFreeformSpeak } from '@/features/speaking/api'
import { situations, categories, type Situation, type SituationLevel } from '@/data/situations'
import type { IeltsFeedback } from '@/shared/types/api'

// ── Helpers ───────────────────────────────────────────────────────────────────

const LEVEL_COLOR: Record<SituationLevel, string> = {
  A2: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  B1: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  B2: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
}

function bandColor(score: number) {
  if (score >= 7) return 'text-green-600 dark:text-green-400'
  if (score >= 5) return 'text-blue-600 dark:text-blue-400'
  return 'text-orange-500'
}
function bandBg(score: number) {
  if (score >= 7) return 'bg-green-50 border-green-200 dark:bg-green-950/20 dark:border-green-800'
  if (score >= 5) return 'bg-blue-50 border-blue-200 dark:bg-blue-950/20 dark:border-blue-800'
  return 'bg-orange-50 border-orange-200 dark:bg-orange-950/20 dark:border-orange-800'
}
function bandBarColor(score: number) {
  if (score >= 7) return 'bg-green-500'
  if (score >= 5) return 'bg-blue-500'
  return 'bg-orange-400'
}
function bandLabel(score: number) {
  if (score >= 8.5) return 'Expert'
  if (score >= 7.5) return 'Very Good'
  if (score >= 6.5) return 'Good'
  if (score >= 5.5) return 'Competent'
  if (score >= 4.5) return 'Modest'
  return 'Developing'
}

// ── IELTS Band Bar ────────────────────────────────────────────────────────────

function BandBar({ label, value }: { label: string; value: number }) {
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs font-medium">
        <span>{label}</span>
        <span className="font-mono">{value.toFixed(1)}</span>
      </div>
      <div className="h-2 bg-muted rounded-full overflow-hidden">
        <div
          className={cn('h-full rounded-full transition-all duration-700', bandBarColor(value))}
          style={{ width: `${(value / 9) * 100}%` }}
        />
      </div>
    </div>
  )
}

// ── Feedback Panel ────────────────────────────────────────────────────────────

function FeedbackPanel({
  feedback,
  situation,
  onTryAgain,
  onPickAnother,
}: {
  feedback: IeltsFeedback
  situation: Situation
  onTryAgain: () => void
  onPickAnother: () => void
}) {
  const [transcriptOpen, setTranscriptOpen] = useState(false)

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-muted-foreground uppercase tracking-wide font-medium">IELTS Speaking Result</p>
          <h2 className="text-lg font-bold mt-0.5">{situation.icon} {situation.title}</h2>
        </div>
        <Button variant="ghost" size="sm" onClick={onPickAnother}>
          <ArrowLeft className="h-3.5 w-3.5 mr-1" /> Topics
        </Button>
      </div>

      {/* Overall band score */}
      <div className={cn('border rounded-xl p-5', bandBg(feedback.overall))}>
        <div className="flex items-center gap-6">
          <div className="text-center shrink-0">
            <p className="text-xs text-muted-foreground font-medium mb-1">Band Score</p>
            <span className={cn('text-6xl font-black tabular-nums', bandColor(feedback.overall))}>
              {feedback.overall.toFixed(1)}
            </span>
            <p className={cn('text-xs font-semibold mt-1', bandColor(feedback.overall))}>
              {bandLabel(feedback.overall)}
            </p>
          </div>
          <div className="flex-1 space-y-2.5">
            <BandBar label="Fluency & Coherence" value={feedback.fluency} />
            <BandBar label="Grammatical Range"   value={feedback.grammar} />
            <BandBar label="Lexical Resource"    value={feedback.vocabulary} />
            <BandBar label="Pronunciation"       value={feedback.pronunciation} />
          </div>
        </div>
      </div>

      {/* Feedback text */}
      {feedback.feedback && (
        <div className="border rounded-xl p-4 space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Examiner Feedback</p>
          <p className="text-sm leading-relaxed">{feedback.feedback}</p>
        </div>
      )}

      {/* Transcript */}
      {feedback.transcript && (
        <div className="border rounded-xl overflow-hidden">
          <button
            onClick={() => setTranscriptOpen((o) => !o)}
            className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors"
          >
            <span>Your response (transcript)</span>
            {transcriptOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </button>
          {transcriptOpen && (
            <div className="px-4 pb-4 border-t bg-muted/20">
              <p className="pt-3 text-sm leading-relaxed text-muted-foreground italic">
                "{feedback.transcript}"
              </p>
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3 pt-1">
        <Button variant="outline" className="flex-1 gap-1.5" onClick={onTryAgain}>
          <RotateCcw className="h-3.5 w-3.5" /> Try again
        </Button>
        <Button className="flex-1 gap-1.5" onClick={onPickAnother}>
          <ArrowRight className="h-3.5 w-3.5" /> New topic
        </Button>
      </div>
    </div>
  )
}

// ── Speaking Interface ────────────────────────────────────────────────────────

function SpeakingInterface({
  situation,
  onResult,
  onBack,
}: {
  situation: Situation
  onResult: (f: IeltsFeedback) => void
  onBack: () => void
}) {
  const assess = useAssessFreeformSpeak()
  const [hintsOpen, setHintsOpen] = useState(true)

  const handleSubmit = (blob: Blob) => {
    assess.mutate(
      {
        audio: blob,
        situation: situation.title,
        question: situation.question,
        vocab: situation.vocab,
        collocations: situation.collocations,
      },
      { onSuccess: onResult },
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      {/* Back */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-1">
          <ArrowLeft className="h-3.5 w-3.5" /> Topics
        </Button>
        <div className="flex items-center gap-2">
          <span className="text-lg">{situation.icon}</span>
          <span className="text-sm font-semibold">{situation.title}</span>
          <span className={cn('text-xs px-2 py-0.5 rounded-full font-mono font-semibold', LEVEL_COLOR[situation.level])}>
            {situation.level}
          </span>
        </div>
      </div>

      {/* Question */}
      <div className="bg-primary/5 border border-primary/20 rounded-xl p-5">
        <p className="text-xs text-primary font-semibold uppercase tracking-wide mb-2">Question</p>
        <p className="text-lg font-medium leading-relaxed">{situation.question}</p>
      </div>

      <p className="text-sm text-muted-foreground px-1">{situation.description}</p>

      {/* Hints */}
      <div className="border rounded-xl overflow-hidden">
        <button
          onClick={() => setHintsOpen((o) => !o)}
          className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors"
        >
          <span className="flex items-center gap-2">
            Vocabulary & phrase hints
            <span className="text-xs bg-primary/10 text-primary px-1.5 py-0.5 rounded-full">
              {situation.vocab.length + situation.collocations.length}
            </span>
          </span>
          {hintsOpen ? <ChevronUp className="h-4 w-4 shrink-0" /> : <ChevronDown className="h-4 w-4 shrink-0" />}
        </button>

        {hintsOpen && (
          <div className="px-4 pb-4 border-t pt-3 space-y-4">
            <div>
              <p className="text-xs font-medium text-muted-foreground mb-2">Key vocabulary</p>
              <div className="flex flex-wrap gap-1.5">
                {situation.vocab.map((w) => (
                  <span key={w} className="text-xs px-2.5 py-1 bg-muted rounded-full font-medium">{w}</span>
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground mb-2">Useful collocations</p>
              <div className="flex flex-wrap gap-1.5">
                {situation.collocations.map((c) => (
                  <span key={c} className="text-xs px-2.5 py-1 bg-blue-50 dark:bg-blue-950/20 text-blue-700 dark:text-blue-300 rounded-full">{c}</span>
                ))}
              </div>
            </div>
            <div className="border-t pt-3">
              <p className="text-xs font-medium text-muted-foreground mb-1.5">Speaking tips</p>
              {situation.tips.map((t, i) => (
                <p key={i} className="text-xs text-muted-foreground">• {t}</p>
              ))}
            </div>
            <p className="text-xs text-muted-foreground/70 border-t pt-2">
              Aim for 60–90 seconds. Use the hints naturally — don't just list them.
            </p>
          </div>
        )}
      </div>

      {/* Recorder */}
      <div className="border-t pt-5">
        <AudioRecorder maxDurationSec={90} isSubmitting={assess.isPending} onSubmit={handleSubmit} />
      </div>
    </div>
  )
}

// ── Situation Card ────────────────────────────────────────────────────────────

function SituationCard({ situation, onSelect }: { situation: Situation; onSelect: () => void }) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => e.key === 'Enter' && onSelect()}
      className="border rounded-xl p-4 cursor-pointer hover:shadow-md hover:border-primary/40 transition-all group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring space-y-2"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-2xl">{situation.icon}</span>
          <div>
            <p className="text-sm font-semibold leading-snug group-hover:text-primary transition-colors">
              {situation.title}
            </p>
            <p className="text-xs text-muted-foreground capitalize">{situation.category}</p>
          </div>
        </div>
        <span className={cn('text-xs px-2 py-0.5 rounded-full font-mono font-semibold shrink-0', LEVEL_COLOR[situation.level])}>
          {situation.level}
        </span>
      </div>

      <p className="text-xs text-muted-foreground line-clamp-2 italic">
        "{situation.question}"
      </p>

      <div className="flex flex-wrap gap-1">
        {situation.vocab.slice(0, 4).map((w) => (
          <span key={w} className="text-xs px-1.5 py-0.5 bg-muted rounded font-medium text-muted-foreground">{w}</span>
        ))}
        {situation.vocab.length > 4 && (
          <span className="text-xs text-muted-foreground/60">+{situation.vocab.length - 4} more</span>
        )}
      </div>
    </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────

type Stage = 'pick' | 'speaking' | 'result'

export default function SpeakPracticePage() {
  const [stage, setStage] = useState<Stage>('pick')
  const [selected, setSelected] = useState<Situation | null>(null)
  const [feedback, setFeedback] = useState<IeltsFeedback | null>(null)
  const [activeCategory, setActiveCategory] = useState<string | null>(null)
  const [activeLevel, setActiveLevel] = useState<SituationLevel | null>(null)

  const filtered = useMemo(() =>
    situations.filter((s) => {
      if (activeCategory && s.category !== activeCategory) return false
      if (activeLevel && s.level !== activeLevel) return false
      return true
    }),
    [activeCategory, activeLevel],
  )

  if (stage === 'result' && feedback && selected) {
    return (
      <FeedbackPanel
        feedback={feedback}
        situation={selected}
        onTryAgain={() => { setFeedback(null); setStage('speaking') }}
        onPickAnother={() => { setSelected(null); setFeedback(null); setStage('pick') }}
      />
    )
  }

  if (stage === 'speaking' && selected) {
    return (
      <SpeakingInterface
        situation={selected}
        onResult={(f) => { setFeedback(f); setStage('result') }}
        onBack={() => { setSelected(null); setStage('pick') }}
      />
    )
  }

  return (
    <div>
      <PageHeader
        title="Speaking Practice"
        description="Choose a situation, speak for 60–90 seconds, get IELTS band scores and examiner feedback"
      />

      {/* Category filter */}
      <div className="flex items-center gap-2 flex-wrap mb-3">
        <button
          onClick={() => setActiveCategory(null)}
          className={cn(
            'text-xs px-3 py-1.5 rounded-full border font-medium transition-colors flex items-center gap-1.5',
            !activeCategory ? 'bg-primary text-primary-foreground border-primary' : 'hover:bg-muted border-border',
          )}
        >
          <Mic className="h-3 w-3" /> All topics
        </button>
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat === activeCategory ? null : cat)}
            className={cn(
              'text-xs px-3 py-1.5 rounded-full border font-medium transition-colors',
              activeCategory === cat ? 'bg-primary text-primary-foreground border-primary' : 'hover:bg-muted border-border',
            )}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Level filter */}
      <div className="flex items-center gap-2 mb-6">
        {(['A2', 'B1', 'B2'] as SituationLevel[]).map((level) => (
          <button
            key={level}
            onClick={() => setActiveLevel(level === activeLevel ? null : level)}
            className={cn(
              'text-xs px-3 py-1.5 rounded-full border font-mono font-semibold transition-colors',
              activeLevel === level
                ? 'bg-primary text-primary-foreground border-primary'
                : cn('hover:bg-muted border-border', LEVEL_COLOR[level]),
            )}
          >
            {level}
          </button>
        ))}
        {(activeCategory || activeLevel) && (
          <button
            onClick={() => { setActiveCategory(null); setActiveLevel(null) }}
            className="text-xs text-muted-foreground underline underline-offset-2 ml-2 hover:text-foreground transition-colors"
          >
            Clear
          </button>
        )}
      </div>

      {/* Grid */}
      {filtered.length === 0 ? (
        <div className="flex flex-col items-center py-24 text-muted-foreground gap-3">
          <p className="text-base font-medium">No situations found</p>
          <Button variant="outline" size="sm" onClick={() => { setActiveCategory(null); setActiveLevel(null) }}>
            Clear filters
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((s) => (
            <SituationCard key={s.id} situation={s} onSelect={() => { setSelected(s); setStage('speaking') }} />
          ))}
        </div>
      )}
    </div>
  )
}
