// 작업 유형. 조회는 누구나, 변경은 관리자만
import { api } from './client'
import type { CardTypeDef } from './types'

export const listCardTypes = () => api<CardTypeDef[]>('/card-types')

export const adminCreateCardType = (name: string) =>
  api<CardTypeDef>('/admin/card-types', { method: 'POST', body: JSON.stringify({ name }) })

export const adminUpdateCardType = (id: number, name: string) =>
  api<CardTypeDef>(`/admin/card-types/${id}`, { method: 'PATCH', body: JSON.stringify({ name }) })

export const adminDeleteCardType = (id: number) => api<void>(`/admin/card-types/${id}`, { method: 'DELETE' })
