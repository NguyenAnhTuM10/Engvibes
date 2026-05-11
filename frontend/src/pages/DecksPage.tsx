import { BookOpen } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'

export default function DecksPage() {
  return (
    <div>
      <PageHeader title="Flashcard Decks" description="Review your vocabulary decks" />
      <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-3">
        <BookOpen className="h-12 w-12 opacity-30" />
        <p className="text-lg font-medium">Decks coming soon</p>
        <p className="text-sm">Manage and review your flashcard decks here</p>
      </div>
    </div>
  )
}
