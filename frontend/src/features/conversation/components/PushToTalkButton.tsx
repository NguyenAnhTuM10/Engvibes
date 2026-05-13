import { useRef, useCallback } from 'react'
import { Mic, Loader2, MicOff } from 'lucide-react'
import { cn } from '@/lib/utils'

type RecordingState = 'idle' | 'recording' | 'processing' | 'playing'

interface Props {
  recordingState: RecordingState
  disabled?: boolean
  onRecordingComplete: (blob: Blob) => void
  onRecordingStart?: () => void
}

export default function PushToTalkButton({
  recordingState,
  disabled,
  onRecordingComplete,
  onRecordingStart,
}: Props) {
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])

  const startRecording = useCallback(async () => {
    if (recordingState !== 'idle') return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
      chunksRef.current = []
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }
      recorder.onstop = () => {
        stream.getTracks().forEach((t) => t.stop())
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' })
        if (blob.size > 500) {
          onRecordingComplete(blob)
        }
      }
      recorder.start()
      mediaRecorderRef.current = recorder
      onRecordingStart?.()
    } catch {
      // microphone permission denied or not available
    }
  }, [recordingState, onRecordingComplete, onRecordingStart])

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current?.state === 'recording') {
      mediaRecorderRef.current.stop()
    }
  }, [])

  const isDisabled = disabled || recordingState === 'processing' || recordingState === 'playing'

  const label = {
    idle: 'Hold to speak',
    recording: 'Release to send',
    processing: 'Processing...',
    playing: 'AI is speaking...',
  }[recordingState]

  return (
    <div className="flex flex-col items-center gap-3">
      <button
        className={cn(
          'relative flex h-20 w-20 items-center justify-center rounded-full border-4 transition-all duration-150 select-none',
          recordingState === 'idle' && !isDisabled
            ? 'border-primary bg-primary/10 hover:bg-primary/20 active:scale-95 cursor-pointer'
            : '',
          recordingState === 'recording'
            ? 'border-red-500 bg-red-500 scale-110 cursor-pointer'
            : '',
          recordingState === 'processing'
            ? 'border-muted-foreground bg-muted cursor-not-allowed'
            : '',
          recordingState === 'playing'
            ? 'border-green-500 bg-green-50 cursor-not-allowed'
            : '',
        )}
        onMouseDown={startRecording}
        onMouseUp={stopRecording}
        onMouseLeave={stopRecording}
        onTouchStart={(e) => { e.preventDefault(); startRecording() }}
        onTouchEnd={(e) => { e.preventDefault(); stopRecording() }}
        disabled={isDisabled}
        aria-label={label}
      >
        {recordingState === 'recording' && (
          <span className="absolute inset-0 animate-ping rounded-full bg-red-400 opacity-30" />
        )}
        {recordingState === 'processing' ? (
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        ) : recordingState === 'playing' ? (
          <MicOff className="h-8 w-8 text-green-600" />
        ) : (
          <Mic
            className={cn(
              'h-8 w-8',
              recordingState === 'recording' ? 'text-white' : 'text-primary',
            )}
          />
        )}
      </button>
      <p className="text-sm text-muted-foreground">{label}</p>
    </div>
  )
}
