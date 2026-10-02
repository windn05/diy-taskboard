// 작업(카드)과 댓글
import { del, get, patch, post } from './axios'
import type { Card, Comment } from './types'

export const listCards = (workspaceId: number) => get<Card[]>(`/workspaces/${workspaceId}/cards`)

export type CreateCardInput = {
  title: string
  description?: string
  type?: string
  startDate?: string | null
  dueDate?: string | null
  statusId?: number
}

export const createCard = (workspaceId: number, input: CreateCardInput) =>
  post<Card>(`/workspaces/${workspaceId}/cards`, input)

// 보낸 필드만 변경. startDate·dueDate는 null을 보내면 비움
export const updateCard = (cardId: number, input: Partial<CreateCardInput>) =>
  patch<Card>(`/cards/${cardId}`, input)

export const deleteCard = (cardId: number) => del<void>(`/cards/${cardId}`)

export const listComments = (cardId: number) => get<Comment[]>(`/cards/${cardId}/comments`)

export const addComment = (cardId: number, content: string) =>
  post<Comment>(`/cards/${cardId}/comments`, { content })

export const deleteComment = (commentId: number) => del<void>(`/cards/comments/${commentId}`)
