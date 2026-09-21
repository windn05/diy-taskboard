// 서버 시각은 UTC(끝에 Z)로 오고, 표시는 브라우저 시간대(KST)로 변환

const pad = (n: number) => String(n).padStart(2, '0')

/** 로컬 달력 날짜 yyyy-MM-dd. toISOString은 UTC라 KST 오전 9시 전에는 전날이 되므로 미사용 */
export function localDate(date: Date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 서버 시각 → 로컬 날짜 yyyy-MM-dd */
export function localDateOf(iso: string) {
  return localDate(new Date(iso))
}

/** 서버 시각 → 로컬 시각 HH:mm:ss */
export function localTimeOf(iso: string) {
  const d = new Date(iso)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
