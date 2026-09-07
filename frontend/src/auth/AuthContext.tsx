import { createContext, useContext, useState, type ReactNode } from 'react'
import { decodeAccessToken, guestLogin as guestLoginApi, login as loginApi } from '../api/auth'
import { getToken, setToken } from '../api/client'

type CurrentUser = { userId: number; username: string; role: string }

type AuthContextValue = {
  user: CurrentUser | null
  isGuest: boolean
  login: (username: string, password: string) => Promise<void>
  loginAsGuest: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(() => {
    const token = getToken()
    return token ? decodeAccessToken(token) : null
  })

  async function login(username: string, password: string) {
    const tokens = await loginApi({ username, password })
    setToken(tokens.accessToken)
    setUser(decodeAccessToken(tokens.accessToken))
  }

  async function loginAsGuest() {
    const tokens = await guestLoginApi()
    setToken(tokens.accessToken)
    setUser(decodeAccessToken(tokens.accessToken))
  }

  function logout() {
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{ user, isGuest: user?.role === 'GUEST', login, loginAsGuest, logout }}
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
