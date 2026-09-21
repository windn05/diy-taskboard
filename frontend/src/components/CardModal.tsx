import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addComment, deleteCard, deleteComment, listComments, updateCard } from '../api/cards'
import type { Card, CardTypeDef, Member, Status } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AssigneeSelect, DateField, LabeledField, PrioritySelect } from './fields'
import { Modal, SaveIndicator } from './Modal'

export function CardModal({
  card,
  members,
  statuses,
  cardTypes,
  onClose,
  readOnly,
}: {
  card: Card
  members: Member[]
  statuses: Status[]
  cardTypes: CardTypeDef[]
  onClose: () => void
  /** 달력 미리보기 등, 게스트가 아니어도 강제로 읽기 전용 보기로 열 때 쓴다. */
  readOnly?: boolean
}) {
  const queryClient = useQueryClient()
  const { user, isGuest } = useAuth()
  const locked = isGuest || !!readOnly
  const [title, setTitle] = useState(card.title)
  const [description, setDescription] = useState(card.description ?? '')
  const [commentText, setCommentText] = useState('')
  const [savedAt, setSavedAt] = useState<number | null>(null)

  const { data: comments } = useQuery({
    queryKey: ['comments', card.id],
    queryFn: () => listComments(card.id),
  })

  const updateMutation = useMutation({
    mutationFn: (input: Parameters<typeof updateCard>[1]) => updateCard(card.id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards', card.workspaceId] })
      setSavedAt(Date.now())
    },
    onError: (error: Error) => alert(error.message),
  })

  // 저장 표시는 잠깐만 띄운다. 계속 남아 있으면 방금 저장한 건지 예전 것인지 헷갈린다.
  useEffect(() => {
    if (savedAt === null) return
    const timer = setTimeout(() => setSavedAt(null), 2000)
    return () => clearTimeout(timer)
  }, [savedAt])

  const deleteMutation = useMutation({
    mutationFn: () => deleteCard(card.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards', card.workspaceId] })
      onClose()
    },
    onError: (error: Error) => alert(error.message),
  })

  const commentMutation = useMutation({
    mutationFn: () => addComment(card.id, commentText),
    onSuccess: () => {
      setCommentText('')
      queryClient.invalidateQueries({ queryKey: ['comments', card.id] })
    },
    onError: (error: Error) => alert(error.message),
  })

  const deleteCommentMutation = useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['comments', card.id] }),
    onError: (error: Error) => alert(error.message),
  })

  function handleDelete() {
    if (confirm(`"${card.title}" 작업을 삭제할까요? 되돌릴 수 없습니다.`)) deleteMutation.mutate()
  }

  const header = (
    <div>
      <input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        onBlur={() => title.trim() && title !== card.title && updateMutation.mutate({ title })}
        readOnly={locked}
        placeholder="작업명"
        className="w-full rounded border border-transparent px-2 py-1 text-lg font-semibold outline-none read-only:text-slate-600 hover:border-slate-200 focus:border-slate-300 focus:bg-white"
      />
      <div className="mt-1 flex h-4 items-center px-2">
        <SaveIndicator saving={updateMutation.isPending} saved={savedAt !== null} />
      </div>
    </div>
  )

  const footer = (
    <div className="flex items-center justify-between">
      {locked ? (
        <p className="text-xs text-slate-500">{isGuest ? '게스트는 읽기 전용입니다.' : '미리보기 — 읽기 전용입니다.'}</p>
      ) : (
        <button onClick={handleDelete} className="text-sm text-red-600 hover:underline">
          작업 삭제
        </button>
      )}
      <button
        onClick={onClose}
        className="ml-auto rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white hover:bg-slate-800"
      >
        닫기
      </button>
    </div>
  )

  return (
    <Modal title={header} footer={footer} onClose={onClose} dismissible={false}>
      <div className="mb-5 grid grid-cols-3 gap-3 text-sm">
        <LabeledField label="상태">
          <select
            value={card.statusId}
            onChange={(e) => updateMutation.mutate({ statusId: Number(e.target.value) })}
            disabled={locked}
            className="w-full rounded border px-2 py-1.5 disabled:bg-slate-100 disabled:text-slate-500"
          >
            {statuses.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </LabeledField>
        <LabeledField label="타입">
          <select
            value={card.type}
            onChange={(e) => updateMutation.mutate({ type: e.target.value })}
            disabled={locked}
            className="w-full rounded border px-2 py-1.5 disabled:bg-slate-100 disabled:text-slate-500"
          >
            {!cardTypes.some((t) => t.name === card.type) && <option value={card.type}>{card.type}</option>}
            {cardTypes.map((t) => (
              <option key={t.id} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>
        </LabeledField>
        <LabeledField label="우선순위">
          <PrioritySelect
            value={card.priority}
            onChange={(priority) => updateMutation.mutate({ priority })}
            disabled={locked}
          />
        </LabeledField>
        <LabeledField label="담당자">
          <AssigneeSelect
            value={card.assigneeId}
            members={members}
            onChange={(assigneeId) => updateMutation.mutate({ assigneeId })}
            disabled={locked}
          />
        </LabeledField>
        <LabeledField label="시작일">
          <DateField
            value={card.startDate}
            onChange={(startDate) => updateMutation.mutate({ startDate })}
            disabled={locked}
          />
        </LabeledField>
        <LabeledField label="마감일">
          <DateField
            value={card.dueDate}
            onChange={(dueDate) => updateMutation.mutate({ dueDate })}
            disabled={locked}
          />
        </LabeledField>
      </div>

      <label className="mb-5 block space-y-1">
        <span className="text-xs text-slate-500">설명</span>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          onBlur={() => description !== (card.description ?? '') && updateMutation.mutate({ description })}
          rows={12}
          readOnly={locked}
          placeholder={locked ? '' : '입력 후 다른 곳을 클릭하면 저장됩니다.'}
          className="w-full rounded border px-3 py-2 text-sm read-only:bg-slate-100 read-only:text-slate-500"
        />
      </label>

      <section>
        <h4 className="mb-2 text-xs font-semibold text-slate-500">댓글 {comments?.length ?? 0}</h4>

        {!locked && (
          <form
            onSubmit={(e) => {
              e.preventDefault()
              if (commentText.trim()) commentMutation.mutate()
            }}
            className="mb-3 flex gap-2"
          >
            <input
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="댓글 입력"
              className="flex-1 rounded border px-3 py-1.5 text-sm"
            />
            <button
              type="submit"
              disabled={commentMutation.isPending}
              className="rounded bg-slate-900 px-3 py-1.5 text-sm text-white disabled:opacity-50"
            >
              등록
            </button>
          </form>
        )}

        <div className="space-y-2">
          {comments?.map((c) => (
            <div key={c.id} className="group rounded bg-slate-50 px-3 py-2 text-sm">
              <div className="mb-1 flex items-baseline gap-2">
                <span className="text-xs font-semibold text-slate-700">{c.authorName}</span>
                <span className="text-[11px] text-slate-500">{new Date(c.createdAt).toLocaleString()}</span>
                {!locked && user?.userId === c.userId && (
                  <button
                    onClick={() => confirm('댓글을 삭제할까요?') && deleteCommentMutation.mutate(c.id)}
                    className="ml-auto text-[11px] text-slate-500 opacity-0 hover:text-red-600 group-hover:opacity-100"
                  >
                    삭제
                  </button>
                )}
              </div>
              <p className="whitespace-pre-wrap">{c.content}</p>
            </div>
          ))}
          {comments?.length === 0 && <p className="py-3 text-center text-sm text-slate-500">댓글이 없습니다.</p>}
        </div>
      </section>
    </Modal>
  )
}
