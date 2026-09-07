import type { MinutePoint } from '../api/types'

const WIDTH = 600
const HEIGHT = 150
const AXIS_HEIGHT = 16
const PLOT_HEIGHT = HEIGHT - AXIS_HEIGHT

const hhmm = (minute: string) => minute.slice(11)

export function RequestChart({ series }: { series: MinutePoint[] }) {
  if (series.length === 0) return null

  const maxRequests = Math.max(1, ...series.map((p) => p.requests))
  const maxResponse = Math.max(1, ...series.map((p) => p.avgResponseMs))
  const barWidth = WIDTH / series.length

  const responseLine = series
    .map((p, i) => `${i * barWidth + barWidth / 2},${PLOT_HEIGHT - (p.avgResponseMs / maxResponse) * PLOT_HEIGHT}`)
    .join(' ')

  const labelEvery = Math.ceil(series.length / 5)

  return (
    <div>
      <div className="mb-2 flex items-center gap-4 text-[11px] text-slate-500">
        <span className="flex items-center gap-1">
          <span className="inline-block h-2 w-2 rounded-sm bg-sky-400" /> 요청 수 (최대 {maxRequests})
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block h-2 w-2 rounded-sm bg-red-500" /> 에러
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block h-0.5 w-3 bg-amber-500" /> 평균 응답 (최대 {maxResponse}ms)
        </span>
      </div>

      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="w-full" role="img" aria-label="분당 요청 통계">
        <line x1="0" y1={PLOT_HEIGHT} x2={WIDTH} y2={PLOT_HEIGHT} className="stroke-slate-200" strokeWidth="1" />

        {series.map((point, i) => {
          const requestHeight = (point.requests / maxRequests) * PLOT_HEIGHT
          const errorHeight = (point.errors / maxRequests) * PLOT_HEIGHT
          return (
            <g key={point.minute}>
              <title>{`${hhmm(point.minute)} · 요청 ${point.requests}건 · 에러 ${point.errors}건 · 평균 ${point.avgResponseMs}ms`}</title>
              <rect
                x={i * barWidth}
                y={PLOT_HEIGHT - requestHeight}
                width={Math.max(1, barWidth - 1)}
                height={requestHeight}
                className="fill-sky-400"
              />
              {point.errors > 0 && (
                <rect
                  x={i * barWidth}
                  y={PLOT_HEIGHT - errorHeight}
                  width={Math.max(1, barWidth - 1)}
                  height={errorHeight}
                  className="fill-red-500"
                />
              )}
            </g>
          )
        })}

        <polyline points={responseLine} fill="none" className="stroke-amber-500" strokeWidth="1.5" />

        {series.map((point, i) =>
          i % labelEvery === 0 ? (
            <text key={point.minute} x={i * barWidth} y={HEIGHT - 4} className="fill-slate-400 text-[9px]">
              {hhmm(point.minute)}
            </text>
          ) : null,
        )}
      </svg>
    </div>
  )
}
