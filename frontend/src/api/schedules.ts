// 개인 일정. 게스트는 다른 사용자들의 일정을 읽기 전용으로 받음
import { del, get, post } from './axios'
import type { Schedule } from './types'

export const listSchedules = () => get<Schedule[]>('/schedules')

export const createSchedule = (input: { title: string; startDate: string; dueDate: string }) =>
  post<Schedule>('/schedules', input)

export const deleteSchedule = (scheduleId: number) => del<void>(`/schedules/${scheduleId}`)
