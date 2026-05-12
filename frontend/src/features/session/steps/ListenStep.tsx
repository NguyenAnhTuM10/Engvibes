import { useState } from 'react'
import { Button } from '@/components/ui/button'
import VideoPlayer from '@/features/session/components/VideoPlayer'
import SubtitleOverlay from '@/features/session/components/SubtitleOverlay'
import VocabPopup from '@/features/session/components/VocabPopup'
import { useListenSubtitles } from '@/features/session/api'

interface Props {
  sessionId: string
  videoUrl: string
  onComplete: () => void
}

interface SelectedWord {
  word: string
  segmentId: string
}

export default function ListenStep({ sessionId, videoUrl, onComplete }: Props) {
  const { data: segments, isLoading } = useListenSubtitles(sessionId)
  const [currentSec, setCurrentSec] = useState(0)
  const [selected, setSelected] = useState<SelectedWord | null>(null)

  return (
    <div className="flex flex-col gap-4 max-w-4xl mx-auto">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold">Watch and listen carefully</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Click any word to see its definition and add it to your flashcards
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={onComplete}>
          Skip step
        </Button>
      </div>

      <VideoPlayer url={videoUrl} onProgress={setCurrentSec} />

      <div className="border rounded-lg p-4 max-h-72 overflow-y-auto">
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-5 bg-muted rounded animate-pulse w-full" />
            ))}
          </div>
        ) : segments && segments.length > 0 ? (
          <SubtitleOverlay
            segments={segments}
            currentSec={currentSec}
            onWordClick={(word, segmentId) => setSelected({ word, segmentId })}
          />
        ) : (
          <p className="text-sm text-muted-foreground text-center py-6">
            No subtitles available for this video
          </p>
        )}
      </div>

      <div className="flex justify-end">
        <Button onClick={onComplete}>I'm done listening →</Button>
      </div>

      {selected && (
        <VocabPopup
          sessionId={sessionId}
          word={selected.word}
          segmentId={selected.segmentId}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  )
}
