import { api } from './client'
import type { Card, CardPriority, Comment } from './types'

export const listCards = (workspaceId: number) => api<Card[]>(`/workspaces/${workspaceId}/cards`)

export type CreateCardInput = {
  title: string
  description?: string
  type?: string
  priority?: CardPriority
  assigneeId?: number | null
  labels?: string[]
  startDate?: string | null
  dueDate?: string | null
  statusId?: number
}

export const createCard = (workspaceId: number, input: CreateCardInput) =>
  api<Card>(`/workspaces/${workspaceId}/cards`, { method: 'POST', body: JSON.stringify(input) })

export const updateCard = (cardId: number, input: Partial<CreateCardInput>) =>
  api<Card>(`/cards/${cardId}`, { method: 'PATCH', body: JSON.stringify(input) })

export const deleteCard = (cardId: number) => api<void>(`/cards/${cardId}`, { method: 'DELETE' })

export const listComments = (cardId: number) => api<Comment[]>(`/cards/${cardId}/comments`)

export const addComment = (cardId: number, content: string) =>
  api<Comment>(`/cards/${cardId}/comments`, { method: 'POST', body: JSON.stringify({ content }) })

export const deleteComment = (commentId: number) => api<void>(`/cards/comments/${commentId}`, { method: 'DELETE' })
