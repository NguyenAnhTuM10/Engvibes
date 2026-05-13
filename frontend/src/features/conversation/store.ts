import { create } from 'zustand'
import type { ConversationMessage, HintResponse, ConversationEndResponse } from './types'

type RecordingState = 'idle' | 'recording' | 'processing' | 'playing'

interface ConversationState {
  sessionId: string | null
  scenarioId: string | null
  scenarioDisplayName: string
  messages: ConversationMessage[]
  hints: HintResponse | null
  recordingState: RecordingState
  isCompleted: boolean
  endData: ConversationEndResponse | null

  setSession: (sessionId: string, scenarioId: string, displayName: string) => void
  addMessage: (msg: ConversationMessage) => void
  setHints: (hints: HintResponse) => void
  setRecordingState: (state: RecordingState) => void
  setCompleted: (data: ConversationEndResponse) => void
  reset: () => void
}

const initial = {
  sessionId: null,
  scenarioId: null,
  scenarioDisplayName: '',
  messages: [],
  hints: null,
  recordingState: 'idle' as RecordingState,
  isCompleted: false,
  endData: null,
}

export const useConversationStore = create<ConversationState>((set) => ({
  ...initial,
  setSession: (sessionId, scenarioId, scenarioDisplayName) =>
    set({ sessionId, scenarioId, scenarioDisplayName }),
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
  setHints: (hints) => set({ hints }),
  setRecordingState: (recordingState) => set({ recordingState }),
  setCompleted: (endData) => set({ isCompleted: true, endData }),
  reset: () => set(initial),
}))
