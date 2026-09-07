import { useEffect, useState } from 'react'
import { useQueryClient, type QueryClient } from '@tanstack/react-query'
import { createStompClient } from '../api/stomp'
import { getPresence } from '../api/workspaces'
import type { Card, Comment, PresenceUser } from '../api/types'

type CardEvent = { type: 'CREATED' | 'UPDATED' | 'MOVED' | 'DELETED'; card: Card }
type CommentEvent = { workspaceId: number; cardId: number; comment: Comment }

function applyCardEvent(queryClient: QueryClient, workspaceId: number, event: CardEvent) {
  queryClient.setQueryData<Card[]>(['cards', workspaceId], (cards) => {
    if (!cards) return cards
    if (event.type === 'DELETED') return cards.filter((c) => c.id !== event.card.id)
    const index = cards.findIndex((c) => c.id === event.card.id)
    if (index === -1) return [...cards, event.card]
    return cards.map((c) => (c.id === event.card.id ? event.card : c))
  })
}

function applyCommentEvent(queryClient: QueryClient, event: CommentEvent) {
  queryClient.setQueryData<Comment[]>(['comments', event.cardId], (comments) => {
    if (!comments || comments.some((c) => c.id === event.comment.id)) return comments
    return [...comments, event.comment]
  })
}

/**
 * 프로젝트 화면에서 쓰는 단일 STOMP 연결. 접속자·작업·댓글 세 채널을 함께 구독하고,
 * 수신한 이벤트는 재요청 없이 react-query 캐시에 직접 반영한다.
 */
export function useProjectSocket(workspaceId: number | null) {
  const queryClient = useQueryClient()
  const [presentUsers, setPresentUsers] = useState<PresenceUser[]>([])

  useEffect(() => {
    // 프로젝트를 벗어나면 연결하지 않는다. 목록 비우기는 이전 실행의 cleanup이 이미 처리한다.
    if (workspaceId === null || Number.isNaN(workspaceId)) return

    const client = createStompClient()
    if (!client) return

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
        applyCommentEvent(queryClient, JSON.parse(message.body))
      })
      // 구독이 등록되는 순간의 브로드캐스트는 놓칠 수 있으므로 현재 접속자를 한 번 받아온다.
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
