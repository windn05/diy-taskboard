import { api } from './client'
import type { LogEntry, LogLevel, MetricsResponse, SystemStats } from './types'

export const LOG_LEVELS: LogLevel[] = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR']

export function meetsLevel(level: LogLevel, minLevel: LogLevel) {
  return LOG_LEVELS.indexOf(level) >= LOG_LEVELS.indexOf(minLevel)
}

export const getLogs = (level: LogLevel = 'TRACE', limit = 200) =>
  api<LogEntry[]>(`/admin/logs?level=${level}&limit=${limit}`)

export const getLogHistory = (level: LogLevel = 'WARN', limit = 100) =>
  api<LogEntry[]>(`/admin/logs/history?level=${level}&limit=${limit}`)

export const getMetrics = () => api<MetricsResponse>('/admin/metrics')

export const getSystemStats = () => api<SystemStats>('/admin/metrics/system')
