import { create } from 'zustand'
import type { ConversationScenario } from './types'
import type { ConversationReviewResponse } from './api'

// T3.2 — Store cho luồng realtime (luồng A). Chỉ giữ render-affecting state.
// Refs imperative (WebSocket, AudioContext, dbSessionId) GIỮ ở useRef trong component
// — không serialize được, không thuộc về Zustand.

export type ConversationStage = 'pick' | 'connecting' | 'active' | 'reviewing' | 'result'

export interface ConversationMsg {
  role: 'user' | 'assistant'
  text: string
}

interface ConversationState {
  stage: ConversationStage
  sessionInfo: ConversationScenario | null
  messages: ConversationMsg[]
  isAiSpeaking: boolean
  isMuted: boolean
  elapsedSec: number
  feedback: ConversationReviewResponse | null

  setStage: (stage: ConversationStage) => void
  setSessionInfo: (s: ConversationScenario | null) => void
  addMessage: (msg: ConversationMsg) => void
  setAiSpeaking: (v: boolean) => void
  toggleMuted: () => void
  setMuted: (v: boolean) => void
  setElapsedSec: (n: number) => void
  setFeedback: (f: ConversationReviewResponse | null) => void
  reset: () => void
}

const initial = {
  stage: 'pick' as ConversationStage,
  sessionInfo: null,
  messages: [] as ConversationMsg[],
  isAiSpeaking: false,
  isMuted: false,
  elapsedSec: 0,
  feedback: null,
}

export const useConversationStore = create<ConversationState>((set) => ({
  ...initial,
  setStage: (stage) => set({ stage }),
  setSessionInfo: (sessionInfo) => set({ sessionInfo }),
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
  setAiSpeaking: (isAiSpeaking) => set({ isAiSpeaking }),
  toggleMuted: () => set((s) => ({ isMuted: !s.isMuted })),
  setMuted: (isMuted) => set({ isMuted }),
  setElapsedSec: (elapsedSec) => set({ elapsedSec }),
  setFeedback: (feedback) => set({ feedback }),
  reset: () => set(initial),
}))
