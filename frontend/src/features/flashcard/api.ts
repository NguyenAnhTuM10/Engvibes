import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type { ApiResponse, Card, CardSource, Deck, Page } from '@/shared/types/api'

export const useDecks = () =>
  useQuery({
    queryKey: ['decks'],
    queryFn: () =>
      api.get<never, ApiResponse<Deck[]>>('/api/decks').then((r) => r.data),
    staleTime: 2 * 60_000,
  })

export const useCreateDeck = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: { name: string; color?: string }) =>
      api.post<never, ApiResponse<Deck>>('/api/decks', data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      toast.success('Deck created')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useUpdateDeck = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...data }: { id: string; name?: string; color?: string }) =>
      api.patch<never, ApiResponse<Deck>>(`/api/decks/${id}`, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      toast.success('Deck updated')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useDeleteDeck = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/decks/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      toast.success('Deck deleted')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useDeckCards = (deckId: string, page = 0) =>
  useQuery({
    queryKey: ['decks', deckId, 'cards', page],
    queryFn: () =>
      api
        .get<never, ApiResponse<Page<Card>>>(`/api/decks/${deckId}/cards`, {
          params: { page, size: 20 },
        })
        .then((r) => r.data),
    enabled: !!deckId,
  })

export const useDueCards = (deckId: string) =>
  useQuery({
    queryKey: ['decks', deckId, 'due'],
    queryFn: () =>
      api
        .get<never, ApiResponse<Card[]>>(`/api/decks/${deckId}/cards/due`, { params: { limit: 20 } })
        .then((r) => r.data),
    enabled: !!deckId,
  })

export const useAddCard = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: {
      vocabId: string
      deckId?: string
      contextSentence?: string
      sourceVideoId?: string
      sourceType?: CardSource
    }) => api.post<never, ApiResponse<Card>>('/api/cards', data).then((r) => r.data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      if (vars.deckId) qc.invalidateQueries({ queryKey: ['decks', vars.deckId, 'cards'] })
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useReviewCard = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, rating }: { id: string; rating: 1 | 2 | 3 | 4 }) =>
      api.post<never, ApiResponse<Card>>(`/api/cards/${id}/review`, { rating }).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useDeleteCard = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/cards/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      toast.success('Card removed')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}
