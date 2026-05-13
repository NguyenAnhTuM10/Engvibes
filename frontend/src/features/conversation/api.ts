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
