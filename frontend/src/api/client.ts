// 모든 REST 호출의 공통 진입점. 토큰 저장과 에러 처리를 한곳에서 담당
const TOKEN_KEY = 'taskboard_access_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

/** 서버가 준 message를 그대로 보관. 화면은 status로 분기하고 message를 표시 */
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  // /api 접두어는 프록시(개발: Vite, 운영: Caddy)가 떼고 백엔드로 전달
  const res = await fetch(`/api${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  // 토큰 만료·무효. 저장된 토큰을 지우고 로그인 화면으로 이동
  if (res.status === 401) {
    setToken(null)
    window.location.href = '/login'
    throw new ApiError(401, '로그인이 필요합니다.')
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new ApiError(res.status, body.message ?? '요청에 실패했습니다.')
  }

  if (res.status === 204) return undefined as T
  return res.json()
}
