import { create } from 'zustand'

interface SessionState {
  sessionId: string | null
  currentStep: number
  scaffoldLevel: number | null
  isRecording: boolean

  setSession: (id: string, step: number) => void
  advanceStep: (step: number) => void
  setScaffold: (level: number) => void
  startRecording: () => void
  stopRecording: () => void
  reset: () => void
}

export const useSessionStore = create<SessionState>((set) => ({
  sessionId: null,
  currentStep: 0,
  scaffoldLevel: null,
  isRecording: false,

  setSession: (id, step) => set({ sessionId: id, currentStep: step }),
  advanceStep: (step) => set({ currentStep: step }),
  setScaffold: (level) => set({ scaffoldLevel: level }),
  startRecording: () => set({ isRecording: true }),
  stopRecording: () => set({ isRecording: false }),
  reset: () =>
    set({ sessionId: null, currentStep: 0, scaffoldLevel: null, isRecording: false }),
}))
