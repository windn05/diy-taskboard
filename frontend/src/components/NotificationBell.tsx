import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { markAllNotificationsRead, markNotificationRead } from '../api/notifications'
import { useAuth } from '../auth/AuthContext'
import { useNotifications } from '../hooks/useNotifications'
import type { Notification } from '../api/types'
import { BellIcon } from './icons'

function timeAgo(iso: string) {
  const diffMinutes = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (diffMinutes < 1) return '방금'
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffMinutes < 60 * 24) return `${Math.floor(diffMinutes / 60)}시간 전`
  return `${Math.floor(diffMinutes / 1440)}일 전`
}

export function NotificationBell() {
  const { user, isGuest } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // 게스트는 담당자가 될 수 없어 받을 알림이 없다.
  const enabled = !!user && !isGuest
  const { notifications, unreadCount } = useNotifications(user?.userId, enabled)

  useEffect(() => {
    if (!open) return
    function onPointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })

  const readMutation = useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate })
  const readAllMutation = useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate })

  if (!enabled) return null

  function handleClick(notification: Notification) {
    setOpen(false)
    if (!notification.read) readMutation.mutate(notification.id)
    // 알림이 가리키는 작업을 바로 열어준다.
    navigate(`/projects/${notification.workspaceId}?card=${notification.cardId}`)
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative rounded p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
        title="알림"
      >
        <BellIcon size={18} />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 w-80 rounded-lg border bg-white shadow-lg">
          <div className="flex items-center justify-between border-b px-3 py-2">
            <span className="text-sm font-semibold text-slate-700">알림</span>
            {unreadCount > 0 && (
              <button
                onClick={() => readAllMutation.mutate()}
                className="text-xs text-slate-400 hover:text-slate-700"
              >
                모두 읽음
              </button>
            )}
          </div>

          <ul className="max-h-80 overflow-y-auto">
            {notifications.map((notification) => (
              <li key={notification.id}>
                <button
                  onClick={() => handleClick(notification)}
                  className={`flex w-full flex-col items-start gap-0.5 border-b px-3 py-2 text-left last:border-0 hover:bg-slate-50 ${
                    notification.read ? '' : 'bg-sky-50/60'
                  }`}
                >
                  <span className="text-sm text-slate-700">
                    <b>{notification.actorName}</b>님이 <b>{notification.cardTitle}</b>에 댓글을 남겼습니다
                  </span>
                  <span className="text-[11px] text-slate-400">{timeAgo(notification.createdAt)}</span>
                </button>
              </li>
            ))}
            {notifications.length === 0 && (
              <li className="px-3 py-8 text-center text-sm text-slate-400">알림이 없습니다.</li>
            )}
          </ul>
        </div>
      )}
    </div>
  )
}
