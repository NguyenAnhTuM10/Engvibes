import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from '@/features/auth/store'

interface RoleGuardProps {
  role: 'ADMIN' | 'USER'
  children: ReactNode
}

export default function RoleGuard({ role, children }: RoleGuardProps) {
  const user = useAuthStore((s) => s.user)

  if (!user || user.role !== role) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}
