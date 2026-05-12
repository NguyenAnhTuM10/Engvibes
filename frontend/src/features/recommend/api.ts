import { useQuery } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type { ApiResponse, DailyChallenge, Video } from '@/shared/types/api'

export const useRecommendedVideos = (limit = 6) =>
  useQuery({
    queryKey: ['recommend', 'videos', limit],
    queryFn: () =>
      api
        .get<never, ApiResponse<Video[]>>('/api/recommend/videos', { params: { limit } })
        .then((r) => r.data),
    staleTime: 10 * 60_000,
  })

export const useDailyChallenge = () =>
  useQuery({
    queryKey: ['recommend', 'daily-challenge'],
    queryFn: () =>
      api
        .get<never, ApiResponse<DailyChallenge>>('/api/recommend/daily-challenge')
        .then((r) => r.data),
    staleTime: 60 * 60_000,
    retry: false,
  })
