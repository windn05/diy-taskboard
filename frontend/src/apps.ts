import type { ComponentType, SVGProps } from 'react'
import { AdminIcon, BoardIcon, MonitorIcon, SettingsIcon } from './components/icons'

/**
 * 이 서비스가 제공하는 앱 목록. 로그인 화면의 런처와 헤더의 앱 전환 메뉴가 같은 정의를 쓴다.
 * 앱끼리는 셸(사이드바·네비게이션)을 공유하지 않고, 오직 이 런처로만 오간다.
 */
export type AppKey = 'taskboard' | 'admin' | 'monitoring' | 'settings'

export type AppDefinition = {
  key: AppKey
  label: string
  Icon: ComponentType<SVGProps<SVGSVGElement> & { size?: number }>
  path: string
  /** 관리자만 보이고 진입할 수 있는 앱 */
  adminOnly?: boolean
  /** 아직 만들지 않은 자리 */
  comingSoon?: boolean
}

export const APPS: AppDefinition[] = [
  { key: 'taskboard', label: 'TaskBoard', Icon: BoardIcon, path: '/home' },
  { key: 'admin', label: '관리자', Icon: AdminIcon, path: '/admin', adminOnly: true },
  { key: 'monitoring', label: '모니터링', Icon: MonitorIcon, path: '/monitoring', adminOnly: true },
  { key: 'settings', label: '설정', Icon: SettingsIcon, path: '/settings', comingSoon: true },
]

export function availableApps(role: string | undefined) {
  return APPS.filter((app) => !app.adminOnly || role === 'ADMIN')
}
