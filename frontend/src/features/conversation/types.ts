export interface ConversationScenario {
  id: string
  displayName: string
  description: string
  aiRole: string
  userGoal: string
}

export interface HintResponse {
  keywords: string[]
  exampleSentence: string
}

export interface ConversationSessionResponse {
  sessionId: string
  scenarioId: string
  scenarioDisplayName: string
  aiRole: string
  userGoal: string
  firstAiText: string
  firstAiAudioUrl: string | null
  hints: HintResponse
}

export interface ConversationTurnResponse {
  turnNumber: number
  userTranscript: string
  aiText: string
  aiAudioUrl: string | null
  hints: HintResponse
  isLastTurn: boolean
}

export interface ConversationMessage {
  role: 'user' | 'ai'
  text: string
  audioUrl?: string | null
  turnNumber: number
}

export interface GrammarError {
  error: string
  correction: string
}

export interface ConversationEndResponse {
  sessionId: string
  totalTurns: number
  xpEarned: number
  summary: {
    grammarErrors: GrammarError[]
    vocabHighlights: string[]
    encouragement: string
    topTip: string
  } | null
}
