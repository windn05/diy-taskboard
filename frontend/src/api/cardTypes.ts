// 작업 유형. 조회는 누구나, 변경은 관리자만
import { del, get, patch, post } from './axios'
import type { CardTypeDef } from './types'

export const listCardTypes = () => get<CardTypeDef[]>('/card-types')

export const adminCreateCardType = (name: string) => post<CardTypeDef>('/admin/card-types', { name })

export const adminUpdateCardType = (id: number, name: string) => patch<CardTypeDef>(`/admin/card-types/${id}`, { name })

export const adminDeleteCardType = (id: number) => del<void>(`/admin/card-types/${id}`)
