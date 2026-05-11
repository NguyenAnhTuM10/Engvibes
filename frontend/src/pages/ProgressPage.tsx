import { TrendingUp } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'

export default function ProgressPage() {
  return (
    <div>
      <PageHeader title="Progress" description="Track your learning journey" />
      <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
        <TrendingUp className="h-12 w-12 opacity-30" />
        <p className="text-lg font-medium">Stats dashboard coming soon</p>
        <p className="text-sm">Charts and analytics will be shown here</p>
      </div>
    </div>
  )
}
