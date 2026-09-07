import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/dashboard'
import { useAuth } from '../auth/AuthContext'
import type { MyTask, ProjectSummary, RecentRelease } from '../api/types'

const PRIORITY_COLOR: Record<string, string> = {
  LOW: 'bg-slate-200 text-slate-700',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
}

const today = () => new Date().toISOString().slice(0, 10)

export function HomePage() {
  const { user, isGuest } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })

  if (isLoading) return <div className="p-8 text-sm text-slate-400">불러오는 중...</div>
  if (!data) return null

  return (
    <div className="space-y-6 p-8">
      <h1 className="text-xl font-semibold">
        {isGuest ? '둘러보기' : `${user?.username}님, 오늘도 반갑습니다`}
      </h1>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <SummaryCard label="내 담당 작업" value={data.myTaskCount} />
        <SummaryCard label="마감 임박·지남" value={data.dueSoonCount} tone={data.dueSoonCount > 0 ? 'warn' : undefined} />
        <SummaryCard label="안 읽은 알림" value={data.unreadNotificationCount} />
        <SummaryCard label="참여 중인 프로젝트" value={data.projects.length} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <Panel title="내 담당 작업" empty="담당으로 지정된 작업이 없습니다.">
            {data.myTasks.map((task) => (
              <TaskRow key={task.cardId} task={task} />
            ))}
          </Panel>

          <Panel title="마감 임박·지난 작업" empty="마감이 임박한 작업이 없습니다.">
            {data.dueSoon.map((task) => (
              <TaskRow key={task.cardId} task={task} showOverdue />
            ))}
          </Panel>
        </div>

        <div className="space-y-6">
          <Panel title="프로젝트 현황" empty="참여 중인 프로젝트가 없습니다.">
            {data.projects.map((project) => (
              <ProjectRow key={project.workspaceId} project={project} />
            ))}
          </Panel>

          <Panel title="최근 배포" empty="배포 기록이 없습니다.">
            {data.recentReleases.map((release) => (
              <ReleaseRow key={`${release.workspaceId}-${release.version}`} release={release} />
            ))}
          </Panel>
        </div>
      </div>
    </div>
  )
}

function SummaryCard({ label, value, tone }: { label: string; value: number; tone?: 'warn' }) {
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${tone === 'warn' ? 'text-orange-600' : 'text-slate-800'}`}>{value}</p>
    </div>
  )
}

function Panel({ title, empty, children }: { title: string; empty: string; children: React.ReactNode }) {
  const items = Array.isArray(children) ? children : [children]
  const isEmpty = items.flat().filter(Boolean).length === 0

  return (
    <section className="rounded-lg border bg-white">
      <h2 className="border-b px-4 py-2.5 text-sm font-semibold text-slate-700">{title}</h2>
      {isEmpty ? <p className="px-4 py-8 text-center text-sm text-slate-400">{empty}</p> : <div>{children}</div>}
    </section>
  )
}

function TaskRow({ task, showOverdue }: { task: MyTask; showOverdue?: boolean }) {
  const overdue = showOverdue && task.dueDate !== null && task.dueDate < today()

  return (
    <Link
      to={`/projects/${task.workspaceId}?card=${task.cardId}`}
      className="flex items-center gap-2 border-b px-4 py-2 text-sm last:border-0 hover:bg-slate-50"
    >
      <span className="min-w-0 flex-1 truncate">{task.title}</span>
      <span className={`shrink-0 rounded px-1.5 py-0.5 text-[11px] font-medium ${PRIORITY_COLOR[task.priority]}`}>
        {task.priority}
      </span>
      <span className="w-20 shrink-0 truncate text-right text-xs text-slate-400">{task.workspaceName}</span>
      <span className={`w-20 shrink-0 text-right text-xs ${overdue ? 'font-semibold text-red-600' : 'text-slate-400'}`}>
        {task.dueDate ?? '-'}
      </span>
    </Link>
  )
}

function ProjectRow({ project }: { project: ProjectSummary }) {
  return (
    <Link
      to={`/projects/${project.workspaceId}`}
      className="block border-b px-4 py-2.5 last:border-0 hover:bg-slate-50"
    >
      <div className="mb-1 flex items-baseline gap-2">
        <span className="text-sm font-medium text-slate-800">{project.name}</span>
        <span className="text-xs text-slate-400">작업 {project.totalCards}건</span>
      </div>
      <div className="flex flex-wrap gap-1">
        {project.statusCounts.map((status) => (
          <span key={status.statusId} className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">
            {status.statusName} {status.count}
          </span>
        ))}
        {project.statusCounts.length === 0 && <span className="text-[11px] text-slate-400">작업 없음</span>}
      </div>
    </Link>
  )
}

function ReleaseRow({ release }: { release: RecentRelease }) {
  return (
    <Link
      to={`/projects/${release.workspaceId}/releases`}
      className="flex items-center gap-2 border-b px-4 py-2 text-sm last:border-0 hover:bg-slate-50"
    >
      <span className="font-semibold text-slate-800">{release.version}</span>
      <span className="text-xs text-slate-400">{release.workspaceName}</span>
      <span className="ml-auto text-xs text-slate-400">
        작업 {release.cardCount}건 · {new Date(release.releasedAt).toLocaleDateString()}
      </span>
    </Link>
  )
}
