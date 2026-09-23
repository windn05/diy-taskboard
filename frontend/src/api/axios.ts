import axios from 'axios'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

// 세션 쿠키 기반 인증. withCredentials로 쿠키를 실어 보내고, 서버가 httpOnly로 내려주므로
// 토큰을 localStorage에 직접 들고 다니거나 헤더에 붙이는 코드가 필요 없다.
const client = axios.create({ baseURL: '/api', withCredentials: true })

client.interceptors.response.use(
  (res) => res,
  (error) => {
    // /auth/me의 401은 "아직 로그인 안 함"이라는 정상 응답 — AuthContext가 조용히 처리하고
    // ProtectedRoute가 라우팅으로 /login에 보낸다. 그 외 401은 세션이 도중에 끊긴 것이므로 새로고침.
    if (error.response?.status === 401 && !error.config?.url?.endsWith('/auth/me')) {
      window.location.href = '/login'
    }
    const status = error.response?.status ?? 0
    const message = error.response?.data?.message ?? '요청에 실패했습니다.'
    return Promise.reject(new ApiError(status, message))
  },
)

export const get = <T,>(path: string) => client.get<T>(path).then((res) => res.data)

export const post = <T,>(path: string, data?: unknown) => client.post<T>(path, data).then((res) => res.data)

export const patch = <T,>(path: string, data?: unknown) => client.patch<T>(path, data).then((res) => res.data)

export const del = <T,>(path: string) => client.delete<T>(path).then((res) => res.data)
