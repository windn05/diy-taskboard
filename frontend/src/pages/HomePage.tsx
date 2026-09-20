import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/dashboard'
import { useAuth } from '../auth/AuthContext'
import { HomeCalendar } from '../components/HomeCalendar'
import type { MyTask, ProjectSummary, RecentCard, RecentRelease } from '../api/types'

const PRIORITY_COLOR: Record<string, string> = {
  LOW: 'bg-slate-200 text-slate-700',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
}

/** 프로젝트 현황 막대. 상태 순서대로 색을 돌려 쓴다. */
const BAR_COLOR = ['bg-slate-400', 'bg-sky-400', 'bg-indigo-400', 'bg-emerald-400', 'bg-slate-600']

/** 프로젝트가 늘어나도 화면이 계속 길어지지 않게 여기까지만 보여주고 나머지는 목록으로 넘긴다. */
const PROJECT_LIMIT = 5

const today = () => new Date().toISOString().slice(0, 10)

export function HomePage() {
  const { user, isGuest } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })

  if (isLoading) return <div className="p-8 text-sm text-slate-400">불러오는 중...</div>
  if (!data) return null

  const shownProjects = data.projects.slice(0, PROJECT_LIMIT)
  const restCount = data.projects.length - shownProjects.length

  // 색은 상태에 고정한다. 행마다 배열 순서로 칠하면 어떤 상태가 없는 프로젝트에서 색이 밀려
  // 같은 색이 다른 상태를 가리키게 된다.
  const colorByStatus = new Map(
    [...new Set(data.projects.flatMap((p) => p.statusCounts.map((s) => s.statusId)))]
      .sort((a, b) => a - b)
      .map((statusId, index) => [statusId, BAR_COLOR[index % BAR_COLOR.length]]),
  )

  return (
    <div className="space-y-8 p-8">
      <h1 className="text-xl font-semibold">
        {isGuest ? '둘러보기' : `${user?.username}님, 오늘도 반갑습니다`}
      </h1>

      {/*
        패널을 열에 고정 배치하면 어느 패널이 길어지느냐에 따라 반대쪽에 큰 여백이 생긴다.
        흐르는 배치(multi-column)로 두면 내용 양이 달라져도 양쪽 높이가 알아서 비슷해진다.
      */}
      <div className="columns-1 gap-5 lg:columns-2">
        {/* 담당자를 지정해 쓰기 시작하면 그때부터 나타난다. 비어 있는 패널이 자리만 차지하지 않게. */}
        {data.myTasks.length > 0 && (
          <Panel title="내 작업" count={data.myTasks.length}>
            {data.myTasks.map((task) => (
              <TaskRow key={task.cardId} task={task} />
            ))}
          </Panel>
        )}

        <Panel title="마감 임박·지난 작업" count={data.dueSoon.length} empty="마감이 임박한 작업이 없습니다.">
          {data.dueSoon.map((task) => (
            <TaskRow key={task.cardId} task={task} showOverdue />
          ))}
        </Panel>

        <Panel
          title="프로젝트 현황"
          count={data.projects.length}
          empty="참여 중인 프로젝트가 없습니다."
          footer={
            restCount > 0 ? (
              <Link to="/projects" className="block px-4 py-2 text-center text-xs text-slate-500 hover:bg-slate-50">
                {restCount}개 더 보기
              </Link>
            ) : undefined
          }
        >
          {shownProjects.map((project) => (
            <ProjectRow key={project.workspaceId} project={project} colorByStatus={colorByStatus} />
          ))}
        </Panel>

        <Panel title="최근 등록한 작업" empty="등록된 작업이 없습니다.">
          {data.recentCards.map((card) => (
            <RecentCardRow key={card.cardId} card={card} />
          ))}
        </Panel>

        <Panel title="최근 배포" empty="배포 기록이 없습니다.">
          {data.recentReleases.map((release) => (
            <ReleaseRow key={`${release.workspaceId}-${release.version}`} release={release} />
          ))}
        </Panel>
      </div>

      <HomeCalendar tasks={data.calendarTasks} />
    </div>
  )
}

/**
 * 목록 패널. 내용이 적으면 그만큼만 차지하고, 많아지면 정해진 높이 안에서 스크롤한다.
 * 데이터 양에 따라 화면 전체 높이가 출렁이지 않게 하려는 것이다.
 */
function Panel({
  title,
  count,
  empty,
  footer,
  children,
}: {
  title: string
  count?: number
  empty?: string
  footer?: React.ReactNode
  children: React.ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const isEmpty = items.flat().filter(Boolean).length === 0

  return (
    <section className="mb-5 break-inside-avoid overflow-hidden rounded-lg border">
      <h2 className="flex items-baseline gap-2 border-b bg-slate-50 px-4 py-2.5 text-sm font-semibold text-slate-600">
        {title}
        {count !== undefined && count > 0 && <span className="text-xs font-normal text-slate-400">{count}</span>}
      </h2>
      {isEmpty ? (
        <p className="bg-white px-4 py-6 text-center text-sm text-slate-400">{empty}</p>
      ) : (
        <div className="max-h-72 divide-y divide-slate-100 overflow-y-auto bg-white">{children}</div>
      )}
      {footer && <div className="border-t bg-white">{footer}</div>}
    </section>
  )
}

function TaskRow({ task, showOverdue }: { task: MyTask; showOverdue?: boolean }) {
  const overdue = showOverdue && task.dueDate !== null && task.dueDate < today()

  return (
    <Link to={`/projects/${task.workspaceId}?card=${task.cardId}`} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-slate-50">
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

function RecentCardRow({ card }: { card: RecentCard }) {
  return (
    <Link to={`/projects/${card.workspaceId}?card=${card.cardId}`} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-slate-50">
      <span className="min-w-0 flex-1 truncate">{card.title}</span>
      <span className="shrink-0 rounded border border-slate-200 px-1.5 py-0.5 text-[11px] text-slate-500">
        {card.statusName}
      </span>
      <span className="w-20 shrink-0 truncate text-right text-xs text-slate-400">{card.workspaceName}</span>
      <span className="w-20 shrink-0 text-right text-xs text-slate-400">{card.createdDate ?? '-'}</span>
    </Link>
  )
}

/**
 * 한 줄로 압축한 프로젝트 요약. 예전에는 상태마다 칩을 달았는데, 상태가 5개인 지금
 * 프로젝트가 늘어날수록 칩이 화면을 채워서 한눈에 비교가 안 됐다. 비율 막대로 바꿨다.
 */
function ProjectRow({ project, colorByStatus }: { project: ProjectSummary; colorByStatus: Map<number, string> }) {
  return (
    <Link to={`/projects/${project.workspaceId}`} className="block px-4 py-2.5 hover:bg-slate-50">
      <div className="flex items-baseline gap-2">
        <span className="min-w-0 flex-1 truncate text-sm font-medium text-slate-800">{project.name}</span>
        <span className="shrink-0 text-xs text-slate-400">{project.totalCards}건</span>
      </div>

      {project.totalCards > 0 ? (
        <div className="mt-1.5 flex h-1.5 overflow-hidden rounded-full bg-slate-100">
          {project.statusCounts.map((status) => (
            <div
              key={status.statusId}
              className={colorByStatus.get(status.statusId) ?? BAR_COLOR[0]}
              style={{ width: `${(status.count / project.totalCards) * 100}%` }}
              title={`${status.statusName} ${status.count}건`}
            />
          ))}
        </div>
      ) : (
        <p className="mt-1.5 text-[11px] text-slate-400">작업 없음</p>
      )}
    </Link>
  )
}

function ReleaseRow({ release }: { release: RecentRelease }) {
  return (
    <Link to={`/projects/${release.workspaceId}/releases`} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-slate-50">
      <span className="font-semibold text-slate-800">{release.version}</span>
      <span className="text-xs text-slate-400">{release.workspaceName}</span>
      <span className="ml-auto text-xs text-slate-400">
        작업 {release.cardCount}건 · {new Date(release.releasedAt).toLocaleDateString()}
      </span>
    </Link>
  )
}
