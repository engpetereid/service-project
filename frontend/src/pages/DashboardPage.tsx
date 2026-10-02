import React from 'react';
import { useAuth } from '../auth/useAuth';
import { usePermissions } from '../auth/usePermissions';
import { useQuery } from '@tanstack/react-query';
import { statisticsApi } from '../api/statistics.api';
import { visitsApi } from '../api/visits.api';
import { selfFollowupApi } from '../api/selfFollowup.api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ROLE_LABELS } from '../utils/arabic';
import { AdminOnboardingChecklist } from '../components/admin/AdminOnboardingChecklist';
import { AdminQuickActions } from '../components/admin/AdminQuickActions';
import { AdminConfigurationWarnings } from '../components/admin/AdminConfigurationWarnings';
import { AdminSystemOverview } from '../components/admin/AdminSystemOverview';
import {
  Users,
  CalendarCheck,
  TrendingUp,
  AlertTriangle,
  UserCheck,
  ChevronLeft,
  Phone,
  School,
  ClipboardCheck,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const { isAdmin, isServant, isClassSecretary } = usePermissions();

  // Admin Setup & System Health Query
  const { data: adminSetup } = useQuery({
    queryKey: ['statistics', 'admin-setup'],
    queryFn: statisticsApi.getAdminSetup,
    enabled: isAdmin,
    staleTime: 30000,
  });

  // Statistics Dashboard Query (Weekly KPIs)
  const { data: stats, isLoading: isStatsLoading } = useQuery({
    queryKey: ['statistics', 'dashboard'],
    queryFn: () => statisticsApi.getDashboard(),
    staleTime: 30000,
  });

  // Absence Alerts Query
  const { data: alerts = [] } = useQuery({
    queryKey: ['statistics', 'absence-alerts'],
    queryFn: () => statisticsApi.getAbsenceAlerts(),
    staleTime: 30000,
  });

  // Servant Current Week Query
  const { data: currentWeekData } = useQuery({
    queryKey: ['visits', 'current-week'],
    queryFn: visitsApi.getCurrentWeek,
    enabled: isServant,
  });

  // Self Follow-up Query for current week
  const { data: selfFollowUpData } = useQuery({
    queryKey: ['self-followup', 'current-week'],
    queryFn: selfFollowupApi.getCurrentWeek,
    enabled: isServant || isClassSecretary,
    staleTime: 30000,
  });

  const primaryRole = user?.roles?.[0]?.role;
  const servantStudents = currentWeekData?.students || [];
  const visitedCount = servantStudents.filter((s) => s.visited).length;
  const totalCount = servantStudents.length;
  const progressPercent = totalCount > 0 ? Math.round((visitedCount / totalCount) * 100) : 0;

  return (
    <div className="space-y-6 pb-8">
      {/* Compact Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-gray-100">
        <div>
          <h1 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight">
            لوحة المتابعة
          </h1>
          <p className="text-xs text-gray-500 mt-0.5 font-medium">
            {primaryRole ? `النطاق الحالي: ${ROLE_LABELS[primaryRole]}` : 'مستخدم مصرح'}
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {(isServant || isClassSecretary) && (
            <Link to="/self-followup" className="flex-1 sm:flex-initial">
              <Button
                variant="outline"
                size="sm"
                className="w-full sm:w-auto font-bold text-xs bg-purple-50 text-purple-700 border-purple-200 hover:bg-purple-100"
              >
                <ClipboardCheck className="w-3.5 h-3.5 ml-1.5" />
                متابعتي الأسبوعية
              </Button>
            </Link>
          )}
          <Link to="/visits" className="flex-1 sm:flex-initial">
            <Button
              variant="outline"
              size="sm"
              className="w-full sm:w-auto font-bold text-xs"
            >
              <CalendarCheck className="w-3.5 h-3.5 ml-1.5" />
              متابعة المخدومين
            </Button>
          </Link>
          <Link to="/attendance" className="flex-1 sm:flex-initial">
            <Button
              variant="outline"
              size="sm"
              className="w-full sm:w-auto font-bold text-xs"
            >
              <UserCheck className="w-3.5 h-3.5 ml-1.5" />
              تسجيل الحضور
            </Button>
          </Link>
        </div>
      </div>

      {/* ADMIN WORKFLOW SECTION (Visible to General Admin) */}
      {isAdmin && adminSetup && (
        <div className="space-y-6 animate-in fade-in">
          {/* 1. Onboarding Checklist */}
          <AdminOnboardingChecklist setupData={adminSetup} />

          {/* 2. Configuration Warnings */}
          <AdminConfigurationWarnings warnings={adminSetup.warnings} />

          {/* 3. Quick Actions Grid */}
          <AdminQuickActions />

          {/* 4. System Overview Metrics */}
          <AdminSystemOverview setupData={adminSetup} />
        </div>
      )}

      {/* SERVANT & CLASS SECRETARY SELF-FOLLOW-UP CARD */}
      {(isServant || isClassSecretary) && (
        <Card className="p-5 bg-gradient-to-br from-white via-purple-50/20 to-white border-purple-100 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold shrink-0">
                <ClipboardCheck className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-bold text-purple-600 uppercase tracking-wider block">
                  متابعتي الروحية والخدمية للأسبوع الحالي
                </span>
                <h3 className="text-base sm:text-lg font-black text-gray-900 mt-0.5">
                  {selfFollowUpData?.record
                    ? `إنجاز متابعتك: ${selfFollowUpData.record.overallPercentage}%`
                    : 'لم تسجل متابعتك لهذا الأسبوع بعد'}
                </h3>
              </div>
            </div>

            <Link to="/self-followup">
              <Button size="sm" variant="primary" className="font-bold bg-purple-600 hover:bg-purple-700 border-none text-white w-full sm:w-auto">
                {selfFollowUpData?.record ? 'تعديل متابعتي' : 'تسجيل متابعتي الآن'}
                <ChevronLeft className="w-4 h-4 mr-1" />
              </Button>
            </Link>
          </div>

          {selfFollowUpData?.record && (
            <div className="space-y-1.5 pt-1">
              <div className="w-full bg-gray-100 h-2.5 rounded-full overflow-hidden p-0.5">
                <div
                  className="bg-purple-600 h-full rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${selfFollowUpData.record.overallPercentage}%` }}
                />
              </div>
              <div className="flex justify-between items-center text-[11px] text-gray-400 font-mono">
                <span>
                  نوتة: {selfFollowUpData.record.noteScore ?? '—'}/{selfFollowUpData.record.maxNoteScoreSnapshot}
                </span>
                <span>المعدل العام: {selfFollowUpData.record.overallPercentage}%</span>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* SERVANT SPECIFIC SECTION */}
      {isServant && totalCount > 0 && (
        <Card className="p-6 bg-gradient-to-br from-white to-primary-50/30 border-primary-100">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <div>
              <span className="text-xs font-bold text-primary-600 uppercase tracking-wider block mb-1">
                 الافتقاد للأسبوع الحالي
              </span>
              <h3 className="text-lg font-black text-gray-900">
                أنجزت افتقاد {visitedCount} من أصل {totalCount} مخدوم
              </h3>
            </div>
            <Link to="/visits">
              <Button size="sm" variant="primary" className="font-bold">
                متابعة مخدومي الأسبوع
                <ChevronLeft className="w-4 h-4 mr-1" />
              </Button>
            </Link>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-gray-100 h-3.5 rounded-full overflow-hidden p-0.5">
            <div
              className="bg-primary-600 h-full rounded-full transition-all duration-500 ease-out"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <div className="flex justify-between items-center text-xs text-gray-500 mt-2 font-medium">
            <span>نسبة الإنجاز: {progressPercent}%</span>
            <span>المتبقي: {totalCount - visitedCount} مخدوم</span>
          </div>
        </Card>
      )}

      {/* WEEKLY ACTIVITY & KPI CARDS GRID */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-black text-gray-900 uppercase tracking-wider flex items-center gap-2">
            <CalendarCheck className="w-4 h-4 text-primary-600" />
            <span>مؤشرات الأسبوع الحالي</span>
          </h3>
          <span className="text-xs text-gray-400 font-medium">ضمن نطاقك المصرح</span>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          <Card className="flex flex-col justify-between">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-gray-500">إجمالي المخدومين</span>
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-2xl font-black text-gray-900">
                {isStatsLoading ? '...' : stats?.totalStudents ?? 0}
              </div>
              <p className="text-[11px] text-gray-400 mt-1">ضمن نطاقك المصرح</p>
            </div>
          </Card>

          <Card className="flex flex-col justify-between">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-gray-500">نسبة الافتقاد الأسبوعي</span>
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <CalendarCheck className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-2xl font-black text-gray-900">
                {isStatsLoading ? '...' : `${stats?.visitPercentage ?? 0}%`}
              </div>
              <p className="text-[11px] text-gray-400 mt-1">
                تم افتقاد {stats?.visitedStudents ?? 0} مخدوم
              </p>
            </div>
          </Card>

          <Card className="flex flex-col justify-between">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-gray-500">المؤشر الروحي العام</span>
              <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-2xl font-black text-gray-900">
                {isStatsLoading ? '...' : `${stats?.overallFollowupIndex ?? 0}%`}
              </div>
              <p className="text-[11px] text-gray-400 mt-1">متوسط المؤشرات المحسوبة</p>
            </div>
          </Card>

          <Card className="flex flex-col justify-between">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-gray-500">تنبيهات الغياب المتتالي</span>
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <AlertTriangle className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-2xl font-black text-gray-900">
                {isStatsLoading ? '...' : stats?.totalAbsenceAlerts ?? 0}
              </div>
              <p className="text-[11px] text-gray-400 mt-1">غياب ≥ أسبوعين متتاليين</p>
            </div>
          </Card>
        </div>
      </div>

      {/* Absence Alerts Section */}
      {alerts.length > 0 && (
        <Card className="p-6 border-amber-200 bg-amber-50/30 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-base">
                  تنبيهات الغياب المتتالي (بحاجة لافتقاد عاجل)
                </h3>
                <p className="text-xs text-gray-500">
                  مخدومين لم يتم افتقادهم لأسبوعين متتاليين أو أكثر
                </p>
              </div>
            </div>
            <Badge variant="warning">{alerts.length} تنبيه</Badge>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 pt-2">
            {alerts.map((a) => (
              <div
                key={a.studentId}
                className="bg-white p-4 rounded-xl border border-amber-100 shadow-sm flex flex-col justify-between space-y-2"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-gray-900 text-sm">{a.studentName}</h4>
                    <span className="text-xs text-red-600 font-bold bg-red-50 px-2 py-0.5 rounded-md">
                      {a.consecutiveWeeksAbsent} أسابيع غياب
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1 flex items-center gap-1.5">
                    <School className="w-3.5 h-3.5 text-gray-400" />
                    {a.ministryName} • {a.className}
                  </p>
                  {a.servantName && (
                    <p className="text-xs text-primary-700 mt-0.5">
                      الخادم المسؤول: {a.servantName}
                    </p>
                  )}
                </div>

                <div className="pt-2 border-t border-gray-50 flex items-center justify-between text-xs font-mono text-gray-500" dir="ltr">
                  <span className="flex items-center gap-1">
                    <Phone className="w-3.5 h-3.5" />
                    {a.phone}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Quick Action Navigation Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
        <Link to="/visits" className="group">
          <Card className="hover:border-primary-300 hover:shadow-md transition p-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-2xl bg-primary-50 text-primary-600 flex items-center justify-center group-hover:bg-primary-600 group-hover:text-white transition">
                  <CalendarCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-sm">متابعة المخدومين</h3>
                  <p className="text-xs text-gray-500 mt-0.5">تسجيل الزيارات والنوتة</p>
                </div>
              </div>
              <ChevronLeft className="w-5 h-5 text-gray-400 group-hover:text-primary-600 transition" />
            </div>
          </Card>
        </Link>

        <Link to="/attendance" className="group">
          <Card className="hover:border-primary-300 hover:shadow-md transition p-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition">
                  <UserCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-sm">تسجيل الحضور</h3>
                  <p className="text-xs text-gray-500 mt-0.5">قداس • اجتماع • تسبحة</p>
                </div>
              </div>
              <ChevronLeft className="w-5 h-5 text-gray-400 group-hover:text-emerald-600 transition" />
            </div>
          </Card>
        </Link>

        <Link to="/students" className="group">
          <Card className="hover:border-primary-300 hover:shadow-md transition p-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-sm">قائمة المخدومين</h3>
                  <p className="text-xs text-gray-500 mt-0.5">تسكين وتعديل وأرشفة المخدومين</p>
                </div>
              </div>
              <ChevronLeft className="w-5 h-5 text-gray-400 group-hover:text-purple-600 transition" />
            </div>
          </Card>
        </Link>
      </div>
    </div>
  );
};
