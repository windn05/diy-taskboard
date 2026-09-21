// 모니터링 (관리자 전용)
import { api } from './client'
import type { LogEntry, LogLevel, MetricsResponse, SystemStats } from './types'

/** 낮은 레벨부터. 순서가 곧 심각도 */
export const LOG_LEVELS: LogLevel[] = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR']

/** level이 minLevel 이상인지 */
export function meetsLevel(level: LogLevel, minLevel: LogLevel) {
  return LOG_LEVELS.indexOf(level) >= LOG_LEVELS.indexOf(minLevel)
}

// 메모리 버퍼의 최근 로그 (재기동하면 사라짐)
export const getLogs = (level: LogLevel = 'TRACE', limit = 200) =>
  api<LogEntry[]>(`/admin/logs?level=${level}&limit=${limit}`)

// DB에 남은 WARN/ERROR 로그
export const getLogHistory = (level: LogLevel = 'WARN', limit = 100) =>
  api<LogEntry[]>(`/admin/logs/history?level=${level}&limit=${limit}`)

export const getMetrics = () => api<MetricsResponse>('/admin/metrics')

export const getSystemStats = () => api<SystemStats>('/admin/metrics/system')
