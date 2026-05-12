import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { X, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useVideo } from '@/features/videos/api'
import { useCreateOrGetSession, useAdvanceStep } from '@/features/session/api'
import { useSessionStore } from '@/features/session/store'
import StepNavigator from '@/features/session/components/StepNavigator'
import WarmupStep from '@/features/session/steps/WarmupStep'
import ListenStep from '@/features/session/steps/ListenStep'
import PhraseStep from '@/features/session/steps/PhraseStep'
import ShadowStep from '@/features/session/steps/ShadowStep'

function ExitDialog({ onConfirm, onCancel }: { onConfirm: () => void; onCancel: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-background rounded-xl p-6 max-w-sm w-full shadow-xl border">
        <div className="flex items-center gap-3 mb-3">
          <AlertCircle className="h-5 w-5 text-orange-500 shrink-0" />
          <h3 className="font-semibold">Exit session?</h3>
        </div>
        <p className="text-sm text-muted-foreground mb-6">
          Your progress will be saved. You can resume this session later from the History page.
        </p>
        <div className="flex gap-3">
          <Button variant="outline" className="flex-1" onClick={onCancel}>
            Keep learning
          </Button>
          <Button variant="destructive" className="flex-1" onClick={onConfirm}>
            Exit
          </Button>
        </div>
      </div>
    </div>
  )
}

function StepPlaceholder({ step, onSkip }: { step: number; onSkip: () => void }) {
  const labels = ['', '', 'Phrase Practice', 'Shadowing', 'Retell Coach', 'Speaking', 'Quick Review']
  return (
    <div className="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground">
      <p className="text-xl font-semibold text-foreground">{labels[step] ?? `Step ${step + 1}`}</p>
      <p className="text-sm">This step will be available in the next update.</p>
      <Button variant="outline" onClick={onSkip}>
        Skip to next step
      </Button>
    </div>
  )
}

export default function VideoSessionPage() {
  const { videoId } = useParams<{ videoId: string }>()
  const navigate = useNavigate()
  const [showExit, setShowExit] = useState(false)

  const { sessionId, currentStep, setSession, advanceStep, reset } = useSessionStore()
  const { data: video } = useVideo(videoId ?? '')
  const createSession = useCreateOrGetSession()
  const advanceMutation = useAdvanceStep()

  useEffect(() => {
    if (!videoId) return
    createSession.mutate(videoId, {
      onSuccess: (session) => {
        setSession(session.id, session.currentStep)
      },
    })
    return () => {
      // don't reset on unmount — user might navigate back
    }
  }, [videoId]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdvance = () => {
    if (!sessionId) return
    if (currentStep >= 6) {
      navigate('/')
      return
    }
    advanceMutation.mutate(
      { sessionId, completed: true },
      {
        onSuccess: (session) => advanceStep(session.currentStep),
      },
    )
  }

  const handleExit = () => {
    reset()
    navigate('/videos')
  }

  if (createSession.isPending && !sessionId) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (createSession.isError && !sessionId) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4 text-muted-foreground">
        <p>Failed to start session</p>
        <Button variant="outline" onClick={() => navigate('/videos')}>
          Back to Videos
        </Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-background">
      {/* Header */}
      <header className="border-b px-4 py-2.5 flex items-center gap-3 shrink-0">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setShowExit(true)}
          aria-label="Exit session"
        >
          <X className="h-4 w-4" />
        </Button>

        <p className="text-sm font-medium truncate flex-1 min-w-0">
          {video?.title ?? 'Loading...'}
        </p>

        <div className="shrink-0 overflow-x-auto">
          <StepNavigator
            currentStep={currentStep}
            onStepClick={(step) => advanceStep(step)}
          />
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 overflow-y-auto p-6">
        {!sessionId ? (
          <div className="flex items-center justify-center h-full">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          </div>
        ) : currentStep === 0 ? (
          <WarmupStep sessionId={sessionId} onComplete={handleAdvance} />
        ) : currentStep === 1 ? (
          <ListenStep
            sessionId={sessionId}
            videoUrl={video?.videoUrl ?? ''}
            onComplete={handleAdvance}
          />
        ) : currentStep === 2 ? (
          <PhraseStep sessionId={sessionId} onComplete={handleAdvance} />
        ) : currentStep === 3 ? (
          <ShadowStep sessionId={sessionId} onComplete={handleAdvance} />
        ) : (
          <StepPlaceholder step={currentStep} onSkip={handleAdvance} />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t px-4 py-2.5 flex items-center justify-between shrink-0">
        <span className="text-xs text-muted-foreground">
          Step {currentStep + 1} of 7
        </span>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleAdvance}
            disabled={advanceMutation.isPending}
          >
            Skip step
          </Button>
          {currentStep >= 6 && (
            <Button size="sm" onClick={handleExit}>
              Finish session
            </Button>
          )}
        </div>
      </footer>

      {showExit && (
        <ExitDialog onConfirm={handleExit} onCancel={() => setShowExit(false)} />
      )}
    </div>
  )
}
