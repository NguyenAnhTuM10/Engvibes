import { History } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'

export default function HistoryPage() {
  return (
    <div>
      <PageHeader title="History" description="Your past learning sessions" />
      <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
        <History className="h-12 w-12 opacity-30" />
        <p className="text-lg font-medium">Session history coming soon</p>
        <p className="text-sm">All completed and in-progress sessions will appear here</p>
      </div>
    </div>
  )
}
