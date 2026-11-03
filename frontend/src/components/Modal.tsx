import { useEffect, type ReactNode } from 'react'
import { CloseIcon } from './icons'

/**
 * 팝업 껍데기. 머리·본문·바닥을 나눠 **본문만 스크롤**.
 * 전체가 한 덩어리로 스크롤되면 댓글이 쌓일 때 입력 필드가 위로 사라짐
 */
export function Modal({
  title,
  onClose,
  footer,
  width = 'max-w-2xl',
  /** 입력 중 실수로 바깥을 클릭해 닫히면 안 되는 폼(예: 새 작업 추가)은 false. Esc는 항상 닫힘 */
  dismissible = true,
  /** 값이 있으면 Esc로 닫기 전에 이 문구로 확인 (작성 중인 내용이 사라지는 폼용) */
  escapeConfirm,
  children,
}: {
  title: ReactNode
  onClose: () => void
  footer?: ReactNode
  width?: string
  dismissible?: boolean
  escapeConfirm?: string
  children: ReactNode
}) {
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Escape') return
      // 입력 중인 칸을 먼저 벗어나, "칸을 벗어나면 저장"되는 제목·설명·날짜가 닫히기 전에 저장되게 함
      if (document.activeElement instanceof HTMLElement) document.activeElement.blur()
      if (escapeConfirm && !confirm(escapeConfirm)) return
      onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose, escapeConfirm])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={dismissible ? onClose : undefined}
    >
      <div
        className={`flex max-h-[85vh] w-full ${width} flex-col overflow-hidden rounded-xl bg-white shadow-xl`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex shrink-0 items-start gap-3 border-b px-5 py-4">
          <div className="min-w-0 flex-1">{title}</div>
          <button
            onClick={onClose}
            className="shrink-0 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
            title="닫기 (Esc)"
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">{children}</div>

        {footer && <div className="shrink-0 border-t bg-slate-50 px-5 py-3">{footer}</div>}
      </div>
    </div>
  )
}

/** 즉시 저장되는 화면에서 "지금 저장됐는지" 표시 */
export function SaveIndicator({ saving, saved }: { saving: boolean; saved: boolean }) {
  if (saving) return <span className="text-xs text-slate-500">저장 중…</span>
  if (saved) return <span className="text-xs text-emerald-600">저장됨</span>
  return null
}
