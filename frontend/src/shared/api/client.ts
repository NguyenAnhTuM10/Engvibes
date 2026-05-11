import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

// Interceptors are set up in main.tsx after auth store is ready to avoid circular deps
export default api
