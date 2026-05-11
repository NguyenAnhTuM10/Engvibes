export interface ApiResponse<T> {
  data: T
  message?: string
  timestamp: string
}

export interface User {
  id: string
  email: string
  username: string
  cefrLevel: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  role: 'USER' | 'ADMIN'
  totalXp: number
  currentStreakDays: number
  createdAt: string
}

export interface AuthResponse {
  token: string
  expiresIn: number
  user: User
}

export interface LoginData {
  email: string
  password: string
}

export interface RegisterData {
  email: string
  username: string
  password: string
  cefrLevel: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
}
