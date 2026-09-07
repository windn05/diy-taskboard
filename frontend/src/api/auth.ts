import { api } from './client'
import type { Tokens, User } from './types'

export const signup = (data: { username: string; password: string; name: string }) =>
  api<User>('/auth/signup', { method: 'POST', body: JSON.stringify(data) })

export const login = (data: { username: string; password: string }) =>
  api<Tokens>('/auth/login', { method: 'POST', body: JSON.stringify(data) })

export const guestLogin = () => api<Tokens>('/auth/guest', { method: 'POST' })

export function decodeAccessToken(token: string): { userId: number; username: string; role: string } | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { userId: payload.userId, username: payload.sub, role: payload.role }
  } catch {
    return null
  }
}
