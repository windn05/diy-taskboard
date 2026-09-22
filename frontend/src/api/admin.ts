// 관리자 전용 API (/admin/**). 프로젝트·멤버·계정 관리
import { del, get, patch, post } from './axios'
import type { AdminWorkspace, Member, User, Workspace } from './types'

export const adminListWorkspaces = () => get<AdminWorkspace[]>('/admin/workspaces')

export const adminCreateWorkspace = (name: string) => post<Workspace>('/admin/workspaces', { name })

export const adminUpdateWorkspace = (id: number, input: { name?: string; visible?: boolean }) =>
  patch<AdminWorkspace>(`/admin/workspaces/${id}`, input)

export const adminDeleteWorkspace = (id: number) => del<void>(`/admin/workspaces/${id}`)

export const adminListUsers = () => get<User[]>('/admin/users')

export const adminCreateUser = (data: { username: string; password: string; name: string; role: string }) =>
  post<User>('/admin/users', data)

export const adminListMembers = (workspaceId: number) => get<Member[]>(`/admin/workspaces/${workspaceId}/members`)

export const adminAddMember = (workspaceId: number, username: string, role: string) =>
  post<Member>(`/admin/workspaces/${workspaceId}/members`, { username, role })

export const adminRemoveMember = (workspaceId: number, userId: number) =>
  del<void>(`/admin/workspaces/${workspaceId}/members/${userId}`)
