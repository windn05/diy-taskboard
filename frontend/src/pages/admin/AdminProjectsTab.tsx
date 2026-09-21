import { Fragment, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminCreateWorkspace, adminDeleteWorkspace, adminListWorkspaces, adminUpdateWorkspace } from '../../api/admin'
import { WorkspaceMembersPanel } from './WorkspaceMembersPanel'

/** 전체 프로젝트 관리: 생성·이름 변경·게스트 공개·멤버 관리·삭제 */
export function AdminProjectsTab() {
  const queryClient = useQueryClient()
  const [newProjectName, setNewProjectName] = useState('')
  const [openMembersFor, setOpenMembersFor] = useState<number | null>(null)

  const { data: projects } = useQuery({ queryKey: ['admin-workspaces'], queryFn: adminListWorkspaces })

  // 관리자 목록과 사이드바의 프로젝트 목록을 함께 갱신
  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['admin-workspaces'] })
    queryClient.invalidateQueries({ queryKey: ['workspaces'] })
  }

  const createMutation = useMutation({
    mutationFn: () => adminCreateWorkspace(newProjectName),
    onSuccess: () => {
      setNewProjectName('')
      invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, input }: { id: number; input: { name?: string; visible?: boolean } }) =>
      adminUpdateWorkspace(id, input),
    onSuccess: invalidate,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminDeleteWorkspace(id),
    onSuccess: invalidate,
  })

  function handleDelete(id: number, name: string) {
    if (confirm(`"${name}" 프로젝트와 소속된 모든 작업을 삭제합니다. 계속할까요?`)) {
      deleteMutation.mutate(id)
    }
  }

  return (
    <div className="max-w-3xl p-8">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (newProjectName.trim()) createMutation.mutate()
        }}
        className="mb-6 flex gap-2"
      >
        <input
          value={newProjectName}
          onChange={(e) => setNewProjectName(e.target.value)}
          placeholder="새 프로젝트 이름"
          className="flex-1 rounded border px-3 py-2 text-sm"
        />
        <button type="submit" className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
          추가
        </button>
      </form>

      <div className="overflow-hidden rounded-lg border bg-white">
        <table className="w-full text-sm">
          <thead className="border-b bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">프로젝트명</th>
              <th className="w-32 px-4 py-2 font-medium">게스트 공개</th>
              <th className="w-20 px-4 py-2 font-medium"></th>
              <th className="w-16 px-4 py-2 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {projects?.map((p) => (
              <Fragment key={p.id}>
              <tr className="border-b last:border-0">
                <td className="px-4 py-2">
                  <input
                    defaultValue={p.name}
                    key={p.id}
                    onBlur={(e) =>
                      e.target.value.trim() &&
                      e.target.value !== p.name &&
                      updateMutation.mutate({ id: p.id, input: { name: e.target.value } })
                    }
                    className="w-full rounded border-transparent px-2 py-1 hover:border-slate-200 focus:border-slate-300"
                  />
                </td>
                <td className="px-4 py-2">
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={p.visible}
                      onChange={(e) => updateMutation.mutate({ id: p.id, input: { visible: e.target.checked } })}
                    />
                    <span className="text-xs text-slate-500">{p.visible ? '공개' : '비공개'}</span>
                  </label>
                </td>
                <td className="px-4 py-2 text-center">
                  <button
                    onClick={() => setOpenMembersFor((id) => (id === p.id ? null : p.id))}
                    className="text-xs text-slate-500 hover:text-slate-800 hover:underline"
                  >
                    멤버 {openMembersFor === p.id ? '닫기' : '관리'}
                  </button>
                </td>
                <td className="px-4 py-2 text-right">
                  <button onClick={() => handleDelete(p.id, p.name)} className="text-xs text-red-600 hover:underline">
                    삭제
                  </button>
                </td>
              </tr>
              {openMembersFor === p.id && (
                <tr className="border-b last:border-0">
                  <td colSpan={4} className="px-4 pb-3">
                    <WorkspaceMembersPanel workspaceId={p.id} />
                  </td>
                </tr>
              )}
              </Fragment>
            ))}

            {projects?.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-slate-500">
                  프로젝트가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
