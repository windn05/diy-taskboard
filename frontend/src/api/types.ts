// 백엔드 DTO와 1:1로 맞춘 응답 타입. 날짜는 ISO 문자열(yyyy-MM-dd / yyyy-MM-ddTHH:mm:ss)
export type User = { id: number; username: string; name: string; role: string }

/** 로그인 성공·세션 조회(GET /auth/me) 응답. 세션 쿠키는 JS가 못 읽으므로 서버가 내려주는 이 값으로 로그인 상태를 판단 */
export type SessionUser = { userId: number; username: string; role: string }

/** myRole: 내 프로젝트 역할(OWNER/ADMIN/MEMBER). 게스트는 GUEST */
export type Workspace = { id: number; name: string; myRole: string }

export type AdminWorkspace = { id: number; name: string; visible: boolean }

export type Member = { userId: number; username: string; name: string; role: string }

export type Status = { id: number; name: string; order: number }

export type CardTypeDef = { id: number; name: string }

export type Card = {
  id: number
  workspaceId: number
  statusId: number
  title: string
  description: string | null
  type: string
  startDate: string | null
  dueDate: string | null
  /** 값이 있으면 배포된(완료된) 작업 */
  releaseId: number | null
  commentCount: number
  createdAt: string
  updatedAt: string
}

export type Comment = {
  id: number
  cardId: number
  userId: number
  authorName: string
  content: string
  createdAt: string
}

export type PresenceUser = { userId: number; username: string; guest: boolean }

export type PresenceResponse = { users: PresenceUser[] }

/** 마감 임박 작업 한 줄 */
export type DueTask = {
  cardId: number
  workspaceId: number
  workspaceName: string
  title: string
  type: string
  dueDate: string | null
}

export type RecentRelease = {
  workspaceId: number
  workspaceName: string
  version: string
  releasedAt: string
  cardCount: number
}

/** 홈 달력의 막대. 시작일·마감일 중 하나만 있으면 하루짜리로 표시 */
export type CalendarTask = {
  cardId: number
  workspaceId: number
  workspaceName: string
  title: string
  startDate: string | null
  dueDate: string | null
}

/** 최근 등록된 작업. 마감 임박 작업이 없을 때도 홈이 비어 보이지 않게 하는 자리 */
export type RecentCard = {
  cardId: number
  workspaceId: number
  workspaceName: string
  title: string
  statusName: string
  type: string
  createdAt: string | null
}

export type Dashboard = {
  dueSoon: DueTask[]
  recentCards: RecentCard[]
  recentReleases: RecentRelease[]
  calendarTasks: CalendarTask[]
}

export type Schedule = {
  id: number
  title: string
  startDate: string
  dueDate: string
}

export type ReleasedCard = { id: number; title: string; type: string }

export type Release = {
  id: number
  workspaceId: number
  version: string
  notes: string | null
  releasedAt: string
  cards: ReleasedCard[]
}

export type LogLevel = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'

export type LogEntry = {
  level: LogLevel
  loggerName: string
  threadName: string
  message: string
  stackTrace: string | null
  loggedAt: string
}

export type MetricsResponse = {
  totalRequests: number
  totalErrors: number
  avgResponseMs: number
  webSocketSessions: number
}

/** 서버(VM) 전체 — 백엔드·DB·프록시가 함께 쓰는 자원 */
export type HostStats = {
  cpuPercent: number
  cpuCores: number
  memUsedMb: number
  memTotalMb: number
  diskUsedGb: number
  diskTotalGb: number
}

/** 백엔드 프로세스. 컨테이너 밖(로컬 등)에서 돌면 컨테이너 메모리는 null */
export type BackendStats = {
  processCpuPercent: number
  containerMemUsedMb: number | null
  containerMemLimitMb: number | null
  heapUsedMb: number
  heapMaxMb: number
  uptimeSeconds: number
}

/**
 * DB. 풀이 HikariCP가 아니거나 Postgres가 아닌 환경에서는 해당 항목이 null.
 * poolWaiting이 0보다 크면 커넥션을 못 얻어 대기 중인 요청이 있다는 의미
 */
export type DbStats = {
  poolActive: number | null
  poolIdle: number | null
  poolMax: number | null
  poolWaiting: number | null
  sizeMb: number | null
}

export type SystemStats = { host: HostStats; backend: BackendStats; db: DbStats }
