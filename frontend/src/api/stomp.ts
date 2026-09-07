import { Client } from '@stomp/stompjs'
import { getToken } from './client'

function brokerUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws`
}

/**
 * REST와 같은 JWT로 인증되는 STOMP 클라이언트를 만든다. 아직 연결하지는 않으므로
 * 호출한 쪽에서 onConnect를 지정한 뒤 activate() 해야 한다.
 * 토큰이 없으면 null — 로그인 전에는 연결할 이유가 없다.
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
