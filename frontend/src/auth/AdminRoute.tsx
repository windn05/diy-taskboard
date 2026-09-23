import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** 관리자 화면 가드. 화면만 막을 뿐이고 실제 권한은 서버(/admin/**)가 검사 */
export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/home" replace />
  return <>{children}</>
}
