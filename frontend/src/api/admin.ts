// 관리자 전용 API (/admin/**). 프로젝트·멤버·계정 관리
import { api } from './client'
import type { AdminWorkspace, Member, User, Workspace } from './types'

export const adminListWorkspaces = () => api<AdminWorkspace[]>('/admin/workspaces')

export const adminCreateWorkspace = (name: string) =>
  api<Workspace>('/admin/workspaces', { method: 'POST', body: JSON.stringify({ name }) })

export const adminUpdateWorkspace = (id: number, input: { name?: string; visible?: boolean }) =>
  api<AdminWorkspace>(`/admin/workspaces/${id}`, { method: 'PATCH', body: JSON.stringify(input) })

export const adminDeleteWorkspace = (id: number) => api<void>(`/admin/workspaces/${id}`, { method: 'DELETE' })

export const adminListUsers = () => api<User[]>('/admin/users')

export const adminCreateUser = (data: { username: string; password: string; name: string; role: string }) =>
  api<User>('/admin/users', { method: 'POST', body: JSON.stringify(data) })

export const adminListMembers = (workspaceId: number) =>
  api<Member[]>(`/admin/workspaces/${workspaceId}/members`)

export const adminAddMember = (workspaceId: number, username: string, role: string) =>
  api<Member>(`/admin/workspaces/${workspaceId}/members`, {
    method: 'POST',
    body: JSON.stringify({ username, role }),
  })

export const adminRemoveMember = (workspaceId: number, userId: number) =>
  api<void>(`/admin/workspaces/${workspaceId}/members/${userId}`, { method: 'DELETE' })
