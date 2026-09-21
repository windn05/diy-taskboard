import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { availableApps, type AppKey } from '../apps'
import { useAuth } from '../auth/AuthContext'
import { ChangePasswordModal } from './ChangePasswordModal'
import { NotificationBell } from './NotificationBell'
import { ChevronDownIcon } from './icons'
import { PresenceBar } from './PresenceBar'
import type { PresenceUser } from '../api/types'

/** 모든 앱이 공유하는 상단 헤더. 앱 전환은 오직 여기 런처로만 한다. */
export function AppHeader({
  currentApp,
  presentUsers,
}: {
  currentApp: AppKey
  /** 프로젝트 화면에서만 넘어온다. 관리자 앱에는 접속자 개념이 없다. */
  presentUsers?: PresenceUser[]
}) {
  const { user, isGuest, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const launcherRef = useRef<HTMLDivElement>(null)

  const apps = availableApps(user?.role)
  const current = apps.find((app) => app.key === currentApp)
  const CurrentIcon = current?.Icon

  useEffect(() => {
    if (!open) return
    function onPointerDown(event: MouseEvent) {
      if (!launcherRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b bg-white px-6">
      <div className="relative" ref={launcherRef}>
        <button
          onClick={() => setOpen((v) => !v)}
          className="flex items-center gap-2 rounded px-2 py-1 text-sm font-semibold text-slate-800 hover:bg-slate-100"
          title="앱 전환"
        >
          {CurrentIcon && <CurrentIcon size={16} className="text-slate-500" />}
          {current?.label}
          <ChevronDownIcon size={12} className="text-slate-500" />
        </button>

        {open && (
          <div className="absolute left-0 top-full z-50 mt-1 w-48 rounded-lg border bg-white p-1 shadow-lg">
            {apps.map((app) => (
              <button
                key={app.key}
                disabled={app.comingSoon}
                onClick={() => {
                  setOpen(false)
                  navigate(app.path)
                }}
                className={`flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm ${
                  app.comingSoon
                    ? 'cursor-not-allowed text-slate-300'
                    : app.key === currentApp
                      ? 'bg-slate-900 text-white'
                      : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <app.Icon size={16} />
                <span className="flex-1">{app.label}</span>
                {app.comingSoon && <span className="text-[9px]">준비중</span>}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="flex items-center gap-3 text-sm">
        {presentUsers && presentUsers.length > 0 && (
          <>
            <PresenceBar users={presentUsers} meId={user?.userId} />
            <span className="h-4 w-px bg-slate-200" />
          </>
        )}
        <NotificationBell />
        <span className="text-slate-600">{user?.username}</span>
        {isGuest ? (
          // 흰 배경이 아니라 slate-100 칩 위에 얹히므로 한 단계 더 진하게 가야 4.5:1을 넘는다.
          <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">게스트 · 읽기 전용</span>
        ) : (
          // 게스트는 저장되는 계정이 아니라 바꿀 비밀번호가 없다.
          <button
            onClick={() => setChangingPassword(true)}
            className="text-xs text-slate-500 hover:text-slate-700"
          >
            비밀번호 변경
          </button>
        )}
        <button onClick={logout} className="text-slate-500 hover:text-slate-700">
          로그아웃
        </button>
      </div>

      {changingPassword && <ChangePasswordModal onClose={() => setChangingPassword(false)} />}
    </header>
  )
}
