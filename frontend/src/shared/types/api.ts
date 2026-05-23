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

// ── Video ────────────────────────────────────────────────────────────────────

export type VideoStatus = 'DRAFT' | 'PROCESSING' | 'PUBLISHED' | 'FAILED'

export interface WarmupWord {
  word: string
  ipa: string
  definition: string
  cefrLevel: CefrLevel
  partOfSpeech: string
}

export interface Video {
  id: string
  title: string
  description?: string
  thumbnailUrl: string | null
  videoUrl: string | null
  durationSec: number
  cefrLevel: CefrLevel
  topic: string
  status: VideoStatus
  errorMessage?: string | null
  viewCount: number
  createdAt?: string
  summary?: string | null
  keyPoints?: string[] | null
  speakingQuestion?: string | null
  warmupWords?: WarmupWord[] | null
  collocations?: Record<string, string[]> | null
}

export interface VideoFilter {
  cefrLevel?: CefrLevel
  topic?: string
  search?: string
  page?: number
  size?: number
}

export interface SubtitleWord {
  word: string
  startMs: number
  endMs: number
}

export interface SubtitleSegment {
  id: string
  orderIndex: number
  startMs: number
  endMs: number
  text: string
  words?: SubtitleWord[] | null
}

// ── Session ──────────────────────────────────────────────────────────────────

export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface Session {
  id: string
  videoId: string
  status: SessionStatus
  currentStep: number
  completedSteps?: number[]
  scaffoldLevel: number | null
  totalXpEarned: number
  startedAt?: string
  completedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface WarmupWordResponse {
  vocabId: string
  word: string
  ipa: string | null
  definition: string | null
  cefrLevel: CefrLevel | null
  partOfSpeech: string | null
}

export interface SessionHistoryItem {
  id: string
  videoId: string
  videoTitle: string
  videoThumbnailUrl: string | null
  status: SessionStatus
  currentStep: number
  totalXpEarned: number
  startedAt?: string
  completedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

// ── Phrase / Shadow ───────────────────────────────────────────────────────────

export type WordMatchStatus = 'MATCH' | 'MISSING' | 'EXTRA' | 'MISPRONOUNCED'

export interface WordMatch {
  word: string
  status: WordMatchStatus
}

export interface PhraseAttemptResult {
  transcript: string
  wordMatches: WordMatch[]
  score: number
  attemptNumber: number
  maxAttempts: number
}

export interface ShadowAttemptResult extends PhraseAttemptResult {
  weakPhonemes: string[]
}

// ── Retell ────────────────────────────────────────────────────────────────────

export interface RetellScaffoldResponse {
  scaffoldLevel: number
  wordBank?: string[] | null
  sentenceStarters?: string[] | null
  storyFrame?: string | null
  keyPoints?: string[] | null
}

export interface GrammarIssue {
  errorQuote: string
  correction: string
  explanation: string
}

export interface RetellFeedback {
  score: number
  coverageScore: number
  vocabularyScore: number
  grammarScore: number
  transcript: string
  coveredPoints: string[]
  missedPoints: string[]
  usedVocab: string[]
  missedVocab: string[]
  grammarIssues: GrammarIssue[]
  positiveNotes: string[]
  improvementTips: string[]
  modelAnswer: string
}

// ── Speak ─────────────────────────────────────────────────────────────────────

export interface SpeakingQuestionResponse {
  question: string
  suggestedVocab: string[] | null
  collocations: string[] | null
  sampleOpening?: string | null
  structureTips?: string[] | null
}

export interface SpeakFeedback {
  score: number
  fluencyScore: number
  grammarScore: number
  vocabVarietyScore: number
  transcript: string
  vocabFromVideoUsed: string[]
  grammarIssues: GrammarIssue[]
  positiveNotes: string[]
  improvementTips: string[]
  modelAnswer: string
}

export interface IeltsFeedback {
  transcript: string
  fluency: number
  grammar: number
  vocabulary: number
  pronunciation: number
  overall: number
  feedback: string
}

// ── Stats ─────────────────────────────────────────────────────────────────────

export interface OverviewStats {
  streakDays: number
  totalXp: number
  videosCompleted: number
  vocabMastered: number
  avgRetellScore7d: number | null
}

export interface DailyActivity {
  date: string
  totalMinutes: number
  byActivity: Record<string, number>
}

export interface PhonemeStats {
  phoneme: string
  errorRate: number
  totalAttempts: number
  errors: number
}

// vocab-growth backend shape: { "2026-05-09": { A1: 71, A2: 29 }, ... }
export type VocabGrowthData = Record<string, Partial<Record<CefrLevel, number>>>

export interface DailyChallenge {
  videoId: string
  videoTitle: string
  vocabToReview: number
  randomPhrase?: string | null
}
