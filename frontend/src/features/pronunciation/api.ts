import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type {
  AttemptResult,
  PronunciationSentence,
  PronunciationSession,
  PronunciationWord,
  VideoSentenceSource,
} from './types'

// Nội dung tĩnh: video (đã publish + có phụ đề) để lấy câu luyện phát âm
export function useVideoSources() {
  return useQuery({
    queryKey: ['pronunciation-video-sources'],
    queryFn: () => api.get<VideoSentenceSource[]>('/api/pronunciation/videos').then(r => r.data),
    staleTime: 5 * 60_000,
  })
}

// Nội dung tĩnh: danh sách từ luyện phát âm (kèm IPA + câu ví dụ)
export function useWords() {
  return useQuery({
    queryKey: ['pronunciation-words'],
    queryFn: () => api.get<PronunciationWord[]>('/api/pronunciation/words').then(r => r.data),
    staleTime: Infinity,   // nội dung tĩnh — không đổi trong phiên
  })
}

// Nội dung tĩnh: danh sách câu luyện phát âm
export function useSentences() {
  return useQuery({
    queryKey: ['pronunciation-sentences'],
    queryFn: () => api.get<PronunciationSentence[]>('/api/pronunciation/sentences').then(r => r.data),
    staleTime: Infinity,
  })
}

// Tạo session mới cho một từ/câu
export function useCreateSession() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { targetText: string; sessionType?: string }) =>
      api.post<PronunciationSession>('/api/pronunciation/sessions', {
        targetText: params.targetText,
        sessionType: params.sessionType ?? 'WORD',
      }).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pronunciation-sessions'] }),
  })
}

// Lấy thông tin session (targetIpa, bestScore)
export function useSession(sessionId: string | null) {
  return useQuery({
    queryKey: ['pronunciation-session', sessionId],
    queryFn: () =>
      api.get<PronunciationSession>(`/api/pronunciation/sessions/${sessionId}`).then(r => r.data),
    enabled: !!sessionId,
    staleTime: 10_000,
  })
}

// Lịch sử các lần attempt
export function useAttempts(sessionId: string | null) {
  return useQuery({
    queryKey: ['pronunciation-attempts', sessionId],
    queryFn: () =>
      api.get<AttemptResult[]>(`/api/pronunciation/sessions/${sessionId}/attempts`).then(r => r.data),
    enabled: !!sessionId,
  })
}

// Submit audio — trả về attemptId ngay, kết quả đến qua WebSocket
export function useSubmitAttempt(sessionId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (audioBlob: Blob) => {
      const form = new FormData()
      form.append('audio', audioBlob, 'recording.webm')
      return api.post<string>(`/api/pronunciation/sessions/${sessionId}/attempt`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 10_000,  // chỉ chờ 202, không chờ kết quả
      }).then(r => r.data)  // trả về attemptId (UUID string)
    },
    onSuccess: () => {
      // Reload attempts sau khi pipeline hoàn tất (được trigger từ WS handler)
      qc.invalidateQueries({ queryKey: ['pronunciation-attempts', sessionId] })
      qc.invalidateQueries({ queryKey: ['pronunciation-session', sessionId] })
    },
  })
}
