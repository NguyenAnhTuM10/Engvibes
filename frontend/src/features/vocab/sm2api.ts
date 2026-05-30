import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type { Sm2Deck, Sm2Card, QueueItem, ReviewResult, ImportSummary } from './types'

const BASE = '/api/sm2'

export function useVocabDecks() {
  return useQuery({
    queryKey: ['vocab-decks'],
    queryFn: () => api.get<Sm2Deck[]>(`${BASE}/decks`).then(r => r.data),
  })
}

export function useCreateVocabDeck() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: { name: string; description?: string }) =>
      api.post<Sm2Deck>(`${BASE}/decks`, data).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vocab-decks'] }),
  })
}

export function useVocabCards(deckId: string | undefined) {
  return useQuery({
    queryKey: ['vocab-cards', deckId],
    queryFn: () => api.get<Sm2Card[]>(`${BASE}/decks/${deckId}/cards`).then(r => r.data),
    enabled: !!deckId,
  })
}

export function useReviewQueue(deckId: string | undefined, limit = 50) {
  return useQuery({
    queryKey: ['vocab-queue', deckId],
    queryFn: () =>
      api.get<QueueItem[]>(`${BASE}/review/queue`, { params: { deck_id: deckId, limit } })
         .then(r => r.data),
    enabled: !!deckId,
    staleTime: 0,
  })
}

export function useSubmitReview() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, quality }: { cardId: string; quality: number }) =>
      api.post<ReviewResult>(`${BASE}/review/${cardId}`, { quality }).then(r => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vocab-queue'] })
      qc.invalidateQueries({ queryKey: ['vocab-cards'] })
    },
  })
}

export function useImportText(deckId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: { content: string; termSep: string; cardSep: string }) =>
      api.post<ImportSummary>(`${BASE}/decks/${deckId}/import/text`, data).then(r => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vocab-cards', deckId] })
      qc.invalidateQueries({ queryKey: ['vocab-queue', deckId] })
    },
  })
}

export function useImportCsv(deckId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return api.post<ImportSummary>(`${BASE}/decks/${deckId}/import/csv`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }).then(r => r.data)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vocab-cards', deckId] })
      qc.invalidateQueries({ queryKey: ['vocab-queue', deckId] })
    },
  })
}

export function useImportJson(deckId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return api.post<ImportSummary>(`${BASE}/decks/${deckId}/import/json`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }).then(r => r.data)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vocab-cards', deckId] })
      qc.invalidateQueries({ queryKey: ['vocab-queue', deckId] })
    },
  })
}
