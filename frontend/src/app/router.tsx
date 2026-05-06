import { createBrowserRouter } from 'react-router-dom'
import HomePage from '@/pages/HomePage'
import DemoPage from '@/pages/DemoPage'

const router = createBrowserRouter([
  {
    path: '/',
    element: <HomePage />,
  },
  {
    path: '/demo',
    element: <DemoPage />,
  },
])

export default router
