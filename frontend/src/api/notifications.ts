// 본인 알림
import { get, patch, post } from './axios'
import type { NotificationList } from './types'

export const listNotifications = () => get<NotificationList>('/notifications')

export const markNotificationRead = (id: number) => patch<void>(`/notifications/${id}/read`)

export const markAllNotificationsRead = () => post<void>('/notifications/read-all')
