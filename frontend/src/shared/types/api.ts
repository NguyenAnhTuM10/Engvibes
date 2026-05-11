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

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  last: boolean
}

export type CefrLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'

export interface Vocab {
  id: string
  word: string
  partOfSpeech: string
  cefrLevel: CefrLevel
  ipa: string
  phonemes: string
  definition: string
}

export interface Deck {
  id: string
  name: string
  color: string | null
  isDefault: boolean
  cardCount: number
  dueCount: number
}

export type CardState = 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING'
export type CardSource = 'MANUAL' | 'WARMUP' | 'LISTEN' | 'QUICK_REVIEW'

export interface Card {
  id: string
  vocab: Vocab
  deckId: string
  state: CardState
  stability: number
  difficulty: number
  nextReview: string
  lastReview: string | null
  reviewCount: number
  lapseCount: number
  contextSentence: string | null
  sourceVideoId: string | null
  sourceType: CardSource | null
}
