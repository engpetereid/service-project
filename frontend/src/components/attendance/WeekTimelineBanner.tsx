import React, { useState } from 'react';
import { WeekResponse } from '../../types/visit.types';
import { formatDate } from '../../utils/date';
import { Button } from '../ui/Button';
import {
  Calendar,
  ChevronRight,
  ChevronLeft,
  Lock,
  Unlock,
  Info,
  ChevronUp,
  Sparkles,
  RotateCcw,
} from 'lucide-react';

interface WeekTimelineBannerProps {
  currentWeek: WeekResponse | null;
  selectedWeek: WeekResponse | null;
  allWeeks: WeekResponse[];
  onSelectWeek: (week: WeekResponse) => void;
  isAdmin?: boolean;
}

export const WeekTimelineBanner: React.FC<WeekTimelineBannerProps> = ({
  currentWeek,
  selectedWeek,
  allWeeks,
  onSelectWeek,
  isAdmin = false,
}) => {
  const [showExplanation, setShowExplanation] = useState(false);

  if (!selectedWeek) return null;

  const isCurrent = currentWeek?.id === selectedWeek.id;
  const isLocked = selectedWeek.locked;

  // Calculate days remaining until lock if not locked
  const calculateDaysRemaining = (endDateStr: string) => {
    const endDate = new Date(endDateStr);
    const lockDate = new Date(endDate);
    lockDate.setDate(lockDate.getDate() + 30);
    const today = new Date();
    const diffTime = lockDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return Math.max(0, diffDays);
  };

  const daysRemaining = calculateDaysRemaining(selectedWeek.endDate);

  // Find index in sorted weeks list
  // Usually sorted desc by startDate (newest first)
  const currentIndex = allWeeks.findIndex((w) => w.id === selectedWeek.id);
  const hasNextWeek = currentIndex > 0; // newer week exists
  const hasPrevWeek = currentIndex < allWeeks.length - 1 && currentIndex !== -1; // older week exists

  const handlePrev = () => {
    if (hasPrevWeek) {
      onSelectWeek(allWeeks[currentIndex + 1]);
    }
  };

  const handleNext = () => {
    if (hasNextWeek) {
      onSelectWeek(allWeeks[currentIndex - 1]);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 space-y-3">
      {/* Top row: Status, date range, and quick nav */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Week Info & Status */}
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-mono font-black text-sm text-gray-900 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-primary-600" />
              <span>
                الجمعة {formatDate(selectedWeek.startDate)} — الخميس {formatDate(selectedWeek.endDate)}
              </span>
            </span>

            {/* Status Pills */}
            {isCurrent && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                الأسبوع الخدمي الحالي
              </span>
            )}

            {isLocked ? (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-gray-100 text-gray-700 border border-gray-200">
                <Lock className="w-3 h-3 text-gray-500" />
                <span>مغلق (مرور أكثر من 30 يوماً)</span>
                {isAdmin && <span className="text-[10px] text-primary-700 font-bold">(صلاحية أمين عام)</span>}
              </span>
            ) : !isCurrent ? (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
                <Unlock className="w-3 h-3 text-blue-500" />
                <span>متاح للتعديل (متبقي {daysRemaining} يوماً على القفل)</span>
              </span>
            ) : null}
          </div>

          <p className="text-xs text-gray-500">
            {isLocked && !isAdmin
              ? 'هذا الأسبوع مغلق تاريخياً ولا يمكن تسجيل أو تعديل الحضور فيه.'
              : isCurrent
              ? 'يتم تسجيل حضور القداس واجتماع الخدمة للأسبوع الجاري بصورة فورية.'
              : 'يمكنك مراجعة وتعديل بيانات الحضور لهذا الأسبوع السابق طالما لم تنته مهلة الـ 30 يوماً.'}
          </p>
        </div>

        {/* Navigation Controls */}
        <div className="flex items-center gap-2 shrink-0">
          <Button
            variant="outline"
            size="sm"
            onClick={handlePrev}
            disabled={!hasPrevWeek}
            className="text-xs font-bold"
            title="الأسبوع السابق"
          >
            <ChevronRight className="w-4 h-4 ml-1" />
            السابق
          </Button>

          {!isCurrent && currentWeek && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onSelectWeek(currentWeek)}
              className="text-xs font-bold bg-primary-50 text-primary-700 border-primary-200 hover:bg-primary-100"
            >
              <RotateCcw className="w-3.5 h-3.5 ml-1" />
              الأسبوع الحالي
            </Button>
          )}

          <Button
            variant="outline"
            size="sm"
            onClick={handleNext}
            disabled={!hasNextWeek}
            className="text-xs font-bold"
            title="الأسبوع التالي"
          >
            التالي
            <ChevronLeft className="w-4 h-4 mr-1" />
          </Button>

          <button
            type="button"
            onClick={() => setShowExplanation(!showExplanation)}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition"
            title="شرح آلية الأسابيع وقفل الـ 30 يوماً"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Explanatory Dropdown */}
      {showExplanation && (
        <div className="p-3.5 bg-gray-50 rounded-xl border border-gray-200 text-xs text-gray-700 space-y-2 animate-in fade-in slide-in-from-top-1">
          <div className="flex items-center justify-between font-bold text-gray-900">
            <span className="flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-primary-600" />
              كيف تعمل الأسابيع الخدمية وقاعدة الـ 30 يوماً؟
            </span>
            <button
              type="button"
              onClick={() => setShowExplanation(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              <ChevronUp className="w-3.5 h-3.5" />
            </button>
          </div>
          <ul className="list-disc list-inside space-y-1 text-gray-600 leading-relaxed pr-1">
            <li>
              <strong>دورة الأسبوع الخدمي:</strong> يبدأ الأسبوع دائماً يوم <strong>الجمعة</strong> في تمام الساعة 00:00 وينتهي يوم <strong>الخميس</strong> الساعة 23:59:59.
            </li>
            <li>
              <strong>قاعدة قفل الـ 30 يوماً:</strong> تظل الأسابيع مفتوحة لتسجيل وتعديل الحضور والافتقاد طوال الأسبوع ولمدة <strong>30 يوماً</strong> إضافية بعد انتهائه.
            </li>
            <li>
              <strong>حماية البيانات:</strong> بعد انقضاء الـ 30 يوماً يُقفل الأسبوع تلقائياً للقراءة فقط حفاظاً على الإحصائيات، مع إمكانية التعديل الاستثنائي للأمين العام فقط.
            </li>
          </ul>
        </div>
      )}
    </div>
  );
};
