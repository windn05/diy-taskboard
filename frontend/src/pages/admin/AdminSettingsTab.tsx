import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminCreateStatus, adminDeleteStatus, adminRenameStatus, listStatuses } from '../../api/statuses'
import { adminCreateCardType, adminDeleteCardType, adminUpdateCardType, listCardTypes } from '../../api/cardTypes'

type NamedItem = { id: number; name: string }

type PanelProps = {
  title: string
  placeholder: string
  queryKey: string
  list: () => Promise<NamedItem[]>
  create: (name: string) => Promise<unknown>
  rename: (id: number, name: string) => Promise<unknown>
  remove: (id: number) => Promise<unknown>
}

export function AdminSettingsTab() {
  return (
    <div className="grid max-w-3xl grid-cols-2 gap-8 p-8">
      <NamedListPanel
        title="작업 상태"
        placeholder="새 상태 이름"
        queryKey="statuses"
        list={listStatuses}
        create={adminCreateStatus}
        rename={adminRenameStatus}
        remove={adminDeleteStatus}
      />
      <NamedListPanel
        title="작업 타입"
        placeholder="새 타입 이름"
        queryKey="card-types"
        list={listCardTypes}
        create={adminCreateCardType}
        rename={adminUpdateCardType}
        remove={adminDeleteCardType}
      />
    </div>
  )
}

/** 이름만 가진 전역 목록(상태·타입)의 추가/이름변경/삭제 패널. */
function NamedListPanel({ title, placeholder, queryKey, list, create, rename, remove }: PanelProps) {
  const queryClient = useQueryClient()
  const [newName, setNewName] = useState('')

  const { data: items } = useQuery({ queryKey: [queryKey], queryFn: list })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: [queryKey] })
  }

  // 삭제는 서버가 거절할 수 있다(예: 작업이 남아있는 상태). 조용히 실패하면 사용자가 이유를 알 수 없다.
  const showError = (error: Error) => alert(error.message)

  const createMutation = useMutation({
    mutationFn: () => create(newName),
    onSuccess: () => {
      setNewName('')
      invalidate()
    },
    onError: showError,
  })

  const renameMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => rename(id, name),
    onSuccess: invalidate,
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => remove(id),
    onSuccess: invalidate,
    onError: showError,
  })

  return (
    <section className="rounded-lg border bg-white p-5">
      <h2 className="mb-3 text-sm font-semibold text-slate-700">{title}</h2>

      <ul className="mb-3 space-y-1.5">
        {items?.map((item) => (
          <li key={item.id} className="flex items-center gap-2">
            <input
              key={item.id}
              defaultValue={item.name}
              onBlur={(e) =>
                e.target.value.trim() &&
                e.target.value !== item.name &&
                renameMutation.mutate({ id: item.id, name: e.target.value })
              }
              className="flex-1 rounded border px-2 py-1 text-sm"
            />
            <button onClick={() => deleteMutation.mutate(item.id)} className="text-xs text-red-600 hover:underline">
              삭제
            </button>
          </li>
        ))}
      </ul>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (newName.trim()) createMutation.mutate()
        }}
        className="flex gap-1"
      >
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder={placeholder}
          className="flex-1 rounded border px-2 py-1.5 text-sm"
        />
        <button type="submit" className="rounded bg-slate-900 px-3 py-1.5 text-xs text-white">
          추가
        </button>
      </form>
    </section>
  )
}
