// 배포(버전) 기록
import { del, get, patch, post } from './axios'
import type { Card, Release } from './types'

export const listReleases = (workspaceId: number) => get<Release[]>(`/workspaces/${workspaceId}/releases`)

// 아직 배포되지 않은 작업. statusId를 주면 해당 상태만
export const listReleaseCandidates = (workspaceId: number, statusId?: number) =>
  get<Card[]>(`/workspaces/${workspaceId}/releases/candidates${statusId ? `?statusId=${statusId}` : ''}`)

export const createRelease = (
  workspaceId: number,
  input: { version: string; notes: string; cardIds: number[]; completedStatusId?: number },
) => post<Release>(`/workspaces/${workspaceId}/releases`, input)

export const updateRelease = (releaseId: number, input: { version?: string; notes?: string }) =>
  patch<Release>(`/releases/${releaseId}`, input)

export const deleteRelease = (releaseId: number) => del<void>(`/releases/${releaseId}`)
