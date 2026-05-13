import { useEffect, useRef, useState } from 'react'
import { Volume2, Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ConversationMessage } from '../types'

interface Props {
  messages: ConversationMessage[]
  isProcessing: boolean
}

export default function ConversationChat({ messages, isProcessing }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null)
  const [playingTurn, setPlayingTurn] = useState<number | null>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isProcessing])

  const playAudio = (url: string, turnNumber: number) => {
    setPlayingTurn(turnNumber)
    const audio = new Audio(url)
    audio.onended = () => setPlayingTurn(null)
    audio.onerror = () => setPlayingTurn(null)
    audio.play().catch(() => setPlayingTurn(null))
  }

  return (
    <div className="flex flex-col gap-4 py-4">
      {messages.map((msg) => (
        <div
          key={`${msg.role}-${msg.turnNumber}`}
          className={cn('flex', msg.role === 'user' ? 'justify-end' : 'justify-start')}
        >
          <div
            className={cn(
              'max-w-[80%] rounded-2xl px-4 py-3 text-sm',
              msg.role === 'ai'
                ? 'bg-muted text-foreground rounded-tl-none'
                : 'bg-primary text-primary-foreground rounded-tr-none',
            )}
          >
            <p className="leading-relaxed">{msg.text}</p>
            {msg.role === 'ai' && msg.audioUrl && (
              <button
                onClick={() => playAudio(msg.audioUrl!, msg.turnNumber)}
                className="mt-2 flex items-center gap-1 text-xs opacity-70 hover:opacity-100 transition-opacity"
                aria-label="Play AI audio"
              >
                {playingTurn === msg.turnNumber ? (
                  <Loader2 className="h-3 w-3 animate-spin" />
                ) : (
                  <Volume2 className="h-3 w-3" />
                )}
                <span>{playingTurn === msg.turnNumber ? 'Playing...' : 'Play'}</span>
              </button>
            )}
          </div>
        </div>
      ))}

      {isProcessing && (
        <div className="flex justify-start">
          <div className="max-w-[80%] rounded-2xl rounded-tl-none bg-muted px-4 py-3">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>AI is thinking...</span>
            </div>
          </div>
        </div>
      )}

      <div ref={bottomRef} />
    </div>
  )
}
