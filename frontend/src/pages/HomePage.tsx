import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import apiClient from '@/shared/api/client'

interface HealthData {
  status: string
  timestamp: string
}

interface ApiResponse<T> {
  data: T
  message: string
}

export default function HomePage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['health'],
    queryFn: () => apiClient.get('/api/health') as Promise<ApiResponse<HealthData>>,
  })

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-xl shadow p-8 max-w-md w-full">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">English Learning Platform</h1>
        <p className="text-gray-500 mb-6">Đồ án tốt nghiệp — Listening + Speaking with AI</p>

        <div className="border rounded-lg p-4 bg-gray-50">
          <p className="text-sm font-medium text-gray-600 mb-1">Backend status:</p>
          {isLoading && <p className="text-yellow-600">Connecting...</p>}
          {isError && <p className="text-red-600">Cannot reach backend</p>}
          {data && (
            <p className="text-green-600 font-semibold">
              {data.data?.status ?? 'UP'}
            </p>
          )}
        </div>

        <div className="mt-4">
          <Link to="/demo" className="text-sm text-blue-600 hover:underline">
            → Go to Demo page (FE↔BE↔DB test)
          </Link>
        </div>
      </div>
    </div>
  )
}
