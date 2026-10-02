import React, { useState } from 'react';

export interface LineSeries {
  id: string;
  name: string;
  color: string;
  data: number[]; // Values corresponding to labels
}

interface LineChartProps {
  labels: string[];
  series: LineSeries[];
  height?: number;
  yAxisSuffix?: string;
  maxValue?: number;
}

export const LineChart: React.FC<LineChartProps> = ({
  labels,
  series,
  height = 240,
  yAxisSuffix = '%',
  maxValue = 100,
}) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  if (!labels.length || !series.length) {
    return (
      <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
        لا توجد بيانات متاحة لعرض المنحنى
      </div>
    );
  }

  // SVG dimensions
  const width = 600;
  const paddingX = 45;
  const paddingY = 30;
  const chartWidth = width - paddingX * 2;
  const chartHeight = height - paddingY * 2;

  // Grid steps (0, 25, 50, 75, 100)
  const steps = [0, 25, 50, 75, 100];

  const getX = (index: number) => {
    if (labels.length === 1) return paddingX + chartWidth / 2;
    return paddingX + (index / (labels.length - 1)) * chartWidth;
  };

  const getY = (val: number) => {
    const clamped = Math.max(0, Math.min(maxValue, val));
    return paddingY + chartHeight - (clamped / maxValue) * chartHeight;
  };

  return (
    <div className="w-full">
      {/* Legend */}
      <div className="flex flex-wrap items-center gap-4 mb-3 text-xs font-semibold">
        {series.map((s) => (
          <div key={s.id} className="flex items-center gap-1.5">
            <span
              className="w-3 h-3 rounded-full shrink-0 shadow-xs"
              style={{ backgroundColor: s.color }}
            />
            <span className="text-gray-700">{s.name}</span>
          </div>
        ))}
      </div>

      {/* SVG Canvas */}
      <div className="relative w-full overflow-hidden">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto overflow-visible select-none"
        >
          <defs>
            {series.map((s) => (
              <linearGradient
                key={`grad-${s.id}`}
                id={`grad-${s.id}`}
                x1="0"
                y1="0"
                x2="0"
                y2="1"
              >
                <stop offset="0%" stopColor={s.color} stopOpacity="0.25" />
                <stop offset="100%" stopColor={s.color} stopOpacity="0.0" />
              </linearGradient>
            ))}
          </defs>

          {/* Horizontal Grid lines */}
          {steps.map((step) => {
            const y = getY(step);
            return (
              <g key={step}>
                <line
                  x1={paddingX}
                  y1={y}
                  x2={width - paddingX}
                  y2={y}
                  stroke="#E5E7EB"
                  strokeDasharray={step === 0 ? 'none' : '3 3'}
                  strokeWidth="1"
                />
                <text
                  x={paddingX - 8}
                  y={y + 3.5}
                  textAnchor="end"
                  className="text-[10px] fill-gray-400 font-mono font-medium"
                >
                  {step}
                  {yAxisSuffix}
                </text>
              </g>
            );
          })}

          {/* Area Fills and Lines */}
          {series.map((s) => {
            if (s.data.length === 0) return null;

            const points = s.data.map((val, i) => `${getX(i)},${getY(val)}`);
            const polylinePoints = points.join(' ');

            // Area polygon: start at bottom, go through points, close at bottom
            const areaPoints = [
              `${getX(0)},${getY(0)}`,
              polylinePoints,
              `${getX(s.data.length - 1)},${getY(0)}`,
            ].join(' ');

            return (
              <g key={s.id}>
                {/* Shaded Area */}
                <polygon
                  points={areaPoints}
                  fill={`url(#grad-${s.id})`}
                  className="transition-opacity duration-300"
                />
                {/* Line */}
                <polyline
                  points={polylinePoints}
                  fill="none"
                  stroke={s.color}
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="transition-all duration-300"
                />
                {/* Data Points */}
                {s.data.map((val, i) => {
                  const cx = getX(i);
                  const cy = getY(val);
                  const isHovered = hoveredIndex === i;
                  return (
                    <circle
                      key={i}
                      cx={cx}
                      cy={cy}
                      r={isHovered ? 5.5 : 3.5}
                      fill="#FFFFFF"
                      stroke={s.color}
                      strokeWidth={isHovered ? 3 : 2}
                      className="transition-all duration-150 cursor-pointer"
                    />
                  );
                })}
              </g>
            );
          })}

          {/* Hover Column & X-Axis Labels */}
          {labels.map((label, i) => {
            const x = getX(i);
            const isHovered = hoveredIndex === i;
            return (
              <g
                key={i}
                onMouseEnter={() => setHoveredIndex(i)}
                onMouseLeave={() => setHoveredIndex(null)}
                className="cursor-pointer"
              >
                {/* Transparent hit area */}
                <rect
                  x={x - chartWidth / (labels.length * 2)}
                  y={paddingY}
                  width={chartWidth / labels.length}
                  height={chartHeight}
                  fill="transparent"
                />

                {/* Vertical hover indicator line */}
                {isHovered && (
                  <line
                    x1={x}
                    y1={paddingY}
                    x2={x}
                    y2={height - paddingY}
                    stroke="#94A3B8"
                    strokeWidth="1.5"
                    strokeDasharray="2 2"
                  />
                )}

                {/* X Label */}
                <text
                  x={x}
                  y={height - paddingY + 18}
                  textAnchor="middle"
                  className={`text-[11px] transition-colors ${
                    isHovered ? 'fill-primary-700 font-bold' : 'fill-gray-500 font-medium'
                  }`}
                >
                  {label}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Hover Tooltip Overlay */}
        {hoveredIndex !== null && (
          <div
            className="absolute top-2 pointer-events-none bg-gray-900/90 backdrop-blur-xs text-white text-xs rounded-xl p-2.5 shadow-xl z-20 transition-all border border-white/10"
            style={{
              left: `${(getX(hoveredIndex) / width) * 100}%`,
              transform: 'translateX(-50%)',
            }}
          >
            <div className="font-bold text-gray-200 border-b border-gray-700 pb-1 mb-1.5 text-center">
              {labels[hoveredIndex]}
            </div>
            <div className="space-y-1">
              {series.map((s) => (
                <div key={s.id} className="flex items-center justify-between gap-3 text-[11px]">
                  <span className="flex items-center gap-1.5">
                    <span
                      className="w-2 h-2 rounded-full"
                      style={{ backgroundColor: s.color }}
                    />
                    <span className="text-gray-300">{s.name}:</span>
                  </span>
                  <span className="font-mono font-bold text-white">
                    {s.data[hoveredIndex] ?? 0}
                    {yAxisSuffix}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
