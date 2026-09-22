// 홈 화면 데이터
import { get } from './axios'
import type { Dashboard } from './types'

export const getDashboard = () => get<Dashboard>('/dashboard')
