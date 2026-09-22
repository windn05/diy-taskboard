import axios from 'axios'

const TOKEN_KEY = 'taskboard_access_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      setToken(null)
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
