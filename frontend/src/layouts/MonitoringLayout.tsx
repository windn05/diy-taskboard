import { Outlet } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'

/** 모니터링 앱의 셸. 관리자 앱과 셸을 공유하지 않는다 — 탭 없이 페이지 하나뿐이라 헤더만 둔다. */
export function MonitoringLayout() {
  return (
    <div className="flex h-screen flex-col">
      <AppHeader currentApp="monitoring" />
      <main className="flex-1 overflow-y-auto bg-slate-50">
        <Outlet />
      </main>
    </div>
  )
}
