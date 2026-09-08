import { api } from './client'
import type { Schedule } from './types'

export const listSchedules = () => api<Schedule[]>('/schedules')

export const createSchedule = (input: { title: string; startDate: string; dueDate: string }) =>
  api<Schedule>('/schedules', { method: 'POST', body: JSON.stringify(input) })

export const deleteSchedule = (scheduleId: number) => api<void>(`/schedules/${scheduleId}`, { method: 'DELETE' })
