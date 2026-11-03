import { useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router-dom'
import { listCards } from '../api/cards'
import { listWorkspaces } from '../api/workspaces'
import { listCardTypes } from '../api/cardTypes'
import { listReleases } from '../api/releases'
import { listStatuses } from '../api/statuses'
import { CardModal } from '../components/CardModal'
import { NewTaskModal } from '../components/NewTaskModal'
import { Pagination } from '../components/Pagination'
import { PlusIcon } from '../components/icons'
import { TypeBadge } from '../components/TypeBadge'
import { useAuth } from '../auth/AuthContext'

// 상태는 관리자가 정하므로 이름이 아닌 순서로 색을 순환 사용
const STATUS_COLOR = ['bg-slate-100 text-slate-700', 'bg-blue-100 text-blue-700', 'bg-amber-100 text-amber-700', 'bg-emerald-100 text-emerald-700']
const PAGE_SIZE = 15

type Filters = { title: string; periodStart: string; periodEnd: string; statusId: number | ''; type: string }
const EMPTY_FILTERS: Filters = { title: '', periodStart: '', periodEnd: '', statusId: '', type: '' }

const FILTER_CLASS = 'rounded border bg-white px-2 py-1.5 text-sm'

/** 프로젝트의 작업 목록(표). 행을 누르면 작업 상세 팝업 열림 */
export function ProjectTasksPage() {
  const { workspaceId } = useParams()
  // 프로젝트를 옮겨도 같은 화면이 재사용돼 검색 조건이 남으므로, 프로젝트마다 새로 그려 초기화
  return <ProjectTasks key={workspaceId} wsId={Number(workspaceId)} />
}

function ProjectTasks({ wsId }: { wsId: number }) {
  const { isGuest } = useAuth()
  const [addingTask, setAddingTask] = useState(false)
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)
  // 홈·달력 등에서 넘어올 때 ?card=123 으로 특정 작업을 바로 열기
  const [searchParams, setSearchParams] = useSearchParams()
  const cardParam = searchParams.get('card')
  // 열려 있는 작업은 URL이 유일한 기준. 다른 화면에서 들어오든 행을 누르든 같은 경로를 타고,
  // 덤으로 특정 작업 화면을 그대로 공유·북마크 가능
  const activeCardId = cardParam ? Number(cardParam) : null

  function setCardParam(cardId: number | null) {
    const next = new URLSearchParams(searchParams)
    if (cardId === null) next.delete('card')
    else next.set('card', String(cardId))
    setSearchParams(next, { replace: true })
  }

  const setFilter = <K extends keyof Filters>(key: K, value: Filters[K]) => setFilters((f) => ({ ...f, [key]: value }))
  const filtering = JSON.stringify(filters) !== JSON.stringify(EMPTY_FILTERS)

  const { data: statuses } = useQuery({ queryKey: ['statuses'], queryFn: listStatuses })
  const { data: cardTypes } = useQuery({ queryKey: ['card-types'], queryFn: listCardTypes })
  const { data: cards } = useQuery({ queryKey: ['cards', wsId], queryFn: () => listCards(wsId) })
  const { data: projects } = useQuery({ queryKey: ['workspaces'], queryFn: listWorkspaces })
  const { data: releases } = useQuery({ queryKey: ['releases', wsId], queryFn: () => listReleases(wsId) })
  // 배포된 작업은 상태 옆에 버전 표시
  const releaseVersion = new Map(releases?.map((r) => [r.id, r.version]))

  // 작업명과 겹치는 기간으로 거른 뒤 시작일 최신순. 날짜가 하나뿐이면 하루짜리 작업으로 본다.
  const tasks = useMemo(() => {
    if (!cards) return []
    const { title, periodStart, periodEnd, statusId, type } = filters
    const keyword = title.trim().toLocaleLowerCase()
    return cards
      .filter((card) => !keyword || card.title.toLocaleLowerCase().includes(keyword))
      .filter((card) => {
        const taskStart = card.startDate ?? card.dueDate
        const taskEnd = card.dueDate ?? card.startDate
        return (!periodStart || (!!taskEnd && taskEnd >= periodStart)) && (!periodEnd || (!!taskStart && taskStart <= periodEnd))
      })
      .filter((card) => !statusId || card.statusId === statusId)
      .filter((card) => !type || card.type === type)
      .sort((a, b) => {
        if (!a.startDate) return 1
        if (!b.startDate) return -1
        return b.startDate.localeCompare(a.startDate)
      })
  }, [cards, filters])

  // 검색 조건이 바뀌면 1쪽부터. 쪽 번호를 조건과 묶어 저장해 effect 없이 초기화
  const filterKey = JSON.stringify(filters)
  const [pageState, setPageState] = useState({ key: filterKey, page: 1 })
  const pageCount = Math.max(1, Math.ceil(tasks.length / PAGE_SIZE))
  // 작업 삭제 등으로 쪽 수가 줄면 마지막 쪽으로
  const page = Math.min(pageState.key === filterKey ? pageState.page : 1, pageCount)
  const pageTasks = tasks.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
  const tableRef = useRef<HTMLDivElement>(null)

  function changePage(next: number) {
    setPageState({ key: filterKey, page: next })
    tableRef.current?.scrollIntoView({ block: 'start' })
  }

  const activeCard = cards?.find((c) => c.id === activeCardId) ?? null

  if (!statuses || !cards) {
    return <div className="p-8 text-sm text-slate-500">불러오는 중...</div>
  }

  const statusName = (statusId: number) => statuses.find((s) => s.id === statusId)?.name ?? '-'
  const statusColorIndex = (statusId: number) => statuses.findIndex((s) => s.id === statusId)

  return (
    <div className="p-8">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div className="flex flex-1 flex-wrap items-center gap-2">
          <span className="text-xs text-slate-500">기간</span>
          <input
            type="date"
            value={filters.periodStart}
            max={filters.periodEnd || undefined}
            onChange={(event) => setFilter('periodStart', event.target.value)}
            aria-label="조회 시작일"
            className={FILTER_CLASS}
          />
          <span className="text-slate-400">~</span>
          <input
            type="date"
            value={filters.periodEnd}
            min={filters.periodStart || undefined}
            onChange={(event) => setFilter('periodEnd', event.target.value)}
            aria-label="조회 종료일"
            className={FILTER_CLASS}
          />
          <select
            value={filters.statusId}
            onChange={(event) => setFilter('statusId', event.target.value ? Number(event.target.value) : '')}
            aria-label="상태 필터"
            className={FILTER_CLASS}
          >
            <option value="">상태 전체</option>
            {statuses.map((status) => (
              <option key={status.id} value={status.id}>
                {status.name}
              </option>
            ))}
          </select>
          <select
            value={filters.type}
            onChange={(event) => setFilter('type', event.target.value)}
            aria-label="타입 필터"
            className={FILTER_CLASS}
          >
            <option value="">타입 전체</option>
            {cardTypes?.map((type) => (
              <option key={type.id} value={type.name}>
                {type.name}
              </option>
            ))}
          </select>
          <input
            type="search"
            value={filters.title}
            onChange={(event) => setFilter('title', event.target.value)}
            placeholder="작업명 검색"
            aria-label="작업명 검색"
            className="w-full max-w-xs rounded border bg-white px-3 py-1.5 text-sm outline-none focus:border-slate-400"
          />
          <button
            type="button"
            onClick={() => setFilters(EMPTY_FILTERS)}
            disabled={!filtering}
            className="rounded px-2 py-1.5 text-sm text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:cursor-default disabled:text-slate-300 disabled:hover:bg-transparent"
          >
            초기화
          </button>
        </div>
        {!isGuest && (
          <button
            onClick={() => setAddingTask(true)}
            className="flex shrink-0 items-center gap-1.5 rounded bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800"
          >
            <PlusIcon size={14} />새 작업
          </button>
        )}
      </div>

      <div ref={tableRef} className="scroll-mt-4 overflow-hidden rounded-lg border bg-white">
        <table className="w-full text-sm">
          <thead className="border-b bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="w-14 whitespace-nowrap px-4 py-2.5 text-center font-medium">번호</th>
              <th className="whitespace-nowrap px-4 py-2.5 font-medium">상태</th>
              <th className="px-4 py-2.5 font-medium">작업명</th>
              <th className="whitespace-nowrap px-4 py-2.5 font-medium">타입</th>
              <th className="whitespace-nowrap px-4 py-2.5 font-medium">시작일</th>
              <th className="whitespace-nowrap px-4 py-2.5 font-medium">마감일</th>
            </tr>
          </thead>
          <tbody>
            {pageTasks.map((card, index) => (
              <tr
                key={card.id}
                onClick={() => setCardParam(card.id)}
                className="cursor-pointer border-b last:border-0 hover:bg-slate-50"
              >
                {/* 현재 검색 결과 안에서의 순번 (쪽을 넘겨도 이어짐) */}
                <td className="whitespace-nowrap px-4 py-2.5 text-center text-xs tabular-nums text-slate-400">
                  {(page - 1) * PAGE_SIZE + index + 1}
                </td>
                <td className="whitespace-nowrap px-4 py-2.5">
                  <span
                    className={`rounded px-2 py-0.5 text-xs font-medium ${
                      STATUS_COLOR[statusColorIndex(card.statusId) % STATUS_COLOR.length] ?? STATUS_COLOR[0]
                    }`}
                  >
                    {statusName(card.statusId)}
                  </span>
                  {card.releaseId && (
                    <span className="ml-1.5 text-[11px] font-medium text-slate-500">
                      {releaseVersion.get(card.releaseId) ?? '-'}
                    </span>
                  )}
                </td>
                <td className="px-4 py-2.5 font-medium">
                  {card.title}
                  {card.commentCount > 0 && (
                    <span className="ml-1.5 inline-flex items-center rounded-full bg-slate-700 px-1.5 py-0.5 text-[11px] font-semibold text-white">
                      {card.commentCount}
                    </span>
                  )}
                </td>
                <td className="whitespace-nowrap px-4 py-2.5">
                  <TypeBadge type={card.type} />
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">{card.startDate ?? '-'}</td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">{card.dueDate ?? '-'}</td>
              </tr>
            ))}

            {tasks.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                  {filtering ? '검색 결과가 없습니다.' : '작업이 없습니다.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {tasks.length > 0 && (
        <div className="mt-3 flex items-center justify-between">
          <span className="text-xs text-slate-500">
            {tasks.length}건 중 {(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, tasks.length)}
          </span>
          <Pagination page={page} pageCount={pageCount} onChange={changePage} />
        </div>
      )}

      {activeCard && (
        <CardModal
          card={activeCard}
          statuses={statuses}
          cardTypes={cardTypes ?? []}
          onClose={() => setCardParam(null)}
        />
      )}

      {addingTask && projects && (
        <NewTaskModal projects={projects} defaultWorkspaceId={wsId} onClose={() => setAddingTask(false)} />
      )}
    </div>
  )
}
