import { Mic, Square, RotateCcw, Send, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useAudioRecorder } from '@/shared/hooks/useAudioRecorder'

interface Props {
  maxDurationSec?: number
  isSubmitting?: boolean
  onSubmit: (blob: Blob) => void
  disabled?: boolean
}

export default function AudioRecorder({
  maxDurationSec = 90,
  isSubmitting = false,
  onSubmit,
  disabled = false,
}: Props) {
  const { state, audioBlob, duration, permissionDenied, start, stop, reset } =
    useAudioRecorder(maxDurationSec)

  const fmt = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`

  if (permissionDenied) {
    return (
      <div className="flex items-center gap-2 text-sm text-destructive border border-destructive/30 rounded-lg px-4 py-3 bg-destructive/5">
        <AlertCircle className="h-4 w-4 shrink-0" />
        Microphone permission denied. Please allow microphone access in your browser settings.
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center gap-4">
      {/* Mic button */}
      <div className="relative">
        <button
          onClick={state === 'recording' ? stop : start}
          disabled={disabled || isSubmitting || state === 'preview'}
          aria-label={state === 'recording' ? 'Stop recording' : 'Start recording'}
          className={cn(
            'h-16 w-16 rounded-full flex items-center justify-center transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            state === 'recording'
              ? 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/30'
              : 'bg-muted hover:bg-muted/80 text-muted-foreground',
            (disabled || isSubmitting || state === 'preview') && 'opacity-50 cursor-not-allowed',
          )}
        >
          {state === 'recording' ? (
            <Square className="h-6 w-6" />
          ) : (
            <Mic className="h-6 w-6" />
          )}
        </button>
        {state === 'recording' && (
          <span className="absolute inset-0 rounded-full animate-ping bg-red-500/30 pointer-events-none" />
        )}
      </div>

      {/* Timer */}
      {state === 'recording' && (
        <div className="flex items-center gap-1.5 text-sm font-mono text-red-500">
          <span className="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
          {fmt(duration)} / {fmt(maxDurationSec)}
        </div>
      )}

      {state === 'idle' && (
        <p className="text-xs text-muted-foreground">Click mic to record (max {fmt(maxDurationSec)})</p>
      )}

      {/* Preview controls */}
      {state === 'preview' && audioBlob && (
        <div className="w-full space-y-3">
          <audio src={URL.createObjectURL(audioBlob)} controls className="w-full h-10" />
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              className="flex-1 gap-1.5"
              onClick={reset}
              disabled={isSubmitting}
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Re-record
            </Button>
            <Button
              size="sm"
              className="flex-1 gap-1.5"
              onClick={() => onSubmit(audioBlob)}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
              ) : (
                <Send className="h-3.5 w-3.5" />
              )}
              {isSubmitting ? 'Analyzing...' : 'Submit'}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
