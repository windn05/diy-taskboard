// 본인 알림
import { api } from './client'
import type { NotificationList } from './types'

export const listNotifications = () => api<NotificationList>('/notifications')

export const markNotificationRead = (id: number) =>
  api<void>(`/notifications/${id}/read`, { method: 'PATCH' })

export const markAllNotificationsRead = () => api<void>('/notifications/read-all', { method: 'POST' })
