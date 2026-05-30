// Realtime conversation (luồng A). Types turn-based cũ đã gỡ cùng backend luồng B.

export interface ConversationScenario {
  id: string
  displayName: string
  description: string
  aiRole: string
  userGoal: string
  openingLine: string
}
