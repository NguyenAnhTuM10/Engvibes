export interface Sm2Deck {
  id: string
  name: string
  description: string | null
  createdAt: string
}

export interface Sm2Card {
  id: string
  deckId: string
  front: string
  back: string
  ipa: string | null
  exampleSentence: string | null
  createdAt: string
}

export interface QueueItem {
  cardId: string
  deckId: string
  front: string
  back: string
  ipa: string | null
  exampleSentence: string | null
  isNew: boolean
  repetitions: number | null
  intervalDays: number | null
  easeFactor: number | null
  dueDate: string | null
  lastReviewed: string | null
}

export interface ReviewResult {
  cardId: string
  quality: number
  repetitions: number
  intervalDays: number
  easeFactor: number
  dueDate: string
  lastReviewed: string
}

export interface ImportSummary {
  imported: number
  skipped: number
  errors: { line: number; reason: string }[]
}
