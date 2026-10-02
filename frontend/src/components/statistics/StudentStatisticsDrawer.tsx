import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { statisticsApi } from '../../api/statistics.api';
import { Drawer } from '../ui/Drawer';
import { Spinner } from '../ui/Spinner';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import {
  User,
  Phone,
  School,
  HeartHandshake,
  Award,
  Clock,
} from 'lucide-react';

interface StudentStatisticsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  studentId: number | null;
}

export const StudentStatisticsDrawer: React.FC<StudentStatisticsDrawerProps> = ({
  isOpen,
  onClose,
  studentId,
}) => {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['statistics', 'student', studentId],
    queryFn: () => (studentId ? statisticsApi.getStudentStatistics(studentId) : null),
    enabled: isOpen && !!studentId,
  });

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title="الملف الروحي والإحصائي للمخدوم"
      size="md"
    >
      {isLoading ? (
        <div className="py-20 flex justify-center items-center">
          <Spinner size="lg" />
        </div>
      ) : error || !stats ? (
        <div className="p-6 text-center text-red-600 bg-red-50 rounded-2xl">
          تعذر تحميل بيانات المخدوم أو ليس لديك صلاحية لعرضها.
        </div>
      ) : (
        <div className="space-y-6">
          {/* Header Card */}
          <div className="bg-primary-50/50 p-5 rounded-2xl border border-primary-100 flex items-start gap-4">
            <div className="w-12 h-12 rounded-2xl bg-primary-600 text-white flex items-center justify-center font-bold shrink-0 shadow-md shadow-primary-500/10">
              <User className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-gray-900">{stats.studentName}</h3>
              <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500">
                <span className="flex items-center gap-1">
                  <School className="w-3.5 h-3.5 text-gray-400" />
                  {stats.ministryName} • {stats.className}
                </span>
                {stats.phone && (
                  <span className="flex items-center gap-1 font-mono" dir="ltr">
                    <Phone className="w-3.5 h-3.5 text-gray-400" />
                    {stats.phone}
                  </span>
                )}
              </div>
              {stats.servantName && (
                <p className="text-xs text-primary-700 font-semibold pt-1">
                  الخادم المسؤول: {stats.servantName}
                </p>
              )}
            </div>
          </div>

          {/* Spiritual Indicators Grid */}
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">
              مؤشرات الحضور والمتابعة (العام الحالي)
            </h4>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <Card className="p-3 text-center bg-emerald-50/30 border-emerald-100">
                <span className="text-[11px] text-gray-500 block mb-1">الافتقاد الأسبوعي</span>
                <span className="text-lg font-black text-emerald-700 font-mono">
                  {stats.visitPercentage}%
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">
                  {stats.visitedWeeksCount} من {stats.totalWeeksCount} أسبوع
                </span>
              </Card>

              <Card className="p-3 text-center bg-blue-50/30 border-blue-100">
                <span className="text-[11px] text-gray-500 block mb-1">حضور القداس</span>
                <span className="text-lg font-black text-blue-700 font-mono">
                  {stats.massAttendancePercentage}%
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">
                  {stats.massAttendanceCount} قداس
                </span>
              </Card>

              <Card className="p-3 text-center bg-indigo-50/30 border-indigo-100">
                <span className="text-[11px] text-gray-500 block mb-1">حضور الاجتماع</span>
                <span className="text-lg font-black text-indigo-700 font-mono">
                  {stats.meetingAttendancePercentage}%
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">
                  {stats.meetingAttendanceCount} اجتماع
                </span>
              </Card>

              <Card className="p-3 text-center bg-purple-50/30 border-purple-100">
                <span className="text-[11px] text-gray-500 block mb-1">حضور التسبحة</span>
                <span className="text-lg font-black text-purple-700 font-mono">
                  {stats.tasbehaAttendancePercentage}%
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">
                  {stats.tasbehaAttendanceCount} تسبحة
                </span>
              </Card>
            </div>
          </div>

          {/* Average Spiritual Note Score */}
          <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
            <h4 className="text-xs font-bold text-gray-700 mb-3 flex items-center gap-1.5">
              <Award className="w-4 h-4 text-purple-600" />
              متوسط درجة النوتة الروحية
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

          {/* Confessions Status */}
          <div className="p-4 rounded-2xl border border-purple-100 bg-purple-50/20 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                <HeartHandshake className="w-5 h-5" />
              </div>
              <div>
                <h5 className="font-bold text-gray-900 text-sm">سجل الاعتراف</h5>
                <p className="text-xs text-gray-500 mt-0.5">
                  {stats.lastConfessionDate
                    ? `آخر اعتراف مسجل: ${stats.lastConfessionDate}`
                    : 'لم يسجل اعتراف في هذا العام بعد'}
                </p>
              </div>
            </div>
            <Badge variant="primary" className="font-bold text-xs">
              {stats.totalConfessionsCount} اعترافات
            </Badge>
          </div>

          {/* Recent Visits Timeline */}
          <div>
            <h4 className="text-xs font-bold text-gray-700 mb-3 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-gray-400" />
              آخر افتقادات مسجلة للمخدوم
            </h4>
            {stats.recentVisits.length === 0 ? (
              <p className="text-xs text-gray-400 text-center py-4 bg-gray-50 rounded-xl">
                لا توجد زيارات مسجلة للمخدوم حتى الآن
              </p>
            ) : (
              <div className="space-y-2.5">
                {stats.recentVisits.map((v) => (
                  <div
                    key={v.visitId}
                    className="p-3 rounded-xl border border-gray-100 bg-white shadow-2xs text-xs space-y-1.5"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-gray-900">
                        {v.method === 'VISIT' ? 'افتفاد (زيارة)' : 'مكالمة هاتفية'}
                      </span>
                      <span className="text-[11px] text-gray-400 font-mono">
                        {v.recordedDate}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-gray-500 text-[11px]">
                      <span>درجة النوتة: {v.noteScore ?? '-'}</span>
                      {v.servantName && (
                        <span className="text-primary-700 font-semibold mr-auto">
                          سجل بواسطة: {v.servantName}
                        </span>
                      )}
                    </div>

                    {v.notes && (
                      <p className="text-gray-600 bg-gray-50 p-2 rounded-lg text-[11px] italic">
                        "{v.notes}"
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </Drawer>
  );
};
