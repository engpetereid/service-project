import React from 'react';
import { Check, X, UserCheck } from 'lucide-react';

interface AttendanceToggleCardProps {
  studentId: number;
  studentName: string;
  className?: string;
  ministryName?: string;
  servantName?: string;
  recordedByInfo?: string;
  isPresent: boolean;
  isToggling?: boolean;
  disabled?: boolean;
  onToggle: (studentId: number, newPresent: boolean) => void;
}

export const AttendanceToggleCard: React.FC<AttendanceToggleCardProps> = ({
  studentId,
  studentName,
  className,
  ministryName,
  servantName,
  recordedByInfo,
  isPresent,
  isToggling = false,
  disabled = false,
  onToggle,
}) => {
  const handleToggleClick = () => {
    if (disabled || isToggling) return;
    onToggle(studentId, !isPresent);
  };

  return (
    <div
      onClick={handleToggleClick}
      className={`p-3.5 sm:p-4 rounded-2xl border transition flex items-center justify-between gap-3 cursor-pointer select-none ${
        isPresent
          ? 'bg-emerald-50/50 border-emerald-200 shadow-sm'
          : 'bg-white border-gray-100 hover:border-gray-200'
      } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      {/* Student Details */}
      <div className="flex items-center gap-3">
        <div
          className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 transition ${
            isPresent
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'bg-gray-100 text-gray-600'
          }`}
        >
          {isPresent ? <Check className="w-5 h-5 stroke-[2.5]" /> : studentName.charAt(0)}
        </div>

        <div>
          <h4 className="font-bold text-gray-900 text-sm leading-tight">
            {studentName}
          </h4>

          <div className="flex items-center gap-2 mt-0.5 text-[11px] text-gray-500 flex-wrap">
            {className && <span>{className}</span>}
            {ministryName && <span>&bull; {ministryName}</span>}
            {servantName && (
              <span className="inline-flex items-center gap-1 text-primary-700 font-semibold bg-primary-50 px-1.5 py-0.5 rounded-md">
                <UserCheck className="w-3 h-3" />
                {servantName}
              </span>
            )}
          </div>

          {recordedByInfo && isPresent && (
            <p className="text-[10px] text-gray-400 mt-1 font-mono">
              {recordedByInfo}
            </p>
          )}
        </div>
      </div>

      {/* Toggle Button */}
      <div className="flex items-center gap-2 shrink-0">
        <button
          type="button"
          disabled={disabled || isToggling}
          onClick={(e) => {
            e.stopPropagation();
            handleToggleClick();
          }}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-1.5 min-h-[44px] min-w-[85px] justify-center ${
            isPresent
              ? 'bg-emerald-600 text-white shadow-sm hover:bg-emerald-700'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          {isToggling ? (
            <span className="inline-block w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
          ) : isPresent ? (
            <>
              <Check className="w-4 h-4" />
              <span>حاضر</span>
            </>
          ) : (
            <>
              <X className="w-3.5 h-3.5" />
              <span>غائب</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
