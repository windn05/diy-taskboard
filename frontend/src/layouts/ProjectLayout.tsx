import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useParams } from 'react-router-dom'
import { listWorkspaces } from '../api/workspaces'

/** 프로젝트 안에서 작업 목록과 배포를 오가는 탭. */
export function ProjectLayout() {
  const { workspaceId } = useParams()
  const { data: projects } = useQuery({ queryKey: ['workspaces'], queryFn: listWorkspaces })
  const project = projects?.find((p) => String(p.id) === workspaceId)

  const tabs = [
    { to: `/projects/${workspaceId}`, label: '작업 목록', end: true },
    { to: `/projects/${workspaceId}/releases`, label: '배포', end: false },
  ]

  return (
    <div className="flex h-full flex-col">
      <div className="shrink-0 border-b bg-white px-8 pt-5">
        <h1 className="mb-4 text-xl font-semibold">{project?.name ?? '프로젝트'}</h1>
        <nav className="flex gap-6">
          {tabs.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `border-b-2 pb-3 text-sm font-medium ${
                  isActive
                    ? 'border-slate-900 text-slate-900'
                    : 'border-transparent text-slate-500 hover:text-slate-700'
                }`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="flex-1 overflow-y-auto">
        <Outlet />
      </div>
    </div>
  )
}
