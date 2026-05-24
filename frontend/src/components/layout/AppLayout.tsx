import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import {
  Home,
  Play,
  BookOpen,
  TrendingUp,
  History,
  User,
  Shield,
  ChevronLeft,
  ChevronRight,
  Mic,
  MessageSquare,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import AppHeader from './AppHeader'
import { useAuthStore } from '@/features/auth/store'

interface NavItem {
  label: string
  href: string
  icon: React.ReactNode
  adminOnly?: boolean
}

const navItems: NavItem[] = [
  { label: 'Home', href: '/', icon: <Home className="h-4 w-4" /> },
  { label: 'Videos', href: '/videos', icon: <Play className="h-4 w-4" /> },
  { label: 'Speaking', href: '/speak', icon: <Mic className="h-4 w-4" /> },
  { label: 'Conversation', href: '/conversation', icon: <MessageSquare className="h-4 w-4" /> },
  { label: 'Decks', href: '/decks', icon: <BookOpen className="h-4 w-4" /> },
  { label: 'Progress', href: '/progress', icon: <TrendingUp className="h-4 w-4" /> },
  { label: 'History', href: '/history', icon: <History className="h-4 w-4" /> },
  { label: 'Profile', href: '/profile', icon: <User className="h-4 w-4" /> },
  { label: 'Admin', href: '/admin/videos', icon: <Shield className="h-4 w-4" />, adminOnly: true },
]

function SidebarContent({ collapsed }: { collapsed: boolean }) {
  const user = useAuthStore((s) => s.user)
  const visibleItems = navItems.filter((item) => !item.adminOnly || user?.role === 'ADMIN')

  return (
    <nav className="flex flex-col gap-1 px-2 py-4">
      {visibleItems.map((item) => (
        <NavLink
          key={item.href}
          to={item.href}
          end={item.href === '/'}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              'hover:bg-accent hover:text-accent-foreground',
              isActive
                ? 'bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground'
                : 'text-muted-foreground',
              collapsed && 'justify-center px-2',
            )
          }
          title={collapsed ? item.label : undefined}
        >
          {item.icon}
          {!collapsed && <span>{item.label}</span>}
        </NavLink>
      ))}
    </nav>
  )
}

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Desktop sidebar */}
      <aside
        className={cn(
          'hidden md:flex flex-col border-r bg-background transition-all duration-200 shrink-0',
          collapsed ? 'w-16' : 'w-60',
        )}
      >
        <div className={cn('flex items-center h-14 px-4 border-b', collapsed && 'justify-center px-2')}>
          {!collapsed && (
            <span className="font-bold text-sm text-foreground truncate">English Learning</span>
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          <SidebarContent collapsed={collapsed} />
        </div>

        <Separator />
        <div className="p-2 flex justify-end">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setCollapsed(!collapsed)}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </Button>
        </div>
      </aside>

      {/* Mobile sidebar drawer */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="w-60 p-0">
          <SheetTitle className="sr-only">Navigation menu</SheetTitle>
          <div className="flex items-center h-14 px-4 border-b">
            <span className="font-bold text-sm">English Learning</span>
          </div>
          <SidebarContent collapsed={false} />
        </SheetContent>
      </Sheet>

      {/* Main content */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <AppHeader onMobileMenuToggle={() => setMobileOpen(true)} />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
