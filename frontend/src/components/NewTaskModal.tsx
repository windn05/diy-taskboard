import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createCard } from '../api/cards'
import { listMembers } from '../api/workspaces'
import { listCardTypes } from '../api/cardTypes'
import { listStatuses } from '../api/statuses'
import type { Card, Workspace } from '../api/types'
import { AssigneeSelect, DateField, LabeledField, PrioritySelect } from './fields'
import { Modal } from './Modal'

/** 새 작업 만들기. 사이드바의 + 버튼에서 열고, 생성 후 해당 프로젝트로 이동 */
export function NewTaskModal({
  projects,
  defaultWorkspaceId,
  onClose,
}: {
  projects: Workspace[]
  /** 프로젝트 안에서 열었다면 그 프로젝트를 미리 선택 */
  defaultWorkspaceId?: number
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [workspaceId, setWorkspaceId] = useState<number>(defaultWorkspaceId ?? projects[0]?.id ?? 0)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [statusId, setStatusId] = useState<number | ''>('')
  const [type, setType] = useState('')
  const [priority, setPriority] = useState<Card['priority']>('MEDIUM')
  const [assigneeId, setAssigneeId] = useState<number | null>(null)
  const [startDate, setStartDate] = useState<string | null>(null)
  const [dueDate, setDueDate] = useState<string | null>(null)

  const { data: statuses } = useQuery({ queryKey: ['statuses'], queryFn: listStatuses })
  const { data: cardTypes } = useQuery({ queryKey: ['card-types'], queryFn: listCardTypes })
  const { data: members } = useQuery({
    queryKey: ['members', workspaceId],
    queryFn: () => listMembers(workspaceId),
    enabled: !!workspaceId,
  })

  // 고르지 않았으면 첫 번째 상태·유형을 기본값으로 사용
  const selectedStatusId = statusId || statuses?.[0]?.id || ''
  const selectedType = type || cardTypes?.[0]?.name || ''

  const createMutation = useMutation({
    mutationFn: () =>
      createCard(workspaceId, {
        title,
        description: description || undefined,
        type: selectedType || undefined,
        priority,
        assigneeId,
        startDate,
        dueDate,
        statusId: selectedStatusId || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards', workspaceId] })
      onClose()
      navigate(`/projects/${workspaceId}`)
    },
  })

  const canSubmit = !!workspaceId && title.trim().length > 0

  const footer = (
    <div className="flex justify-end gap-2">
      <button type="button" onClick={onClose} className="rounded px-3 py-1.5 text-sm text-slate-500 hover:bg-slate-100">
        취소
      </button>
      <button
        type="submit"
        form="new-task-form"
        disabled={!canSubmit || createMutation.isPending}
        className="rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-40"
      >
        생성
      </button>
    </div>
  )

  return (
    <Modal title={<h2 className="text-lg font-semibold">새 작업</h2>} footer={footer} onClose={onClose} dismissible={false}>
      <form
        id="new-task-form"
        onSubmit={(e) => {
          e.preventDefault()
          if (canSubmit) createMutation.mutate()
        }}
        className="space-y-4"
      >
        <div className="grid grid-cols-[auto_1fr] gap-3 text-sm">
          <label className="block space-y-1">
            <span className="block text-xs text-slate-500">프로젝트</span>
            <select
              value={workspaceId}
              onChange={(e) => {
                setWorkspaceId(Number(e.target.value))
                setAssigneeId(null) // 담당자는 프로젝트 멤버 중에서만 선택 가능
              }}
              className="block w-40 rounded border px-3 py-2 text-sm"
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block space-y-1">
            <span className="block text-xs text-slate-500">작업명</span>
            <input
              autoFocus
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="작업명 입력"
              className="w-full rounded border px-3 py-2 text-sm"
            />
          </label>
        </div>

        <div className="grid grid-cols-3 gap-3 text-sm">
          <label className="space-y-1">
            <span className="text-xs text-slate-500">상태</span>
            <select
              value={selectedStatusId}
              onChange={(e) => setStatusId(Number(e.target.value))}
              className="w-full rounded border px-2 py-1.5"
            >
              {statuses?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1">
            <span className="text-xs text-slate-500">타입</span>
            <select value={selectedType} onChange={(e) => setType(e.target.value)} className="w-full rounded border px-2 py-1.5">
              {cardTypes?.map((t) => (
                <option key={t.id} value={t.name}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <LabeledField label="우선순위">
            <PrioritySelect value={priority} onChange={setPriority} />
          </LabeledField>
          <LabeledField label="담당자">
            <AssigneeSelect value={assigneeId} members={members ?? []} onChange={setAssigneeId} />
          </LabeledField>
          <LabeledField label="시작일">
            <DateField value={startDate} onChange={setStartDate} />
          </LabeledField>
          <LabeledField label="마감일">
            <DateField value={dueDate} onChange={setDueDate} />
          </LabeledField>
        </div>

        <label className="block space-y-1">
          <span className="text-xs text-slate-500">설명</span>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={12}
            className="w-full rounded border px-3 py-2 text-sm"
          />
        </label>
      </form>
    </Modal>
  )
}
