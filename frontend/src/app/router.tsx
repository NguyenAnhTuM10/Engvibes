import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import AuthGuard from '@/components/AuthGuard'

const HomePage = lazy(() => import('@/pages/HomePage'))
const LoginPage = lazy(() => import('@/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/RegisterPage'))

function PageLoader() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
    </div>
  )
}

const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <Suspense fallback={<PageLoader />}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: '/register',
    element: (
      <Suspense fallback={<PageLoader />}>
        <RegisterPage />
      </Suspense>
    ),
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <Suspense fallback={<PageLoader />}>
          <HomePage />
        </Suspense>
      </AuthGuard>
    ),
  },
])

export default router
