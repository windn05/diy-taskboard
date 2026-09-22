// 개인 일정 (본인에게만 보임)
import { del, get, post } from './axios'
import type { Schedule } from './types'

export const listSchedules = () => get<Schedule[]>('/schedules')

export const createSchedule = (input: { title: string; startDate: string; dueDate: string }) =>
  post<Schedule>('/schedules', input)

export const deleteSchedule = (scheduleId: number) => del<void>(`/schedules/${scheduleId}`)
