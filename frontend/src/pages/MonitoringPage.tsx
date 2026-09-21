import { useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getLogHistory, getMetrics, getSystemStats } from '../api/monitoring'
import type { LogEntry, LogLevel } from '../api/types'
import { useLogStream } from '../hooks/useLogStream'

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
 * 지표 묶음. 무엇을 기준으로 잰 값인지를 제목 옆에 붙인다 — 같은 "메모리"라도 서버 전체(VM)와
 * 백엔드 컨테이너는 전혀 다른 숫자라서.
 */
function MetricBox({ title, scope, children }: { title: string; scope: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border bg-white px-3 py-2">
      <header className="mb-1.5 flex items-baseline gap-2">
        <h2 className="text-sm font-semibold text-slate-800">{title}</h2>
        <span className="text-[11px] text-slate-500">{scope}</span>
      </header>
      <div className="space-y-1">{children}</div>
    </section>
  )
}

/** 사용률 한 줄: 이름 · 게이지 · 퍼센트 · 부연. percent가 null이면 아직 값이 없는 것이다. */
function GaugeRow({ label, percent, detail }: { label: string; percent: number | null; detail: string }) {
  const danger = percent !== null && percent >= DANGER_PERCENT
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-24 shrink-0 whitespace-nowrap text-slate-600">{label}</span>
      <div className="h-1.5 min-w-8 flex-1 overflow-hidden rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full ${danger ? 'bg-red-500' : 'bg-slate-700'}`}
          style={{ width: `${Math.min(percent ?? 0, 100)}%` }}
        />
      </div>
      <span className={`w-9 shrink-0 text-right font-semibold ${danger ? 'text-red-600' : 'text-slate-800'}`}>
        {percent === null ? '-' : `${percent.toFixed(0)}%`}
      </span>
      <span className="w-28 shrink-0 truncate text-[11px] text-slate-500" title={detail}>
        {detail}
      </span>
    </div>
  )
}

/** 수치 한 줄: 이름 · 값. */
function ValueRow({ label, value, detail, tone }: { label: string; value: string; detail?: string; tone?: 'danger' }) {
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-24 shrink-0 whitespace-nowrap text-slate-600">{label}</span>
      <span className={`font-semibold ${tone === 'danger' ? 'text-red-600' : 'text-slate-800'}`}>{value}</span>
      {detail && (
        <span className="truncate text-[11px] text-slate-500" title={detail}>
          {detail}
        </span>
      )}
    </div>
  )
}

function LogRow({ entry }: { entry: LogEntry }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <div className="border-b px-3 py-1.5 last:border-0 hover:bg-slate-50">
      <div className="flex items-center gap-2 text-[11px] text-slate-500">
        <span className="shrink-0 font-mono">{timeOf(entry.loggedAt)}</span>
        <span className="min-w-0 truncate" title={entry.loggerName}>
          {shortLogger(entry.loggerName)}
        </span>
        {entry.stackTrace && (
          <button onClick={() => setExpanded((v) => !v)} className="ml-auto shrink-0 hover:text-slate-800">
            {expanded ? '접기' : '스택'}
          </button>
        )}
      </div>
      <p className="break-all font-mono text-[11px] leading-snug text-slate-700">{entry.message}</p>
      {expanded && entry.stackTrace && (
        <pre className="mt-1 max-h-60 overflow-auto rounded bg-slate-900 p-2 text-[10px] leading-relaxed text-slate-100">
          {entry.stackTrace}
        </pre>
      )}
    </div>
  )
}

/** 레벨 하나가 자기 영역을 갖는다. 목록만 안에서 스크롤되고, 헤더 색이 어느 레벨인지 알려준다. */
function LogPanel({ title, accent, entries, empty }: { title: string; accent: string; entries: LogEntry[]; empty: string }) {
  return (
    <section className="flex h-72 min-h-0 flex-col overflow-hidden rounded-lg border bg-white md:h-auto">
      <header className={`flex shrink-0 items-center gap-2 border-b border-t-2 px-3 py-1.5 ${accent}`}>
        <h2 className="text-sm font-semibold text-slate-800">{title}</h2>
        <span className="ml-auto text-xs text-slate-500">{entries.length}건</span>
      </header>
      <div className="min-h-0 flex-1 overflow-y-auto">
        {entries.map((entry, index) => (
          <LogRow key={`${entry.loggedAt}-${index}`} entry={entry} />
        ))}
        {entries.length === 0 && <p className="px-4 py-6 text-center text-sm text-slate-500">{empty}</p>}
      </div>
    </section>
  )
}

const INFO_AND_BELOW: LogLevel[] = ['TRACE', 'DEBUG', 'INFO']

/** 실시간 버퍼와 DB 기록에 같은 항목이 모두 있을 수 있어 합치면서 중복을 뺀다. 최신이 위. */
function mergeLogs(live: LogEntry[], saved: LogEntry[], level: LogLevel) {
  const seen = new Set<string>()
  return [...live, ...saved]
    .filter((entry) => entry.level === level)
    .filter((entry) => {
      const key = `${entry.loggedAt}|${entry.loggerName}|${entry.message}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .sort((a, b) => b.loggedAt.localeCompare(a.loggedAt))
}

export function MonitoringPage() {
  const { data: metrics } = useQuery({ queryKey: ['metrics'], queryFn: getMetrics, refetchInterval: 5000 })
  const { data: system } = useQuery({ queryKey: ['system-stats'], queryFn: getSystemStats, refetchInterval: 5000 })
  const { entries, connected } = useLogStream()
  // WARN/ERROR는 DB에 남은 기록(재시작 전 것 포함)도 함께 보여준다. 새로 쌓이는 것도 따라오도록 주기적으로 다시 읽는다.
  const { data: history } = useQuery({
    queryKey: ['log-history'],
    queryFn: () => getLogHistory('WARN', 100),
    refetchInterval: 10000,
  })

  const infoLogs = useMemo(() => entries.filter((entry) => INFO_AND_BELOW.includes(entry.level)), [entries])
  const warnLogs = useMemo(() => mergeLogs(entries, history ?? [], 'WARN'), [entries, history])
  const errorLogs = useMemo(() => mergeLogs(entries, history ?? [], 'ERROR'), [entries, history])

  const host = system?.host
  const backend = system?.backend
  const db = system?.db
  const containerMem =
    backend && backend.containerMemUsedMb != null && backend.containerMemLimitMb != null
      ? { used: backend.containerMemUsedMb, limit: backend.containerMemLimitMb }
      : null

  return (
    // 한 화면에 담는다: 위는 지표(작게), 아래는 INFO · WARN · ERROR 세 영역. 페이지는 스크롤되지 않고 로그 목록만 각자 안에서 스크롤된다.
    <div className="flex flex-col gap-3 p-4 md:h-full">
      {/* 박스가 4개라 중간 폭에서는 2×2로 접고, 넓을 때만 한 줄에 편다. */}
      <div className="grid shrink-0 grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
        <MetricBox title="서버" scope="VM 전체">
          <GaugeRow
            label="CPU"
            percent={host ? host.cpuPercent : null}
            detail={host ? `vCPU ${host.cpuCores}개` : '불러오는 중'}
          />
          <GaugeRow
            label="메모리"
            percent={host ? ratio(host.memUsedMb, host.memTotalMb) : null}
            detail={host ? `${host.memUsedMb.toLocaleString()} / ${host.memTotalMb.toLocaleString()} MB` : '불러오는 중'}
          />
          <GaugeRow
            label="디스크"
            percent={host ? ratio(host.diskUsedGb, host.diskTotalGb) : null}
            detail={host ? `${host.diskUsedGb} / ${host.diskTotalGb} GB` : '불러오는 중'}
          />
        </MetricBox>

        <MetricBox title="백엔드" scope="Spring Boot 프로세스">
          <GaugeRow
            label="프로세스 CPU"
            percent={backend ? backend.processCpuPercent : null}
            detail="서버 전체 중 몫"
          />
          {containerMem ? (
            <GaugeRow
              label="컨테이너 메모리"
              percent={ratio(containerMem.used, containerMem.limit)}
              detail={`${containerMem.used.toLocaleString()} / ${containerMem.limit.toLocaleString()} MB`}
            />
          ) : (
            <ValueRow label="컨테이너 메모리" value="-" detail={backend ? '컨테이너 밖에서 실행 중' : '불러오는 중'} />
          )}
          <GaugeRow
            label="JVM 힙"
            percent={backend ? ratio(backend.heapUsedMb, backend.heapMaxMb) : null}
            detail={backend ? `${backend.heapUsedMb.toLocaleString()} / ${backend.heapMaxMb.toLocaleString()} MB` : '불러오는 중'}
          />
        </MetricBox>

        <MetricBox title="DB" scope="PostgreSQL">
          {db && db.poolMax != null && db.poolActive != null ? (
            <GaugeRow
              label="커넥션 풀"
              percent={ratio(db.poolActive, db.poolMax)}
              detail={`사용 ${db.poolActive} · 유휴 ${db.poolIdle ?? 0} / ${db.poolMax}`}
            />
          ) : (
            <ValueRow label="커넥션 풀" value="-" detail={db ? 'HikariCP가 아님' : '불러오는 중'} />
          )}
          {/* CPU·메모리가 멀쩡해도 여기가 0을 넘으면 요청이 커넥션을 못 얻고 멈춰 있는 것이다. */}
          <ValueRow
            label="대기 중인 요청"
            value={db?.poolWaiting != null ? `${db.poolWaiting}` : '-'}
            detail={db?.poolWaiting ? '커넥션을 기다리는 중' : undefined}
            tone={db?.poolWaiting ? 'danger' : undefined}
          />
          <ValueRow
            label="DB 용량"
            value={db?.sizeMb != null ? `${db.sizeMb.toLocaleString()} MB` : '-'}
            detail={db && db.sizeMb == null ? 'PostgreSQL이 아님' : undefined}
          />
        </MetricBox>

        <MetricBox title="트래픽" scope="최근 60분">
          <div className="grid grid-cols-2 gap-x-4 gap-y-1">
            <ValueRow label="요청 수" value={`${metrics?.totalRequests ?? 0}`} />
            <ValueRow
              label="에러 응답"
              value={`${metrics?.totalErrors ?? 0}`}
              tone={metrics && metrics.totalErrors > 0 ? 'danger' : undefined}
            />
            <ValueRow label="응답시간" value={`${metrics?.avgResponseMs ?? 0}ms`} />
            <ValueRow label="WS 세션" value={`${metrics?.webSocketSessions ?? 0}`} />
            <ValueRow label="가동 시간" value={backend ? formatUptime(backend.uptimeSeconds) : '-'} />
          </div>
        </MetricBox>
      </div>

      <div className="flex min-h-0 flex-1 flex-col gap-2">
        <div className="flex shrink-0 items-center gap-2 text-xs text-slate-500">
          <h2 className="text-sm font-semibold text-slate-800">로그</h2>
          {/* 이 연결 표시는 로그 스트림에만 해당한다. 위 수치들은 5초마다 따로 조회한다. */}
          <span className={`inline-block h-2 w-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-slate-300'}`} />
          {connected ? '실시간 연결됨' : '연결 끊김'}
        </div>
        <div className="grid min-h-0 flex-1 grid-cols-1 gap-3 md:grid-cols-3">
          <LogPanel title="INFO" accent="border-t-sky-500" entries={infoLogs} empty="수신된 로그가 없습니다." />
          <LogPanel title="WARN" accent="border-t-amber-500" entries={warnLogs} empty="경고가 없습니다." />
          <LogPanel title="ERROR" accent="border-t-red-500" entries={errorLogs} empty="에러가 없습니다." />
        </div>
      </div>
    </div>
  )
}
