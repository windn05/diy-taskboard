import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminAddMember, adminListMembers, adminListUsers, adminRemoveMember } from '../../api/admin'

const ROLES = [
  { value: 'MEMBER', label: '멤버' },
  { value: 'ADMIN', label: '관리자' },
  { value: 'OWNER', label: '소유자' },
]

const ROLE_LABEL: Record<string, string> = { OWNER: '소유자', ADMIN: '관리자', MEMBER: '멤버' }

/**
 * 프로젝트 멤버 추가/제거. 프로젝트 목록은 멤버십 기준으로 보이므로,
 * 여기서 추가해야 해당 사용자의 TaskBoard에 그 프로젝트가 노출
 */
export function WorkspaceMembersPanel({ workspaceId }: { workspaceId: number }) {
  const queryClient = useQueryClient()
  const [username, setUsername] = useState('')
  const [role, setRole] = useState('MEMBER')

  const { data: members } = useQuery({
    queryKey: ['admin-members', workspaceId],
    queryFn: () => adminListMembers(workspaceId),
  })
  const { data: users } = useQuery({ queryKey: ['admin-users'], queryFn: adminListUsers })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['admin-members', workspaceId] })
    // 멤버십이 바뀌면 해당 사용자에게 보이는 프로젝트 목록도 변경
    queryClient.invalidateQueries({ queryKey: ['workspaces'] })
  }

  const showError = (error: Error) => alert(error.message)

  const addMutation = useMutation({
    mutationFn: () => adminAddMember(workspaceId, username, role),
    onSuccess: () => {
      setUsername('')
      invalidate()
    },
    onError: showError,
  })

  const removeMutation = useMutation({
    mutationFn: (userId: number) => adminRemoveMember(workspaceId, userId),
    onSuccess: invalidate,
    onError: showError,
  })

  const memberIds = new Set(members?.map((m) => m.userId))
  const candidates = users?.filter((u) => !memberIds.has(u.id)) ?? []

  return (
    <div className="rounded border bg-slate-50 p-3">
      <p className="mb-2 text-xs font-semibold text-slate-500">멤버</p>

      <ul className="mb-3 space-y-1">
        {members?.map((member) => (
          <li key={member.userId} className="flex items-center gap-2 text-sm">
            <span className="font-medium">{member.name}</span>
            <span className="text-xs text-slate-500">{member.username}</span>
            <span className="rounded bg-white px-1.5 py-0.5 text-[11px] text-slate-500">
              {ROLE_LABEL[member.role] ?? member.role}
            </span>
            <button
              onClick={() => removeMutation.mutate(member.userId)}
              className="ml-auto text-xs text-red-600 hover:underline"
            >
              제외
            </button>
          </li>
        ))}
        {members?.length === 0 && <li className="text-xs text-slate-500">멤버가 없습니다.</li>}
      </ul>

      {candidates.length > 0 ? (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (username) addMutation.mutate()
          }}
          className="flex gap-1"
        >
          <select
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="flex-1 rounded border px-2 py-1 text-sm"
          >
            <option value="">추가할 사용자 선택</option>
            {candidates.map((user) => (
              <option key={user.id} value={user.username}>
                {user.name} ({user.username})
              </option>
            ))}
          </select>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            className="rounded border px-2 py-1 text-sm"
          >
            {ROLES.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
          <button type="submit" className="rounded bg-slate-900 px-3 py-1 text-xs text-white">
            추가
          </button>
        </form>
      ) : (
        <p className="text-xs text-slate-500">추가할 수 있는 사용자가 없습니다.</p>
      )}
    </div>
  )
}
