import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/axios'
import { BoardIcon } from '../components/icons'

const LOGIN_MENUS = [
  {
    key: 'taskboard',
    label: 'TaskBoard',
    description: '프로젝트의 작업, 일정, 배포 기록 등을 한 곳에서 관리',
  },
] as const

type LoginMenuKey = (typeof LOGIN_MENUS)[number]['key']

/** 로그인. 왼쪽 메뉴는 사용 가능한 서비스와 준비 중인 서비스를 안내 */
export function LoginPage() {
  const { login, loginAsGuest } = useAuth()
  const navigate = useNavigate()
  const [selectedMenuKey, setSelectedMenuKey] = useState<LoginMenuKey>('taskboard')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const selectedMenu = LOGIN_MENUS.find((menu) => menu.key === selectedMenuKey) ?? LOGIN_MENUS[0]
  const singleMenu = LOGIN_MENUS.length === 1

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
      navigate('/home')
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
    <div className="grid min-h-screen place-items-center bg-[linear-gradient(135deg,#f8fafc,#e2e8f0)] p-4 sm:p-6">
      <main className="grid w-full max-w-5xl overflow-hidden rounded-[2rem] border border-white/80 bg-white shadow-[0_32px_80px_-32px_rgba(15,23,42,0.35)] md:grid-cols-[1.05fr_0.95fr]">
        <section className="hidden min-h-[620px] grid-rows-2 bg-[linear-gradient(145deg,#111827,#020617)] text-white md:grid">
          <div className="flex min-h-0 flex-col px-9 pb-5 pt-6">
            <div className="mb-3 flex items-center gap-2">
              <span className="size-2 rounded-full bg-slate-400" />
              <h1 className="text-xs font-semibold tracking-[0.18em] text-slate-300">MENU</h1>
            </div>
            <nav
              className={`mx-auto flex min-h-0 w-full flex-1 flex-wrap content-center justify-center gap-3 overflow-y-auto pr-1 ${
                singleMenu ? 'max-w-56' : 'max-w-[300px]'
              }`}
            >
              {LOGIN_MENUS.map((menu) => (
                <button
                  key={menu.key}
                  type="button"
                  onClick={() => setSelectedMenuKey(menu.key)}
                  className={`flex flex-col items-center justify-center rounded-2xl border p-4 text-center transition duration-200 ${
                    singleMenu ? 'h-40 w-56 gap-4' : 'h-28 w-36 gap-3'
                  } ${
                    selectedMenuKey === menu.key
                      ? 'border-white/25 bg-white/10 text-white shadow-[0_10px_30px_-18px_rgba(255,255,255,0.45)]'
                      : 'border-white/[0.06] bg-white/[0.03] text-slate-400 hover:border-white/10 hover:bg-white/[0.07]'
                  }`}
                >
                  <span
                    className={`grid shrink-0 place-items-center rounded-xl ${singleMenu ? 'size-14' : 'size-10'} ${
                      selectedMenuKey === menu.key ? 'bg-white text-slate-950' : 'bg-white/[0.06] text-slate-500'
                    }`}
                  >
                    <BoardIcon size={singleMenu ? 28 : 20} />
                  </span>
                  <span className={`${singleMenu ? 'text-base' : 'text-sm'} font-semibold`}>{menu.label}</span>
                </button>
              ))}
            </nav>
          </div>

          <div className="flex min-h-0 flex-col border-t border-white/10 bg-[linear-gradient(145deg,rgba(255,255,255,0.08),rgba(255,255,255,0.02))] px-9 pb-9 pt-7">
            <p className="mb-5 text-[10px] font-semibold tracking-[0.2em] text-slate-400">SELECTED SERVICE</p>
            <div className="mb-5 flex items-center gap-3">
              <span className="grid size-11 place-items-center rounded-2xl bg-white/10 text-slate-300 ring-1 ring-white/10">
                <BoardIcon size={22} />
              </span>
              <p className="text-2xl font-semibold tracking-tight text-white">{selectedMenu.label}</p>
            </div>
            <p className="max-w-sm text-base leading-7 text-slate-300">{selectedMenu.description}</p>
          </div>
        </section>

        <form onSubmit={handleSubmit} className="flex min-h-[560px] flex-col justify-center bg-white px-7 py-10 sm:px-14 md:min-h-[620px]">
          <div className="mb-9 flex items-center gap-2 font-semibold text-slate-800 md:hidden">
            <span className="grid size-9 place-items-center rounded-xl bg-slate-100 text-slate-700">
              <BoardIcon size={18} />
            </span>
            TaskBoard
          </div>

          <div className="mb-8">

            <h2 className="text-3xl font-bold tracking-tight text-slate-950">로그인</h2>
          </div>

          <div className="space-y-5">
            <label className="block">
              <span className="mb-2 block text-xs font-semibold text-slate-600">아이디</span>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-slate-500 focus:bg-white focus:ring-4 focus:ring-slate-100"
                required
              />
            </label>
            <label className="block">
              <span className="mb-2 block text-xs font-semibold text-slate-600">비밀번호</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-slate-500 focus:bg-white focus:ring-4 focus:ring-slate-100"
                required
              />
            </label>
          </div>

          {error && <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="mt-7 w-full rounded-xl bg-slate-900 py-3.5 text-sm font-semibold text-white shadow-[0_12px_24px_-12px_rgba(15,23,42,0.65)] transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>

          <div className="my-6 flex items-center gap-3 text-xs text-slate-400">
            <span className="h-px flex-1 bg-slate-200" />또는<span className="h-px flex-1 bg-slate-200" />
          </div>

          <button
            type="button"
            onClick={handleGuest}
            disabled={loading}
            className="w-full rounded-xl border border-slate-200 py-3 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            게스트로 둘러보기
          </button>
        </form>
      </main>
    </div>
  )
}
