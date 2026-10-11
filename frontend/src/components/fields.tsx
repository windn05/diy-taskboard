import { useEffect, useRef, useState, type ReactNode } from 'react'

/**
 * 작업 상세 팝업과 새 작업 팝업이 공유하는 입력 요소.
 * 두 화면의 필드 구성·순서를 맞추기 위해, 한쪽만 바뀌지 않도록 여기에 모음
 */

const SELECT_CLASS = 'w-full rounded border px-2 py-1.5 disabled:bg-slate-100 disabled:text-slate-500'

export function LabeledField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="space-y-1">
      <span className="text-xs text-slate-500">{label}</span>
      {children}
    </label>
  )
}

/** 폼 상태용 날짜 칸. 바뀔 때마다 그대로 전달 (저장은 폼 제출 시) */
export function DateField({
  value,
  onChange,
  disabled,
}: {
  value: string | null
  onChange: (value: string | null) => void
  disabled?: boolean
}) {
  return (
    <input
      type="date"
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value || null)}
      disabled={disabled}
      className={SELECT_CLASS}
    />
  )
}

const SAVE_DELAY_MS = 700

/**
 * 바로 저장하는 날짜 칸. 키보드로 칠 때는 연·월·일 칸마다 값이 바뀌므로
 * 입력이 잠시 멈추거나 칸을 벗어날 때 한 번만 저장 (중간값 저장·요청 순서 뒤바뀜 방지)
 */
export function AutoSaveDateField({
  value,
  onSave,
  disabled,
}: {
  value: string | null
  onSave: (value: string | null) => void
  disabled?: boolean
}) {
  const [draft, setDraft] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const pending = useRef<string | null | undefined>(undefined)
  const latest = useRef({ value, onSave })
  latest.current = { value, onSave }

  // 저장 대기 중인 값을 지금 저장. 서버 값과 같으면 생략
  function flush() {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    const next = pending.current
    pending.current = undefined
    if (next !== undefined && next !== latest.current.value) latest.current.onSave(next)
  }

  // 대기 중에 팝업이 닫혀도 입력한 값은 저장
  useEffect(() => flush, [])

  return (
    <input
      type="date"
      // 입력 중에는 직접 친 값, 그 외에는 서버 값 (저장 후 다시 받아온 값·다른 사람의 변경 반영)
      value={draft ?? value ?? ''}
      onFocus={() => setDraft(value ?? '')}
      onChange={(e) => {
        setDraft(e.target.value)
        // 일부 칸만 지운 미완성 날짜(badInput)는 저장하지 않음. 전부 비우면 마감일 삭제
        if (e.target.validity.badInput) return
        pending.current = e.target.value || null
        if (timer.current) clearTimeout(timer.current)
        timer.current = setTimeout(flush, SAVE_DELAY_MS)
      }}
      onBlur={() => {
        flush()
        setDraft(null)
      }}
      disabled={disabled}
      className={SELECT_CLASS}
    />
  )
}
