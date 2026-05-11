import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import api from '@/shared/api/client'
import { useAuthStore } from './store'
import type { AuthResponse, LoginData, RegisterData, User } from '@/shared/types/api'

export const useLogin = () => {
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: LoginData) =>
      api.post<never, { data: AuthResponse }>('/api/auth/login', data).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      navigate('/')
    },
    onError: (err: Error) => {
      toast.error(err.message)
    },
  })
}

export const useRegister = () => {
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: RegisterData) =>
      api.post<never, { data: AuthResponse }>('/api/auth/register', data).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      navigate('/')
    },
    onError: (err: Error) => {
      toast.error(err.message)
    },
  })
}

export const useLogout = () => {
  const { logout } = useAuthStore()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return () => {
    logout()
    queryClient.clear()
    navigate('/login')
  }
}

export const useCurrentUser = () => {
  const { isAuthenticated } = useAuthStore()

  return useQuery({
    queryKey: ['me'],
    queryFn: () => api.get<never, { data: User }>('/api/me').then((r) => r.data),
    enabled: isAuthenticated(),
    staleTime: 5 * 60_000,
  })
}
