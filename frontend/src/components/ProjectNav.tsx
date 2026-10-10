import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import type { Workspace } from '../api/types'
import { ChevronDownIcon } from './icons'

/** 관리자가 이 이름으로 지정한 그룹은 맨 아래에 기본 접힘으로 표시 */
export const ARCHIVE_GROUP = '보관됨'
const OTHER_GROUP = '기타'
const COLLAPSED_KEY = 'taskboard.sidebar.collapsedGroups'

// 일반 그룹(이름순) → 기타 → 보관됨
const groupRank = (name: string) => (name === ARCHIVE_GROUP ? 2 : name === OTHER_GROUP ? 1 : 0)

function groupProjects(projects: Workspace[]) {
  const groups = new Map<string, Workspace[]>()
  for (const project of projects) {
    const name = project.groupName ?? OTHER_GROUP
    groups.set(name, [...(groups.get(name) ?? []), project])
  }
  return [...groups.entries()].sort(([a], [b]) => groupRank(a) - groupRank(b) || a.localeCompare(b, 'ko'))
}

// 접힘 상태는 사용자 편의 기능이라 저장소를 못 쓰는 환경(시크릿 창 등)에서도 기본값으로 동작
function loadCollapsed(): Set<string> {
  try {
    const saved = localStorage.getItem(COLLAPSED_KEY)
    if (saved) return new Set(JSON.parse(saved) as string[])
  } catch {
    // 기본값 사용
  }
  return new Set([ARCHIVE_GROUP])
}

function saveCollapsed(collapsed: Set<string>) {
  try {
    localStorage.setItem(COLLAPSED_KEY, JSON.stringify([...collapsed]))
  } catch {
    // 저장 실패 시 이번 접속에서만 유지
  }
}

/** 사이드바 프로젝트 목록. 그룹이 하나도 없으면 예전처럼 평면 목록 */
export function ProjectNav({ projects, activeId }: { projects: Workspace[]; activeId: number | null }) {
  const [collapsed, setCollapsed] = useState(loadCollapsed)

  function toggle(name: string) {
    const next = new Set(collapsed)
    if (next.has(name)) next.delete(name)
    else next.add(name)
    setCollapsed(next)
    saveCollapsed(next)
  }

  if (projects.every((p) => !p.groupName)) {
    return (
      <nav className="space-y-0.5">
        {projects.map((p) => (
          <ProjectLink key={p.id} project={p} />
        ))}
      </nav>
    )
  }

  return (
    <nav className="space-y-2">
      {groupProjects(projects).map(([name, items]) => {
        // 열려 있는 프로젝트가 속한 그룹은 접혀 있어도 펼쳐 보여 현재 위치를 잃지 않게 함
        const open = !collapsed.has(name) || items.some((p) => p.id === activeId)
        return (
          <div key={name}>
            <button
              onClick={() => toggle(name)}
              aria-expanded={open}
              className="flex w-full items-center gap-1 rounded px-2 py-1 text-left text-xs font-semibold text-slate-500 hover:bg-slate-100"
            >
              <ChevronDownIcon size={12} className={`transition-transform ${open ? '' : '-rotate-90'}`} />
              <span className="flex-1">{name}</span>
              {!open && <span className="font-normal text-slate-400">{items.length}</span>}
            </button>
            {open && (
              <div className="mt-0.5 space-y-0.5 pl-3">
                {items.map((p) => (
                  <ProjectLink key={p.id} project={p} />
                ))}
              </div>
            )}
          </div>
        )
      })}
    </nav>
  )
}

function ProjectLink({ project }: { project: Workspace }) {
  return (
    <NavLink
      to={`/projects/${project.id}`}
      className={({ isActive }) =>
        `block rounded px-2 py-1.5 text-sm ${isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'}`
      }
    >
      {project.name}
    </NavLink>
  )
}
