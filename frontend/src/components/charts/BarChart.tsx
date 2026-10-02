import React from 'react';

export interface BarItem {
  id: string | number;
  label: string;
  value: number; // e.g. percentage (0-100) or count
  sublabel?: string;
  color?: string;
  onClick?: () => void;
}

interface BarChartProps {
  items: BarItem[];
  maxValue?: number;
  valueSuffix?: string;
  orientation?: 'horizontal' | 'vertical';
  emptyMessage?: string;
}

export const BarChart: React.FC<BarChartProps> = ({
  items,
  maxValue = 100,
  valueSuffix = '%',
  orientation = 'horizontal',
  emptyMessage = 'لا توجد بيانات متاحة للمقارنة',
}) => {
  if (!items.length) {
    return (
      <div className="flex items-center justify-center h-40 text-gray-400 text-sm">
        {emptyMessage}
      </div>
    );
  }

  const effectiveMax = maxValue > 0 ? maxValue : Math.max(...items.map((i) => i.value), 1);

  if (orientation === 'horizontal') {
    return (
      <div className="space-y-3 w-full">
        {items.map((item) => {
          const percent = Math.min(100, Math.max(0, (item.value / effectiveMax) * 100));
          const barColor = item.color || '#4A3E3D'; // Default primary brand color

          return (
            <div
              key={item.id}
              onClick={item.onClick}
              className={`p-2.5 rounded-xl transition duration-150 ${
                item.onClick ? 'cursor-pointer hover:bg-gray-50 active:bg-gray-100' : ''
              }`}
            >
              <div className="flex items-center justify-between text-xs font-semibold mb-1.5">
                <div className="flex items-center gap-2">
                  <span className="text-gray-900">{item.label}</span>
                  {item.sublabel && (
                    <span className="text-[11px] text-gray-400 font-normal">
                      ({item.sublabel})
                    </span>
                  )}
                </div>
                <span className="font-mono font-bold text-gray-900">
                  {item.value}
                  {valueSuffix}
                </span>
              </div>

              {/* Progress Track */}
              <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden p-0.5">
                <div
                  className="h-full rounded-full transition-all duration-500 ease-out"
                  style={{
                    width: `${percent}%`,
                    backgroundColor: barColor,
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  // Vertical orientation
  return (
    <div className="w-full">
      <div className="flex items-end justify-between gap-2 h-48 pt-6 pb-2 px-2 border-b border-gray-100">
        {items.map((item) => {
          const percent = Math.min(100, Math.max(0, (item.value / effectiveMax) * 100));
          const barColor = item.color || '#4A3E3D';

          return (
            <div
              key={item.id}
              onClick={item.onClick}
              className={`flex-1 flex flex-col items-center h-full justify-end group ${
                item.onClick ? 'cursor-pointer' : ''
              }`}
            >
              {/* Value Tooltip / Label */}
              <span className="text-[10px] font-mono font-bold text-gray-700 mb-1 opacity-80 group-hover:opacity-100 group-hover:text-primary-700 transition">
                {item.value}
                {valueSuffix}
              </span>

              {/* Bar Fill */}
              <div className="w-full max-w-[36px] bg-gray-100 rounded-t-xl overflow-hidden flex flex-col justify-end h-full">
                <div
                  className="w-full rounded-t-xl transition-all duration-500 ease-out group-hover:brightness-110"
                  style={{
                    height: `${percent}%`,
                    backgroundColor: barColor,
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {/* X-Labels */}
      <div className="flex justify-between gap-2 mt-2 px-2">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex-1 text-center truncate text-[11px] font-semibold text-gray-600"
            title={item.label}
          >
            {item.label}
          </div>
        ))}
      </div>
    </div>
  );
};
