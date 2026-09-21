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
  /** 입력 중 실수로 바깥 클릭/Esc로 닫히면 안 되는 폼(예: 새 작업 추가)은 false */
  dismissible = true,
  children,
}: {
  title: ReactNode
  onClose: () => void
  footer?: ReactNode
  width?: string
  dismissible?: boolean
  children: ReactNode
}) {
  useEffect(() => {
    if (!dismissible) return
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose, dismissible])

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
            title={dismissible ? '닫기 (Esc)' : '닫기'}
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
