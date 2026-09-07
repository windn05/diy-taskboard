import type { PresenceUser } from '../api/types'

const AVATAR_COLOR = [
  'bg-sky-500',
  'bg-violet-500',
  'bg-emerald-500',
  'bg-amber-500',
  'bg-rose-500',
  'bg-cyan-500',
]

function colorFor(userId: number) {
  return AVATAR_COLOR[Math.abs(userId) % AVATAR_COLOR.length]
}

function initial(username: string) {
  return username.startsWith('guest-') ? 'G' : username[0]?.toUpperCase() ?? '?'
}

export function PresenceBar({ users, meId }: { users: PresenceUser[]; meId?: number }) {
  if (users.length === 0) {
    return <span className="text-xs text-slate-400">접속자 없음</span>
  }

  return (
    <div className="flex items-center gap-2">
      <div className="flex -space-x-2">
        {users.map((u) => (
          <span
            key={u.userId}
            title={`${u.username}${u.guest ? ' (게스트)' : ''}${u.userId === meId ? ' · 나' : ''}`}
            className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold text-white ring-2 ${
              u.guest ? 'bg-slate-400 ring-slate-300' : `${colorFor(u.userId)} ring-white`
            }`}
          >
            {initial(u.username)}
          </span>
        ))}
      </div>
      <span className="text-xs text-slate-400">{users.length}명 접속 중</span>
    </div>
  )
}
