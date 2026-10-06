import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { statisticsApi } from '../../api/statistics.api';
import { Drawer } from '../ui/Drawer';
import { Spinner } from '../ui/Spinner';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import {
  UserCheck,
  Award,
  Users,
  CalendarCheck,
  BookOpen,
  CheckCircle2,
  XCircle,
  Clock,
  Phone,
  School,
  Sparkles,
  AlertCircle,
} from 'lucide-react';

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
      title="الملف الروحي ومؤشرات أداء الخادم"
      size="lg"
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
          <div className="bg-primary-50/50 p-5 rounded-2xl border border-primary-100 flex items-start justify-between gap-4">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-primary-600 text-white flex items-center justify-center font-bold shrink-0 shadow-md shadow-primary-500/10">
                <UserCheck className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-lg font-bold text-gray-900">{stats.servantName}</h3>
                <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500">
                  {(stats.ministryName || stats.className) && (
                    <span className="flex items-center gap-1">
                      <School className="w-3.5 h-3.5 text-gray-400" />
                      {stats.ministryName ? stats.ministryName : ''}
                      {stats.ministryName && stats.className ? ' • ' : ''}
                      {stats.className ? stats.className : ''}
                    </span>
                  )}
                  {stats.phone && (
                    <span className="flex items-center gap-1 font-mono" dir="ltr">
                      <Phone className="w-3.5 h-3.5 text-gray-400" />
                      {stats.phone}
                    </span>
                  )}
                </div>
              </div>
            </div>

            <div className="text-left shrink-0">
              <Badge variant="primary" className="text-xs">
                مسؤول عن {stats.assignedStudentsCount} مخدوم
              </Badge>
            </div>
          </div>

          {/* Current Week 4 Core Metrics */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold text-gray-700 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-primary-600" />
                أداء الخادم خلال الأسبوع المحدد
              </h4>
              {!stats.recordedFollowUp ? (
                <Badge variant="warning" className="text-[11px] gap-1">
                  <AlertCircle className="w-3 h-3" />
                  لم يسجل المتابعة بعد
                </Badge>
              ) : (
                <Badge variant="success" className="text-[11px] gap-1">
                  <CheckCircle2 className="w-3 h-3" />
                  تم تسجيل المتابعة
                </Badge>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {/* 1. النوتة الروحية */}
              <Card className="p-4 bg-purple-50/30 border-purple-100 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-bold text-gray-600 flex items-center gap-1">
                      <BookOpen className="w-3.5 h-3.5 text-purple-600" />
                      النوتة الروحية
                    </span>
                    {stats.notePercentage !== null && (
                      <span className="text-xs font-mono font-bold text-purple-700">
                        {stats.notePercentage}%
                      </span>
                    )}
                  </div>
                  <div className="text-xl font-black text-purple-900 font-mono">
                    {stats.noteScore !== null && stats.maxNoteScore !== null
                      ? `${stats.noteScore} / ${stats.maxNoteScore}`
                      : 'غير مسجل'}
                  </div>
                </div>
                <div className="mt-3">
                  <div className="w-full bg-purple-100 h-1.5 rounded-full overflow-hidden">
                    <div
                      className="bg-purple-600 h-full rounded-full transition-all"
                      style={{ width: `${Math.min(100, stats.notePercentage ?? 0)}%` }}
                    />
                  </div>
                  <p className="text-[10px] text-gray-400 mt-1">
                    {stats.notePercentage !== null && stats.notePercentage !== undefined
                      ? stats.notePercentage >= 80
                        ? 'مستوى ممتاز'
                        : stats.notePercentage >= 50
                        ? 'مستوى جيد'
                        : 'بحاجة للاهتمام'
                      : 'لم يتم رصد الدرجات'}
                  </p>
                </div>
              </Card>

              {/* 2. اجتماع الخدمة */}
              <Card className="p-4 bg-sky-50/30 border-sky-100 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-bold text-gray-600 flex items-center gap-1">
                      <Users className="w-3.5 h-3.5 text-sky-600" />
                      اجتماع الخدمة
                    </span>
                  </div>
                  <div className="flex items-center gap-2 mt-1">
                    {stats.attendedServiceMeeting === true ? (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        حاضر
                      </span>
                    ) : stats.attendedServiceMeeting === false ? (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-rose-700 bg-rose-50 px-2.5 py-1 rounded-lg border border-rose-200">
                        <XCircle className="w-4 h-4 text-rose-600" />
                        غائب
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-gray-500 bg-gray-50 px-2.5 py-1 rounded-lg border border-gray-200">
                        <Clock className="w-4 h-4 text-gray-400" />
                        غير مسجل
                      </span>
                    )}
                  </div>
                </div>
                <p className="text-[10px] text-gray-400 mt-3">
                  حضور اجتماع الخدمة الأسبوعي
                </p>
              </Card>

              {/* 3. القداس الإلهي */}
              <Card className="p-4 bg-indigo-50/30 border-indigo-100 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-bold text-gray-600 flex items-center gap-1">
                      <UserCheck className="w-3.5 h-3.5 text-indigo-600" />
                      القداس الإلهي
                    </span>
                  </div>
                  <div className="flex items-center gap-2 mt-1">
                    {stats.attendedMass === true ? (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        حاضر
                      </span>
                    ) : stats.attendedMass === false ? (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-rose-700 bg-rose-50 px-2.5 py-1 rounded-lg border border-rose-200">
                        <XCircle className="w-4 h-4 text-rose-600" />
                        غائب
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 text-sm font-bold text-gray-500 bg-gray-50 px-2.5 py-1 rounded-lg border border-gray-200">
                        <Clock className="w-4 h-4 text-gray-400" />
                        غير مسجل
                      </span>
                    )}
                  </div>
                </div>
                <p className="text-[10px] text-gray-400 mt-3">
                  المواظبة على سر الإفخارستيا
                </p>
              </Card>

              {/* 4. إنجاز الافتقاد */}
              <Card className="p-4 bg-emerald-50/30 border-emerald-100 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-bold text-gray-600 flex items-center gap-1">
                      <CalendarCheck className="w-3.5 h-3.5 text-emerald-600" />
                      إنجاز الافتقاد
                    </span>
                    <span className="text-xs font-mono font-bold text-emerald-700">
                      {stats.visitPercentage}%
                    </span>
                  </div>
                  <div className="text-xl font-black text-emerald-900 font-mono">
                    {stats.visitedCount} / {stats.assignedStudentsCount}
                  </div>
                </div>
                <div className="mt-3">
                  <div className="w-full bg-emerald-100 h-1.5 rounded-full overflow-hidden">
                    <div
                      className="bg-emerald-600 h-full rounded-full transition-all"
                      style={{ width: `${Math.min(100, stats.visitPercentage)}%` }}
                    />
                  </div>
                  <p className="text-[10px] text-gray-400 mt-1">
                    متبقي {Math.max(0, stats.assignedStudentsCount - stats.visitedCount)} مخدوم
                  </p>
                </div>
              </Card>
            </div>

            {/* Additional Weekly Activities Badges */}
            {(stats.attendedTasbeha !== null || stats.attendedManagementMeeting !== null || stats.averageNoteScore !== null) && (
              <div className="flex flex-wrap items-center gap-2 pt-1 text-xs">
                {stats.attendedTasbeha !== null && (
                  <Badge variant={stats.attendedTasbeha ? 'success' : 'neutral'}>
                    التسبحة: {stats.attendedTasbeha ? 'حاضر' : 'غائب'}
                  </Badge>
                )}
                {stats.attendedManagementMeeting !== null && (
                  <Badge variant={stats.attendedManagementMeeting ? 'success' : 'neutral'}>
                    اجتماع الإدارة: {stats.attendedManagementMeeting ? 'حاضر' : 'غائب'}
                  </Badge>
                )}
                {stats.averageNoteScore !== null && (
                  <Badge variant="neutral" className="gap-1">
                    <Award className="w-3 h-3 text-purple-600" />
                    متوسط درجات نوتة مخدوميه: {stats.averageNoteScore}
                  </Badge>
                )}
              </div>
            )}
          </div>

          {/* Cumulative Annual Indicators */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              المؤشرات التراكمية على مدار العام الدراسي
            </h4>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2.5">
              <Card className="p-3 text-center bg-gray-50/50">
                <span className="text-[11px] text-gray-500 block mb-1">تسجيل المتابعة</span>
                <span className="text-lg font-black text-gray-800 font-mono">
                  {stats.annualRecordingRate !== null && stats.annualRecordingRate !== undefined
                    ? `${stats.annualRecordingRate}%`
                    : '-'}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">نسبة الأسابيع</span>
              </Card>

              <Card className="p-3 text-center bg-purple-50/30 border-purple-100">
                <span className="text-[11px] text-gray-500 block mb-1">متوسط النوتة</span>
                <span className="text-lg font-black text-purple-700 font-mono">
                  {stats.annualAverageNotePercentage !== null && stats.annualAverageNotePercentage !== undefined
                    ? `${stats.annualAverageNotePercentage}%`
                    : '-'}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">سنوي</span>
              </Card>

              <Card className="p-3 text-center bg-indigo-50/30 border-indigo-100">
                <span className="text-[11px] text-gray-500 block mb-1">حضور القداس</span>
                <span className="text-lg font-black text-indigo-700 font-mono">
                  {stats.annualMassAttendanceRate !== null && stats.annualMassAttendanceRate !== undefined
                    ? `${stats.annualMassAttendanceRate}%`
                    : '-'}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">سنوي</span>
              </Card>

              <Card className="p-3 text-center bg-sky-50/30 border-sky-100">
                <span className="text-[11px] text-gray-500 block mb-1">اجتماع الخدمة</span>
                <span className="text-lg font-black text-sky-700 font-mono">
                  {stats.annualMeetingAttendanceRate !== null && stats.annualMeetingAttendanceRate !== undefined
                    ? `${stats.annualMeetingAttendanceRate}%`
                    : '-'}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">سنوي</span>
              </Card>

              <Card className="p-3 text-center bg-emerald-50/30 border-emerald-100 col-span-2 sm:col-span-1">
                <span className="text-[11px] text-gray-500 block mb-1">نسبة الافتقاد</span>
                <span className="text-lg font-black text-emerald-700 font-mono">
                  {stats.annualVisitPercentage !== null && stats.annualVisitPercentage !== undefined
                    ? `${stats.annualVisitPercentage}%`
                    : '-'}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">سنوي</span>
              </Card>
            </div>
          </div>

          {/* Recent Weeks History */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              سجل الأسابيع الأخيرة
            </h4>
            <div className="border border-gray-100 rounded-2xl overflow-hidden shadow-2xs">
              <table className="w-full text-right text-xs">
                <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                  <tr>
                    <th className="py-2.5 px-3">الأسبوع</th>
                    <th className="py-2.5 px-3">النوتة الروحية</th>
                    <th className="py-2.5 px-3">القداس</th>
                    <th className="py-2.5 px-3">الاجتماع</th>
                    <th className="py-2.5 px-3">الافتقاد</th>
                    <th className="py-2.5 px-3">المتابعة</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {(!stats.recentWeeks || stats.recentWeeks.length === 0) ? (
                    <tr>
                      <td colSpan={6} className="text-center py-6 text-gray-400">
                        لا توجد سجلات سابقة متاحة لهذا الخادم
                      </td>
                    </tr>
                  ) : (
                    stats.recentWeeks.map((w) => (
                      <tr key={w.weekId} className="hover:bg-gray-50/50 transition">
                        <td className="py-2.5 px-3 font-mono font-medium text-gray-700">
                          {w.weekStartDate}
                        </td>
                        <td className="py-2.5 px-3 font-mono">
                          {w.noteScore !== null && w.maxNoteScore !== null ? (
                            <span className="font-bold text-purple-700">
                              {w.noteScore}/{w.maxNoteScore} ({w.notePercentage}%)
                            </span>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                        <td className="py-2.5 px-3">
                          {w.attendedMass === true ? (
                            <Badge variant="success" className="text-[10px]">حاضر</Badge>
                          ) : w.attendedMass === false ? (
                            <Badge variant="danger" className="text-[10px]">غائب</Badge>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                        <td className="py-2.5 px-3">
                          {w.attendedServiceMeeting === true ? (
                            <Badge variant="success" className="text-[10px]">حاضر</Badge>
                          ) : w.attendedServiceMeeting === false ? (
                            <Badge variant="danger" className="text-[10px]">غائب</Badge>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                        <td className="py-2.5 px-3 font-mono">
                          <span className="font-bold text-emerald-700">
                            {w.visitedCount}/{w.assignedCount} ({w.visitPercentage}%)
                          </span>
                        </td>
                        <td className="py-2.5 px-3">
                          {w.recorded ? (
                            <span className="text-emerald-600 font-bold text-[11px] flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" />
                              مسجلة
                            </span>
                          ) : (
                            <span className="text-amber-600 text-[11px] flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              غير مسجلة
                            </span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </Drawer>
  );
};
