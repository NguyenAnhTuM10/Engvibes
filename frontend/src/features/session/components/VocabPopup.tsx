import { X, BookPlus, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import CefrBadge from '@/components/ui/CefrBadge'
import { useVocabInfo, useAddVocabFromListen } from '@/features/session/api'

interface Props {
  sessionId: string
  word: string
  segmentId?: string
  onClose: () => void
}

export default function VocabPopup({ sessionId, word, segmentId, onClose }: Props) {
  const { data: vocab, isLoading, isError } = useVocabInfo(sessionId, word)
  const addVocab = useAddVocabFromListen(sessionId)

  const handleAdd = () => {
    if (!vocab) return
    addVocab.mutate({ vocabId: vocab.id, segmentId })
  }

  return (
    <div className="fixed inset-0 z-40 flex items-end sm:items-center justify-center p-4" onClick={onClose}>
      <div className="absolute inset-0 bg-black/30" />
      <div
        className="relative z-50 bg-background border rounded-xl shadow-xl w-full max-w-sm p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-bold text-lg">{word}</span>
            {vocab?.cefrLevel && <CefrBadge level={vocab.cefrLevel} />}
          </div>
          <button onClick={onClose} aria-label="Close" className="text-muted-foreground hover:text-foreground ml-2 shrink-0">
            <X className="h-4 w-4" />
          </button>
        </div>

        {isLoading && (
          <div className="flex justify-center py-6">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <p className="text-sm text-muted-foreground py-4 text-center">
            Word not found in vocabulary
          </p>
        )}

        {vocab && !isLoading && (
          <>
            <div className="space-y-1 mb-4">
              {vocab.ipa && (
                <p className="text-sm text-muted-foreground font-mono">{vocab.ipa}</p>
              )}
              {vocab.partOfSpeech && (
                <p className="text-xs italic text-muted-foreground">{vocab.partOfSpeech}</p>
              )}
              <p className="text-sm leading-relaxed">{vocab.definition}</p>
            </div>

            <Button
              size="sm"
              className="w-full gap-2"
              onClick={handleAdd}
              disabled={addVocab.isPending || addVocab.isSuccess}
            >
              {addVocab.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <BookPlus className="h-4 w-4" />
              )}
              {addVocab.isSuccess ? 'Added!' : 'Add to flashcards'}
            </Button>
          </>
        )}
      </div>
    </div>
  )
}
