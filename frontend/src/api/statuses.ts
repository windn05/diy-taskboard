// 작업 상태(칸반 컬럼). 조회는 누구나, 변경은 관리자만
import { del, get, patch, post } from './axios'
import type { Status } from './types'

export const listStatuses = () => get<Status[]>('/statuses')

export const adminCreateStatus = (name: string) => post<Status>('/admin/statuses', { name })

export const adminRenameStatus = (id: number, name: string) => patch<Status>(`/admin/statuses/${id}`, { name })

export const adminDeleteStatus = (id: number) => del<void>(`/admin/statuses/${id}`)
