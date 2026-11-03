// 개인 일정. 게스트는 다른 사용자들의 일정을 읽기 전용으로 받음
import { del, get, post, put } from './axios'
import type { Schedule } from './types'

export type ScheduleInput = { title: string; startDate: string; dueDate: string }

export const listSchedules = () => get<Schedule[]>('/schedules')

export const createSchedule = (input: ScheduleInput) => post<Schedule>('/schedules', input)

// 수정도 제목·기간 전체를 보냄
export const updateSchedule = (scheduleId: number, input: ScheduleInput) => put<Schedule>(`/schedules/${scheduleId}`, input)

export const deleteSchedule = (scheduleId: number) => del<void>(`/schedules/${scheduleId}`)
