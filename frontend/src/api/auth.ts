// 로그인·로그아웃(세션)
import { get, post } from './axios'
import type { SessionUser } from './types'

export const login = (data: { username: string; password: string }) => post<SessionUser>('/auth/login', data)

export const guestLogin = () => post<SessionUser>('/auth/guest')

export const changePassword = (data: { currentPassword: string; newPassword: string }) =>
  post<void>('/auth/password', data)

/** 새로고침 시 로그인 상태 복원용. 세션 쿠키가 없거나 만료됐으면 401 */
export const getMe = () => get<SessionUser>('/auth/me')

export const logout = () => post<void>('/auth/logout')
