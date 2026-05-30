import { useMutation, useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type { ConversationScenario } from './types'

export const useScenarios = () =>
  useQuery({
    queryKey: ['conversation-scenarios'],
    queryFn: () =>
      api.get<never, { data: ConversationScenario[] }>('/api/conversation/scenarios').then((r) => r.data),
    staleTime: Infinity,
  })

// ── Review (server-authoritative transcript) ───────────────────────────────────

export interface ConversationReviewRequest {
  // T2.2 — sessionId là nguồn transcript chính thức; server tự load, không nhận turns.
  sessionId: string
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

export const useConversationReview = () =>
  useMutation({
    mutationFn: (req: ConversationReviewRequest) =>
      api
        .post<never, { data: ConversationReviewResponse }>('/api/conversation/realtime-review', req)
        .then((r) => r.data),
    onError: (err: Error) => toast.error('Failed to get feedback: ' + err.message),
  })

// ── History (T2 — session realtime đã persist) ──────────────────────────────────

export interface ConversationSessionRow {
  id: string
  scenarioId: string
  status: string
  totalTurns: number
  xpEarned: number
  summary: string | null      // JSON string của ConversationReviewResponse
  createdAt: string
  endedAt: string | null
}

export const useConversationHistory = () =>
  useQuery({
    queryKey: ['conversation-history'],
    queryFn: () =>
      api
        .get<never, { data: ConversationSessionRow[] }>('/api/conversation/history')
        .then((r) => r.data),
    staleTime: 30_000,
  })
