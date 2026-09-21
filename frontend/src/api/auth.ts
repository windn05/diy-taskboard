// 로그인·토큰
import { api } from './client'
import type { Tokens } from './types'

export const login = (data: { username: string; password: string }) =>
  api<Tokens>('/auth/login', { method: 'POST', body: JSON.stringify(data) })

export const guestLogin = () => api<Tokens>('/auth/guest', { method: 'POST' })

export const changePassword = (data: { currentPassword: string; newPassword: string }) =>
  api<void>('/auth/password', { method: 'POST', body: JSON.stringify(data) })

/**
 * 토큰 payload에서 사용자 정보 추출. 서명 검증은 하지 않음 —
 * 화면 표시용일 뿐이고, 실제 권한 판단은 서버가 매 요청마다 수행
 */
export function decodeAccessToken(token: string): { userId: number; username: string; role: string } | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { userId: payload.userId, username: payload.sub, role: payload.role }
  } catch {
    return null
  }
}
