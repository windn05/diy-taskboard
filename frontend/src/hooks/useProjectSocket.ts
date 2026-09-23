import { useEffect, useState } from 'react'
import { useQueryClient, type QueryClient } from '@tanstack/react-query'
import { createStompClient } from '../api/stomp'
import { getPresence } from '../api/workspaces'
import type { Card, Comment, PresenceUser } from '../api/types'

type CardEvent = { type: 'CREATED' | 'UPDATED' | 'MOVED' | 'DELETED'; card: Card }
type CommentEvent = { type: 'CREATED' | 'DELETED'; workspaceId: number; cardId: number; comment: Comment }

/** 작업 이벤트를 목록 캐시에 반영. 모르는 작업의 UPDATED는 새로 추가(생성 이벤트를 놓친 경우) */
function applyCardEvent(queryClient: QueryClient, workspaceId: number, event: CardEvent) {
  queryClient.setQueryData<Card[]>(['cards', workspaceId], (cards) => {
    if (!cards) return cards
    if (event.type === 'DELETED') return cards.filter((c) => c.id !== event.card.id)
    const index = cards.findIndex((c) => c.id === event.card.id)
    if (index === -1) return [...cards, event.card]
    return cards.map((c) => (c.id === event.card.id ? event.card : c))
  })
}

/** 댓글 이벤트를 열려 있는 댓글 목록과 작업의 댓글 수에 반영 */
function applyCommentEvent(queryClient: QueryClient, workspaceId: number, event: CommentEvent) {
  queryClient.setQueryData<Comment[]>(['comments', event.cardId], (comments) => {
    if (!comments) return comments
    if (event.type === 'DELETED') return comments.filter((c) => c.id !== event.comment.id)
    if (comments.some((c) => c.id === event.comment.id)) return comments
    return [...comments, event.comment]
  })

  // 작업 목록의 댓글 수도 다시 불러오지 않고 그 자리에서 갱신
  queryClient.setQueryData<Card[]>(['cards', workspaceId], (cards) => {
    if (!cards) return cards
    const delta = event.type === 'DELETED' ? -1 : 1
    return cards.map((c) => (c.id === event.cardId ? { ...c, commentCount: Math.max(0, c.commentCount + delta) } : c))
  })
}

/**
 * 프로젝트 화면에서 쓰는 단일 STOMP 연결. 접속자·작업·댓글 세 채널을 함께 구독하고,
 * 수신한 이벤트는 재요청 없이 react-query 캐시에 직접 반영
 */
export function useProjectSocket(workspaceId: number | null) {
  const queryClient = useQueryClient()
  const [presentUsers, setPresentUsers] = useState<PresenceUser[]>([])

  useEffect(() => {
    // 프로젝트를 벗어나면 연결하지 않음. 목록 비우기는 이전 실행의 cleanup이 이미 처리
    if (workspaceId === null || Number.isNaN(workspaceId)) return

    const client = createStompClient()

    let active = true
    const topic = (channel: string) => `/topic/workspaces/${workspaceId}/${channel}`

    client.onConnect = () => {
      client.subscribe(topic('presence'), (message) => {
        setPresentUsers(JSON.parse(message.body).users)
      })
      client.subscribe(topic('cards'), (message) => {
        applyCardEvent(queryClient, workspaceId, JSON.parse(message.body))
      })
      client.subscribe(topic('comments'), (message) => {
        applyCommentEvent(queryClient, workspaceId, JSON.parse(message.body))
      })
      // 구독이 등록되는 순간의 브로드캐스트는 놓칠 수 있으므로 현재 접속자를 한 번 조회
      getPresence(workspaceId).then((res) => {
        if (active) setPresentUsers(res.users)
      })
    }

    client.activate()
    return () => {
      active = false
      setPresentUsers([])
      client.deactivate()
    }
  }, [workspaceId, queryClient])

  return presentUsers
}
