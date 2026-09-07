import type { ReactNode } from 'react'
import type { CardPriority, Member } from '../api/types'

/**
 * 작업 상세 팝업과 새 작업 팝업이 공유하는 입력 요소.
 * 두 화면의 필드 구성·순서를 맞추기로 했으므로, 한쪽만 바뀌는 일이 없도록 여기에 모은다.
 */

const PRIORITIES: { value: CardPriority; label: string }[] = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
]

const SELECT_CLASS = 'w-full rounded border px-2 py-1.5 disabled:bg-slate-100 disabled:text-slate-500'

export function LabeledField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="space-y-1">
      <span className="text-xs text-slate-500">{label}</span>
      {children}
    </label>
  )
}

export function PrioritySelect({
  value,
  onChange,
  disabled,
}: {
  value: CardPriority
  onChange: (value: CardPriority) => void
  disabled?: boolean
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value as CardPriority)}
      disabled={disabled}
      className={SELECT_CLASS}
    >
      {PRIORITIES.map((priority) => (
        <option key={priority.value} value={priority.value}>
          {priority.label}
        </option>
      ))}
    </select>
  )
}

export function AssigneeSelect({
  value,
  members,
  onChange,
  disabled,
}: {
  value: number | null
  members: Member[]
  onChange: (value: number | null) => void
  disabled?: boolean
}) {
  return (
    <select
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}
      disabled={disabled}
      className={SELECT_CLASS}
    >
      <option value="">미지정</option>
      {members.map((member) => (
        <option key={member.userId} value={member.userId}>
          {member.name}
        </option>
      ))}
    </select>
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
