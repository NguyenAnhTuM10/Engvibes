import { Shield } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'

export default function AdminVideosPage() {
  return (
    <div>
      <PageHeader title="Admin — Videos" description="Manage video content" />
      <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
        <Shield className="h-12 w-12 opacity-30" />
        <p className="text-lg font-medium">Admin panel coming soon</p>
        <p className="text-sm">Upload and manage learning videos here</p>
      </div>
    </div>
  )
}
