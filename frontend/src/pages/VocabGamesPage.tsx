import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import api from '@/shared/api/client'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ArrowLeft, CheckCircle2, XCircle, Shuffle } from 'lucide-react'
import { cn } from '@/lib/utils'
import { toast } from 'sonner'

const BASE = '/api/sm2/games'

type GameMode = 'menu' | 'mc' | 'matching' | 'typing'

// ─── Types ────────────────────────────────────────────────────────────────

interface MCQuestion { cardId: string; front: string; options: string[] }
interface MCResult   { correct: boolean; correctAnswer: string; intervalDays: number }

interface MatchTerm  { cardId: string; front: string }
interface MatchGame  { terms: MatchTerm[]; definitions: string[] }
interface MatchResult { correct: number; total: number; wrongPairs: {cardId:string;front:string;yourAnswer:string;correctAnswer:string}[] }

interface TypingQ    { cardId: string; back: string }
interface TypingResult { correct: boolean; correctAnswer: string; editDistance: number; intervalDays: number }

// ─── Main Page ────────────────────────────────────────────────────────────

export default function VocabGamesPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [mode, setMode] = useState<GameMode>('menu')

  return (
    <div className="max-w-2xl mx-auto py-6 px-4 space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon"
          onClick={() => mode === 'menu' ? navigate(`/vocab/${id}`) : setMode('menu')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-xl font-bold">
          {mode === 'menu' ? 'Games' : mode === 'mc' ? 'Multiple Choice' : mode === 'matching' ? 'Matching' : 'Typing Recall'}
        </h1>
      </div>

      {mode === 'menu' && <GameMenu deckId={id!} onSelect={setMode} />}
      {mode === 'mc'      && <MultipleChoiceGame deckId={id!} />}
      {mode === 'matching' && <MatchingGameUI deckId={id!} />}
      {mode === 'typing'  && <TypingGame deckId={id!} />}
    </div>
  )
}

// ─── Menu ─────────────────────────────────────────────────────────────────

function GameMenu({ onSelect }: { deckId: string; onSelect: (m: GameMode) => void }) {
  const games = [
    { mode: 'mc' as GameMode,      label: 'Multiple Choice', desc: 'Pick the correct translation from 4 options', icon: '🔤' },
    { mode: 'matching' as GameMode, label: 'Matching',        desc: 'Connect each word to its definition',         icon: '🔗' },
    { mode: 'typing' as GameMode,   label: 'Typing Recall',   desc: 'See the meaning, type the word',             icon: '⌨️' },
  ]
  return (
    <div className="grid gap-3">
      {games.map(g => (
        <Card key={g.mode} className="cursor-pointer hover:shadow-sm transition-shadow"
          onClick={() => onSelect(g.mode)}>
          <CardContent className="py-4 px-5 flex items-center gap-4">
            <span className="text-3xl">{g.icon}</span>
            <div>
              <p className="font-semibold">{g.label}</p>
              <p className="text-sm text-muted-foreground">{g.desc}</p>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

// ─── 1. Multiple Choice ────────────────────────────────────────────────────

function MultipleChoiceGame({ deckId }: { deckId: string }) {
  const { data: questions, isLoading, refetch } = useQuery({
    queryKey: ['game-mc', deckId],
    queryFn: () => api.get<MCQuestion[]>(`${BASE}/multiple-choice`, { params: { deck_id: deckId, count: 10 } }).then(r => r.data),
    staleTime: 0,
  })
  const answer = useMutation({
    mutationFn: ({ cardId, selected }: { cardId: string; selected: string }) =>
      api.post<MCResult>(`${BASE}/answer`, { cardId, selected }).then(r => r.data),
  })

  const [idx, setIdx]       = useState(0)
  const [result, setResult] = useState<MCResult | null>(null)
  const [done, setDone]     = useState(0)

  const q = questions?.[idx]

  const handleSelect = async (opt: string) => {
    if (result || !q) return
    const r = await answer.mutateAsync({ cardId: q.cardId, selected: opt })
    setResult(r)
  }

  const next = () => {
    setResult(null)
    setDone(d => d + 1)
    setIdx(i => i + 1)
  }

  if (isLoading) return <div className="text-center py-12 text-muted-foreground">Loading…</div>
  if (!questions?.length) return <div className="text-center py-12 text-muted-foreground">Not enough cards</div>

  if (idx >= questions.length) {
    return (
      <div className="text-center py-12 space-y-4">
        <p className="text-2xl">🎉 Done! Reviewed {done} cards.</p>
        <Button onClick={() => { setIdx(0); setDone(0); refetch() }}>Play again</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between text-sm text-muted-foreground">
        <span>{idx + 1} / {questions.length}</span>
        <span>SRS updates after each answer</span>
      </div>

      <Card>
        <CardContent className="pt-6 pb-4 text-center">
          <p className="text-4xl font-bold mb-1">{q?.front}</p>
          <p className="text-sm text-muted-foreground">Choose the correct meaning</p>
        </CardContent>
      </Card>

      <div className="grid grid-cols-2 gap-3">
        {q?.options.map(opt => {
          const isCorrect = result?.correctAnswer === opt
          const isSelected = result && opt !== result.correctAnswer
          return (
            <button key={opt}
              disabled={!!result}
              onClick={() => handleSelect(opt)}
              className={cn(
                'rounded-lg border-2 p-3 text-sm text-left transition-colors',
                !result && 'hover:border-primary/60 hover:bg-accent',
                result && isCorrect && 'bg-green-100 border-green-500 text-green-800',
                result && isSelected && 'opacity-40',
                !result && 'border-muted',
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
            ? <><CheckCircle2 className="h-4 w-4" /> Correct! Next review in {result.intervalDays}d</>
            : <><XCircle className="h-4 w-4" /> Wrong. Answer: {result.correctAnswer}</>}
          <Button size="sm" variant="ghost" className="ml-auto" onClick={next}>Next →</Button>
        </div>
      )}
    </div>
  )
}

// ─── 2. Matching ───────────────────────────────────────────────────────────

function MatchingGameUI({ deckId }: { deckId: string }) {
  const { data: game, isLoading, refetch } = useQuery({
    queryKey: ['game-match', deckId],
    queryFn: () => api.get<MatchGame>(`${BASE}/matching`, { params: { deck_id: deckId, count: 6 } }).then(r => r.data),
    staleTime: 0,
  })
  const check = useMutation({
    mutationFn: (pairs: { cardId: string; matchedBack: string }[]) =>
      api.post<MatchResult>(`${BASE}/matching/check`, { pairs }).then(r => r.data),
  })

  const [selected, setSelected] = useState<string | null>(null)   // cardId of selected term
  const [pairs, setPairs]        = useState<Record<string, string>>({}) // cardId → matchedBack
  const [result, setResult]      = useState<MatchResult | null>(null)

  if (isLoading) return <div className="text-center py-12 text-muted-foreground">Loading…</div>
  if (!game) return null

  const handleTerm = (cardId: string) => {
    if (result || pairs[cardId]) return
    setSelected(s => s === cardId ? null : cardId)
  }

  const handleDef = (def: string) => {
    if (result || !selected) return
    setPairs(p => ({ ...p, [selected]: def }))
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
        <div className={cn('p-3 rounded-lg text-sm font-medium flex items-center gap-2',
          result.correct === result.total ? 'bg-green-50 text-green-700' : 'bg-yellow-50 text-yellow-700')}>
          <CheckCircle2 className="h-4 w-4" />
          {result.correct}/{result.total} correct
          {result.wrongPairs.map(w => (
            <span key={w.cardId} className="ml-2 text-red-600">
              {w.front} → {w.correctAnswer}
            </span>
          ))}
          <Button size="sm" variant="ghost" className="ml-auto"
            onClick={() => { setPairs({}); setResult(null); refetch() }}>
            <Shuffle className="h-3 w-3 mr-1" /> New
          </Button>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Terms</p>
          {game.terms.map(t => (
            <button key={t.cardId}
              onClick={() => handleTerm(t.cardId)}
              className={cn(
                'w-full rounded-lg border-2 px-3 py-2 text-sm text-left transition-colors',
                pairs[t.cardId] ? 'border-green-400 bg-green-50 text-green-800' :
                selected === t.cardId ? 'border-primary bg-primary/5' :
                'border-muted hover:border-primary/40',
              )}
            >
              {t.front}
              {pairs[t.cardId] && <span className="text-xs block text-green-600 truncate">{pairs[t.cardId]}</span>}
            </button>
          ))}
        </div>

        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Definitions</p>
          {game.definitions.map(def => (
            <button key={def}
              onClick={() => handleDef(def)}
              disabled={matchedDefs.has(def)}
              className={cn(
                'w-full rounded-lg border-2 px-3 py-2 text-sm text-left transition-colors',
                matchedDefs.has(def) ? 'border-muted bg-muted text-muted-foreground opacity-50' :
                selected ? 'border-primary/40 hover:border-primary hover:bg-accent' :
                'border-muted',
              )}
            >
              {def}
            </button>
          ))}
        </div>
      </div>

      {!result && (
        <Button className="w-full" disabled={!allPaired || check.isPending} onClick={handleCheck}>
          Check Answers
        </Button>
      )}
    </div>
  )
}

// ─── 3. Typing Recall ──────────────────────────────────────────────────────

function TypingGame({ deckId }: { deckId: string }) {
  const { data: questions, isLoading, refetch } = useQuery({
    queryKey: ['game-typing', deckId],
    queryFn: () => api.get<TypingQ[]>(`${BASE}/typing`, { params: { deck_id: deckId, count: 10 } }).then(r => r.data),
    staleTime: 0,
  })
  const check = useMutation({
    mutationFn: ({ cardId, typed }: { cardId: string; typed: string }) =>
      api.post<TypingResult>(`${BASE}/typing/check`, { cardId, typed }).then(r => r.data),
  })

  const [idx, setIdx]       = useState(0)
  const [typed, setTyped]   = useState('')
  const [result, setResult] = useState<TypingResult | null>(null)
  const [done, setDone]     = useState(0)

  const q = questions?.[idx]

  const handleSubmit = async () => {
    if (!q || result) return
    const r = await check.mutateAsync({ cardId: q.cardId, typed })
    setResult(r)
  }

  const next = () => {
    setResult(null); setTyped(''); setDone(d => d + 1); setIdx(i => i + 1)
  }

  if (isLoading) return <div className="text-center py-12 text-muted-foreground">Loading…</div>
  if (!questions?.length) return <div className="text-center py-12 text-muted-foreground">Not enough cards</div>

  if (idx >= questions.length) {
    return (
      <div className="text-center py-12 space-y-4">
        <p className="text-2xl">✅ Done! Reviewed {done} cards.</p>
        <Button onClick={() => { setIdx(0); setDone(0); setTyped(''); refetch() }}>Play again</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between text-sm text-muted-foreground">
        <span>{idx + 1} / {questions.length}</span>
        <span className="text-xs">Typos of 1 character accepted</span>
      </div>

      <Card>
        <CardContent className="pt-6 pb-4 text-center">
          <p className="text-sm text-muted-foreground mb-1">What word means…</p>
          <p className="text-2xl font-semibold">{q?.back}</p>
        </CardContent>
      </Card>

      <div className="flex gap-2">
        <Input
          autoFocus
          placeholder="Type the English word…"
          value={typed}
          onChange={e => setTyped(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !result && handleSubmit()}
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
            ? <><CheckCircle2 className="h-4 w-4" /> Correct! (edit distance: {result.editDistance})</>
            : <><XCircle className="h-4 w-4" /> Answer: <strong>{result.correctAnswer}</strong> (distance: {result.editDistance})</>}
          <Button size="sm" variant="ghost" className="ml-auto" onClick={next}>Next →</Button>
        </div>
      )}
    </div>
  )
}
