// Nội dung tĩnh phục vụ từ backend (data/pronunciation_content.json)
export interface PronunciationWord {
  text: string
  targetSound: string        // âm trọng tâm IPA, vd "θ"
  ipa: string                // IPA đầy đủ của từ, vd "θɪŋk"
  exampleSentence: string
  group: string              // nhóm âm, vd "Final Consonants"
}

export interface PronunciationSentence {
  text: string
  level: 'B1' | 'B2' | 'C1'
  targetSound: string | null // âm trọng tâm của drill, null nếu không nhắm 1 âm
  category: string
}

export interface WordAnalysis {
  word: string
  heard: string | null
  wordIpa: string
  score: number   // 0–100
}

export interface PhonemeMatch {
  position: number
  expected: string          // phoneme đúng, vd: "θ"
  actual: string | null     // phoneme user nói, null = bị bỏ sót
  matched: boolean
  tip: string | null        // gợi ý sửa lỗi
}

export interface AttemptResult {
  attemptId: string
  attemptNumber: number
  transcript: string        // Whisper nhận ra
  targetIpa: string | null  // IPA của từ đúng
  actualIpa: string         // IPA của transcript
  overallScore: number      // 0–100
  accuracyScore: number
  fluencyScore: number
  phonemeMatches: PhonemeMatch[]
  wordAnalyses: WordAnalysis[] | null
  createdAt: string
}

export interface PronunciationSession {
  id: string
  targetText: string
  targetIpa: string | null
  sessionType: string
  attemptCount: number
  bestScore: number | null
  createdAt: string
}

// Payload nhận từ WebSocket
export interface PronunciationProgress {
  attemptId: string
  type: 'PROCESSING' | 'TRANSCRIBED' | 'COMPLETED' | 'FAILED'
  progress: number          // 0–100
  message: string
  result: AttemptResult | null  // chỉ có khi type='COMPLETED'
}

export type ProcessingStatus = 'idle' | 'processing' | 'transcribed' | 'completed' | 'error'
