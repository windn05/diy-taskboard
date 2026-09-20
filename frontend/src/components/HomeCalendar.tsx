import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listCards } from '../api/cards'
import { listCardTypes } from '../api/cardTypes'
import { createSchedule, deleteSchedule, listSchedules } from '../api/schedules'
import { listStatuses } from '../api/statuses'
import { listMembers } from '../api/workspaces'
import type { CalendarTask } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { CardModal } from './CardModal'
import { Modal } from './Modal'
import { PlusIcon } from './icons'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
/** 하루 칸에 기본으로 보여줄 바 개수. 넘치면 "+N건 더보기"로 그 주를 펼친다. */
// 홈을 한 화면에 담기 위한 밀도. 한 주가 높을수록 6주짜리 달이 화면 밖으로 밀린다.
// 넘치는 일정은 "+N"으로 접고, 그 주를 누르면 펼쳐진다.
const MAX_VISIBLE_PER_DAY = 4
const LANE_HEIGHT = 18

type ColorSet = { bg: string; text: string; border: string }

/** 프로젝트마다 고정된 색을 준다 — workspaceId로 나눠서 매번 같은 색이 나오게.
 * 진한 배경에 흰 글씨는 여러 줄 쌓이면 눈에 부담스러워서, 옅은 배경 + 진한 글자 + 왼쪽 색띠로 바꿨다.
 * 배경이 -50이면 흰 페이지와 거의 구분이 안 돼서 -100으로, 글자는 -900으로 더 진하게 잡았다. */
const PROJECT_COLORS: ColorSet[] = [
  { bg: 'bg-blue-100', text: 'text-blue-900', border: 'border-blue-600' },
  { bg: 'bg-emerald-100', text: 'text-emerald-900', border: 'border-emerald-600' },
  { bg: 'bg-orange-100', text: 'text-orange-900', border: 'border-orange-600' },
  { bg: 'bg-cyan-100', text: 'text-cyan-900', border: 'border-cyan-600' },
  { bg: 'bg-amber-100', text: 'text-amber-900', border: 'border-amber-600' },
  { bg: 'bg-violet-100', text: 'text-violet-900', border: 'border-violet-600' },
]
const SCHEDULE_COLOR: ColorSet = { bg: 'bg-fuchsia-100', text: 'text-fuchsia-900', border: 'border-fuchsia-600' }

type Bar = {
  key: string
  title: string
  start: string
  end: string
  color: ColorSet
  onClick?: () => void
  onDelete?: () => void
}

function pad(n: number) {
  return n < 10 ? `0${n}` : `${n}`
}

function toISO(y: number, m: number, d: number) {
  return `${y}-${pad(m + 1)}-${pad(d)}`
}

function parseISO(s: string) {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function dayDiff(a: string, b: string) {
  return Math.round((parseISO(a).getTime() - parseISO(b).getTime()) / 86400000)
}

function buildWeeks(year: number, month: number): string[][] {
  const first = new Date(year, month, 1)
  const gridStart = new Date(year, month, 1 - first.getDay())
  const weeks: string[][] = []
  for (let w = 0; w < 6; w++) {
    const week: string[] = []
    for (let d = 0; d < 7; d++) {
      const dt = new Date(gridStart)
      dt.setDate(gridStart.getDate() + w * 7 + d)
      week.push(toISO(dt.getFullYear(), dt.getMonth(), dt.getDate()))
    }
    weeks.push(week)
  }
  return weeks
}

/** 하루짜리든 여러 날짜짜리든, 겹치지 않게 한 주 안에서 lane에 배치한다. 전부 바 형태로 꽉 차게 그린다. */
function placeBars(week: string[], bars: Bar[]) {
  const weekStart = week[0]
  const weekEnd = week[6]
  const overlapping = bars
    .filter((bar) => bar.start <= weekEnd && bar.end >= weekStart)
    .map((bar) => ({
      bar,
      startIdx: Math.min(Math.max(dayDiff(bar.start, weekStart), 0), 6),
      endIdx: Math.min(Math.max(dayDiff(bar.end, weekStart), 0), 6),
      continuesLeft: bar.start < weekStart,
      continuesRight: bar.end > weekEnd,
    }))
    .sort((a, b) => a.startIdx - b.startIdx || a.endIdx - b.endIdx)

  const laneEnds: number[] = []
  return overlapping.map((item) => {
    let lane = laneEnds.findIndex((end) => end < item.startIdx)
    if (lane === -1) {
      lane = laneEnds.length
      laneEnds.push(item.endIdx)
    } else {
      laneEnds[lane] = item.endIdx
    }
    return { ...item, lane }
  })
}

function NewScheduleForm({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const today = toISO(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())
  const [title, setTitle] = useState('')
  const [startDate, setStartDate] = useState(today)
  const [dueDate, setDueDate] = useState(today)

  const createMutation = useMutation({
    mutationFn: () => createSchedule({ title, startDate, dueDate }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] })
      onClose()
    },
  })

  const canSubmit = title.trim().length > 0 && dueDate >= startDate

  const footer = (
    <div className="flex justify-end gap-2">
      <button type="button" onClick={onClose} className="rounded px-3 py-1.5 text-sm text-slate-500 hover:bg-slate-100">
        취소
      </button>
      <button
        type="submit"
        form="new-schedule-form"
        disabled={!canSubmit || createMutation.isPending}
        className="rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-40"
      >
        추가
      </button>
    </div>
  )

  return (
    <Modal title={<h2 className="text-lg font-semibold">새 일정</h2>} footer={footer} onClose={onClose} dismissible={false} width="max-w-sm">
      <form
        id="new-schedule-form"
        onSubmit={(e) => {
          e.preventDefault()
          if (canSubmit) createMutation.mutate()
        }}
        className="space-y-4"
      >
        <label className="block space-y-1 text-sm">
          <span className="text-xs text-slate-500">제목</span>
          <input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="일정 제목"
            className="w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="block space-y-1 text-sm">
            <span className="text-xs text-slate-500">시작일</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full rounded border px-2 py-1.5 text-sm"
            />
          </label>
          <label className="block space-y-1 text-sm">
            <span className="text-xs text-slate-500">종료일</span>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full rounded border px-2 py-1.5 text-sm"
            />
          </label>
        </div>
        {dueDate < startDate && <p className="text-xs text-red-600">종료일은 시작일보다 빠를 수 없습니다.</p>}
      </form>
    </Modal>
  )
}

function WeekRow({
  week,
  bars,
  monthIndex,
  todayIso,
}: {
  week: string[]
  bars: Bar[]
  monthIndex: number
  todayIso: string
}) {
  const [expanded, setExpanded] = useState(false)
  const placed = useMemo(() => placeBars(week, bars), [week, bars])
  const laneLimit = expanded ? Infinity : MAX_VISIBLE_PER_DAY
  const visible = placed.filter((p) => p.lane < laneLimit)
  const hiddenBars = placed.filter((p) => p.lane >= laneLimit)
  // 실제로 쓰인 줄 수. 일정이 적은 주까지 최대 높이로 잡으면 달력 아래가 빈 띠처럼 남는다.
  const usedLanes = Math.max(0, ...placed.map((p) => p.lane + 1))
  const laneCount = expanded ? usedLanes : Math.min(usedLanes, MAX_VISIBLE_PER_DAY)
  const hiddenBarsAt = (dayIdx: number) => hiddenBars.filter((p) => p.startIdx <= dayIdx && p.endIdx >= dayIdx).length

  return (
    // 남는 높이는 주들이 나눠 갖는다(달력 아래에 빈 공간이 남지 않게). 모자랄 때는 줄어들지 않고 스크롤한다.
    <div className="relative flex-1 shrink-0 border-b last:border-0">
      {/* 요일 칸 배경·세로 구분선. 위 세 개의 그리드(날짜/바/점) 뒤에 깔려서 한 주 높이 전체를 관통한다. */}
      <div className="pointer-events-none absolute inset-0 grid grid-cols-7">
        {week.map((dateIso, i) => (
          <div key={dateIso} className={`${i < 6 ? 'border-r' : ''} ${parseISO(dateIso).getMonth() !== monthIndex ? 'bg-slate-50' : ''}`} />
        ))}
      </div>

      <div className="grid grid-cols-7">
        {week.map((dateIso, i) => {
          const inMonth = parseISO(dateIso).getMonth() === monthIndex
          const isToday = dateIso === todayIso
          return (
            <div key={dateIso} className="px-1.5 pt-1 text-right text-xs">
              <span
                className={
                  isToday
                    ? 'rounded-full bg-slate-900 px-1.5 py-0.5 text-white'
                    : !inMonth
                      ? 'text-slate-300'
                      : i === 0
                        ? 'text-red-500'
                        : i === 6
                          ? 'text-blue-500'
                          : 'text-slate-600'
                }
              >
                {parseISO(dateIso).getDate()}
              </span>
            </div>
          )
        })}
      </div>

      <div className="relative mb-1 px-0.5" style={{ height: laneCount * LANE_HEIGHT + 2 }}>
        {expanded && (
          <button
            type="button"
            onClick={() => setExpanded(false)}
            className="absolute right-1 top-0 z-10 text-[10px] text-slate-400 hover:text-slate-700"
          >
            접기 ▲
          </button>
        )}
        {visible.map(({ bar, startIdx, endIdx, lane, continuesLeft, continuesRight }) => (
          <div
            key={bar.key}
            onClick={bar.onClick}
            className={`group absolute flex items-center gap-1 truncate rounded border-l-4 px-1.5 text-xs font-semibold leading-[18px] ${bar.color.bg} ${bar.color.text} ${bar.color.border} ${bar.onClick ? 'cursor-pointer hover:brightness-95' : ''}`}
            style={{
              left: `calc(${(startIdx / 7) * 100}% + 2px)`,
              width: `calc(${((endIdx - startIdx + 1) / 7) * 100}% - 4px)`,
              top: lane * LANE_HEIGHT,
              height: LANE_HEIGHT - 2,
            }}
            title={bar.title}
          >
            {continuesLeft && <span className="shrink-0">◂</span>}
            <span className="min-w-0 flex-1 truncate">{bar.title}</span>
            {continuesRight && <span className="shrink-0">▸</span>}
            {bar.onDelete && (
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  bar.onDelete!()
                }}
                className="shrink-0 opacity-0 group-hover:opacity-100"
              >
                ×
              </button>
            )}
          </div>
        ))}
      </div>

      {!expanded && hiddenBars.length > 0 && (
        <div className="grid grid-cols-7 pb-1">
          {week.map((dateIso, i) => {
            const overflow = hiddenBarsAt(i)
            return (
              <div key={dateIso} className="min-w-0 px-1.5">
                {overflow > 0 && (
                  <button
                    type="button"
                    onClick={() => setExpanded(true)}
                    className="block truncate text-[10px] font-medium text-slate-500 hover:text-slate-800 hover:underline"
                  >
                    +{overflow}건 더보기
                  </button>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

/** 달력에서 작업 바를 클릭했을 때 그 자리에서 읽기 전용으로 미리보기만 띄운다 — 프로젝트 화면으로 이동시키지 않는다. */
function CardPreviewModal({ workspaceId, cardId, onClose }: { workspaceId: number; cardId: number; onClose: () => void }) {
  const { data: cards } = useQuery({ queryKey: ['cards', workspaceId], queryFn: () => listCards(workspaceId) })
  const { data: statuses } = useQuery({ queryKey: ['statuses'], queryFn: listStatuses })
  const { data: cardTypes } = useQuery({ queryKey: ['card-types'], queryFn: listCardTypes })
  const { data: members } = useQuery({ queryKey: ['members', workspaceId], queryFn: () => listMembers(workspaceId) })

  const card = cards?.find((c) => c.id === cardId)

  if (!card || !statuses) {
    return (
      <Modal title={<h2 className="text-lg font-semibold">불러오는 중...</h2>} onClose={onClose}>
        <p className="py-8 text-center text-sm text-slate-400">작업 정보를 불러오는 중입니다...</p>
      </Modal>
    )
  }

  return <CardModal card={card} members={members ?? []} statuses={statuses} cardTypes={cardTypes ?? []} onClose={onClose} readOnly />
}

export function HomeCalendar({ tasks }: { tasks: CalendarTask[] }) {
  const queryClient = useQueryClient()
  const { isGuest } = useAuth()
  const [cursor, setCursor] = useState(() => {
    const now = new Date()
    return { year: now.getFullYear(), month: now.getMonth() }
  })
  const [addingSchedule, setAddingSchedule] = useState(false)
  const [preview, setPreview] = useState<{ workspaceId: number; cardId: number } | null>(null)

  const { data: schedules } = useQuery({ queryKey: ['schedules'], queryFn: listSchedules })

  const deleteMutation = useMutation({
    mutationFn: (scheduleId: number) => deleteSchedule(scheduleId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['schedules'] }),
  })

  const weeks = useMemo(() => buildWeeks(cursor.year, cursor.month), [cursor])

  const projectColor = (workspaceId: number) => PROJECT_COLORS[workspaceId % PROJECT_COLORS.length]

  const bars = useMemo<Bar[]>(() => {
    const taskBars: Bar[] = tasks
      .filter((t) => t.startDate || t.dueDate)
      .map((t) => ({
        key: `task-${t.cardId}`,
        title: t.title,
        start: t.startDate ?? t.dueDate!,
        end: t.dueDate ?? t.startDate!,
        color: projectColor(t.workspaceId),
        onClick: () => setPreview({ workspaceId: t.workspaceId, cardId: t.cardId }),
      }))
    const scheduleBars: Bar[] = (schedules ?? []).map((s) => ({
      key: `schedule-${s.id}`,
      title: s.title,
      start: s.startDate,
      end: s.dueDate,
      color: SCHEDULE_COLOR,
      onDelete: () => confirm(`"${s.title}" 일정을 삭제할까요?`) && deleteMutation.mutate(s.id),
    }))
    return [...taskBars, ...scheduleBars]
  }, [tasks, schedules, deleteMutation])

  const monthLabel = `${cursor.year}.${pad(cursor.month + 1)}`
  const todayIso = toISO(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())
  const goToday = () => {
    const now = new Date()
    setCursor({ year: now.getFullYear(), month: now.getMonth() })
  }

  return (
    // 부모가 준 높이를 채우고, 달이 길어 넘칠 때만 달력 안에서 스크롤한다(페이지는 스크롤되지 않게).
    <section className="flex h-full min-h-0 flex-col overflow-hidden rounded-lg border bg-white">
      <div className="flex shrink-0 items-center justify-between border-b px-4 py-2.5">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCursor((c) => (c.month === 0 ? { year: c.year - 1, month: 11 } : { year: c.year, month: c.month - 1 }))}
            className="rounded px-2 py-1 text-sm text-slate-500 hover:bg-slate-100"
          >
            ‹
          </button>
          <h2 className="w-20 text-center text-sm font-semibold text-slate-700">{monthLabel}</h2>
          <button
            onClick={() => setCursor((c) => (c.month === 11 ? { year: c.year + 1, month: 0 } : { year: c.year, month: c.month + 1 }))}
            className="rounded px-2 py-1 text-sm text-slate-500 hover:bg-slate-100"
          >
            ›
          </button>
          <button onClick={goToday} className="rounded border px-2 py-1 text-xs text-slate-500 hover:bg-slate-100">
            오늘
          </button>
        </div>
        {!isGuest && (
          <button
            onClick={() => setAddingSchedule(true)}
            className="flex items-center gap-1 rounded bg-slate-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-800"
          >
            <PlusIcon size={12} />새 일정
          </button>
        )}
      </div>

      <div className="grid shrink-0 grid-cols-7 border-b text-center text-xs">
        {WEEKDAYS.map((d, i) => (
          <div
            key={d}
            className={`py-1.5 ${i < 6 ? 'border-r' : ''} ${i === 0 ? 'text-red-400' : i === 6 ? 'text-blue-400' : 'text-slate-400'}`}
          >
            {d}
          </div>
        ))}
      </div>

      <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
        {weeks.map((week) => (
          <WeekRow key={week[0]} week={week} bars={bars} monthIndex={cursor.month} todayIso={todayIso} />
        ))}
      </div>

      {addingSchedule && <NewScheduleForm onClose={() => setAddingSchedule(false)} />}
      {preview && <CardPreviewModal workspaceId={preview.workspaceId} cardId={preview.cardId} onClose={() => setPreview(null)} />}
    </section>
  )
}
