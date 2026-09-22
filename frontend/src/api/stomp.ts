import { Client } from '@stomp/stompjs'
import { getToken } from './axios'

// REST와 같은 호스트의 /ws. 페이지가 https면 wss로 연결
function brokerUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws`
}

/**
 * REST와 같은 JWT로 인증되는 STOMP 클라이언트 생성. 연결은 하지 않으므로
 * 호출한 쪽에서 onConnect를 지정한 뒤 activate() 호출 필요.
 * 토큰이 없으면 null — 로그인 전에는 연결할 이유 없음
 */
export function createStompClient(): Client | null {
  const token = getToken()
  if (!token) return null

  return new Client({
    brokerURL: brokerUrl(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
  })
}
