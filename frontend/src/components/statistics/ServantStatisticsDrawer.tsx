import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { statisticsApi } from '../../api/statistics.api';
import { Drawer } from '../ui/Drawer';
import { Spinner } from '../ui/Spinner';
import { Card } from '../ui/Card';
import { UserCheck, Award, Users, CalendarCheck } from 'lucide-react';

interface ServantStatisticsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  servantId: number | null;
  weekId?: number;
}

export const ServantStatisticsDrawer: React.FC<ServantStatisticsDrawerProps> = ({
  isOpen,
  onClose,
  servantId,
  weekId,
}) => {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['statistics', 'servant', servantId, weekId],
    queryFn: () => (servantId ? statisticsApi.getServantStatistics(servantId, weekId) : null),
    enabled: isOpen && !!servantId,
  });

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title="مؤشرات أداء الخادم الأسبوعية"
      size="md"
    >
      {isLoading ? (
        <div className="py-20 flex justify-center items-center">
          <Spinner size="lg" />
        </div>
      ) : error || !stats ? (
        <div className="p-6 text-center text-red-600 bg-red-50 rounded-2xl">
          تعذر تحميل إحصائيات الخادم أو ليس لديك صلاحية للاطلاع عليها.
        </div>
      ) : (
        <div className="space-y-6">
          {/* Header Card */}
          <div className="bg-primary-50/50 p-5 rounded-2xl border border-primary-100 flex items-start gap-4">
            <div className="w-12 h-12 rounded-2xl bg-primary-600 text-white flex items-center justify-center font-bold shrink-0 shadow-md shadow-primary-500/10">
              <UserCheck className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-gray-900">{stats.servantName}</h3>
              <p className="text-xs text-gray-500 mt-0.5">
                مسؤول عن {stats.assignedStudentsCount} مخدوم
              </p>
            </div>
          </div>

          {/* Follow-up Progress Card */}
          <Card className="p-5 bg-gradient-to-br from-white to-primary-50/20 border-primary-100">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold text-gray-600 flex items-center gap-1.5">
                <CalendarCheck className="w-4 h-4 text-primary-600" />
                إنجاز الافتقاد للأسبوع
              </span>
              <span className="text-sm font-black text-primary-700 font-mono">
                {stats.visitPercentage}%
              </span>
            </div>

            <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden p-0.5">
              <div
                className="bg-primary-600 h-full rounded-full transition-all duration-500 ease-out"
                style={{ width: `${stats.visitPercentage}%` }}
              />
            </div>

            <div className="flex justify-between items-center text-xs text-gray-500 mt-2 font-medium">
              <span>تم افتقاد: {stats.visitedCount} مخدوم</span>
              <span>المتبقي: {Math.max(0, stats.assignedStudentsCount - stats.visitedCount)} مخدوم</span>
            </div>
          </Card>

          {/* Average Spiritual Note Score of assigned students */}
          <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
            <h4 className="text-xs font-bold text-gray-700 mb-3 flex items-center gap-1.5">
              <Award className="w-4 h-4 text-purple-600" />
              متوسط درجة النوتة الروحية للمخدومين
            </h4>
            <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-2xs flex items-center justify-between">
              <span className="text-xs font-bold text-gray-600 flex items-center gap-1.5">
                <Award className="w-3.5 h-3.5 text-purple-500" />
                متوسط درجة النوتة
              </span>
              <span className="text-lg font-bold text-purple-700 font-mono">
                {stats.averageNoteScore !== null ? stats.averageNoteScore : '-'}
              </span>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-blue-50 border border-blue-100 text-xs text-blue-800 flex items-center gap-2">
            <Users className="w-4 h-4 text-blue-600 shrink-0" />
            <span>
              يتم احتساب النسب والمتوسطات وفق الأسبوع المحدد في لوحة الإحصائيات.
            </span>
          </div>
        </div>
      )}
    </Drawer>
  );
};
