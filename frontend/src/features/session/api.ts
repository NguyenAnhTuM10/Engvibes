import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type {
  ApiResponse,
  Card,
  Page,
  PhraseAttemptResult,
  RetellFeedback,
  RetellScaffoldResponse,
  Session,
  SpeakFeedback,
  SpeakingQuestionResponse,
  SessionHistoryItem,
  ShadowAttemptResult,
  SubtitleSegment,
  Vocab,
  WarmupWordResponse,
} from '@/shared/types/api'

// ── Session lifecycle ─────────────────────────────────────────────────────────

export const useCreateOrGetSession = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (videoId: string) =>
      api
        .post<never, ApiResponse<Session>>('/api/sessions', { videoId })
        .then((r) => r.data),
    onSuccess: (session) => {
      qc.setQueryData(['sessions', session.id], session)
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useSession = (id: string) =>
  useQuery({
    queryKey: ['sessions', id],
    queryFn: () =>
      api.get<never, ApiResponse<Session>>(`/api/sessions/${id}`).then((r) => r.data),
    enabled: !!id,
  })

export const useAdvanceStep = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      sessionId,
      completed = true,
      score,
    }: {
      sessionId: string
      completed?: boolean
      score?: number
    }) =>
      api
        .patch<never, ApiResponse<Session>>(`/api/sessions/${sessionId}/step`, {
          completed,
          score,
        })
        .then((r) => r.data),
    onSuccess: (session) => {
      qc.setQueryData(['sessions', session.id], session)
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useFinishSession = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (sessionId: string) =>
      api
        .post<never, ApiResponse<Session>>(`/api/sessions/${sessionId}/finish`, {})
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sessions'] })
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useSessionHistory = (page = 0) =>
  useQuery({
    queryKey: ['sessions', 'history', page],
    queryFn: () =>
      api
        .get<never, ApiResponse<Page<SessionHistoryItem>>>('/api/sessions/history', {
          params: { page, size: 10 },
        })
        .then((r) => r.data),
    staleTime: 2 * 60_000,
  })

// ── Warmup ────────────────────────────────────────────────────────────────────

export const useWarmupWords = (sessionId: string) =>
  useQuery({
    queryKey: ['sessions', sessionId, 'warmup'],
    queryFn: () =>
      api
        .get<never, ApiResponse<WarmupWordResponse[]>>(`/api/sessions/${sessionId}/warmup`)
        .then((r) => r.data),
    enabled: !!sessionId,
    staleTime: 60 * 60_000,
  })

export const useMarkWarmup = (sessionId: string) =>
  useMutation({
    mutationFn: ({ vocabId, status }: { vocabId: string; status: 'known' | 'new' }) =>
      api.post(`/api/sessions/${sessionId}/warmup/mark`, { vocabId, status }),
    onError: (err: Error) => toast.error(err.message),
  })

// ── Listen ────────────────────────────────────────────────────────────────────

export const useListenSubtitles = (sessionId: string) =>
  useQuery({
    queryKey: ['sessions', sessionId, 'subtitles'],
    queryFn: () =>
      api
        .get<never, ApiResponse<SubtitleSegment[]>>(
          `/api/sessions/${sessionId}/listen/subtitles`,
        )
        .then((r) => r.data),
    enabled: !!sessionId,
    staleTime: 60 * 60_000,
  })

export const useVocabInfo = (sessionId: string, word: string | null) =>
  useQuery({
    queryKey: ['sessions', sessionId, 'vocab-info', word],
    queryFn: () =>
      api
        .get<never, ApiResponse<Vocab>>(`/api/sessions/${sessionId}/listen/vocab-info`, {
          params: { word },
        })
        .then((r) => r.data),
    enabled: !!sessionId && !!word,
    staleTime: 60 * 60_000,
    retry: false,
  })

export const useAddVocabFromListen = (sessionId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ vocabId, segmentId }: { vocabId: string; segmentId?: string }) =>
      api.post(`/api/sessions/${sessionId}/listen/add-vocab`, { vocabId, segmentId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      toast.success('Added to flashcards')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

// ── Phrase Practice ───────────────────────────────────────────────────────────

export const useSubmitPhraseAttempt = (sessionId: string) =>
  useMutation({
    mutationFn: ({ segmentIdx, audio }: { segmentIdx: number; audio: Blob }) => {
      const form = new FormData()
      form.append('audio', audio, 'recording.webm')
      return api
        .post<never, ApiResponse<PhraseAttemptResult>>(
          `/api/sessions/${sessionId}/phrase/${segmentIdx}/attempt`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })

// ── Shadow ────────────────────────────────────────────────────────────────────

export const useSubmitShadowAttempt = (sessionId: string) =>
  useMutation({
    mutationFn: ({ segmentIdx, audio }: { segmentIdx: number; audio: Blob }) => {
      const form = new FormData()
      form.append('audio', audio, 'recording.webm')
      return api
        .post<never, ApiResponse<ShadowAttemptResult>>(
          `/api/sessions/${sessionId}/shadow/${segmentIdx}/attempt`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })

// ── Retell ────────────────────────────────────────────────────────────────────

export const useStartRetell = (sessionId: string) =>
  useMutation({
    mutationFn: (scaffoldLevel: number) =>
      api
        .post<never, ApiResponse<RetellScaffoldResponse>>(
          `/api/sessions/${sessionId}/retell/start`,
          { scaffoldLevel },
        )
        .then((r) => r.data),
    onError: (err: Error) => toast.error(err.message),
  })

export const useSubmitRetellAttempt = (sessionId: string) =>
  useMutation({
    mutationFn: (audio: Blob) => {
      const form = new FormData()
      form.append('audio', audio, 'recording.webm')
      return api
        .post<never, ApiResponse<RetellFeedback>>(
          `/api/sessions/${sessionId}/retell/attempt`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })

// ── Speak ─────────────────────────────────────────────────────────────────────

export const useSpeakingQuestion = (sessionId: string) =>
  useQuery({
    queryKey: ['sessions', sessionId, 'speak-question'],
    queryFn: () =>
      api
        .get<never, ApiResponse<SpeakingQuestionResponse>>(
          `/api/sessions/${sessionId}/speak/question`,
        )
        .then((r) => r.data),
    enabled: !!sessionId,
    staleTime: 60 * 60_000,
  })

export const useSubmitSpeakAttempt = (sessionId: string) =>
  useMutation({
    mutationFn: (audio: Blob) => {
      const form = new FormData()
      form.append('audio', audio, 'recording.webm')
      return api
        .post<never, ApiResponse<SpeakFeedback>>(
          `/api/sessions/${sessionId}/speak/attempt`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        .then((r) => r.data)
    },
    onError: (err: Error) => toast.error(err.message),
  })

// ── Quick Review ──────────────────────────────────────────────────────────────

export const useQuickReviewCards = (sessionId: string) =>
  useQuery({
    queryKey: ['sessions', sessionId, 'quick-review'],
    queryFn: () =>
      api
        .get<never, ApiResponse<Card[]>>(`/api/sessions/${sessionId}/quick-review`)
        .then((r) => r.data),
    enabled: !!sessionId,
    staleTime: 5 * 60_000,
  })

export const useQuickReviewCard = (sessionId: string) =>
  useMutation({
    mutationFn: ({ cardId, rating }: { cardId: string; rating: 1 | 2 | 3 | 4 }) =>
      api
        .post(`/api/sessions/${sessionId}/quick-review/review/${cardId}`, { rating }),
    onError: (err: Error) => toast.error(err.message),
  })
