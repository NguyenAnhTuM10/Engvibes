import { useMutation, useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type {
  ConversationScenario,
  ConversationSessionResponse,
  ConversationTurnResponse,
  ConversationEndResponse,
} from './types'

export const useScenarios = () =>
  useQuery({
    queryKey: ['conversation-scenarios'],
    queryFn: () =>
      api.get<never, { data: ConversationScenario[] }>('/api/conversation/scenarios').then((r) => r.data),
    staleTime: Infinity,
  })

export const useStartConversation = () =>
  useMutation({
    mutationFn: (scenarioId: string) =>
      api
        .post<never, { data: ConversationSessionResponse }>('/api/conversation/start', { scenarioId })
        .then((r) => r.data),
    onError: (err: Error) => toast.error(err.message),
  })

export const useSubmitTurn = (sessionId: string) =>
  useMutation({
    mutationFn: (audioBlob: Blob) => {
      const form = new FormData()
      form.append('audio', audioBlob, 'recording.webm')
      return api
        .post<never, { data: ConversationTurnResponse }>(
          `/api/conversation/${sessionId}/turn`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60_000 },
        )
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })

export const useEndConversation = (sessionId: string) =>
  useMutation({
    mutationFn: () =>
      api
        .post<never, { data: ConversationEndResponse }>(`/api/conversation/${sessionId}/end`)
        .then((r) => r.data),
    onError: (err: Error) => toast.error(err.message),
  })

// ── Realtime conversation ─────────────────────────────────────────────────────

export interface RealtimeTokenResponse {
  token: string
  model: string
  scenarioId: string
  scenarioDisplayName: string
  aiRole: string
  userGoal: string
  openingLine: string
}

export interface ConversationReviewRequest {
  scenarioId: string
  turns: { role: 'user' | 'assistant'; text: string }[]
  durationSec: number
}

export interface ConversationReviewResponse {
  fluency: number
  grammar: number
  vocabulary: number
  overall: number
  strengths: string
  improvements: string
  encouragement: string
  grammarNotes: { error: string; correction: string }[]
}

export const useGetRealtimeToken = () =>
  useMutation({
    mutationFn: (scenarioId: string) =>
      api
        .post<never, { data: RealtimeTokenResponse }>('/api/conversation/realtime-token', { scenarioId })
        .then((r) => r.data),
    onError: (err: Error) => toast.error('Failed to start conversation: ' + err.message),
  })

export const useConversationReview = () =>
  useMutation({
    mutationFn: (req: ConversationReviewRequest) =>
      api
        .post<never, { data: ConversationReviewResponse }>('/api/conversation/realtime-review', req)
        .then((r) => r.data),
    onError: (err: Error) => toast.error('Failed to get feedback: ' + err.message),
  })
