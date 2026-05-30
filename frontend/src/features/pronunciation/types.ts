// Nội dung tĩnh phục vụ từ backend (data/pronunciation_content.json)
export interface PronunciationWord {
  text: string
  targetSound: string        // âm trọng tâm IPA, vd "θ"
  ipa: string                // IPA đầy đủ của từ, vd "θɪŋk"
  exampleSentence: string
  group: string              // nhóm âm, vd "Final Consonants"
  vi: string                 // nghĩa tiếng Việt
  commonError: string        // lỗi người Việt hay mắc
  minimalPair: string | null // từ tương phản tối thiểu (vd "sink")
  tip: string                // gợi ý sửa lỗi ngắn
}

export interface PronunciationSentence {
  text: string
  level: 'B1' | 'B2' | 'C1'
  targetSound: string | null // âm trọng tâm của drill, null nếu không nhắm 1 âm
  category: string
  vi: string                 // bản dịch tiếng Việt
  tip: string                // ghi chú trọng tâm luyện
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

// Thay đổi trên deck SRS "Sounds to practice" sau 1 lần phát âm
export interface SoundCardChange {
  cardId: string
  word: string
  action: 'ADDED' | 'DEMOTED' | 'PROMOTED'
  ipa: string | null
  score: number
  intervalDays: number | null
  dueDate: string | null
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
  soundCardChanges: SoundCardChange[] | null  // tự đẩy từ yếu vào SRS
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
