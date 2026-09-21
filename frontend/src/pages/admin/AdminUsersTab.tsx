import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminCreateUser, adminListUsers } from '../../api/admin'
import { ApiError } from '../../api/client'

const ROLE_LABELS: Record<string, string> = { ADMIN: '관리자', USER: '일반 사용자' }

/** 계정 생성은 여기서만 가능 — 공개 회원가입 화면 없음 */
export function AdminUsersTab() {
  const queryClient = useQueryClient()
  const { data: users } = useQuery({ queryKey: ['admin-users'], queryFn: adminListUsers })

  const [username, setUsername] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('USER')
  const [error, setError] = useState<string | null>(null)

  const createMutation = useMutation({
    mutationFn: () => adminCreateUser({ username, name, password, role }),
    onSuccess: () => {
      setUsername('')
      setName('')
      setPassword('')
      setRole('USER')
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (err: Error) => setError(err instanceof ApiError ? err.message : '계정 생성에 실패했습니다.'),
  })

  const canSubmit = username.trim() && name.trim() && password.length >= 8

  return (
    <div className="grid max-w-4xl gap-8 p-8 md:grid-cols-[1fr_320px]">
      <section className="rounded-lg border bg-white p-5">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">계정 목록</h2>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-xs text-slate-500">
              <th className="pb-2 font-medium">아이디</th>
              <th className="pb-2 font-medium">이름</th>
              <th className="pb-2 font-medium">역할</th>
            </tr>
          </thead>
          <tbody>
            {users?.map((user) => (
              <tr key={user.id} className="border-b last:border-0">
                <td className="py-2 text-slate-700">{user.username}</td>
                <td className="py-2 text-slate-500">{user.name}</td>
                <td className="py-2">
                  <span
                    className={`rounded px-1.5 py-0.5 text-[11px] ${
                      user.role === 'ADMIN' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {ROLE_LABELS[user.role] ?? user.role}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="h-fit rounded-lg border bg-white p-5">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">새 계정</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (canSubmit) createMutation.mutate()
          }}
          className="space-y-2"
        >
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="아이디"
            autoComplete="off"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="이름"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호 (8자 이상)"
            autoComplete="new-password"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            className="w-full rounded border px-2 py-1.5 text-sm"
          >
            <option value="USER">일반 사용자</option>
            <option value="ADMIN">관리자</option>
          </select>

          {error && <p className="text-xs text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={!canSubmit || createMutation.isPending}
            className="w-full rounded bg-slate-900 px-3 py-2 text-xs font-medium text-white disabled:opacity-40"
          >
            계정 만들기
          </button>
        </form>
        <p className="mt-3 text-[11px] leading-relaxed text-slate-500">
          만든 계정의 비밀번호는 본인이 로그인한 뒤 헤더에서 직접 바꿉니다. 관리자가 대신 바꿀 수는 없습니다.
        </p>
      </section>
    </div>
  )
}
