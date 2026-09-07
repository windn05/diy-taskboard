import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getLogHistory, getMetrics, meetsLevel } from '../api/monitoring'
import type { LogEntry, LogLevel } from '../api/types'
import { RequestChart } from '../components/RequestChart'
import { useLogStream } from '../hooks/useLogStream'

const LEVEL_FILTERS: { label: string; value: LogLevel }[] = [
  { label: '전체', value: 'TRACE' },
  { label: 'INFO+', value: 'INFO' },
  { label: 'WARN+', value: 'WARN' },
  { label: 'ERROR', value: 'ERROR' },
]

const LEVEL_STYLE: Record<LogLevel, string> = {
  TRACE: 'bg-slate-100 text-slate-500',
  DEBUG: 'bg-slate-100 text-slate-500',
  INFO: 'bg-sky-100 text-sky-700',
  WARN: 'bg-amber-100 text-amber-700',
  ERROR: 'bg-red-100 text-red-700',
}

const shortLogger = (name: string) => name.split('.').pop() ?? name
const timeOf = (iso: string) => iso.slice(11, 19)

function SummaryCard({ label, value, tone }: { label: string; value: string; tone?: 'danger' }) {
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${tone === 'danger' ? 'text-red-600' : 'text-slate-800'}`}>{value}</p>
    </div>
  )
}

function LogRow({ entry }: { entry: LogEntry }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <div className="border-b px-3 py-1.5 last:border-0 hover:bg-slate-50">
      <div className="flex items-start gap-2">
        <span className="shrink-0 font-mono text-[11px] text-slate-400">{timeOf(entry.loggedAt)}</span>
        <span className={`shrink-0 rounded px-1.5 text-[10px] font-semibold ${LEVEL_STYLE[entry.level]}`}>
          {entry.level}
        </span>
        <span className="shrink-0 text-[11px] text-slate-400" title={entry.loggerName}>
          {shortLogger(entry.loggerName)}
        </span>
        <span className="min-w-0 flex-1 break-all font-mono text-[11px] text-slate-700">{entry.message}</span>
        {entry.stackTrace && (
          <button
            onClick={() => setExpanded((v) => !v)}
            className="shrink-0 text-[11px] text-slate-400 hover:text-slate-700"
          >
            {expanded ? '접기' : '스택'}
          </button>
        )}
      </div>
      {expanded && entry.stackTrace && (
        <pre className="mt-1 max-h-60 overflow-auto rounded bg-slate-900 p-2 text-[10px] leading-relaxed text-slate-100">
          {entry.stackTrace}
        </pre>
      )}
    </div>
  )
}

export function MonitoringPage() {
  const [level, setLevel] = useState<LogLevel>('TRACE')
  const [source, setSource] = useState<'live' | 'history'>('live')

  const { data: metrics } = useQuery({ queryKey: ['metrics'], queryFn: getMetrics, refetchInterval: 5000 })
  const { entries, connected } = useLogStream()
  const { data: history } = useQuery({
    queryKey: ['log-history'],
    queryFn: () => getLogHistory('WARN', 100),
    enabled: source === 'history',
  })

  const shown = useMemo(() => {
    const list = source === 'live' ? entries : (history ?? [])
    return list.filter((entry) => meetsLevel(entry.level, level))
  }, [source, entries, history, level])

  const errorCount = entries.filter((entry) => entry.level === 'ERROR').length

  return (
    <div className="p-8">
      <div className="mb-4 flex items-center justify-end">
        <span className="flex items-center gap-1.5 text-xs text-slate-500">
          <span className={`inline-block h-2 w-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-slate-300'}`} />
          {connected ? '실시간 연결됨' : '연결 끊김'}
        </span>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <SummaryCard label="요청 수 (최근 60분)" value={`${metrics?.totalRequests ?? 0}`} />
        <SummaryCard
          label="에러 응답"
          value={`${metrics?.totalErrors ?? 0}`}
          tone={metrics && metrics.totalErrors > 0 ? 'danger' : undefined}
        />
        <SummaryCard label="평균 응답시간" value={`${metrics?.avgResponseMs ?? 0}ms`} />
        <SummaryCard label="WebSocket 세션" value={`${metrics?.webSocketSessions ?? 0}`} />
      </div>

      <div className="mb-6 rounded-lg border bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">분당 요청 통계</h2>
        {metrics ? (
          <RequestChart series={metrics.series} />
        ) : (
          <p className="text-sm text-slate-400">불러오는 중...</p>
        )}
      </div>

      <div className="rounded-lg border bg-white">
        <div className="flex flex-wrap items-center gap-3 border-b px-4 py-2.5">
          <h2 className="text-sm font-semibold text-slate-700">서버 로그</h2>

          <div className="flex gap-1">
            {(['live', 'history'] as const).map((value) => (
              <button
                key={value}
                onClick={() => setSource(value)}
                className={`rounded px-2 py-0.5 text-xs ${
                  source === value ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-100'
                }`}
              >
                {value === 'live' ? '실시간' : '저장된 기록'}
              </button>
            ))}
          </div>

          <div className="flex gap-1">
            {LEVEL_FILTERS.map((filter) => (
              <button
                key={filter.value}
                onClick={() => setLevel(filter.value)}
                className={`rounded px-2 py-0.5 text-xs ${
                  level === filter.value ? 'bg-slate-200 text-slate-800' : 'text-slate-500 hover:bg-slate-100'
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>

          <span className="ml-auto flex items-center gap-2 text-xs text-slate-400">
            {errorCount > 0 && (
              <span className="rounded-full bg-red-100 px-2 py-0.5 font-semibold text-red-700">
                ERROR {errorCount}
              </span>
            )}
            {shown.length}건
          </span>
        </div>

        <div className="max-h-[420px] overflow-y-auto">
          {shown.map((entry, index) => (
            <LogRow key={`${entry.loggedAt}-${index}`} entry={entry} />
          ))}
          {shown.length === 0 && (
            <p className="px-4 py-8 text-center text-sm text-slate-400">
              {source === 'history' ? '저장된 로그가 없습니다.' : '수신된 로그가 없습니다.'}
            </p>
          )}
        </div>
      </div>
    </div>
  )
}
