import type { ReactNode } from 'react'

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
