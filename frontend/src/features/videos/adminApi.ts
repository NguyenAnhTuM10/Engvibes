import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import type { ApiResponse, Page, Video } from '@/shared/types/api'

export interface VideoStatus {
  id: string
  status: string
  errorMessage?: string | null
}

export const useAdminVideos = (page = 0) =>
  useQuery({
    queryKey: ['admin', 'videos', page],
    queryFn: () =>
      api
        .get<never, ApiResponse<Page<Video>>>('/api/admin/videos', { params: { page, size: 20 } })
        .then((r) => r.data),
    staleTime: 30_000,
  })

export const useAdminVideoStatus = (id: string, enabled: boolean) =>
  useQuery({
    queryKey: ['admin', 'videos', id, 'status'],
    queryFn: () =>
      api.get<never, ApiResponse<VideoStatus>>(`/api/admin/videos/${id}/status`).then((r) => r.data),
    enabled: !!id && enabled,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PROCESSING' ? 3000 : false
    },
  })

export const useUploadVideo = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      file,
      metadata,
    }: {
      file: File
      metadata: { title: string; topic: string; cefrLevel: string; description?: string }
    }) => {
      const form = new FormData()
      form.append('file', file)
      form.append(
        'metadata',
        new Blob([JSON.stringify(metadata)], { type: 'application/json' }),
      )
      return api
        .post<never, ApiResponse<Video>>('/api/admin/videos', form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then((r) => r.data)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'videos'] })
      toast.success('Video uploaded')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useProcessVideo = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      api.post(`/api/admin/videos/${id}/process`),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['admin', 'videos'] })
      qc.invalidateQueries({ queryKey: ['admin', 'videos', id, 'status'] })
      toast.success('Processing started')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}

export const useDeleteVideo = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/admin/videos/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'videos'] })
      toast.success('Video deleted')
    },
    onError: (err: Error) => toast.error(err.message),
  })
}
