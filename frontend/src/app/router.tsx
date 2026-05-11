import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import AuthGuard from '@/components/AuthGuard'
import AppLayout from '@/components/layout/AppLayout'
import RoleGuard from '@/features/auth/components/RoleGuard'

const HomePage = lazy(() => import('@/pages/HomePage'))
const LoginPage = lazy(() => import('@/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/RegisterPage'))
const VideosPage = lazy(() => import('@/pages/VideosPage'))
const DecksPage = lazy(() => import('@/pages/DecksPage'))
const ProgressPage = lazy(() => import('@/pages/ProgressPage'))
const HistoryPage = lazy(() => import('@/pages/HistoryPage'))
const ProfilePage = lazy(() => import('@/pages/ProfilePage'))
const AdminVideosPage = lazy(() => import('@/pages/AdminVideosPage'))
const DeckDetailPage = lazy(() => import('@/pages/DeckDetailPage'))

function PageLoader() {
  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
    </div>
  )
}

function Wrap({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageLoader />}>{children}</Suspense>
}

const router = createBrowserRouter([
  {
    path: '/login',
    element: <Wrap><LoginPage /></Wrap>,
  },
  {
    path: '/register',
    element: <Wrap><RegisterPage /></Wrap>,
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <AppLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Wrap><HomePage /></Wrap> },
      { path: 'videos', element: <Wrap><VideosPage /></Wrap> },
      { path: 'decks', element: <Wrap><DecksPage /></Wrap> },
      { path: 'decks/:id', element: <Wrap><DeckDetailPage /></Wrap> },
      { path: 'progress', element: <Wrap><ProgressPage /></Wrap> },
      { path: 'history', element: <Wrap><HistoryPage /></Wrap> },
      { path: 'profile', element: <Wrap><ProfilePage /></Wrap> },
      {
        path: 'admin/videos',
        element: (
          <RoleGuard role="ADMIN">
            <Wrap><AdminVideosPage /></Wrap>
          </RoleGuard>
        ),
      },
    ],
  },
])

export default router
