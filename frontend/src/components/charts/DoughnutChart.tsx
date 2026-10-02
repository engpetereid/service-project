import React from 'react';

export interface DoughnutSegment {
  id: string;
  label: string;
  value: number;
  color: string;
}

interface DoughnutChartProps {
  segments: DoughnutSegment[];
  size?: number;
  strokeWidth?: number;
  centerLabel?: string;
  centerValue?: string | number;
}

export const DoughnutChart: React.FC<DoughnutChartProps> = ({
  segments,
  size = 180,
  strokeWidth = 22,
  centerLabel,
  centerValue,
}) => {
  const total = segments.reduce((sum, s) => sum + s.value, 0);

  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  let accumulatedPercent = 0;

  return (
    <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
      {/* SVG Ring */}
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          className="transform -rotate-90"
        >
          {/* Background circle track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke="#F3F4F6"
            strokeWidth={strokeWidth}
          />

          {/* Segment strokes */}
          {total > 0 &&
            segments.map((segment) => {
              const percent = (segment.value / total) * 100;
              const strokeDasharray = `${(percent / 100) * circumference} ${circumference}`;
              const strokeDashoffset = -((accumulatedPercent / 100) * circumference);
              accumulatedPercent += percent;

              return (
                <circle
                  key={segment.id}
                  cx={size / 2}
                  cy={size / 2}
                  r={radius}
                  fill="transparent"
                  stroke={segment.color}
                  strokeWidth={strokeWidth}
                  strokeDasharray={strokeDasharray}
                  strokeDashoffset={strokeDashoffset}
                  strokeLinecap="round"
                  className="transition-all duration-700 ease-out"
                />
              );
            })}
        </svg>

        {/* Center content */}
        {(centerValue !== undefined || centerLabel) && (
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center p-2">
            {centerValue !== undefined && (
              <span className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight font-mono">
                {centerValue}
              </span>
            )}
            {centerLabel && (
              <span className="text-[11px] font-semibold text-gray-500 mt-0.5">
                {centerLabel}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Legend list */}
      <div className="space-y-2 text-xs">
        {segments.map((segment) => {
          const percent = total > 0 ? Math.round((segment.value / total) * 100) : 0;
          return (
            <div key={segment.id} className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <span
                  className="w-3 h-3 rounded-full shrink-0 shadow-xs"
                  style={{ backgroundColor: segment.color }}
                />
                <span className="font-semibold text-gray-700">{segment.label}</span>
              </div>
              <div className="flex items-center gap-2 font-mono">
                <span className="text-gray-900 font-bold">{segment.value}</span>
                <span className="text-gray-400 text-[10px]">({percent}%)</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
