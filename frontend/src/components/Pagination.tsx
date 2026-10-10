/** 쪽 번호 이동. 쪽이 많으면 현재 쪽 주변 5개만 표시, 한 쪽뿐이면 숨김 */
export function Pagination({
  page,
  pageCount,
  onChange,
}: {
  page: number
  pageCount: number
  onChange: (page: number) => void
}) {
  if (pageCount <= 1) return null

  const start = Math.max(1, Math.min(page - 2, pageCount - 4))
  const pages = Array.from({ length: Math.min(5, pageCount) }, (_, i) => start + i)
  const button = 'min-w-8 rounded px-2 py-1 text-sm disabled:cursor-default disabled:text-slate-300'

  return (
    <nav aria-label="쪽 이동" className="flex items-center gap-1">
      <button onClick={() => onChange(page - 1)} disabled={page === 1} className={`${button} hover:bg-slate-100`}>
        이전
      </button>
      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
          className={`${button} ${p === page ? 'bg-slate-900 font-medium text-white' : 'text-slate-600 hover:bg-slate-100'}`}
        >
          {p}
        </button>
      ))}
      <button
        onClick={() => onChange(page + 1)}
        disabled={page === pageCount}
        className={`${button} hover:bg-slate-100`}
      >
        다음
      </button>
    </nav>
  )
}
