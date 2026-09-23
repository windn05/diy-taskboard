import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** 로그인한 사용자(게스트 포함)만 통과. 세션 쿠키는 JS가 못 읽어 GET /auth/me 응답을 기다려야 하므로,
 * 그 사이(loading)엔 아직 모르는 것이지 "로그인 안 함"이 아니다 — 이때 리다이렉트하면 새로고침마다 튕겨나간다 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}
