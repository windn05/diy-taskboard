import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/dashboard'
import { useAuth } from '../auth/AuthContext'
import { HomeCalendar } from '../components/HomeCalendar'
import type { DueTask, RecentCard, RecentRelease } from '../api/types'
import { TypeBadge } from '../components/TypeBadge'
import { localDate, localDateOf } from '../time'

/** 홈(대시보드). 왼쪽은 작업 목록 패널, 오른쪽은 달력과 최근 배포 */
export function HomePage() {
  const { user, isGuest } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })

  if (isLoading) return <div className="p-8 text-sm text-slate-500">불러오는 중...</div>
  if (!data) return null

  return (
    // 홈은 한 화면에 표시 — 페이지 자체는 스크롤하지 않고, 넘치는 쪽(패널 열·달력)만 안에서 스크롤
    <div className="flex h-full min-h-0 flex-col gap-4 p-6">
      <h1 className="shrink-0 text-lg font-semibold">
        {isGuest ? '둘러보기' : `${user?.username}님, 오늘도 반갑습니다`}
      </h1>

      {/*
        넓은 화면에서는 왼쪽에 패널, 오른쪽에 달력을 나란히 둬 한 화면에 배치.
        좁은 화면에서는 위아래로 쌓고 이 영역만 스크롤
      */}
      <div className="grid min-h-0 flex-1 gap-4 overflow-y-auto lg:grid-cols-[minmax(320px,1fr)_1.7fr] lg:overflow-hidden">
        <div className="flex min-h-0 flex-col gap-4 lg:overflow-y-auto lg:pr-1">
          <Panel title="마감 임박·지난 작업" count={data.dueSoon.length} empty="마감이 임박한 작업이 없습니다.">
            {data.dueSoon.map((task) => (
              <TaskRow key={task.cardId} task={task} />
            ))}
          </Panel>

          {/* 마지막 패널이 남는 높이를 차지 — 왼쪽 열 아래가 비어 오른쪽과 어긋나 보이지 않게 */}
          <Panel title="최근 등록한 작업" empty="등록된 작업이 없습니다." grow>
            {data.recentCards.map((card) => (
              <RecentCardRow key={card.cardId} card={card} />
            ))}
          </Panel>
        </div>

        {/* 달력이 남은 높이를 차지하고, 최근 배포는 그 아래에 자기 높이만큼만 배치 */}
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
 * 목록 패널. 내용만큼만 차지 — 스크롤은 패널이 아니라 바깥 열이 담당.
 * 패널마다 스크롤을 두면 스크롤바가 중첩돼 어느 것을 굴리는지 헷갈림
 */
function Panel({
  title,
  count,
  empty,
  grow,
  children,
}: {
  title: string
  count?: number
  empty?: string
  /** 열에 남는 높이를 이 패널이 차지. 열의 마지막 패널에만 사용 */
  grow?: boolean
  children: React.ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const isEmpty = items.flat().filter(Boolean).length === 0

  return (
    <section className={`flex flex-col overflow-hidden rounded-lg border ${grow ? 'min-h-0 flex-1' : 'shrink-0'}`}>
      <h2 className="flex shrink-0 items-baseline gap-2 border-b bg-slate-50 px-4 py-2.5 text-sm font-semibold text-slate-600">
        {title}
        {count !== undefined && count > 0 && <span className="text-xs font-normal text-slate-500">{count}</span>}
      </h2>
      {isEmpty ? (
        <p className="bg-white px-4 py-6 text-center text-sm text-slate-500">{empty}</p>
      ) : (
        <div className={`divide-y divide-slate-100 bg-white ${grow ? 'min-h-0 flex-1 overflow-y-auto' : ''}`}>
          {children}
        </div>
      )}
    </section>
  )
}

function TaskRow({ task }: { task: DueTask }) {
  // 마감일(yyyy-MM-dd)과 오늘 날짜를 문자열로 비교
  const overdue = task.dueDate !== null && task.dueDate < localDate()

  return (
    <Link to={`/projects/${task.workspaceId}?card=${task.cardId}`} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-slate-50">
      <span className="min-w-0 flex-1 truncate">{task.title}</span>
      <span className="shrink-0">
        <TypeBadge type={task.type} />
      </span>
      <span className="w-20 shrink-0 truncate text-right text-xs text-slate-500">{task.workspaceName}</span>
      <span className={`w-20 shrink-0 text-right text-xs ${overdue ? 'font-semibold text-red-600' : 'text-slate-500'}`}>
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
      <span className="w-20 shrink-0 truncate text-right text-xs text-slate-500">{card.workspaceName}</span>
      <span className="w-20 shrink-0 text-right text-xs text-slate-500">{card.createdAt ? localDateOf(card.createdAt) : '-'}</span>
    </Link>
  )
}

function ReleaseRow({ release }: { release: RecentRelease }) {
  return (
    <Link to={`/projects/${release.workspaceId}/releases`} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-slate-50">
      <span className="font-semibold text-slate-800">{release.version}</span>
      <span className="text-xs text-slate-500">{release.workspaceName}</span>
      <span className="ml-auto text-xs text-slate-500">
        작업 {release.cardCount}건 · {new Date(release.releasedAt).toLocaleDateString()}
      </span>
    </Link>
  )
}
