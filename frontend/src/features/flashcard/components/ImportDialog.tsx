import { useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, Upload } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useImportCards } from '@/features/flashcard/api'
import type { ImportSummary } from '@/shared/types/api'

/** Bulk-import custom cards into a deck from pasted text, CSV or JSON. */
export default function ImportDialog({ deckId }: { deckId: string }) {
  const [open, setOpen] = useState(false)
  const [result, setResult] = useState<ImportSummary | null>(null)
  const [textContent, setTextContent] = useState('')
  const csvRef = useRef<HTMLInputElement>(null)
  const jsonRef = useRef<HTMLInputElement>(null)

  const importCards = useImportCards(deckId)

  const showResult = (r: ImportSummary) => {
    setResult(r)
    toast.success(`Imported ${r.imported} cards${r.skipped ? `, skipped ${r.skipped}` : ''}`)
  }

  const handleText = async () => {
    if (!textContent.trim()) return
    const r = await importCards.mutateAsync({
      kind: 'text', content: textContent, termSep: '\\t', cardSep: '\\n',
    })
    setTextContent('')
    showResult(r)
  }

  const handleFile = async (file: File | null | undefined, kind: 'csv' | 'json') => {
    if (!file) return
    showResult(await importCards.mutateAsync({ kind, file }))
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { setOpen(v); if (!v) { setResult(null); setTextContent('') } }}>
      <DialogTrigger asChild>
        <Button variant="outline" className="gap-2">
          <Upload className="h-4 w-4" /> Import
        </Button>
      </DialogTrigger>

      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Import cards</DialogTitle>
        </DialogHeader>

        <Tabs defaultValue="text">
          <TabsList className="w-full">
            <TabsTrigger value="text" className="flex-1">Paste Text</TabsTrigger>
            <TabsTrigger value="csv" className="flex-1">CSV</TabsTrigger>
            <TabsTrigger value="json" className="flex-1">JSON</TabsTrigger>
          </TabsList>

          <TabsContent value="text" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              One card per line. Use <kbd className="px-1 bg-muted rounded text-xs">Tab</kbd> between
              front and back. Optional: add IPA and an example sentence after more Tabs.
            </p>
            <Textarea
              rows={8}
              placeholder={'think\tto have a thought\nspeak\tto talk\nlearn\tto acquire knowledge\tlɜːn\tShe learns quickly.'}
              value={textContent}
              onChange={(e) => setTextContent(e.target.value)}
              className="font-mono text-sm resize-none"
            />
            <Button className="w-full" disabled={!textContent.trim() || importCards.isPending} onClick={handleText}>
              Import
            </Button>
          </TabsContent>

          <TabsContent value="csv" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              Columns: <code className="bg-muted px-1 rounded">front,back,ipa,exampleSentence</code>.
              Header row optional — auto-detected.
            </p>
            <FileDrop
              label="Click to select .csv file"
              accept=".csv,text/csv"
              inputRef={csvRef}
              onFile={(f) => handleFile(f, 'csv')}
            />
          </TabsContent>

          <TabsContent value="json" className="space-y-3 mt-4">
            <p className="text-xs text-muted-foreground">
              JSON array:{' '}
              <code className="bg-muted px-1 rounded text-xs">{'[{"front":"...","back":"...","ipa":"..."}]'}</code>
            </p>
            <FileDrop
              label="Click to select .json file"
              accept=".json,application/json"
              inputRef={jsonRef}
              onFile={(f) => handleFile(f, 'json')}
            />
          </TabsContent>
        </Tabs>

        {result && (
          <div className="mt-3 rounded-lg border p-3 space-y-2">
            <div className="flex gap-4 text-sm font-medium">
              <span className="flex items-center gap-1 text-green-600">
                <CheckCircle2 className="h-4 w-4" /> {result.imported} imported
              </span>
              {result.skipped > 0 && (
                <span className="text-muted-foreground">{result.skipped} skipped (already in deck)</span>
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

function FileDrop({
  label, accept, inputRef, onFile,
}: {
  label: string
  accept: string
  inputRef: React.RefObject<HTMLInputElement | null>
  onFile: (file: File | undefined) => void
}) {
  return (
    <div
      className="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 transition-colors"
      onClick={() => inputRef.current?.click()}
    >
      <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
      <p className="text-sm text-muted-foreground">{label}</p>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={(e) => { onFile(e.target.files?.[0]); e.target.value = '' }}
      />
    </div>
  )
}
