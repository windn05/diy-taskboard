import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { createRelease, deleteRelease, listReleaseCandidates, listReleases, updateRelease } from '../api/releases'
import { listStatuses } from '../api/statuses'
import { useAuth } from '../auth/AuthContext'
import type { Card, Release, Status } from '../api/types'
import { CheckIcon } from '../components/icons'

// 시작일 최신순. 시작일이 없는 작업은 뒤로 보낸다 (작업 목록과 동일한 규칙).
function byStartDateDesc(a: Card, b: Card) {
  if (!a.startDate) return 1
  if (!b.startDate) return -1
  return b.startDate.localeCompare(a.startDate)
}

/** 이 타입은 배포로 묶을 작업이 아니라서 후보 목록에서 뺀다. */
const EXCLUDED_TYPE = '문서 및 기타작업'

/**
 * 배포 흐름: 아직 배포되지 않은 작업 중에서 고르고, 버전·패치노트를 적어 확정한다.
 * 확정하면 고른 작업들이 지정한 상태(보통 "배포 완료")로 넘어간다.
 */
export function ProjectReleasesPage() {
  const { workspaceId } = useParams()
  const wsId = Number(workspaceId)
  const { isGuest } = useAuth()
  const queryClient = useQueryClient()

  const { data: statuses } = useQuery({ queryKey: ['statuses'], queryFn: listStatuses })
  const { data: releases } = useQuery({ queryKey: ['releases', wsId], queryFn: () => listReleases(wsId) })

  if (!statuses) return <div className="p-8 text-sm text-slate-500">불러오는 중...</div>

  return (
    <div className="space-y-6 p-8">
      {!isGuest && <NewReleaseForm workspaceId={wsId} statuses={statuses} queryClient={queryClient} />}
      <ReleaseHistory releases={releases ?? []} readOnly={isGuest} queryClient={queryClient} workspaceId={wsId} />
    </div>
  )
}

/** "개발 완료"처럼 배포 직전 단계로 보이는 상태를 기본 필터로 제안한다. 없으면 전체를 보여준다. */
function guessDefaultStatusId(statuses: Status[]) {
  return statuses.find((s) => s.name.includes('개발 완료'))?.id ?? ''
}

function guessCompletedStatusId(statuses: Status[]) {
  return statuses.find((s) => s.name.includes('배포 완료'))?.id ?? ''
}

type QueryClient = ReturnType<typeof useQueryClient>

function NewReleaseForm({
  workspaceId,
  statuses,
  queryClient,
}: {
  workspaceId: number
  statuses: Status[]
  queryClient: QueryClient
}) {
  const [version, setVersion] = useState('')
  const [notes, setNotes] = useState('')
  const [filterStatusId, setFilterStatusId] = useState<number | ''>(() => guessDefaultStatusId(statuses))
  const [completedStatusId, setCompletedStatusId] = useState<number | ''>(() => guessCompletedStatusId(statuses))
  const [selected, setSelected] = useState<number[]>([])

  const { data: candidatesRaw } = useQuery({
    queryKey: ['release-candidates', workspaceId, filterStatusId],
    queryFn: () => listReleaseCandidates(workspaceId, filterStatusId || undefined),
  })
  const candidates = useMemo(
    () => candidatesRaw && candidatesRaw.filter((c) => c.type !== EXCLUDED_TYPE).sort(byStartDateDesc),
    [candidatesRaw],
  )

  const createMutation = useMutation({
    mutationFn: () =>
      createRelease(workspaceId, {
        version: version.trim(),
        notes,
        cardIds: selected,
        completedStatusId: completedStatusId || undefined,
      }),
    onSuccess: () => {
      setVersion('')
      setNotes('')
      setSelected([])
      queryClient.invalidateQueries({ queryKey: ['releases', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['release-candidates', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['cards', workspaceId] })
    },
    onError: (error: Error) => alert(error.message),
  })

  const toggle = (cardId: number) =>
    setSelected((prev) => (prev.includes(cardId) ? prev.filter((id) => id !== cardId) : [...prev, cardId]))

  const canSubmit = version.trim().length > 0 && selected.length > 0

  return (
    <section className="rounded-lg border bg-white p-5">
      <h2 className="mb-4 text-sm font-semibold text-slate-700">새 배포</h2>

      <div className="mb-4 grid grid-cols-2 gap-3">
        <label className="space-y-1">
          <span className="text-xs text-slate-500">버전</span>
          <input
            value={version}
            onChange={(e) => setVersion(e.target.value)}
            placeholder="v1.0.0"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
        </label>
        <label className="space-y-1">
          <span className="text-xs text-slate-500">배포 후 작업 상태</span>
          <select
            value={completedStatusId}
            onChange={(e) => setCompletedStatusId(e.target.value ? Number(e.target.value) : '')}
            className="w-full rounded border px-2 py-1.5 text-sm"
          >
            <option value="">변경하지 않음</option>
            {statuses.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      <label className="mb-4 block space-y-1">
        <span className="text-xs text-slate-500">패치노트</span>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={5}
          placeholder="이번 배포에 포함된 변경 사항을 적어주세요."
          className="w-full rounded border px-2 py-1.5 text-sm"
        />
      </label>

      <div className="mb-2 flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-500">포함할 작업 ({selected.length}개 선택)</span>
        <select
          value={filterStatusId}
          onChange={(e) => {
            setFilterStatusId(e.target.value ? Number(e.target.value) : '')
            setSelected([])
          }}
          className="rounded border px-2 py-1 text-xs"
        >
          <option value="">모든 상태</option>
          {statuses.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
      </div>

      <ul className="mb-4 max-h-60 overflow-y-auto rounded border">
        {candidates?.map((card) => (
          <li key={card.id} className="border-b last:border-0">
            <label className="flex cursor-pointer items-center gap-2 px-3 py-2 text-sm hover:bg-slate-50">
              <input type="checkbox" checked={selected.includes(card.id)} onChange={() => toggle(card.id)} />
              <span className="flex-1">{card.title}</span>
              <span className="text-xs text-slate-500">{card.type}</span>
            </label>
          </li>
        ))}
        {candidates?.length === 0 && (
          <li className="px-3 py-6 text-center text-sm text-slate-500">배포할 수 있는 작업이 없습니다.</li>
        )}
      </ul>

      <button
        onClick={() => canSubmit && createMutation.mutate()}
        disabled={!canSubmit || createMutation.isPending}
        className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
      >
        배포 확정
      </button>
    </section>
  )
}

function ReleaseHistory({
  releases,
  readOnly,
  queryClient,
  workspaceId,
}: {
  releases: Release[]
  readOnly: boolean
  queryClient: QueryClient
  workspaceId: number
}) {
  return (
    <section>
      <h2 className="mb-3 text-sm font-semibold text-slate-700">배포 이력</h2>
      <div className="space-y-3">
        {releases.map((release) => (
          <ReleaseCard
            key={release.id}
            release={release}
            readOnly={readOnly}
            queryClient={queryClient}
            workspaceId={workspaceId}
          />
        ))}
        {releases.length === 0 && (
          <p className="rounded-lg border bg-white px-4 py-8 text-center text-sm text-slate-500">
            아직 배포 기록이 없습니다.
          </p>
        )}
      </div>
    </section>
  )
}

function ReleaseCard({
  release,
  readOnly,
  queryClient,
  workspaceId,
}: {
  release: Release
  readOnly: boolean
  queryClient: QueryClient
  workspaceId: number
}) {
  const [editing, setEditing] = useState(false)
  const [notes, setNotes] = useState(release.notes ?? '')

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['releases', workspaceId] })
    queryClient.invalidateQueries({ queryKey: ['release-candidates', workspaceId] })
    queryClient.invalidateQueries({ queryKey: ['cards', workspaceId] })
  }

  const updateMutation = useMutation({
    mutationFn: () => updateRelease(release.id, { notes }),
    onSuccess: () => {
      setEditing(false)
      invalidate()
    },
    onError: (error: Error) => alert(error.message),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteRelease(release.id),
    onSuccess: invalidate,
    onError: (error: Error) => alert(error.message),
  })

  function handleDelete() {
    if (confirm(`${release.version} 배포를 취소할까요? 포함된 작업은 다시 미배포 상태가 됩니다.`)) {
      deleteMutation.mutate()
    }
  }

  return (
    <article className="rounded-lg border bg-white p-4">
      <div className="mb-2 flex items-baseline gap-2">
        <h3 className="text-base font-semibold text-slate-800">{release.version}</h3>
        <span className="text-xs text-slate-500">{new Date(release.releasedAt).toLocaleString()}</span>
        <span className="text-xs text-slate-500">· 작업 {release.cards.length}건</span>
        {!readOnly && (
          <div className="ml-auto flex gap-2">
            <button
              onClick={() => setEditing((v) => !v)}
              className="text-xs text-slate-500 hover:text-slate-800 hover:underline"
            >
              {editing ? '취소' : '패치노트 수정'}
            </button>
            <button onClick={handleDelete} className="text-xs text-red-600 hover:underline">
              배포 취소
            </button>
          </div>
        )}
      </div>

      {editing ? (
        <div className="mb-3">
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={5}
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <button
            onClick={() => updateMutation.mutate()}
            className="mt-2 rounded bg-slate-900 px-3 py-1.5 text-xs text-white"
          >
            저장
          </button>
        </div>
      ) : (
        release.notes && (
          <p className="mb-3 whitespace-pre-wrap rounded bg-slate-50 px-3 py-2 text-sm text-slate-700">
            {release.notes}
          </p>
        )
      )}

      <ul className="space-y-1">
        {release.cards.map((card) => (
          <li key={card.id} className="flex items-center gap-2 text-sm text-slate-600">
            <CheckIcon size={14} className="shrink-0 text-emerald-500" />
            <span className="flex-1">{card.title}</span>
            <span className="text-xs text-slate-500">{card.type}</span>
          </li>
        ))}
      </ul>
    </article>
  )
}
