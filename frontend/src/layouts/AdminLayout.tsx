import { NavLink, Outlet } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'

const TABS = [
  { to: '/admin', label: '프로젝트 관리', end: true },
  { to: '/admin/users', label: '계정 관리', end: false },
  { to: '/admin/settings', label: '상태/타입 관리', end: false },
]

/** 관리자 앱의 셸. TaskBoard와 사이드바를 공유하지 않고, 탭은 URL로 직접 진입할 수 있다. */
export function AdminLayout() {
  return (
    <div className="flex h-screen flex-col">
      <AppHeader currentApp="admin" />

      <div className="shrink-0 border-b bg-white px-8 pt-5">
        <h1 className="mb-4 text-xl font-semibold">관리자</h1>
        <nav className="flex gap-6">
          {TABS.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `border-b-2 pb-3 text-sm font-medium ${
                  isActive
                    ? 'border-slate-900 text-slate-900'
                    : 'border-transparent text-slate-400 hover:text-slate-600'
                }`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </div>

      <main className="flex-1 overflow-y-auto bg-slate-50">
        <Outlet />
      </main>
    </div>
  )
}
