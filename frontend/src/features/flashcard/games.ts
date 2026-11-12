import { useMutation, useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type { ApiResponse, CardState } from '@/shared/types/api'

// Vocabulary games on a flashcard deck. Answers are graded server-side and fed back
// into the FSRS schedule (correct → Good, wrong → Again), except matching.

const BASE = '/api/games'

export interface MCQuestion { cardId: string; front: string; options: string[] }
export interface MCResult {
  correct: boolean
  correctAnswer: string
  cardId: string
  state: CardState
  nextReview: string
}

export interface MatchTerm { cardId: string; front: string }
export interface MatchGame { terms: MatchTerm[]; definitions: string[] }
export interface MatchResult {
  correct: number
  total: number
  wrongPairs: { cardId: string; front: string; yourAnswer: string; correctAnswer: string }[]
}

export interface TypingQuestion { cardId: string; back: string }
export interface TypingResult {
  correct: boolean
  correctAnswer: string
  editDistance: number
  cardId: string
  state: CardState
  nextReview: string
}

const get = <T,>(path: string, params: Record<string, unknown>) =>
  api.get<never, ApiResponse<T>>(`${BASE}${path}`, { params }).then((r) => r.data)

const post = <T,>(path: string, body: unknown) =>
  api.post<never, ApiResponse<T>>(`${BASE}${path}`, body).then((r) => r.data)

export const useMultipleChoice = (deckId: string) =>
  useQuery({
    queryKey: ['games', 'mc', deckId],
    queryFn: () => get<MCQuestion[]>('/multiple-choice', { deckId, count: 10 }),
    staleTime: 0,
    retry: false,
  })

export const useAnswerMultipleChoice = () =>
  useMutation({
    mutationFn: (body: { cardId: string; selected: string }) =>
      post<MCResult>('/multiple-choice/answer', body),
    onError: (err: Error) => toast.error(err.message),
  })

export const useMatching = (deckId: string) =>
  useQuery({
    queryKey: ['games', 'matching', deckId],
    queryFn: () => get<MatchGame>('/matching', { deckId, count: 6 }),
    staleTime: 0,
    retry: false,
  })

export const useCheckMatching = () =>
  useMutation({
    mutationFn: (pairs: { cardId: string; matchedBack: string }[]) =>
      post<MatchResult>('/matching/check', { pairs }),
    onError: (err: Error) => toast.error(err.message),
  })

export const useTyping = (deckId: string) =>
  useQuery({
    queryKey: ['games', 'typing', deckId],
    queryFn: () => get<TypingQuestion[]>('/typing', { deckId, count: 10 }),
    staleTime: 0,
    retry: false,
  })

export const useCheckTyping = () =>
  useMutation({
    mutationFn: (body: { cardId: string; typed: string }) =>
      post<TypingResult>('/typing/check', body),
    onError: (err: Error) => toast.error(err.message),
  })
