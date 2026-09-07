export type User = { id: number; username: string; name: string; role: string }

export type Tokens = { accessToken: string; refreshToken: string }

export type Workspace = { id: number; name: string; ownerId: number; myRole: string }

export type AdminWorkspace = { id: number; name: string; ownerId: number; visible: boolean }

export type Member = { userId: number; username: string; name: string; role: string }

export type Status = { id: number; name: string; order: number }

export type CardTypeDef = { id: number; name: string }

export type CardPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type Card = {
  id: number
  workspaceId: number
  statusId: number
  title: string
  description: string | null
  type: string
  priority: CardPriority
  assigneeId: number | null
  labels: string[]
  startDate: string | null
  dueDate: string | null
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

export type MyTask = {
  cardId: number
  workspaceId: number
  workspaceName: string
  title: string
  statusName: string
  priority: CardPriority
  dueDate: string | null
}

export type StatusCount = { statusId: number; statusName: string; count: number }

export type ProjectSummary = {
  workspaceId: number
  name: string
  totalCards: number
  statusCounts: StatusCount[]
}

export type RecentRelease = {
  workspaceId: number
  workspaceName: string
  version: string
  releasedAt: string
  cardCount: number
}

export type Dashboard = {
  myTaskCount: number
  dueSoonCount: number
  unreadNotificationCount: number
  myTasks: MyTask[]
  dueSoon: MyTask[]
  projects: ProjectSummary[]
  recentReleases: RecentRelease[]
}

export type ReleasedCard = { id: number; title: string; type: string; priority: CardPriority }

export type Release = {
  id: number
  workspaceId: number
  version: string
  notes: string | null
  releasedAt: string
  cards: ReleasedCard[]
}

export type Notification = {
  id: number
  type: 'COMMENT'
  workspaceId: number
  cardId: number
  cardTitle: string
  actorName: string
  read: boolean
  createdAt: string
}

export type NotificationList = { notifications: Notification[]; unreadCount: number }

export type LogLevel = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'

export type LogEntry = {
  level: LogLevel
  loggerName: string
  threadName: string
  message: string
  stackTrace: string | null
  loggedAt: string
}

export type MinutePoint = { minute: string; requests: number; errors: number; avgResponseMs: number }

export type MetricsResponse = {
  series: MinutePoint[]
  totalRequests: number
  totalErrors: number
  avgResponseMs: number
  webSocketSessions: number
}
