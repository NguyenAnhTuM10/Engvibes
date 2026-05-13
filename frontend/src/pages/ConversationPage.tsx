import { useState, useEffect, useCallback } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import ScenarioPicker from '@/features/conversation/components/ScenarioPicker'
import ConversationChat from '@/features/conversation/components/ConversationChat'
import PushToTalkButton from '@/features/conversation/components/PushToTalkButton'
import HintsPanel from '@/features/conversation/components/HintsPanel'
import SessionSummary from '@/features/conversation/components/SessionSummary'
import { useConversationStore } from '@/features/conversation/store'
import { useSubmitTurn, useEndConversation } from '@/features/conversation/api'
import type { ConversationSessionResponse } from '@/features/conversation/types'

type PageState = 'pick' | 'chat' | 'summary'

function playAudio(url: string, onEnd: () => void) {
  const audio = new Audio(url)
  audio.onended = onEnd
  audio.onerror = onEnd
  audio.play().catch(onEnd)
}

export default function ConversationPage() {
  const [pageState, setPageState] = useState<PageState>('pick')

  const {
    sessionId,
    scenarioDisplayName,
    messages,
    hints,
    recordingState,
    isCompleted,
    endData,
    addMessage,
    setHints,
    setRecordingState,
    setCompleted,
    reset,
  } = useConversationStore()

  const submitTurn = useSubmitTurn(sessionId ?? '')
  const endConversation = useEndConversation(sessionId ?? '')

  // Declare handleEndSession first so handleRecordingComplete can reference it
  const handleEndSession = useCallback(() => {
    endConversation.mutate(undefined, {
      onSuccess: (data) => {
        setCompleted(data)
        setPageState('summary')
      },
      onError: () => {
        setCompleted({
          sessionId: sessionId ?? '',
          totalTurns: messages.length,
          xpEarned: 0,
          summary: null,
        })
        setPageState('summary')
      },
    })
  }, [endConversation, setCompleted, sessionId, messages.length])

  const handleRecordingComplete = useCallback((blob: Blob) => {
    setRecordingState('processing')
    submitTurn.mutate(blob, {
      onSuccess: (data) => {
        addMessage({
          role: 'user',
          text: data.userTranscript || '(no speech detected)',
          turnNumber: data.turnNumber,
        })
        addMessage({
          role: 'ai',
          text: data.aiText,
          audioUrl: data.aiAudioUrl,
          turnNumber: data.turnNumber,
        })
        setHints(data.hints)

        if (data.isLastTurn) {
          handleEndSession()
          return
        }

        if (data.aiAudioUrl) {
          setRecordingState('playing')
          playAudio(data.aiAudioUrl, () => setRecordingState('idle'))
        } else {
          setRecordingState('idle')
        }
      },
      onError: () => setRecordingState('idle'),
    })
  }, [submitTurn, addMessage, setHints, setRecordingState, handleEndSession])

  const handleSessionStarted = useCallback((data: ConversationSessionResponse) => {
    addMessage({ role: 'ai', text: data.firstAiText, audioUrl: data.firstAiAudioUrl, turnNumber: 0 })
    setHints(data.hints)
    setPageState('chat')

    if (data.firstAiAudioUrl) {
      setRecordingState('playing')
      playAudio(data.firstAiAudioUrl, () => setRecordingState('idle'))
    }
  }, [addMessage, setHints, setRecordingState])

  const handleRestart = () => {
    reset()
    setPageState('pick')
  }

  useEffect(() => {
    if (isCompleted && pageState === 'chat') setPageState('summary')
  }, [isCompleted, pageState])

  if (pageState === 'pick') {
    return (
      <div className="py-4">
        <ScenarioPicker onStarted={handleSessionStarted} />
      </div>
    )
  }

  if (pageState === 'summary' && endData) {
    return (
      <div className="py-4">
        <SessionSummary data={endData} onRestart={handleRestart} />
      </div>
    )
  }

  const userTurns = messages.filter((m) => m.role === 'user').length

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)] max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between py-3 border-b shrink-0">
        <div>
          <h2 className="font-semibold text-sm">{scenarioDisplayName}</h2>
          <p className="text-xs text-muted-foreground">
            {userTurns} turn{userTurns !== 1 ? 's' : ''} completed
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleEndSession}
          disabled={endConversation.isPending}
        >
          <X className="h-4 w-4 mr-1" />
          End
        </Button>
      </div>

      {/* Chat area */}
      <div className="flex-1 overflow-y-auto px-1">
        <ConversationChat
          messages={messages}
          isProcessing={recordingState === 'processing'}
        />
      </div>

      {/* Bottom controls */}
      <div className="shrink-0 space-y-3 pt-3 border-t">
        <HintsPanel hints={hints} />
        <div className="flex justify-center pb-2">
          <PushToTalkButton
            recordingState={recordingState}
            disabled={isCompleted}
            onRecordingComplete={handleRecordingComplete}
            onRecordingStart={() => setRecordingState('recording')}
          />
        </div>
      </div>
    </div>
  )
}
