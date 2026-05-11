import { useQuery } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type { ApiResponse, Page, Vocab } from '@/shared/types/api'

export const useSearchVocab = (q: string, page = 0) =>
  useQuery({
    queryKey: ['vocab', 'search', q, page],
    queryFn: () =>
      api
        .get<never, { data: Page<Vocab> }>('/api/vocab/search', { params: { q, page, size: 20 } })
        .then((r) => r.data),
    enabled: q.trim().length >= 1,
    staleTime: 5 * 60_000,
  })

export const useVocab = (id: string) =>
  useQuery({
    queryKey: ['vocab', id],
    queryFn: () =>
      api.get<never, ApiResponse<Vocab>>(`/api/vocab/${id}`).then((r) => r.data),
    enabled: !!id,
    staleTime: 10 * 60_000,
  })

export const useVocabByWord = (word: string) =>
  useQuery({
    queryKey: ['vocab', 'word', word],
    queryFn: () =>
      api.get<never, ApiResponse<Vocab>>(`/api/vocab/by-word/${word}`).then((r) => r.data),
    enabled: !!word,
    staleTime: 10 * 60_000,
  })
