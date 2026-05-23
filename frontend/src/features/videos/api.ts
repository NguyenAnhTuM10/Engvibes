import { useQuery } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type { ApiResponse, Page, SubtitleSegment, Video, VideoFilter } from '@/shared/types/api'

export const useVideos = (filter: VideoFilter = {}) =>
  useQuery({
    queryKey: ['videos', filter],
    queryFn: () =>
      api
        .get<never, ApiResponse<Page<Video>>>('/api/videos', { params: filter })
        .then((r) => r.data),
    staleTime: 5 * 60_000,
  })

export const useVideo = (id: string) =>
  useQuery({
    queryKey: ['videos', id],
    queryFn: () =>
      api.get<never, ApiResponse<Video>>(`/api/videos/${id}`).then((r) => r.data),
    enabled: !!id,
    staleTime: 5 * 60_000,
  })

export const useRecommendedVideos = (limit = 6) =>
  useQuery({
    queryKey: ['videos', 'recommended', limit],
    queryFn: () =>
      api
        .get<never, ApiResponse<Video[]>>('/api/recommend/videos', { params: { limit } })
        .then((r) => r.data),
    staleTime: 10 * 60_000,
  })

export const useVideoSubtitles = (videoId: string) =>
  useQuery({
    queryKey: ['videos', videoId, 'subtitles'],
    queryFn: () =>
      api
        .get<never, ApiResponse<SubtitleSegment[]>>(`/api/videos/${videoId}/subtitles`)
        .then((r) => r.data),
    enabled: !!videoId,
    staleTime: 60 * 60_000,
  })
