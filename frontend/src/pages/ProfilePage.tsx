import { useState } from 'react'
import { useTheme } from 'next-themes'
import { Moon, Sun, Monitor } from 'lucide-react'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import CefrBadge from '@/components/ui/CefrBadge'
import { useAuthStore } from '@/features/auth/store'
import { useCurrentUser, useUpdateUser, useLogout } from '@/features/auth/api'
import type { CefrLevel } from '@/shared/types/api'

const CEFR_LEVELS: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

function CefrDialog({
  current,
  onClose,
}: {
  current: CefrLevel
  onClose: () => void
}) {
  const [selected, setSelected] = useState<CefrLevel>(current)
  const update = useUpdateUser()

  const handleSave = () => {
    update.mutate({ cefrLevel: selected }, { onSuccess: onClose })
  }

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-background border rounded-xl p-6 max-w-sm w-full shadow-xl">
        <h3 className="font-semibold mb-4">Update CEFR Level</h3>
        <div className="grid grid-cols-3 gap-2 mb-6">
          {CEFR_LEVELS.map((level) => (
            <button
              key={level}
              onClick={() => setSelected(level)}
              className={`py-2 rounded-lg text-sm font-semibold transition-colors border-2 ${
                selected === level ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/50'
              }`}
            >
              {level}
            </button>
          ))}
        </div>
        <div className="flex gap-3">
          <Button variant="outline" className="flex-1" onClick={onClose}>Cancel</Button>
          <Button className="flex-1" onClick={handleSave} disabled={update.isPending}>
            {update.isPending ? 'Saving...' : 'Save'}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default function ProfilePage() {
  const { data: user } = useCurrentUser()
  const storeUser = useAuthStore((s) => s.user)
  const displayUser = user ?? storeUser
  const logout = useLogout()
  const { theme, setTheme } = useTheme()
  const [showCefrDialog, setShowCefrDialog] = useState(false)

  return (
    <div className="space-y-6 max-w-lg">
      <PageHeader title="Profile" description="Your account details" />

      {/* User info */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-4 mb-5">
            <div className="h-14 w-14 rounded-full bg-primary flex items-center justify-center text-primary-foreground text-xl font-bold shrink-0">
              {displayUser?.username?.slice(0, 2).toUpperCase() ?? '??'}
            </div>
            <div>
              <p className="font-semibold text-lg leading-tight">{displayUser?.username}</p>
              <p className="text-sm text-muted-foreground">{displayUser?.email}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground text-xs mb-1">CEFR Level</p>
              <div className="flex items-center gap-2">
                {displayUser?.cefrLevel && <CefrBadge level={displayUser.cefrLevel} />}
                <button
                  onClick={() => setShowCefrDialog(true)}
                  className="text-xs text-primary hover:underline"
                >
                  Update
                </button>
              </div>
            </div>
            <div>
              <p className="text-muted-foreground text-xs mb-1">Role</p>
              <p className="font-medium">{displayUser?.role ?? '—'}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs mb-1">Total XP</p>
              <p className="font-bold text-lg">{(displayUser?.totalXp ?? 0).toLocaleString()}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs mb-1">Streak</p>
              <p className="font-bold text-lg">{displayUser?.currentStreakDays ?? 0} days 🔥</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Theme */}
      <Card>
        <CardHeader><CardTitle className="text-sm">Appearance</CardTitle></CardHeader>
        <CardContent>
          <div className="flex gap-2">
            {[
              { value: 'light', icon: <Sun className="h-4 w-4" />, label: 'Light' },
              { value: 'dark', icon: <Moon className="h-4 w-4" />, label: 'Dark' },
              { value: 'system', icon: <Monitor className="h-4 w-4" />, label: 'System' },
            ].map(({ value, icon, label }) => (
              <Button
                key={value}
                variant={theme === value ? 'default' : 'outline'}
                size="sm"
                className="gap-1.5"
                onClick={() => setTheme(value)}
              >
                {icon} {label}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Account actions */}
      <Card>
        <CardHeader><CardTitle className="text-sm">Account</CardTitle></CardHeader>
        <CardContent>
          <Button variant="destructive" size="sm" onClick={logout}>
            Sign out
          </Button>
        </CardContent>
      </Card>

      {showCefrDialog && displayUser?.cefrLevel && (
        <CefrDialog current={displayUser.cefrLevel} onClose={() => setShowCefrDialog(false)} />
      )}
    </div>
  )
}
