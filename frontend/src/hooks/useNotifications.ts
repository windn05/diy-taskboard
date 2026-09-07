import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createStompClient } from '../api/stomp'
import { listNotifications } from '../api/notifications'
import type { Notification, NotificationList } from '../api/types'

/**
 * 내 알림. 목록은 REST로 가져오고, 이후 새 알림은 개인 채널로 푸시받는다.
 * 헤더에서 쓰이므로 어느 앱에 있든 알림이 도착한다.
 */
export function useNotifications(userId: number | undefined, enabled: boolean) {
  const queryClient = useQueryClient()

  const { data } = useQuery({
    queryKey: ['notifications'],
    queryFn: listNotifications,
    enabled,
  })

  useEffect(() => {
    if (!enabled || userId === undefined) return

    const client = createStompClient()
    if (!client) return

    client.onConnect = () => {
      client.subscribe(`/topic/users/${userId}/notifications`, (message) => {
        const incoming: Notification = JSON.parse(message.body)
        queryClient.setQueryData<NotificationList>(['notifications'], (prev) => {
          if (!prev) return prev
          if (prev.notifications.some((n) => n.id === incoming.id)) return prev
          return {
            notifications: [incoming, ...prev.notifications],
            unreadCount: prev.unreadCount + 1,
          }
        })
      })
    }

    client.activate()
    return () => {
      client.deactivate()
    }
  }, [enabled, userId, queryClient])

  return {
    notifications: data?.notifications ?? [],
    unreadCount: data?.unreadCount ?? 0,
  }
}
