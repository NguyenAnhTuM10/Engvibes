import { useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  useVocabCards, useReviewQueue,
  useImportText, useImportCsv, useImportJson,
} from '@/features/vocab/sm2api'
import type { ImportSummary } from '@/features/vocab/types'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger,
} from '@/components/ui/dialog'
import {
  Tabs, TabsContent, TabsList, TabsTrigger,
} from '@/components/ui/tabs'
import { ArrowLeft, Upload, Play, CheckCircle2, AlertCircle, Gamepad2 } from 'lucide-react'
import { toast } from 'sonner'

export default function VocabDeckPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: cards = [], isLoading: cardsLoading } = useVocabCards(id)
  const { data: queue = [] } = useReviewQueue(id)
  const dueCount = queue.length

  return (
    <div className="max-w-2xl mx-auto py-6 px-4 space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate('/vocab')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1">
          <h1 className="text-xl font-bold">Deck</h1>
          <p className="text-xs text-muted-foreground">{cards.length} cards</p>
        </div>
        <div className="flex gap-2">
          <ImportDialog deckId={id!} />
          <Button
            variant="outline"
            onClick={() => navigate(`/vocab/${id}/games`)}
            className="gap-2"
          >
            <Gamepad2 className="h-4 w-4" /> Games
          </Button>
          <Button
            disabled={dueCount === 0}
            onClick={() => navigate(`/vocab/${id}/review`)}
            className="gap-2"
          >
            <Play className="h-4 w-4" />
            Review {dueCount > 0 && <Badge variant="secondary">{dueCount}</Badge>}
          </Button>
        </div>
      </div>

      {/* Card list */}
      {cardsLoading ? (
        <div className="text-center py-12 text-muted-foreground">Loading…</div>
      ) : cards.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <p>No cards yet.</p>
          <p className="text-sm mt-1">Use Import to add cards from text, CSV, or JSON.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {cards.map(card => (
            <Card key={card.id} className="hover:shadow-sm transition-shadow">
              <CardContent className="py-3 px-4 flex items-start gap-4">
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">{card.front}</p>
                  {card.ipa && (
                    <p className="text-xs font-mono text-muted-foreground">/{card.ipa}/</p>
                  )}
                </div>
                <div className="flex-1 min-w-0 text-right">
                  <p className="text-sm text-muted-foreground truncate">{card.back}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Import Dialog ──────────────────────────────────────────────────────────

function ImportDialog({ deckId }: { deckId: string }) {
  const [open, setOpen]       = useState(false)
  const [result, setResult]   = useState<ImportSummary | null>(null)
  const [textContent, setTextContent] = useState('')
  const csvRef  = useRef<HTMLInputElement>(null)
  const jsonRef = useRef<HTMLInputElement>(null)

  const importText = useImportText(deckId)
  const importCsv  = useImportCsv(deckId)
  const importJson = useImportJson(deckId)

  const showResult = (r: ImportSummary) => {
    setResult(r)
    toast.success(`Imported ${r.imported} cards${r.skipped ? `, skipped ${r.skipped}` : ''}`)
  }

  const handleText = async () => {
    if (!textContent.trim()) return
    const r = await importText.mutateAsync({ content: textContent, termSep: '\\t', cardSep: '\\n' })
    setTextContent('')
    showResult(r)
  }

  const handleFile = async (file: File | null | undefined, type: 'csv' | 'json') => {
    if (!file) return
    const r = type === 'csv'
      ? await importCsv.mutateAsync(file)
      : await importJson.mutateAsync(file)
    showResult(r)
  }

  const isPending = importText.isPending || importCsv.isPending || importJson.isPending

  return (
    <Dialog open={open} onOpenChange={v => { setOpen(v); if (!v) { setResult(null); setTextContent('') } }}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm" className="gap-2">
          <Upload className="h-4 w-4" /> Import
        </Button>
      </DialogTrigger>

      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Import Vocabulary</DialogTitle>
        </DialogHeader>

        <Tabs defaultValue="text">
          <TabsList className="w-full">
            <TabsTrigger value="text" className="flex-1">Paste Text</TabsTrigger>
            <TabsTrigger value="csv"  className="flex-1">CSV</TabsTrigger>
            <TabsTrigger value="json" className="flex-1">JSON</TabsTrigger>
          </TabsList>

          {/* ── Text tab ── */}
          <TabsContent value="text" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              One card per line. Use <kbd className="px-1 bg-muted rounded text-xs">Tab</kbd> between
              front and back. Optional: add IPA and example after a second Tab.
            </p>
            <Textarea
              rows={8}
              placeholder={"think\tto have a thought\nspeak\tto talk\nlearn\tto acquire knowledge\t lɜːrn\tShe learns quickly."}
              value={textContent}
              onChange={e => setTextContent(e.target.value)}
              className="font-mono text-sm resize-none"
            />
            <Button
              className="w-full"
              disabled={!textContent.trim() || isPending}
              onClick={handleText}
            >
              Import
            </Button>
          </TabsContent>

          {/* ── CSV tab ── */}
          <TabsContent value="csv" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              Columns: <code className="bg-muted px-1 rounded">front,back,ipa,exampleSentence</code>.
              Header row optional — auto-detected.
            </p>
            <div
              className="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 transition-colors"
              onClick={() => csvRef.current?.click()}
            >
              <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
              <p className="text-sm text-muted-foreground">Click to select .csv file</p>
              <input
                ref={csvRef} type="file" accept=".csv,text/csv"
                className="hidden"
                onChange={e => handleFile(e.target.files?.[0], 'csv')}
              />
            </div>
          </TabsContent>

          {/* ── JSON tab ── */}
          <TabsContent value="json" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              JSON array:{' '}
              <code className="bg-muted px-1 rounded text-xs">
                {'[{"front":"...","back":"...","ipa":"..."}]'}
              </code>
            </p>
            <div
              className="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 transition-colors"
              onClick={() => jsonRef.current?.click()}
            >
              <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
              <p className="text-sm text-muted-foreground">Click to select .json file</p>
              <input
                ref={jsonRef} type="file" accept=".json,application/json"
                className="hidden"
                onChange={e => handleFile(e.target.files?.[0], 'json')}
              />
            </div>
          </TabsContent>
        </Tabs>

        {/* Import result summary */}
        {result && (
          <div className="mt-3 rounded-lg border p-3 space-y-2">
            <div className="flex gap-4 text-sm font-medium">
              <span className="flex items-center gap-1 text-green-600">
                <CheckCircle2 className="h-4 w-4" /> {result.imported} imported
              </span>
              {result.skipped > 0 && (
                <span className="text-muted-foreground">{result.skipped} skipped (dupe)</span>
              )}
            </div>
            {result.errors.length > 0 && (
              <div className="space-y-1">
                {result.errors.map((e, i) => (
                  <p key={i} className="text-xs flex items-start gap-1 text-destructive">
                    <AlertCircle className="h-3 w-3 mt-0.5 shrink-0" />
                    Line {e.line}: {e.reason}
                  </p>
                ))}
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
