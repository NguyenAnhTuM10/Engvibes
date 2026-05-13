import { Briefcase, Coffee, Hotel, Stethoscope, Calendar, Loader2 } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useScenarios, useStartConversation } from '../api'
import { useConversationStore } from '../store'
import type { ConversationSessionResponse } from '../types'

const SCENARIO_ICONS: Record<string, React.ReactNode> = {
  JOB_INTERVIEW: <Briefcase className="h-6 w-6" />,
  COFFEE_SHOP: <Coffee className="h-6 w-6" />,
  HOTEL_CHECKIN: <Hotel className="h-6 w-6" />,
  DOCTOR_APPOINTMENT: <Stethoscope className="h-6 w-6" />,
  MAKING_PLANS: <Calendar className="h-6 w-6" />,
}

interface Props {
  onStarted: (data: ConversationSessionResponse) => void
}

export default function ScenarioPicker({ onStarted }: Props) {
  const { data: scenarios = [], isLoading } = useScenarios()
  const startMutation = useStartConversation()
  const { setSession } = useConversationStore()

  const handleSelect = (scenarioId: string, displayName: string) => {
    startMutation.mutate(scenarioId, {
      onSuccess: (data) => {
        setSession(data.sessionId, data.scenarioId, data.scenarioDisplayName)
        onStarted(data)
      },
    })
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold">Conversation Practice</h1>
        <p className="mt-2 text-muted-foreground">
          Choose a scenario and practice speaking English with an AI conversation partner.
          You'll get keyword hints whenever you need inspiration.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {scenarios.map((scenario) => (
          <Card
            key={scenario.id}
            className="cursor-pointer border-2 transition-colors hover:border-primary"
            onClick={() => !startMutation.isPending && handleSelect(scenario.id, scenario.displayName)}
          >
            <CardHeader className="pb-2">
              <div className="flex items-center gap-3">
                <div className="rounded-md bg-primary/10 p-2 text-primary">
                  {SCENARIO_ICONS[scenario.id] ?? <Briefcase className="h-6 w-6" />}
                </div>
                <CardTitle className="text-base">{scenario.displayName}</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              <CardDescription>{scenario.description}</CardDescription>
              <div className="text-xs text-muted-foreground">
                <span className="font-medium">AI plays:</span> {scenario.aiRole}
              </div>
              <div className="text-xs text-muted-foreground">
                <span className="font-medium">Your goal:</span> {scenario.userGoal}
              </div>
              <Button
                size="sm"
                className="mt-2 w-full"
                disabled={startMutation.isPending}
              >
                {startMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  'Start Practice'
                )}
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
