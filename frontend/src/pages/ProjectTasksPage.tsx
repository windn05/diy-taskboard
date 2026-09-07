import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router-dom'
import { deleteCard, listCards } from '../api/cards'
import { listMembers, listWorkspaces } from '../api/workspaces'
import { listCardTypes } from '../api/cardTypes'
import { listStatuses } from '../api/statuses'
import type { Card } from '../api/types'
import { CardModal } from '../components/CardModal'
import { NewTaskModal } from '../components/NewTaskModal'
import { PlusIcon } from '../components/icons'
import { useAuth } from '../auth/AuthContext'

const PRIORITY_COLOR: Record<Card['priority'], string> = {
  LOW: 'bg-slate-200 text-slate-700',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
}

const STATUS_COLOR = ['bg-slate-100 text-slate-700', 'bg-blue-100 text-blue-700', 'bg-amber-100 text-amber-700', 'bg-emerald-100 text-emerald-700']

export function ProjectTasksPage() {
  const { workspaceId } = useParams()
  const wsId = Number(workspaceId)
  const queryClient = useQueryClient()
  const { isGuest } = useAuth()
  const [addingTask, setAddingTask] = useState(false)
  // 알림에서 넘어올 때 ?card=123 으로 특정 작업을 바로 연다.
  const [searchParams, setSearchParams] = useSearchParams()
  const cardParam = searchParams.get('card')
  // 열려 있는 작업은 URL이 유일한 기준이다. 알림에서 들어오든 행을 누르든 같은 경로를 타고,
  // 덤으로 특정 작업 화면을 그대로 공유·북마크할 수 있다.
  const activeCardId = cardParam ? Number(cardParam) : null

  function setCardParam(cardId: number | null) {
    const next = new URLSearchParams(searchParams)
    if (cardId === null) next.delete('card')
    else next.set('card', String(cardId))
    setSearchParams(next, { replace: true })
  }

  const deleteMutation = useMutation({
    mutationFn: (cardId: number) => deleteCard(cardId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cards', wsId] }),
  })

  function handleDelete(e: React.MouseEvent, cardId: number, title: string) {
    e.stopPropagation()
    if (confirm(`"${title}" 작업을 삭제할까요?`)) deleteMutation.mutate(cardId)
  }

  const { data: statuses } = useQuery({ queryKey: ['statuses'], queryFn: listStatuses })
  const { data: members } = useQuery({ queryKey: ['members', wsId], queryFn: () => listMembers(wsId) })
  const { data: cardTypes } = useQuery({ queryKey: ['card-types'], queryFn: listCardTypes })
  const { data: cards } = useQuery({ queryKey: ['cards', wsId], queryFn: () => listCards(wsId) })
  const { data: projects } = useQuery({ queryKey: ['workspaces'], queryFn: listWorkspaces })

  const tasks = useMemo(() => {
    if (!cards || !statuses) return []
    const statusOrder = new Map(statuses.map((s) => [s.id, s.order]))
    return [...cards].sort((a, b) => (statusOrder.get(a.statusId) ?? 0) - (statusOrder.get(b.statusId) ?? 0))
  }, [cards, statuses])

  const activeCard = tasks.find((c) => c.id === activeCardId) ?? null

  if (!statuses || !cards) {
    return <div className="p-8 text-sm text-slate-400">불러오는 중...</div>
  }

  const statusName = (statusId: number) => statuses.find((s) => s.id === statusId)?.name ?? '-'
  const statusColorIndex = (statusId: number) => statuses.findIndex((s) => s.id === statusId)

  return (
    <div className="p-8">
      {!isGuest && (
        <div className="mb-4 flex items-center justify-end">
          <button
            onClick={() => setAddingTask(true)}
            className="flex items-center gap-1.5 rounded bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800"
          >
            <PlusIcon size={14} />새 작업
          </button>
        </div>
      )}

      <div className="overflow-hidden rounded-lg border bg-white">
        <table className="w-full text-sm">
          <thead className="border-b bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">작업명</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">상태</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">타입</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">우선순위</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">담당자</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">시작일</th>
              <th className="whitespace-nowrap px-4 py-2 font-medium">마감일</th>
              {!isGuest && <th className="w-12 px-4 py-2 font-medium"></th>}
            </tr>
          </thead>
          <tbody>
            {tasks.map((card) => (
              <tr
                key={card.id}
                onClick={() => setCardParam(card.id)}
                className="cursor-pointer border-b last:border-0 hover:bg-slate-50"
              >
                <td className="px-4 py-2.5 font-medium">{card.title}</td>
                <td className="whitespace-nowrap px-4 py-2.5">
                  <span
                    className={`rounded px-2 py-0.5 text-xs font-medium ${
                      STATUS_COLOR[statusColorIndex(card.statusId) % STATUS_COLOR.length] ?? STATUS_COLOR[0]
                    }`}
                  >
                    {statusName(card.statusId)}
                  </span>
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">{card.type}</td>
                <td className="whitespace-nowrap px-4 py-2.5">
                  <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${PRIORITY_COLOR[card.priority]}`}>
                    {card.priority}
                  </span>
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">
                  {members?.find((m) => m.userId === card.assigneeId)?.name ?? '-'}
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">{card.startDate ?? '-'}</td>
                <td className="whitespace-nowrap px-4 py-2.5 text-slate-500">{card.dueDate ?? '-'}</td>
                {!isGuest && (
                  <td className="whitespace-nowrap px-4 py-2.5 text-right">
                    <button
                      onClick={(e) => handleDelete(e, card.id, card.title)}
                      className="text-xs text-red-600 hover:underline"
                    >
                      삭제
                    </button>
                  </td>
                )}
              </tr>
            ))}

            {tasks.length === 0 && (
              <tr>
                <td colSpan={isGuest ? 7 : 8} className="px-4 py-8 text-center text-slate-400">
                  작업이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {activeCard && (
        <CardModal
          card={activeCard}
          members={members ?? []}
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
