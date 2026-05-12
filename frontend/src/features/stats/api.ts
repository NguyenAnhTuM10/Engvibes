import { useQuery } from '@tanstack/react-query'
import api from '@/shared/api/client'
import type {
  ApiResponse,
  DailyActivity,
  OverviewStats,
  PhonemeStats,
  VocabGrowthData,
} from '@/shared/types/api'

export const useOverviewStats = () =>
  useQuery({
    queryKey: ['stats', 'overview'],
    queryFn: () =>
      api.get<never, ApiResponse<OverviewStats>>('/api/stats/overview').then((r) => r.data),
    staleTime: 5 * 60_000,
  })

// backend trả DailyActivity[] thẳng, không phải { days: [] }
export const useWeeklyActivity = () =>
  useQuery({
    queryKey: ['stats', 'weekly'],
    queryFn: () =>
      api.get<never, ApiResponse<DailyActivity[]>>('/api/stats/weekly').then((r) => r.data),
    staleTime: 5 * 60_000,
  })

export const usePhonemeStats = () =>
  useQuery({
    queryKey: ['stats', 'phonemes'],
    queryFn: () =>
      api.get<never, ApiResponse<PhonemeStats[]>>('/api/stats/phonemes').then((r) => r.data),
    staleTime: 5 * 60_000,
  })

// backend trả { "2026-05-09": { A1: 71, A2: 29 }, ... }
export const useVocabGrowth = () =>
  useQuery({
    queryKey: ['stats', 'vocab-growth'],
    queryFn: () =>
      api.get<never, ApiResponse<VocabGrowthData>>('/api/stats/vocab-growth').then((r) => r.data),
    staleTime: 5 * 60_000,
  })
