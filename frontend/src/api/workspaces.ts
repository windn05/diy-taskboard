// 사용자 관점의 프로젝트 조회. 생성·멤버 관리는 admin.ts
import { api } from './client'
import type { Member, PresenceResponse, Workspace } from './types'

export const listWorkspaces = () => api<Workspace[]>('/workspaces')

export const listMembers = (workspaceId: number) => api<Member[]>(`/workspaces/${workspaceId}/members`)

// 접속자 초기 목록. 이후 변경은 WebSocket으로 수신
export const getPresence = (workspaceId: number) =>
  api<PresenceResponse>(`/workspaces/${workspaceId}/presence`)
