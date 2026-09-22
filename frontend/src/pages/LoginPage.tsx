import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/axios'
import { APPS, type AppKey } from '../apps'

/** 로그인. 왼쪽에서 고른 앱으로 로그인 후 바로 이동. 게스트는 항상 TaskBoard 홈으로 이동 */
export function LoginPage() {
  const { login, loginAsGuest } = useAuth()
  const navigate = useNavigate()
  const [selectedApp, setSelectedApp] = useState<AppKey>('taskboard')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
      navigate(APPS.find((app) => app.key === selectedApp)?.path ?? '/projects')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function handleGuest() {
    setError(null)
    setLoading(true)
    try {
      await loginAsGuest()
      navigate('/home')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '게스트 로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-6">
      <div className="flex w-full max-w-3xl overflow-hidden rounded-3xl bg-white shadow-xl">
        {/* 좌측: 메뉴 영역 */}
        <div className="hidden w-72 shrink-0 items-center justify-center border-r bg-slate-50 sm:flex">
          <div className="w-48 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="mb-3 text-center text-xs font-semibold tracking-wide text-slate-500">메뉴 선택</p>
            <div className="space-y-2">
              {APPS.map((app) => (
                <button
                  key={app.key}
                  type="button"
                  disabled={app.comingSoon}
                  onClick={() => setSelectedApp(app.key)}
                  className={`flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-xs font-medium transition ${
                    app.comingSoon
                      ? 'cursor-not-allowed text-slate-300'
                      : selectedApp === app.key
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  <app.Icon size={18} />
                  <span className="flex-1">{app.label}</span>
                  {app.comingSoon && <span className="text-[9px]">준비중</span>}
                  {app.adminOnly && !app.comingSoon && <span className="text-[9px] opacity-60">관리자</span>}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 우측: 로그인 폼 */}
        <form onSubmit={handleSubmit} className="flex flex-1 flex-col justify-center gap-4 px-10 py-14">
          <h1 className="mb-2 text-center text-2xl font-bold text-slate-800">로그인</h1>
          {error && <p className="text-center text-sm text-red-600">{error}</p>}

          <input
            type="text"
            placeholder="아이디"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full rounded-full bg-slate-100 px-5 py-3 text-sm outline-none focus:ring-2 focus:ring-slate-400"
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-full bg-slate-100 px-5 py-3 text-sm outline-none focus:ring-2 focus:ring-slate-400"
            required
          />
          <button
            type="submit"
            disabled={loading}
            className="mt-2 w-full rounded-full bg-slate-900 py-3 text-sm font-semibold tracking-wide text-white shadow transition hover:bg-slate-800 disabled:opacity-50"
          >
            LOGIN
          </button>
          <button
            type="button"
            onClick={handleGuest}
            disabled={loading}
            className="w-full rounded-full border border-slate-300 py-3 text-sm font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
          >
            게스트로 둘러보기
          </button>
        </form>
      </div>
    </div>
  )
}
