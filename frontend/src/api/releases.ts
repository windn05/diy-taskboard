import { api } from './client'
import type { Card, Release } from './types'

export const listReleases = (workspaceId: number) => api<Release[]>(`/workspaces/${workspaceId}/releases`)

export const listReleaseCandidates = (workspaceId: number, statusId?: number) =>
  api<Card[]>(
    `/workspaces/${workspaceId}/releases/candidates${statusId ? `?statusId=${statusId}` : ''}`,
  )

export const createRelease = (
  workspaceId: number,
  input: { version: string; notes: string; cardIds: number[]; completedStatusId?: number },
) => api<Release>(`/workspaces/${workspaceId}/releases`, { method: 'POST', body: JSON.stringify(input) })

export const updateRelease = (releaseId: number, input: { version?: string; notes?: string }) =>
  api<Release>(`/releases/${releaseId}`, { method: 'PATCH', body: JSON.stringify(input) })

export const deleteRelease = (releaseId: number) => api<void>(`/releases/${releaseId}`, { method: 'DELETE' })
