import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type { AttemptResult, PronunciationSession } from './types'

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
