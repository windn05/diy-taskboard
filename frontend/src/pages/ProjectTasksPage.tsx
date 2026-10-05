import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router-dom'
import { listCards } from '../api/cards'
import { listWorkspaces } from '../api/workspaces'
import { listCardTypes } from '../api/cardTypes'
import { listReleases } from '../api/releases'
import { listStatuses } from '../api/statuses'
import { CardModal } from '../components/CardModal'
import { NewTaskModal } from '../components/NewTaskModal'
import { PlusIcon } from '../components/icons'
import { TypeBadge } from '../components/TypeBadge'
import { useAuth } from '../auth/AuthContext'

// 상태는 관리자가 정하므로 이름이 아닌 순서로 색을 순환 사용
const STATUS_COLOR = ['bg-slate-100 text-slate-700', 'bg-blue-100 text-blue-700', 'bg-amber-100 text-amber-700', 'bg-emerald-100 text-emerald-700']

/** 프로젝트의 작업 목록(표). 행을 누르면 작업 상세 팝업 열림 */
export function ProjectTasksPage() {
  const { workspaceId } = useParams()
  const wsId = Number(workspaceId)
  const { isGuest } = useAuth()
  const [addingTask, setAddingTask] = useState(false)
  const [titleSearch, setTitleSearch] = useState('')
  const [periodStart, setPeriodStart] = useState('')
  const [periodEnd, setPeriodEnd] = useState('')
  const [statusFilter, setStatusFilter] = useState<number | ''>('')
  const [typeFilter, setTypeFilter] = useState('')
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
    const keyword = titleSearch.trim().toLocaleLowerCase()
    return cards
      .filter((card) => !keyword || card.title.toLocaleLowerCase().includes(keyword))
      .filter((card) => {
        const taskStart = card.startDate ?? card.dueDate
        const taskEnd = card.dueDate ?? card.startDate
        return (!periodStart || (!!taskEnd && taskEnd >= periodStart)) && (!periodEnd || (!!taskStart && taskStart <= periodEnd))
      })
      .filter((card) => !statusFilter || card.statusId === statusFilter)
      .filter((card) => !typeFilter || card.type === typeFilter)
      .sort((a, b) => {
        if (!a.startDate) return 1
        if (!b.startDate) return -1
        return b.startDate.localeCompare(a.startDate)
      })
  }, [cards, titleSearch, periodStart, periodEnd, statusFilter, typeFilter])

  const activeCard = cards?.find((c) => c.id === activeCardId) ?? null

  if (!statuses || !cards) {
    return <div className="p-8 text-sm text-slate-500">불러오는 중...</div>
  }

  const statusName = (statusId: number) => statuses.find((s) => s.id === statusId)?.name ?? '-'
  const statusColorIndex = (statusId: number) => statuses.findIndex((s) => s.id === statusId)

  return (
    <div className="p-8">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="flex flex-1 flex-wrap items-center gap-2">
          <span className="text-xs text-slate-500">기간</span>
          <input
            type="date"
            value={periodStart}
            max={periodEnd || undefined}
            onChange={(event) => setPeriodStart(event.target.value)}
            aria-label="조회 시작일"
            className="rounded border bg-white px-2 py-1.5 text-sm"
          />
          <span className="text-slate-400">~</span>
          <input
            type="date"
            value={periodEnd}
            min={periodStart || undefined}
            onChange={(event) => setPeriodEnd(event.target.value)}
            aria-label="조회 종료일"
            className="rounded border bg-white px-2 py-1.5 text-sm"
          />
          <input
            type="search"
            value={titleSearch}
            onChange={(event) => setTitleSearch(event.target.value)}
            placeholder="작업명 검색"
            aria-label="작업명 검색"
            className="w-full max-w-sm rounded border bg-white px-3 py-1.5 text-sm outline-none focus:border-slate-400"
          />
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

      <div className="overflow-hidden rounded-lg border bg-white">
        <table className="w-full text-sm">
          <thead className="border-b bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="whitespace-nowrap px-4 py-2 font-medium">
                <select
                  value={statusFilter}
                  onChange={(event) => setStatusFilter(event.target.value ? Number(event.target.value) : '')}
                  aria-label="상태 필터"
                  className="rounded border bg-white px-2 py-1 text-xs"
                >
                  <option value="">상태 전체</option>
                  {statuses.map((status) => (
                    <option key={status.id} value={status.id}>
                      {status.name}
                    </option>
                  ))}
                </select>
              </th>
              <th className="px-4 py-2 font-medium">작업명</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">
                <select
                  value={typeFilter}
                  onChange={(event) => setTypeFilter(event.target.value)}
                  aria-label="타입 필터"
                  className="rounded border bg-white px-2 py-1 text-xs"
                >
                  <option value="">타입 전체</option>
                  {cardTypes?.map((type) => (
                    <option key={type.id} value={type.name}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">시작일</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">마감일</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((card) => (
              <tr
                key={card.id}
                onClick={() => setCardParam(card.id)}
                className="cursor-pointer border-b last:border-0 hover:bg-slate-50"
              >
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
                <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                  {titleSearch.trim() || periodStart || periodEnd || statusFilter || typeFilter
                    ? '검색 결과가 없습니다.'
                    : '작업이 없습니다.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

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
