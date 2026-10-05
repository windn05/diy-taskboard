import type { ComponentType, SVGProps } from 'react'
import { AdminIcon, BoardIcon, MonitorIcon } from './components/icons'

/**
 * 이 서비스가 제공하는 앱 목록. 로그인 화면의 런처와 헤더의 앱 전환 메뉴가 같은 정의를 사용.
 * 앱끼리는 셸(사이드바·네비게이션)을 공유하지 않고, 이 런처로만 이동
 */
export type AppKey = 'taskboard' | 'admin' | 'monitoring'

export type AppDefinition = {
  key: AppKey
  label: string
  Icon: ComponentType<SVGProps<SVGSVGElement> & { size?: number }>
  path: string
  /** 관리자만 보이고 진입 가능한 앱 */
  adminOnly?: boolean
}

export const APPS: AppDefinition[] = [
  { key: 'taskboard', label: 'TaskBoard', Icon: BoardIcon, path: '/home' },
  { key: 'admin', label: '관리자', Icon: AdminIcon, path: '/admin', adminOnly: true },
  { key: 'monitoring', label: '모니터링', Icon: MonitorIcon, path: '/monitoring', adminOnly: true },
]

/** 역할에 따라 보여줄 앱 목록 */
export function availableApps(role: string | undefined) {
  return APPS.filter((app) => !app.adminOnly || role === 'ADMIN')
}
