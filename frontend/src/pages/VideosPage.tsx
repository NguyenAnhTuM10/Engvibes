import { Play } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'

export default function VideosPage() {
  return (
    <div>
      <PageHeader title="Videos" description="Browse and learn from video lessons" />
      <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
        <Play className="h-12 w-12 opacity-30" />
        <p className="text-lg font-medium">Video library coming soon</p>
        <p className="text-sm">This will show all available learning videos</p>
      </div>
    </div>
  )
}
