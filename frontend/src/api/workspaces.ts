import { api } from './client'
import type { Member, PresenceResponse, Workspace } from './types'

export const listWorkspaces = () => api<Workspace[]>('/workspaces')

export const listMembers = (workspaceId: number) => api<Member[]>(`/workspaces/${workspaceId}/members`)

export const getPresence = (workspaceId: number) =>
  api<PresenceResponse>(`/workspaces/${workspaceId}/presence`)

export const addMember = (workspaceId: number, username: string, role: string) =>
  api<Member>(`/workspaces/${workspaceId}/members`, { method: 'POST', body: JSON.stringify({ username, role }) })
