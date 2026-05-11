import { User } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Card, CardContent } from '@/components/ui/card'
import { useAuthStore } from '@/features/auth/store'
import { useCurrentUser } from '@/features/auth/api'

export default function ProfilePage() {
  const storeUser = useAuthStore((s) => s.user)
  const { data: user } = useCurrentUser()
  const displayUser = user ?? storeUser

  return (
    <div>
      <PageHeader title="Profile" description="Your account details" />
      <div className="max-w-lg space-y-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4 mb-4">
              <div className="h-14 w-14 rounded-full bg-primary flex items-center justify-center text-primary-foreground text-xl font-bold">
                {displayUser?.username?.slice(0, 2).toUpperCase() ?? '??'}
              </div>
              <div>
                <p className="font-semibold text-lg">{displayUser?.username}</p>
                <p className="text-sm text-muted-foreground">{displayUser?.email}</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-muted-foreground">CEFR Level</span>
                <p className="font-medium">{displayUser?.cefrLevel ?? '—'}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Role</span>
                <p className="font-medium">{displayUser?.role ?? '—'}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Total XP</span>
                <p className="font-medium">{displayUser?.totalXp ?? 0}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Streak</span>
                <p className="font-medium">{displayUser?.currentStreakDays ?? 0} days</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="flex flex-col items-center justify-center py-12 text-muted-foreground gap-3">
          <User className="h-10 w-10 opacity-30" />
          <p className="text-sm">Full profile editing coming soon</p>
        </div>
      </div>
    </div>
  )
}
