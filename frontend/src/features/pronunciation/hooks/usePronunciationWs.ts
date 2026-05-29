import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/features/auth/store'
import type { AttemptResult, ProcessingStatus, PronunciationProgress } from '../types'

interface UsePronunciationWsResult {
  status: ProcessingStatus
  progress: number          // 0–100
  message: string           // mô tả bước hiện tại hoặc transcript
  result: AttemptResult | null
  resetResult: () => void
}

/**
 * Subscribe vào /topic/pronunciation/{sessionId}.
 * Tự reconnect khi mất kết nối.
 * Trả về trạng thái realtime để UI render.
 */
export function usePronunciationWs(sessionId: string | null): UsePronunciationWsResult {
  const token  = useAuthStore((s) => s.token)

  const [status,   setStatus]   = useState<ProcessingStatus>('idle')
  const [progress, setProgress] = useState(0)
  const [message,  setMessage]  = useState('')
  const [result,   setResult]   = useState<AttemptResult | null>(null)

  const stompRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!sessionId || !token) return

    const wsUrl = import.meta.env.DEV
      ? `${window.location.origin}/ws`
      : `${import.meta.env.VITE_API_URL ?? ''}/ws`

    const client = new Client({
      webSocketFactory: () =>
        new (SockJS as unknown as new (url: string) => WebSocket)(wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/pronunciation/${sessionId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body) as PronunciationProgress
            setProgress(payload.progress)
            setMessage(payload.message)

            switch (payload.type) {
              case 'PROCESSING':
                setStatus('processing')
                break
              case 'TRANSCRIBED':
                setStatus('transcribed')
                break
              case 'COMPLETED':
                setStatus('completed')
                setResult(payload.result)
                break
              case 'FAILED':
                setStatus('error')
                break
            }
          } catch (e) {
            console.error('[PronunciationWS] parse error:', e)
          }
        })
      },
      onStompError: (f) => console.error('[PronunciationWS] STOMP error:', f.headers['message']),
      onWebSocketError: (e) => console.error('[PronunciationWS] WS error:', e),
    })

    client.activate()
    stompRef.current = client

    return () => { void client.deactivate() }
  }, [sessionId, token])

  const resetResult = () => {
    setStatus('idle')
    setProgress(0)
    setMessage('')
    setResult(null)
  }

  return { status, progress, message, result, resetResult }
}
