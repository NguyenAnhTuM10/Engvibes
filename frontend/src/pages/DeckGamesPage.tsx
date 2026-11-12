import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, XCircle, Shuffle } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { useDecks } from '@/features/flashcard/api'
import {
  useAnswerMultipleChoice, useCheckMatching, useCheckTyping,
  useMatching, useMultipleChoice, useTyping,
  type MCResult, type MatchResult, type TypingResult,
} from '@/features/flashcard/games'

type GameMode = 'menu' | 'mc' | 'matching' | 'typing'

const TITLES: Record<GameMode, string> = {
  menu: 'Games',
  mc: 'Multiple Choice',
  matching: 'Matching',
  typing: 'Typing Recall',
}

const formatNextReview = (iso: string) => new Date(iso).toLocaleDateString()

// ─── Main Page ────────────────────────────────────────────────────────────

export default function DeckGamesPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [mode, setMode] = useState<GameMode>('menu')
  const { data: decks = [] } = useDecks()
  const deck = decks.find((d) => d.id === id)

  if (!id) return null

  return (
    <div className="max-w-2xl mx-auto py-6 px-4 space-y-6">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Back"
          onClick={() => (mode === 'menu' ? navigate(`/decks/${id}`) : setMode('menu'))}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-xl font-bold">{TITLES[mode]}</h1>
          {deck && <p className="text-xs text-muted-foreground">{deck.name}</p>}
        </div>
      </div>

      {mode === 'menu' && <GameMenu onSelect={setMode} />}
      {mode === 'mc' && <MultipleChoiceGame deckId={id} />}
      {mode === 'matching' && <MatchingGameUI deckId={id} />}
      {mode === 'typing' && <TypingGame deckId={id} />}
    </div>
  )
}

// ─── Shared states ────────────────────────────────────────────────────────

function Loading() {
  return <div className="text-center py-12 text-muted-foreground">Loading…</div>
}

function NotEnoughCards({ error }: { error: Error | null }) {
  return (
    <div className="text-center py-12 text-muted-foreground">
      {error?.message ?? 'Not enough cards in this deck'}
    </div>
  )
}

// ─── Menu ─────────────────────────────────────────────────────────────────

function GameMenu({ onSelect }: { onSelect: (m: GameMode) => void }) {
  const games = [
    { mode: 'mc' as const, label: 'Multiple Choice', desc: 'Pick the correct meaning from 4 options (needs 4+ cards)', icon: '🔤' },
    { mode: 'matching' as const, label: 'Matching', desc: 'Connect each word to its meaning', icon: '🔗' },
    { mode: 'typing' as const, label: 'Typing Recall', desc: 'See the meaning, type the word', icon: '⌨️' },
  ]
  return (
    <div className="grid gap-3">
      {games.map((g) => (
        <Card key={g.mode} className="cursor-pointer hover:shadow-sm transition-shadow" onClick={() => onSelect(g.mode)}>
          <CardContent className="py-4 px-5 flex items-center gap-4">
            <span className="text-3xl" aria-hidden>{g.icon}</span>
            <div>
              <p className="font-semibold">{g.label}</p>
              <p className="text-sm text-muted-foreground">{g.desc}</p>
            </div>
          </CardContent>
        </Card>
      ))}
      <p className="text-xs text-muted-foreground text-center">
        Multiple choice and typing answers update your review schedule (correct = Good, wrong = Again).
      </p>
    </div>
  )
}

// ─── 1. Multiple Choice ────────────────────────────────────────────────────

function MultipleChoiceGame({ deckId }: { deckId: string }) {
  const { data: questions, isLoading, error, refetch } = useMultipleChoice(deckId)
  const answer = useAnswerMultipleChoice()

  const [idx, setIdx] = useState(0)
  const [result, setResult] = useState<MCResult | null>(null)
  const [selected, setSelected] = useState<string | null>(null)

  const q = questions?.[idx]

  const handleSelect = async (opt: string) => {
    if (result || !q) return
    setSelected(opt)
    setResult(await answer.mutateAsync({ cardId: q.cardId, selected: opt }))
  }

  const next = () => {
    setResult(null)
    setSelected(null)
    setIdx((i) => i + 1)
  }

  if (isLoading) return <Loading />
  if (!questions?.length) return <NotEnoughCards error={error} />

  if (idx >= questions.length) {
    return (
      <div className="text-center py-12 space-y-4">
        <p className="text-2xl">🎉 Done! Answered {questions.length} questions.</p>
        <Button onClick={() => { setIdx(0); refetch() }}>Play again</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="text-sm text-muted-foreground">{idx + 1} / {questions.length}</div>

      <Card>
        <CardContent className="pt-6 pb-4 text-center">
          <p className="text-4xl font-bold mb-1">{q?.front}</p>
          <p className="text-sm text-muted-foreground">Choose the correct meaning</p>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {q?.options.map((opt) => {
          const isCorrect = result?.correctAnswer === opt
          const isWrongPick = result && !result.correct && selected === opt
          return (
            <button
              key={opt}
              disabled={!!result || answer.isPending}
              onClick={() => handleSelect(opt)}
              className={cn(
                'rounded-lg border-2 p-3 text-sm text-left transition-colors',
                !result && 'border-muted hover:border-primary/60 hover:bg-accent',
                result && isCorrect && 'bg-green-100 border-green-500 text-green-800',
                isWrongPick && 'bg-red-100 border-red-500 text-red-800',
                result && !isCorrect && !isWrongPick && 'opacity-40',
              )}
            >
              {opt}
            </button>
          )
        })}
      </div>

      {result && (
        <div className={cn('flex items-center gap-2 text-sm font-medium p-3 rounded-lg',
          result.correct ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700')}>
          {result.correct
            ? <><CheckCircle2 className="h-4 w-4" /> Correct! Next review: {formatNextReview(result.nextReview)}</>
            : <><XCircle className="h-4 w-4" /> Wrong. Answer: {result.correctAnswer}</>}
          <Button size="sm" variant="ghost" className="ml-auto" onClick={next}>Next →</Button>
        </div>
      )}
    </div>
  )
}

// ─── 2. Matching ───────────────────────────────────────────────────────────

function MatchingGameUI({ deckId }: { deckId: string }) {
  const { data: game, isLoading, error, refetch } = useMatching(deckId)
  const check = useCheckMatching()

  const [selected, setSelected] = useState<string | null>(null)       // cardId of selected term
  const [pairs, setPairs] = useState<Record<string, string>>({})      // cardId → matched definition
  const [result, setResult] = useState<MatchResult | null>(null)

  if (isLoading) return <Loading />
  if (!game) return <NotEnoughCards error={error} />

  const handleTerm = (cardId: string) => {
    if (result) return
    if (pairs[cardId]) {
      // Click a matched term again to undo the pair
      setPairs((p) => {
        const nextPairs = { ...p }
        delete nextPairs[cardId]
        return nextPairs
      })
      return
    }
    setSelected((s) => (s === cardId ? null : cardId))
  }

  const handleDef = (def: string) => {
    if (result || !selected) return
    setPairs((p) => ({ ...p, [selected]: def }))
    setSelected(null)
  }

  const handleCheck = async () => {
    const pairList = Object.entries(pairs).map(([cardId, matchedBack]) => ({ cardId, matchedBack }))
    const r = await check.mutateAsync(pairList)
    setResult(r)
    if (r.correct === r.total) toast.success('Perfect match!')
  }

  const matchedDefs = new Set(Object.values(pairs))
  const allPaired = Object.keys(pairs).length === game.terms.length

  return (
    <div className="space-y-4">
      {result && (
        <div className={cn('p-3 rounded-lg text-sm space-y-1',
          result.correct === result.total ? 'bg-green-50 text-green-700' : 'bg-yellow-50 text-yellow-800')}>
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="h-4 w-4" />
            {result.correct}/{result.total} correct
            <Button size="sm" variant="ghost" className="ml-auto"
              onClick={() => { setPairs({}); setResult(null); refetch() }}>
              <Shuffle className="h-3 w-3 mr-1" /> New round
            </Button>
          </div>
          {result.wrongPairs.map((w) => (
            <p key={w.cardId} className="text-red-600">
              {w.front} → {w.correctAnswer}
            </p>
          ))}
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Terms</p>
          {game.terms.map((t) => (
            <button
              key={t.cardId}
              onClick={() => handleTerm(t.cardId)}
              className={cn(
                'w-full rounded-lg border-2 px-3 py-2 text-sm text-left transition-colors',
                pairs[t.cardId] ? 'border-green-400 bg-green-50 text-green-800'
                  : selected === t.cardId ? 'border-primary bg-primary/5'
                  : 'border-muted hover:border-primary/40',
              )}
            >
              {t.front}
              {pairs[t.cardId] && <span className="text-xs block text-green-600 truncate">{pairs[t.cardId]}</span>}
            </button>
          ))}
        </div>

        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Meanings</p>
          {game.definitions.map((def, i) => (
            <button
              key={`${def}-${i}`}
              onClick={() => handleDef(def)}
              disabled={matchedDefs.has(def)}
              className={cn(
                'w-full rounded-lg border-2 px-3 py-2 text-sm text-left transition-colors',
                matchedDefs.has(def) ? 'border-muted bg-muted text-muted-foreground opacity-50'
                  : selected ? 'border-primary/40 hover:border-primary hover:bg-accent'
                  : 'border-muted',
              )}
            >
              {def}
            </button>
          ))}
        </div>
      </div>

      {!result && (
        <Button className="w-full" disabled={!allPaired || check.isPending} onClick={handleCheck}>
          Check answers
        </Button>
      )}
    </div>
  )
}

// ─── 3. Typing Recall ──────────────────────────────────────────────────────

function TypingGame({ deckId }: { deckId: string }) {
  const { data: questions, isLoading, error, refetch } = useTyping(deckId)
  const check = useCheckTyping()

  const [idx, setIdx] = useState(0)
  const [typed, setTyped] = useState('')
  const [result, setResult] = useState<TypingResult | null>(null)

  const q = questions?.[idx]

  const handleSubmit = async () => {
    if (!q || result || !typed.trim()) return
    setResult(await check.mutateAsync({ cardId: q.cardId, typed }))
  }

  const next = () => {
    setResult(null)
    setTyped('')
    setIdx((i) => i + 1)
  }

  if (isLoading) return <Loading />
  if (!questions?.length) return <NotEnoughCards error={error} />

  if (idx >= questions.length) {
    return (
      <div className="text-center py-12 space-y-4">
        <p className="text-2xl">✅ Done! Answered {questions.length} questions.</p>
        <Button onClick={() => { setIdx(0); setTyped(''); refetch() }}>Play again</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between text-sm text-muted-foreground">
        <span>{idx + 1} / {questions.length}</span>
        <span className="text-xs">1-character typos accepted</span>
      </div>

      <Card>
        <CardContent className="pt-6 pb-4 text-center">
          <p className="text-sm text-muted-foreground mb-1">Which word means…</p>
          <p className="text-2xl font-semibold">{q?.back}</p>
        </CardContent>
      </Card>

      <div className="flex gap-2">
        <Input
          autoFocus
          placeholder="Type the English word…"
          aria-label="Your answer"
          value={typed}
          onChange={(e) => setTyped(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
          disabled={!!result}
          className={cn(result && (result.correct ? 'border-green-500' : 'border-red-500'))}
        />
        <Button disabled={!typed.trim() || !!result || check.isPending} onClick={handleSubmit}>
          Check
        </Button>
      </div>

      {result && (
        <div className={cn('flex items-center gap-2 text-sm font-medium p-3 rounded-lg',
          result.correct ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700')}>
          {result.correct
            ? <><CheckCircle2 className="h-4 w-4" /> Correct! Next review: {formatNextReview(result.nextReview)}</>
            : <><XCircle className="h-4 w-4" /> Answer: <strong>{result.correctAnswer}</strong></>}
          <Button size="sm" variant="ghost" className="ml-auto" onClick={next}>Next →</Button>
        </div>
      )}
    </div>
  )
}
