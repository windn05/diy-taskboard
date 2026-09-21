// 작업 상태(칸반 컬럼). 조회는 누구나, 변경은 관리자만
import { api } from './client'
import type { Status } from './types'

export const listStatuses = () => api<Status[]>('/statuses')

export const adminCreateStatus = (name: string) =>
  api<Status>('/admin/statuses', { method: 'POST', body: JSON.stringify({ name }) })

export const adminRenameStatus = (id: number, name: string) =>
  api<Status>(`/admin/statuses/${id}`, { method: 'PATCH', body: JSON.stringify({ name }) })

export const adminDeleteStatus = (id: number) => api<void>(`/admin/statuses/${id}`, { method: 'DELETE' })
