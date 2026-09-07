import { useEffect, useState } from 'react'
import { createStompClient } from '../api/stomp'
import { getLogs } from '../api/monitoring'
import type { LogEntry } from '../api/types'

const MAX_ENTRIES = 500

const keyOf = (entry: LogEntry) => `${entry.loggedAt}|${entry.message}`

/** 서버 로그 실시간 tail. 최신 항목이 앞에 온다. 관리자만 구독할 수 있다. */
export function useLogStream() {
  const [entries, setEntries] = useState<LogEntry[]>([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const client = createStompClient()
    if (!client) return

    let active = true
    client.onConnect = () => {
      if (!active) return
      setConnected(true)
      client.subscribe('/topic/admin/logs', (message) => {
        setEntries((prev) => [JSON.parse(message.body), ...prev].slice(0, MAX_ENTRIES))
      })
      // 구독 등록과 초기 조회 사이에 들어온 로그가 덮이지 않도록, 이미 받은 항목을 남기고 병합한다.
      getLogs('TRACE', MAX_ENTRIES).then((seed) => {
        if (!active) return
        const seedKeys = new Set(seed.map(keyOf))
        setEntries((prev) => [...prev.filter((e) => !seedKeys.has(keyOf(e))), ...seed].slice(0, MAX_ENTRIES))
      })
    }
    client.onWebSocketClose = () => setConnected(false)

    client.activate()
    return () => {
      active = false
      setConnected(false)
      client.deactivate()
    }
  }, [])

  return { entries, connected }
}
