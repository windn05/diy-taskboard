import { useQuery } from '@tanstack/react-query'
import { listCardTypes } from '../api/cardTypes'

// 유형은 관리자가 정하므로 이름이 아닌 등록 순서로 색을 매김.
// 상태 배지(채운 배경)와 섞이지 않도록 테두리 있는 옅은 배지로 구분
const TYPE_COLOR = [
  'border-emerald-300 bg-emerald-50 text-emerald-700',
  'border-rose-300 bg-rose-50 text-rose-700',
  'border-sky-300 bg-sky-50 text-sky-700',
  'border-slate-300 bg-slate-50 text-slate-600',
  'border-violet-300 bg-violet-50 text-violet-700',
  'border-amber-300 bg-amber-50 text-amber-800',
]

// 지워진 유형을 쓰는 작업
const UNKNOWN_COLOR = 'border-slate-200 bg-white text-slate-500'

/** 작업 유형 배지. 같은 유형은 어느 화면에서나 같은 색 */
export function TypeBadge({ type }: { type: string }) {
  const { data: cardTypes } = useQuery({ queryKey: ['card-types'], queryFn: listCardTypes })
  const index = cardTypes?.findIndex((t) => t.name === type) ?? -1
  const color = index >= 0 ? TYPE_COLOR[index % TYPE_COLOR.length] : UNKNOWN_COLOR

  return (
    <span className={`inline-block whitespace-nowrap rounded border px-1.5 py-0.5 text-[11px] font-medium ${color}`}>
      {type}
    </span>
  )
}
