import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import {
  getMe,
  guestLogin as guestLoginApi,
  login as loginApi,
  logout as logoutApi,
} from '../api/auth'
import type { SessionUser } from '../api/types'

type AuthContextValue = {
  user: SessionUser | null
  /** /auth/me 응답을 기다리는 동안(true) ProtectedRoute가 섣불리 /login으로 보내지 않게 함 */
  loading: boolean
  isGuest: boolean
  login: (username: string, password: string) => Promise<void>
  loginAsGuest: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * 로그인 상태. 세션 쿠키는 httpOnly라 JS가 못 읽으므로, 새로고침 때마다 GET /auth/me로
 * 서버에 물어봐서 로그인 상태를 복원한다(로그인 안 돼 있으면 401 — 정상적인 "비로그인" 응답)
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(username: string, password: string) {
    setUser(await loginApi({ username, password }))
  }

  async function loginAsGuest() {
    setUser(await guestLoginApi())
  }

  function logout() {
    setUser(null)
    // 실패해도(이미 만료 등) 프론트 상태는 이미 로그아웃이니 무시
    logoutApi().catch(() => {})
  }

  return (
    <AuthContext.Provider
      value={{ user, loading, isGuest: user?.role === 'GUEST', login, loginAsGuest, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
