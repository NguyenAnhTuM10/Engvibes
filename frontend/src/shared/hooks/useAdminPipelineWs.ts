import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/features/auth/store'

export interface PipelineNotification {
  type:
    | 'PIPELINE_STARTED'
    | 'PIPELINE_EXTRACTING_AUDIO'
    | 'PIPELINE_TRANSCRIBING'
    | 'PIPELINE_SAVING_SUBTITLES'
    | 'PIPELINE_ENRICHING'
    | 'PIPELINE_SUMMARIZING'
    | 'VIDEO_PUBLISHED'
    | 'VIDEO_FAILED'
    | 'ACHIEVEMENT_UNLOCKED'
    | 'STREAK_UPDATED'
  title: string
  message: string
  data: Record<string, string>
  timestamp: string
}

export function useAdminPipelineWs(onMessage: (msg: PipelineNotification) => void) {
  const token = useAuthStore((s) => s.token)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  useEffect(() => {
    if (!token) return

    const isDev = import.meta.env.DEV
    const sockjsUrl = isDev
      ? `${window.location.origin}/ws`
      : `${import.meta.env.VITE_API_URL ?? ''}/ws`

    console.debug('[WS] Connecting to', sockjsUrl)

    const client = new Client({
      webSocketFactory: () => new (SockJS as unknown as new (url: string) => WebSocket)(sockjsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.debug('[WS] Connected — subscribed to /topic/admin/pipeline')
        client.subscribe('/topic/admin/pipeline', (frame) => {
          try {
            const msg = JSON.parse(frame.body) as PipelineNotification
            onMessageRef.current(msg)
          } catch (e) {
            console.error('[WS] Failed to parse frame:', e)
          }
        })
      },
      onDisconnect: () => console.debug('[WS] Disconnected'),
      onStompError: (frame) => console.error('[WS] STOMP error:', frame.headers['message'], frame.body),
      onWebSocketError: (e) => console.error('[WS] WebSocket error:', e),
    })

    client.activate()
    return () => { void client.deactivate() }
  }, [token])
}
