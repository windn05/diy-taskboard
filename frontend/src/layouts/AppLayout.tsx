import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useMatch } from 'react-router-dom'
import { listWorkspaces } from '../api/workspaces'
import { useAuth } from '../auth/AuthContext'
import { AppHeader } from '../components/AppHeader'
import { NewTaskModal } from '../components/NewTaskModal'
import { HomeIcon, PlusIcon } from '../components/icons'
import { useProjectSocket } from '../hooks/useProjectSocket'

/** TaskBoard 앱의 셸. 관리자 기능은 별도 앱이므로 여기서 진입하지 않음 */
export function AppLayout() {
  const { isGuest } = useAuth()
  const { data: projects } = useQuery({ queryKey: ['workspaces'], queryFn: listWorkspaces })
  const [addingTask, setAddingTask] = useState(false)

  // 접속자 표시가 헤더에 있으므로 연결도 여기서 관리.
  // 덕분에 프로젝트 안에서 탭을 오갈 때 소켓이 끊겼다 붙지 않음
  const projectMatch = useMatch({ path: '/projects/:workspaceId', end: false })
  const openWorkspaceId = projectMatch?.params.workspaceId ? Number(projectMatch.params.workspaceId) : null
  const presentUsers = useProjectSocket(openWorkspaceId)

  return (
    <div className="flex h-screen">
      <aside className="flex w-64 shrink-0 flex-col border-r bg-white">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <span className="font-semibold">TaskBoard</span>
          {!isGuest && (
            <button
              onClick={() => setAddingTask(true)}
              className="rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
              title="새 작업"
            >
              <PlusIcon size={16} />
            </button>
          )}
        </div>

        <div className="flex-1 overflow-y-auto p-2">
          <NavLink
            to="/home"
            className={({ isActive }) =>
              `mb-2 flex items-center gap-2 rounded px-2 py-1.5 text-sm ${
                isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`
            }
          >
            <HomeIcon size={15} />홈
          </NavLink>

          <div className="mb-1 px-2 py-1">
            <span className="text-xs font-semibold text-slate-500">프로젝트</span>
          </div>

          <nav className="space-y-0.5">
            {projects?.map((p) => (
              <NavLink
                key={p.id}
                to={`/projects/${p.id}`}
                className={({ isActive }) =>
                  `block rounded px-2 py-1.5 text-sm ${isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'}`
                }
              >
                {p.name}
              </NavLink>
            ))}
          </nav>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <AppHeader currentApp="taskboard" presentUsers={presentUsers} />
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>

      {addingTask && projects && <NewTaskModal projects={projects} onClose={() => setAddingTask(false)} />}
    </div>
  )
}
