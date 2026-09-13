import { useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getLogHistory, getMetrics, getSystemStats, meetsLevel } from '../api/monitoring'
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

/** 이 이상이면 경고색 */
const DANGER_PERCENT = 85

const shortLogger = (name: string) => name.split('.').pop() ?? name
const timeOf = (iso: string) => iso.slice(11, 19)
const ratio = (used: number, total: number) => (total > 0 ? (used / total) * 100 : 0)

function formatUptime(seconds: number) {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return d > 0 ? `${d}일 ${h}시간` : `${h}시간 ${m}분`
}

/**
 * 화면을 크게 나누는 단위. 무엇을 기준으로 잰 값인지(scope)를 제목 옆에 붙인다 —
 * 같은 "메모리"라도 서버 전체와 백엔드 컨테이너는 전혀 다른 숫자라서.
 */
function Section({ title, scope, caption, children }: { title: string; scope: string; caption: string; children: ReactNode }) {
  return (
    <section>
      <header className="mb-4 flex flex-wrap items-baseline gap-x-2 gap-y-1 border-b border-slate-200 pb-2">
        <h2 className="text-base font-semibold text-slate-800">{title}</h2>
        <span className="rounded bg-slate-200 px-1.5 py-0.5 text-[10px] font-semibold tracking-wide text-slate-600">
          {scope}
        </span>
        <p className="text-xs text-slate-400">{caption}</p>
      </header>
      <div className="space-y-4">{children}</div>
    </section>
  )
}

function GroupLabel({ children }: { children: ReactNode }) {
  return <p className="text-xs font-medium text-slate-500">{children}</p>
}

function StatCard({ label, value, detail, tone }: { label: string; value: string; detail?: string; tone?: 'danger' }) {
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${tone === 'danger' ? 'text-red-600' : 'text-slate-800'}`}>{value}</p>
      {detail && <p className="text-xs text-slate-400">{detail}</p>}
    </div>
  )
}

function GaugeCard({ label, percent, detail }: { label: string; percent: number | null; detail: string }) {
  const danger = percent !== null && percent >= DANGER_PERCENT
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${danger ? 'text-red-600' : 'text-slate-800'}`}>
        {percent === null ? '-' : `${percent.toFixed(0)}%`}
      </p>
      <p className="text-xs text-slate-400">{detail}</p>
      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full ${danger ? 'bg-red-500' : 'bg-slate-700'}`}
          style={{ width: `${Math.min(percent ?? 0, 100)}%` }}
        />
      </div>
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
  const { data: system } = useQuery({ queryKey: ['system-stats'], queryFn: getSystemStats, refetchInterval: 5000 })
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
  const host = system?.host
  const backend = system?.backend
  const containerMem =
    backend && backend.containerMemUsedMb != null && backend.containerMemLimitMb != null
      ? { used: backend.containerMemUsedMb, limit: backend.containerMemLimitMb }
      : null

  return (
    <div className="space-y-10 p-8">
      {/* ── 서버 자원 ─────────────────────────────────────────── */}
      <Section title="서버 자원" scope="VM 전체" caption="백엔드·DB·프록시가 함께 쓰는 자원">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <GaugeCard
            label="CPU"
            percent={host ? host.cpuPercent : null}
            detail={host ? `vCPU ${host.cpuCores}개` : '불러오는 중'}
          />
          <GaugeCard
            label="메모리"
            percent={host ? ratio(host.memUsedMb, host.memTotalMb) : null}
            detail={host ? `${host.memUsedMb.toLocaleString()} / ${host.memTotalMb.toLocaleString()} MB` : '불러오는 중'}
          />
          <GaugeCard
            label="디스크"
            percent={host ? ratio(host.diskUsedGb, host.diskTotalGb) : null}
            detail={host ? `${host.diskUsedGb} / ${host.diskTotalGb} GB` : '불러오는 중'}
          />
        </div>
      </Section>

      {/* ── 백엔드 ───────────────────────────────────────────── */}
      <Section title="백엔드" scope="Spring Boot" caption="애플리케이션 트래픽과 프로세스 상태">
        <GroupLabel>트래픽 · 최근 60분</GroupLabel>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard label="요청 수" value={`${metrics?.totalRequests ?? 0}`} />
          <StatCard
            label="에러 응답"
            value={`${metrics?.totalErrors ?? 0}`}
            tone={metrics && metrics.totalErrors > 0 ? 'danger' : undefined}
          />
          <StatCard label="평균 응답시간" value={`${metrics?.avgResponseMs ?? 0}ms`} />
          <StatCard label="WebSocket 세션" value={`${metrics?.webSocketSessions ?? 0}`} />
        </div>

        <GroupLabel>프로세스</GroupLabel>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <GaugeCard
            label="프로세스 CPU"
            percent={backend ? backend.processCpuPercent : null}
            detail="서버 전체 CPU 중 백엔드 몫"
          />
          {containerMem ? (
            <GaugeCard
              label="컨테이너 메모리"
              percent={ratio(containerMem.used, containerMem.limit)}
              detail={`${containerMem.used.toLocaleString()} / ${containerMem.limit.toLocaleString()} MB (한도)`}
            />
          ) : (
            <StatCard
              label="컨테이너 메모리"
              value="-"
              detail={backend ? '컨테이너 밖에서 실행 중' : '불러오는 중'}
            />
          )}
          <GaugeCard
            label="JVM 힙"
            percent={backend ? ratio(backend.heapUsedMb, backend.heapMaxMb) : null}
            detail={backend ? `${backend.heapUsedMb.toLocaleString()} / ${backend.heapMaxMb.toLocaleString()} MB` : '불러오는 중'}
          />
          <StatCard label="가동 시간" value={backend ? formatUptime(backend.uptimeSeconds) : '-'} detail="마지막 재기동 이후" />
        </div>

        <div className="rounded-lg border bg-white p-4">
          <h3 className="mb-3 text-sm font-semibold text-slate-700">분당 요청 통계</h3>
          {metrics ? <RequestChart series={metrics.series} /> : <p className="text-sm text-slate-400">불러오는 중...</p>}
        </div>

        <div className="rounded-lg border bg-white">
          <div className="flex flex-wrap items-center gap-3 border-b px-4 py-2.5">
            <h3 className="text-sm font-semibold text-slate-700">로그</h3>

            {/* 이 연결 표시는 로그 스트림에만 해당한다. 위 수치들은 5초마다 따로 조회한다. */}
            <span className="flex items-center gap-1.5 text-xs text-slate-500">
              <span className={`inline-block h-2 w-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-slate-300'}`} />
              {connected ? '실시간 연결됨' : '연결 끊김'}
            </span>

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
                <span className="rounded-full bg-red-100 px-2 py-0.5 font-semibold text-red-700">ERROR {errorCount}</span>
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
      </Section>
    </div>
  )
}
