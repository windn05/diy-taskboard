// 홈 화면 데이터
import { api } from './client'
import type { Dashboard } from './types'

export const getDashboard = () => api<Dashboard>('/dashboard')
