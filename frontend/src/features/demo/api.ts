import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import apiClient from '@/shared/api/client'

interface PingResponse {
  id: string
  message: string
  createdAt: string
}

interface ApiResponse<T> {
  data: T
  message: string
}

export function useListPings() {
  return useQuery({
    queryKey: ['demo', 'pings'],
    queryFn: () => apiClient.get('/api/demo/pings') as Promise<ApiResponse<PingResponse[]>>,
  })
}

export function useCreatePing() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (message: string) =>
      apiClient.post('/api/demo/pings', { message }) as Promise<ApiResponse<PingResponse>>,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['demo', 'pings'] })
    },
  })
}
