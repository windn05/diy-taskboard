import { useState } from 'react'
import { changePassword } from '../api/auth'
import { ApiError } from '../api/axios'
import { Modal } from './Modal'

/** 본인 비밀번호 변경. 게스트에게는 메뉴 자체가 미노출 */
export function ChangePasswordModal({ onClose }: { onClose: () => void }) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [done, setDone] = useState(false)

  // 길이 규칙(8자)은 서버 검증과 동일. 화면 검증은 편의용
  const canSubmit = currentPassword && newPassword.length >= 8 && newPassword === confirmPassword

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSaving(true)
    try {
      await changePassword({ currentPassword, newPassword })
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '비밀번호 변경에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={<h2 className="text-base font-semibold text-slate-800">비밀번호 변경</h2>}
      onClose={onClose}
      width="max-w-sm"
      footer={
        done ? (
          <button
            onClick={onClose}
            className="w-full rounded bg-slate-900 px-3 py-2 text-xs font-medium text-white"
          >
            닫기
          </button>
        ) : (
          <button
            type="submit"
            form="change-password-form"
            disabled={!canSubmit || saving}
            className="w-full rounded bg-slate-900 px-3 py-2 text-xs font-medium text-white disabled:opacity-40"
          >
            변경
          </button>
        )
      }
    >
      {done ? (
        <p className="py-2 text-sm text-slate-600">
          비밀번호를 바꿨습니다. 이미 발급된 토큰은 만료될 때까지 유효하므로, 다른 기기에서 쓰고 있었다면 그곳에서
          로그아웃해 주세요.
        </p>
      ) : (
        <form id="change-password-form" onSubmit={handleSubmit} className="space-y-2">
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="현재 비밀번호"
            autoComplete="current-password"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="새 비밀번호 (8자 이상)"
            autoComplete="new-password"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="새 비밀번호 확인"
            autoComplete="new-password"
            className="w-full rounded border px-2 py-1.5 text-sm"
          />

          {newPassword && confirmPassword && newPassword !== confirmPassword && (
            <p className="text-xs text-amber-600">새 비밀번호가 서로 다릅니다.</p>
          )}
          {error && <p className="text-xs text-red-600">{error}</p>}
        </form>
      )}
    </Modal>
  )
}
