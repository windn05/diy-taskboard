import { Client } from '@stomp/stompjs'

// REST와 같은 호스트의 /ws. 페이지가 https면 wss로 연결
function brokerUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws`
}

/**
 * STOMP 클라이언트 생성. 연결은 하지 않으므로 호출한 쪽에서 onConnect를 지정한 뒤 activate() 호출 필요.
 * 인증은 세션 쿠키로 하므로(같은 오리진이라 핸드셰이크에 자동으로 실림) 여기서 챙길 게 없고,
 * 비로그인 상태로 연결하면 서버가 CONNECT 단계에서 거부한다.
 */
export function createStompClient(): Client {
  return new Client({ brokerURL: brokerUrl(), reconnectDelay: 5000 })
}
