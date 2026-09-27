// 모니터링 (관리자 전용)
import { get } from './axios'
import type { LogEntry, LogLevel, MetricsResponse, SystemStats } from './types'

// 메모리 버퍼의 최근 로그 (재기동하면 사라짐)
export const getLogs = (level: LogLevel = 'TRACE', limit = 200) =>
  get<LogEntry[]>(`/admin/logs?level=${level}&limit=${limit}`)

// DB에 남은 WARN/ERROR 로그
export const getLogHistory = (level: LogLevel = 'WARN', limit = 100) =>
  get<LogEntry[]>(`/admin/logs/history?level=${level}&limit=${limit}`)

export const getMetrics = () => get<MetricsResponse>('/admin/metrics')

export const getSystemStats = () => get<SystemStats>('/admin/metrics/system')
