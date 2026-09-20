import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/dashboard'
import { useAuth } from '../auth/AuthContext'
import { HomeCalendar } from '../components/HomeCalendar'
import type { MyTask, RecentCard, RecentRelease } from '../api/types'

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
    // 홈은 한 화면에 담는다 — 페이지 자체는 스크롤하지 않고, 넘치는 쪽(패널 열·달력)만 안에서 스크롤한다.
    <div className="flex h-full min-h-0 flex-col gap-4 p-6">
      <h1 className="shrink-0 text-lg font-semibold">
        {isGuest ? '둘러보기' : `${user?.username}님, 오늘도 반갑습니다`}
      </h1>

      {/*
        넓은 화면에서는 왼쪽에 패널, 오른쪽에 달력을 나란히 둬 한 화면에 들어오게 한다.
        좁은 화면에서는 위아래로 쌓고 이 영역만 스크롤한다.
      */}
      <div className="grid min-h-0 flex-1 gap-4 overflow-y-auto lg:grid-cols-[minmax(320px,1fr)_1.7fr] lg:overflow-hidden">
        <div className="min-h-0 space-y-4 lg:overflow-y-auto lg:pr-1">
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

          <Panel title="최근 등록한 작업" empty="등록된 작업이 없습니다.">
            {data.recentCards.map((card) => (
              <RecentCardRow key={card.cardId} card={card} />
            ))}
          </Panel>
        </div>

        {/* 달력이 남은 높이를 차지하고, 최근 배포는 그 아래에 자기 높이만큼만 붙는다. */}
        <div className="flex min-h-0 flex-col gap-4">
          <div className="min-h-[420px] flex-1 lg:min-h-0">
            <HomeCalendar tasks={data.calendarTasks} />
          </div>

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

/**
 * 목록 패널. 내용만큼만 차지한다 — 스크롤은 패널이 아니라 바깥 열이 맡는다.
 * 패널마다 스크롤을 두면 스크롤바가 중첩돼 어느 것을 굴리는지 헷갈린다.
 */
function Panel({
  title,
  count,
  empty,
  children,
}: {
  title: string
  count?: number
  empty?: string
  children: React.ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const isEmpty = items.flat().filter(Boolean).length === 0

  return (
    <section className="overflow-hidden rounded-lg border">
      <h2 className="flex items-baseline gap-2 border-b bg-slate-50 px-4 py-2.5 text-sm font-semibold text-slate-600">
        {title}
        {count !== undefined && count > 0 && <span className="text-xs font-normal text-slate-400">{count}</span>}
      </h2>
      {isEmpty ? (
        <p className="bg-white px-4 py-6 text-center text-sm text-slate-400">{empty}</p>
      ) : (
        <div className="divide-y divide-slate-100 bg-white">{children}</div>
      )}
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
