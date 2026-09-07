import { useQuery } from '@tanstack/react-query'
import { Navigate } from 'react-router-dom'
import { listWorkspaces } from '../api/workspaces'

export function ProjectsIndexPage() {
  const { data: projects, isLoading } = useQuery({ queryKey: ['workspaces'], queryFn: listWorkspaces })

  if (isLoading) return null
  if (projects && projects.length > 0) return <Navigate to={`/projects/${projects[0].id}`} replace />

  return (
    <div className="flex h-full items-center justify-center text-sm text-slate-400">
      좌측에서 프로젝트를 추가해주세요.
    </div>
  )
}
