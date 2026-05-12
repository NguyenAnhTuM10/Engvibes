import { useState, useRef, useCallback } from 'react'

function getSupportedMimeType(): string {
  const types = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/ogg;codecs=opus',
    'audio/ogg',
    'audio/mp4',
  ]
  return types.find((t) => MediaRecorder.isTypeSupported(t)) ?? ''
}

export type RecorderState = 'idle' | 'recording' | 'preview'

export function useAudioRecorder(maxDurationSec = 90) {
  const [state, setState] = useState<RecorderState>('idle')
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null)
  const [duration, setDuration] = useState(0)
  const [permissionDenied, setPermissionDenied] = useState(false)

  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const stop = useCallback(() => {
    recorderRef.current?.stop()
    if (timerRef.current) clearInterval(timerRef.current)
    streamRef.current?.getTracks().forEach((t) => t.stop())
    setState('preview')
  }, [])

  const start = useCallback(async () => {
    setPermissionDenied(false)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      const mimeType = getSupportedMimeType()
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined)
      chunksRef.current = []

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: mimeType || 'audio/webm' })
        setAudioBlob(blob)
      }

      recorderRef.current = recorder
      recorder.start(100)
      setDuration(0)
      setState('recording')

      timerRef.current = setInterval(() => {
        setDuration((d) => {
          if (d + 1 >= maxDurationSec) {
            stop()
            return d + 1
          }
          return d + 1
        })
      }, 1000)
    } catch {
      setPermissionDenied(true)
    }
  }, [maxDurationSec, stop])

  const reset = useCallback(() => {
    setAudioBlob(null)
    setDuration(0)
    setState('idle')
  }, [])

  return { state, audioBlob, duration, permissionDenied, start, stop, reset }
}
