import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { statisticsApi } from '../api/statistics.api';
import { ministriesApi } from '../api/ministries.api';
import { classesApi } from '../api/classes.api';
import { weeksApi } from '../api/weeks.api';
import { studentsApi } from '../api/students.api';
import { useAuth } from '../auth/useAuth';
import { usePermissions } from '../auth/usePermissions';
import { MinistryResponse, GradeClassResponse } from '../types/ministry.types';
import { StudentResponse } from '../types/student.types';
import { WeekResponse } from '../types/visit.types';
import {
  DashboardStatisticsResponse,
  MinistryStatisticsResponse,
  ClassStatisticsResponse,
  ServantPerformanceResponse,
  ServantStatisticsResponse,
  WeeklyTrendDataPoint,
} from '../types/statistics.types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Select } from '../components/ui/Select';
import { Spinner } from '../components/ui/Spinner';
import { Badge } from '../components/ui/Badge';
import { LineChart, LineSeries } from '../components/charts/LineChart';
import { BarChart, BarItem } from '../components/charts/BarChart';
import { DoughnutChart, DoughnutSegment } from '../components/charts/DoughnutChart';
import { StudentStatisticsDrawer } from '../components/statistics/StudentStatisticsDrawer';
import { ServantStatisticsDrawer } from '../components/statistics/ServantStatisticsDrawer';
import { CsvExportDrawer } from '../components/statistics/CsvExportDrawer';
import {
  Users,
  CalendarCheck,
  TrendingUp,
  AlertTriangle,
  UserCheck,
  Printer,
  FileDown,
  Building2,
  School,
  Sparkles,
  Search,
  BookOpen,
  CheckCircle2,
  XCircle,
  Clock,
} from 'lucide-react';

export const StatisticsPage: React.FC = () => {
  const { user } = useAuth();
  const { isAdmin, isServiceSecretary, isClassSecretary, isServant, managedMinistryId, managedClassId } = usePermissions();
  const isPureServant = isServant && !isAdmin && !isServiceSecretary && !isClassSecretary;

  // State
  const [selectedWeekId, setSelectedWeekId] = useState<number | undefined>(undefined);
  const [selectedMinistryId, setSelectedMinistryId] = useState<number | undefined>(managedMinistryId || undefined);
  const [selectedClassId, setSelectedClassId] = useState<number | undefined>(managedClassId || undefined);
  const [activeTab, setActiveTab] = useState<'classes' | 'servants' | 'students'>(
    isPureServant ? 'students' : isClassSecretary ? 'servants' : 'classes'
  );
  const [studentSearch, setStudentSearch] = useState<string>('');
  const [servantSearch, setServantSearch] = useState<string>('');
  const [periodMode, setPeriodMode] = useState<'weekly' | 'monthly' | 'annual'>('weekly');

  // If user is Class Secretary without explicit managedMinistryId, lookup parent ministry
  const { data: managedClass } = useQuery<GradeClassResponse>({
    queryKey: ['classes', 'managed', managedClassId],
    queryFn: () => classesApi.findById(managedClassId!),
    enabled: !!managedClassId && !managedMinistryId,
  });

  // Sync role scopes
  useEffect(() => {
    if (managedMinistryId && !selectedMinistryId && !isAdmin) {
      setSelectedMinistryId(managedMinistryId);
    }
    if (managedClassId && !selectedClassId && !isAdmin) {
      setSelectedClassId(managedClassId);
    }
    if (managedClass?.ministryId && !selectedMinistryId && !isAdmin) {
      setSelectedMinistryId(managedClass.ministryId);
    }
  }, [managedMinistryId, managedClassId, managedClass, selectedMinistryId, selectedClassId, isAdmin]);

  useEffect(() => {
    if (isPureServant) {
      setActiveTab('students');
    }
  }, [isPureServant]);

  // Drawers
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);
  const [selectedServantId, setSelectedServantId] = useState<number | null>(null);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState<boolean>(false);

  // Load weeks
  const { data: weeks = [] } = useQuery<WeekResponse[]>({
    queryKey: ['weeks'],
    queryFn: () => weeksApi.getAll(false),
  });

  // Load ministries (only needed for admin or if user has scope)
  const { data: ministries = [] } = useQuery<MinistryResponse[]>({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isAdmin || isServiceSecretary,
  });

  // Current active week
  const activeWeek: WeekResponse | undefined = weeks.find((w: WeekResponse) => w.id === selectedWeekId) || weeks[0];
  const effectiveWeekId = selectedWeekId || activeWeek?.id;

  // Personal Servant Stats (for logged-in servant)
  const { data: myServantStats } = useQuery<ServantStatisticsResponse | null>({
    queryKey: ['statistics', 'servant', user?.personId, effectiveWeekId],
    queryFn: () => (user?.personId ? statisticsApi.getServantStatistics(user.personId, effectiveWeekId) : null),
    enabled: isServant && !!user?.personId && !!effectiveWeekId,
  });

  // Statistics Dashboard Query
  const { data: dashboardStats, isLoading: isDashboardLoading } = useQuery<DashboardStatisticsResponse>({
    queryKey: ['statistics', 'dashboard', effectiveWeekId],
    queryFn: () => statisticsApi.getDashboard(effectiveWeekId),
    enabled: !!effectiveWeekId,
  });

  // Ministry Statistics Query (if ministry selected and authorized)
  const { data: ministryStats, isLoading: isMinistryLoading } = useQuery<MinistryStatisticsResponse | null>({
    queryKey: ['statistics', 'ministry', selectedMinistryId, effectiveWeekId],
    queryFn: () => (selectedMinistryId ? statisticsApi.getMinistryStatistics(selectedMinistryId, effectiveWeekId) : null),
    enabled: !!selectedMinistryId && !!effectiveWeekId && (isAdmin || isServiceSecretary),
  });

  // Class Statistics Query (if class selected and authorized)
  const { data: classStats, isLoading: isClassLoading } = useQuery<ClassStatisticsResponse | null>({
    queryKey: ['statistics', 'class', selectedClassId, effectiveWeekId],
    queryFn: () => (selectedClassId ? statisticsApi.getClassStatistics(selectedClassId, effectiveWeekId) : null),
    enabled: !!selectedClassId && !!effectiveWeekId && (isAdmin || isServiceSecretary || isClassSecretary),
  });

  // Trends Query
  const trendWeeksCount = periodMode === 'monthly' ? 4 : periodMode === 'annual' ? 24 : 8;
  const { data: trends = [], isLoading: isTrendsLoading } = useQuery<WeeklyTrendDataPoint[]>({
    queryKey: ['statistics', 'trends', trendWeeksCount],
    queryFn: () => statisticsApi.getTrends(trendWeeksCount),
  });

  // Students roster query for student statistics tab
  const { data: students = [], isLoading: isStudentsLoading } = useQuery<StudentResponse[]>({
    queryKey: ['students', selectedMinistryId, selectedClassId, isPureServant ? user?.personId : undefined, studentSearch],
    queryFn: () =>
      studentsApi.findAll({
        ministryId: selectedMinistryId,
        classId: selectedClassId,
        servantId: isPureServant && user?.personId ? user.personId : undefined,
        search: studentSearch || undefined,
      }),
    enabled: activeTab === 'students',
  });

  // Servants Performance Query (for servants tab across class, ministry, or all)
  const { data: servantsPerformance, isLoading: isServantsLoading } = useQuery<ServantPerformanceResponse>({
    queryKey: ['statistics', 'servants', selectedMinistryId, selectedClassId, effectiveWeekId],
    queryFn: () =>
      statisticsApi.getServantsPerformance({
        ministryId: selectedMinistryId,
        classId: selectedClassId,
        weekId: effectiveWeekId,
      }),
    enabled: !isPureServant && activeTab === 'servants' && !!effectiveWeekId,
  });

  const filteredServants = (servantsPerformance?.servants || []).filter((s) => {
    if (!servantSearch.trim()) return true;
    return s.servantName.toLowerCase().includes(servantSearch.trim().toLowerCase());
  });

  // Active ministry object
  const activeMinistry: MinistryResponse | undefined = ministries.find((m: MinistryResponse) => m.id === selectedMinistryId);

  // Available classes for selected ministry
  const { data: availableClasses = [] } = useQuery<GradeClassResponse[]>({
    queryKey: ['classes', selectedMinistryId],
    queryFn: () => classesApi.findAll(selectedMinistryId),
    enabled: !!selectedMinistryId,
  });

  // Determine effective stats for KPI cards
  const stats = selectedClassId
    ? classStats?.dashboard
    : selectedMinistryId
    ? ministryStats?.dashboard
    : dashboardStats;

  const isLoading = isDashboardLoading || isMinistryLoading || isClassLoading;

  // Prepare Line Chart Data
  const trendLabels = trends.map((t: WeeklyTrendDataPoint) => {
    const date = new Date(t.startDate);
    return `${date.getDate()}/${date.getMonth() + 1}`;
  });

  const lineSeries: LineSeries[] = [
    {
      id: 'visits',
      name: 'نسبة الافتقاد',
      color: '#059669', // Emerald
      data: trends.map((t: WeeklyTrendDataPoint) => t.visitPercentage),
    },
    {
      id: 'mass',
      name: 'حضور القداس',
      color: '#2563EB', // Blue
      data: trends.map((t: WeeklyTrendDataPoint) => t.massPercentage),
    },
    {
      id: 'meeting',
      name: 'حضور الاجتماع',
      color: '#4F46E5', // Indigo
      data: trends.map((t: WeeklyTrendDataPoint) => t.meetingPercentage),
    },
    {
      id: 'overall',
      name: 'المؤشر العام',
      color: '#D97706', // Amber
      data: trends.map((t: WeeklyTrendDataPoint) => t.overallFollowupIndex),
    },
  ];

  // Prepare Bar Chart Items for Class Comparison (within selected ministry)
  const classBarItems: BarItem[] = (ministryStats?.classesStats || []).map((cs) => ({
    id: cs.classId,
    label: cs.className,
    value: cs.visitPercentage,
    sublabel: `${cs.visitedStudents} من ${cs.totalStudents}`,
    color: '#059669',
    onClick: () => {
      setSelectedClassId(cs.classId);
    },
  }));

  // Prepare Bar Chart Items for Servant Comparison (within selected class)
  const servantBarItems: BarItem[] = (classStats?.servantsStats || []).map((ss) => ({
    id: ss.servantId,
    label: ss.servantName,
    value: ss.visitPercentage,
    sublabel: `${ss.visitedCount} من ${ss.assignedStudentsCount}`,
    color: '#4A3E3D',
    onClick: () => {
      setSelectedServantId(ss.servantId);
    },
  }));

  // Prepare Doughnut Chart Data
  const totalStudents = stats?.totalStudents || 0;
  const visitedStudents = stats?.visitedStudents || 0;
  const unvisitedStudents = Math.max(0, totalStudents - visitedStudents);

  const followupSegments: DoughnutSegment[] = [
    {
      id: 'visited',
      label: 'تم افتقادهم',
      value: visitedStudents,
      color: '#059669',
    },
    {
      id: 'unvisited',
      label: 'لم يتم افتقادهم',
      value: unvisitedStudents,
      color: '#F59E0B',
    },
  ];

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 print:space-y-4">
      {/* Printable Header (Visible only in print) */}
      <div className="hidden print:block border-b-2 border-gray-800 pb-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 border-2 border-gray-800 flex items-center justify-center font-bold">
              ✝
            </div>
            <div>
              <h1 className="text-xl font-bold">كنيسة السيدة العذراء والأنبا بيشوي</h1>
              <p className="text-xs text-gray-600">نظام المتابعة وخدمة التربية الكنسية</p>
            </div>
          </div>
          <div className="text-left text-xs text-gray-600">
            <p className="font-bold">تقرير الإحصائيات الشامل</p>
            <p>تاريخ الطباعة: {new Date().toLocaleDateString('ar-EG')}</p>
            {activeWeek && <p>الأسبوع: {activeWeek.startDate} إلى {activeWeek.endDate}</p>}
          </div>
        </div>
      </div>

      {/* Screen Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 print:hidden">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight flex items-center gap-2.5">
            <span>الإحصائيات والتقارير</span>

          </h1>
          <p className="text-xs text-gray-500 mt-1">
            {isPureServant
              ? 'متابعة أداء خدمتك الأسبوعية والمؤشرات الروحية للمخدومين المسندين إليك'
              : 'لوحة المؤشرات الروحية ومقارنة أداء الخدمات والخُدّام وتصدير تقارير CSV'}
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            onClick={handlePrint}
            className="font-bold text-xs h-10"
          >
            <Printer className="w-4 h-4 ml-1.5" />
            طباعة التقرير
          </Button>

          <Button
            variant="primary"
            onClick={() => setIsExportDrawerOpen(true)}
            className="font-bold text-xs h-10"
          >
            <FileDown className="w-4 h-4 ml-1.5" />
            تصدير CSV
          </Button>
        </div>
      </div>

      {/* Scope and Filters Bar */}
      {isPureServant ? (
        <Card className="p-4 bg-white border-gray-200 print:hidden space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center font-bold text-sm">
                ✝
              </div>
              <div>
                <h4 className="text-xs font-bold text-gray-900">
                  نطاق الخدمة: مخدوميك المسندين ({user?.fullName})
                </h4>
                <p className="text-[11px] text-gray-500">
                  يتم احتساب النسب والمؤشرات المعروضة أدناه لمخدوميك تلقائياً
                </p>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
              <div className="w-full sm:w-64">
                <Select
                  label="الأسبوع المستهدف"
                  value={effectiveWeekId ? String(effectiveWeekId) : ''}
                  onChange={(e) => setSelectedWeekId(Number(e.target.value))}
                  options={weeks.map((w: WeekResponse) => ({
                    value: String(w.id),
                    label: `أسبوع ${w.startDate} إلى ${w.endDate}`,
                  }))}
                />
              </div>

              <div className="w-full sm:w-48">
                <label className="block text-xs font-bold text-gray-700 mb-1.5">نطاق المقارنة</label>
                <div className="flex bg-gray-100 p-1 rounded-xl">
                  <button
                    type="button"
                    onClick={() => setPeriodMode('weekly')}
                    className={`flex-1 py-1 text-xs font-bold rounded-lg transition ${
                      periodMode === 'weekly'
                        ? 'bg-white text-primary-700 shadow-2xs'
                        : 'text-gray-500 hover:text-gray-900'
                    }`}
                  >
                    أسبوعي
                  </button>
                  <button
                    type="button"
                    onClick={() => setPeriodMode('monthly')}
                    className={`flex-1 py-1 text-xs font-bold rounded-lg transition ${
                      periodMode === 'monthly'
                        ? 'bg-white text-primary-700 shadow-2xs'
                        : 'text-gray-500 hover:text-gray-900'
                    }`}
                  >
                    شهري
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Card>
      ) : (
        <Card className="p-4 bg-white border-gray-200 print:hidden space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-4 gap-3">
            {/* Week Selector */}
            <Select
              label="الأسبوع المستهدف"
              value={effectiveWeekId ? String(effectiveWeekId) : ''}
              onChange={(e) => setSelectedWeekId(Number(e.target.value))}
              options={weeks.map((w: WeekResponse) => ({
                value: String(w.id),
                label: `أسبوع ${w.startDate} إلى ${w.endDate}`,
              }))}
            />

            {/* Ministry Filter */}
            <Select
              label="الخدمة"
              disabled={isServiceSecretary || isClassSecretary}
              value={selectedMinistryId ? String(selectedMinistryId) : ''}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                setSelectedMinistryId(val);
                setSelectedClassId(undefined);
              }}
              options={[
                ...(isAdmin ? [{ value: '', label: 'كافة الخدمات (إجمالي الكنيسة)' }] : []),
                ...ministries.map((m: MinistryResponse) => ({
                  value: String(m.id),
                  label: m.name,
                })),
              ]}
            />

            {/* Class Filter */}
            <Select
              label="الفصل"
              disabled={isClassSecretary || !selectedMinistryId || availableClasses.length === 0}
              value={selectedClassId ? String(selectedClassId) : ''}
              onChange={(e) => setSelectedClassId(e.target.value ? Number(e.target.value) : undefined)}
              options={[
                ...(isAdmin || isServiceSecretary ? [{ value: '', label: 'كافة فصول الخدمة' }] : []),
                ...availableClasses.map((c: GradeClassResponse) => ({
                  value: String(c.id),
                  label: c.name,
                })),
              ]}
            />

            {/* Period Mode Selector */}
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5">نطاق المقارنة</label>
              <div className="flex bg-gray-100 p-1 rounded-xl">
                <button
                  type="button"
                  onClick={() => setPeriodMode('weekly')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${
                    periodMode === 'weekly'
                      ? 'bg-white text-primary-700 shadow-2xs'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  أسبوعي
                </button>
                <button
                  type="button"
                  onClick={() => setPeriodMode('monthly')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${
                    periodMode === 'monthly'
                      ? 'bg-white text-primary-700 shadow-2xs'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  شهري (4 أسابيع)
                </button>
                <button
                  type="button"
                  onClick={() => setPeriodMode('annual')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${
                    periodMode === 'annual'
                      ? 'bg-white text-primary-700 shadow-2xs'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  سنوي
                </button>
              </div>
            </div>
          </div>

          {/* Active filter badges */}
          {(selectedMinistryId || selectedClassId) && (
            <div className="flex items-center gap-2 pt-2 border-t border-gray-100 text-xs text-gray-500">
              <span>النطاق المحدد:</span>
              {activeMinistry && (
                <span className="inline-flex items-center gap-1 bg-primary-50 text-primary-700 px-2 py-0.5 rounded-md font-semibold">
                  <Building2 className="w-3 h-3" />
                  {activeMinistry.name}
                </span>
              )}
              {selectedClassId && (
                <span className="inline-flex items-center gap-1 bg-blue-50 text-blue-700 px-2 py-0.5 rounded-md font-semibold">
                  <School className="w-3 h-3" />
                  {availableClasses.find((c: GradeClassResponse) => c.id === selectedClassId)?.name}
                </span>
              )}
              {isAdmin && (
                <button
                  onClick={() => {
                    setSelectedMinistryId(undefined);
                    setSelectedClassId(undefined);
                  }}
                  className="text-red-500 hover:underline mr-auto text-xs font-medium"
                >
                  إعادة ضبط النطاق
                </button>
              )}
            </div>
          )}
        </Card>
      )}

      {/* Servant Highlight Card (when logged in as servant) */}
      {isPureServant && myServantStats && (
        <Card className="p-4 bg-gradient-to-r from-primary-50/50 via-white to-primary-50/30 border-primary-200 print:hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-primary-700 text-white flex items-center justify-center shadow-xs">
                <UserCheck className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-sm font-black text-gray-900">{myServantStats.servantName}</h3>
                  <Badge variant="primary" className="text-[10px]">خادم مسؤول</Badge>
                </div>
                <p className="text-xs text-gray-500 mt-0.5">
                  مسؤول عن {myServantStats.assignedStudentsCount} مخدوم • تم افتقاد {myServantStats.visitedCount} هذا الأسبوع ({myServantStats.visitPercentage}%)
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="flex items-center gap-3 text-xs border-r border-gray-200 pr-4">
                <div className="text-center">
                  <span className="text-[10px] text-gray-400 block">متوسط النوتة</span>
                  <span className="font-bold text-purple-700 font-mono text-sm">{myServantStats.averageNoteScore ?? '-'}</span>
                </div>
              </div>

              <Button
                size="sm"
                variant="outline"
                onClick={() => setSelectedServantId(user?.personId || null)}
                className="text-xs h-9"
              >
                عرض تفاصيل أدائي
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
        {/* Total Students */}
        <Card className="p-3.5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-gray-500">إجمالي المخدومين</span>
            <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Users className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-gray-900 font-mono">
              {isLoading ? '...' : stats?.totalStudents ?? 0}
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">مخدوم مسكن</p>
          </div>
        </Card>

        {/* Visit Rate */}
        <Card className="p-3.5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-gray-500">نسبة الافتقاد</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CalendarCheck className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-emerald-700 font-mono">
              {isLoading ? '...' : `${stats?.visitPercentage ?? 0}%`}
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {stats?.visitedStudents ?? 0} مخدوم
            </p>
          </div>
        </Card>

        {/* Mass Attendance */}
        <Card className="p-3.5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-gray-500">القداس الإلهي</span>
            <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <UserCheck className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-indigo-700 font-mono">
              {isLoading ? '...' : `${stats?.massAttendancePercentage ?? 0}%`}
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {stats?.massAttendanceCount ?? 0} حاضر
            </p>
          </div>
        </Card>

        {/* Meeting Attendance */}
        <Card className="p-3.5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-gray-500">اجتماع الخدمة</span>
            <div className="w-7 h-7 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <Users className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-sky-700 font-mono">
              {isLoading ? '...' : `${stats?.meetingAttendancePercentage ?? 0}%`}
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {stats?.meetingAttendanceCount ?? 0} حاضر
            </p>
          </div>
        </Card>

        {/* Tasbeha Attendance */}
        <Card className="p-3.5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-gray-500">التسبحة</span>
            <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Sparkles className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-purple-700 font-mono">
              {isLoading ? '...' : `${stats?.tasbehaAttendancePercentage ?? 0}%`}
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {stats?.tasbehaAttendanceCount ?? 0} حاضر
            </p>
          </div>
        </Card>

        {/* Overall Weighted Index */}
        <Card className="p-3.5 flex flex-col justify-between bg-primary-50/30 border-primary-100">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-primary-900">المؤشر العام</span>
            <div className="w-7 h-7 rounded-lg bg-primary-600 text-white flex items-center justify-center">
              <TrendingUp className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-primary-800 font-mono">
              {isLoading ? '...' : `${stats?.overallFollowupIndex ?? 0}%`}
            </div>
            <p className="text-[10px] text-primary-600 mt-0.5 font-medium">المؤشر الروحي</p>
          </div>
        </Card>

        {/* Absence Alerts */}
        <Card className="p-3.5 flex flex-col justify-between bg-amber-50/30 border-amber-200">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-amber-900">تنبيهات الغياب</span>
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center">
              <AlertTriangle className="w-3.5 h-3.5" />
            </div>
          </div>
          <div>
            <div className="text-xl font-black text-amber-800 font-mono">
              {isLoading ? '...' : stats?.totalAbsenceAlerts ?? 0}
            </div>
            <p className="text-[10px] text-amber-600 mt-0.5 font-medium">بحاجة لافتقاد</p>
          </div>
        </Card>
      </div>

      {/* Main Charts Grid: Line Chart & Doughnut Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Multi-Week Trend Line Chart */}
        <Card className="lg:col-span-2 p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <div>
              <h3 className="text-sm font-bold text-gray-900">
                منحنى المتابعة الأسبوعي (المؤشرات عبر الأسابيع)
              </h3>
              <p className="text-xs text-gray-400 mt-0.5">
                تطور نسبة الافتقاد وحضور القداس والاجتماع والمؤشر العام
              </p>
            </div>
            <Badge variant="neutral" className="font-mono text-xs">
              {trends.length} أسابيع
            </Badge>
          </div>

          {isTrendsLoading ? (
            <div className="h-48 flex items-center justify-center">
              <Spinner size="md" />
            </div>
          ) : (
            <LineChart labels={trendLabels} series={lineSeries} height={220} />
          )}
        </Card>

        {/* Circular Distribution Charts */}
        <Card className="p-5 flex flex-col justify-between space-y-4">
          <div className="border-b border-gray-100 pb-3">
            <h3 className="text-sm font-bold text-gray-900">توزيع الافتقاد الأسبوعي</h3>
            <p className="text-xs text-gray-400 mt-0.5">
              نسبة المخدومين الذين تم افتقادهم
            </p>
          </div>

          <div className="py-2">
            <DoughnutChart
              segments={followupSegments}
              size={160}
              strokeWidth={20}
              centerValue={`${stats?.visitPercentage ?? 0}%`}
              centerLabel="نسبة الافتقاد"
            />
          </div>

          <div className="pt-3 border-t border-gray-100">
            <h4 className="text-xs font-bold text-gray-700 mb-2">مزيج حضور الأنشطة</h4>
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              <div className="p-2 rounded-xl bg-blue-50/50">
                <span className="text-[10px] text-gray-500 block">القداس</span>
                <span className="font-bold text-blue-700 font-mono">
                  {stats?.massAttendanceCount ?? 0}
                </span>
              </div>
              <div className="p-2 rounded-xl bg-indigo-50/50">
                <span className="text-[10px] text-gray-500 block">الاجتماع</span>
                <span className="font-bold text-indigo-700 font-mono">
                  {stats?.meetingAttendanceCount ?? 0}
                </span>
              </div>
              <div className="p-2 rounded-xl bg-purple-50/50">
                <span className="text-[10px] text-gray-500 block">التسبحة</span>
                <span className="font-bold text-purple-700 font-mono">
                  {stats?.tasbehaAttendanceCount ?? 0}
                </span>
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Comparative Section: Bar Charts (Service & Servant comparisons - Hidden for pure servants) */}
      {!isPureServant && (
        <div className={`grid grid-cols-1 ${isAdmin || isServiceSecretary ? 'md:grid-cols-2' : ''} gap-6`}>
          {/* Class Comparison in Ministry (Only for Admin & Service Secretary) */}
          {(isAdmin || isServiceSecretary) && (
            <Card className="p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <div>
                  <h3 className="text-sm font-bold text-gray-900">مقارنة فصول الخدمة</h3>
                  <p className="text-xs text-gray-400 mt-0.5">
                    نسبة إنجاز الافتقاد لكل فصل (اضغط للانتقال للفصل)
                  </p>
                </div>
                {activeMinistry && (
                  <Badge variant="primary" className="text-xs">
                    {activeMinistry.name}
                  </Badge>
                )}
              </div>

              {!selectedMinistryId ? (
                <div className="p-8 text-center text-xs text-gray-400">
                  يرجى اختيار خدمة محددة من الأعلى لعرض مقارنة فصولها
                </div>
              ) : classBarItems.length === 0 ? (
                <div className="p-8 text-center text-xs text-gray-400">
                  لا توجد فصول مضافة في هذه الخدمة
                </div>
              ) : (
                <BarChart items={classBarItems} orientation="horizontal" />
              )}
            </Card>
          )}

          {/* Servant Comparison in Class */}
          <Card className="p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div>
                <h3 className="text-sm font-bold text-gray-900">مقارنة خدام الفصل</h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  نسبة إنجاز الافتقاد لكل خادم (اضغط لعرض تفاصيل الخادم)
                </p>
              </div>
              {selectedClassId && (
                <Badge variant="primary" className="text-xs">
                  {availableClasses.find((c) => c.id === selectedClassId)?.name || 'فصل محدد'}
                </Badge>
              )}
            </div>

            {!selectedClassId ? (
              <div className="p-8 text-center text-xs text-gray-400">
                يرجى اختيار فصل محدد من القائمة لعرض مقارنة خُدّامه
              </div>
            ) : servantBarItems.length === 0 ? (
              <div className="p-8 text-center text-xs text-gray-400">
                لا يوجد خُدّام مسكنين في هذا الفصل
              </div>
            ) : (
              <BarChart items={servantBarItems} orientation="horizontal" />
            )}
          </Card>
        </div>
      )}

      {/* Detailed Breakdown Tabs */}
      <Card className="p-5 space-y-4">
        {/* Tab Headers */}
        <div className="flex items-center justify-between border-b border-gray-100 pb-3">
          <div className="flex gap-2">
            {[
              ...(!isPureServant && (isAdmin || isServiceSecretary)
                ? [{ id: 'classes' as const, label: 'فصول الخدمة والمؤشرات' }]
                : []),
              ...(!isPureServant
                ? [{ id: 'servants' as const, label: 'أداء الخدام والدرجات الروحية' }]
                : []),
              {
                id: 'students' as const,
                label: isPureServant ? 'مخدوميني والملف الروحي' : 'سجل المخدومين والملف الروحي',
              },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => setActiveTab(t.id)}
                className={`py-2 px-3.5 text-xs font-bold rounded-xl transition min-h-[40px] ${
                  activeTab === t.id
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-500 hover:text-gray-900'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          {activeTab === 'students' && (
            <div className="relative w-48 sm:w-64">
              <Search className="w-3.5 h-3.5 absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={studentSearch}
                onChange={(e) => setStudentSearch(e.target.value)}
                placeholder="بحث باسم المخدوم..."
                className="w-full pl-3 pr-8 py-1.5 text-xs rounded-xl border border-gray-200 focus:outline-hidden focus:border-primary-500"
              />
            </div>
          )}

          {activeTab === 'servants' && (
            <div className="relative w-48 sm:w-64">
              <Search className="w-3.5 h-3.5 absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={servantSearch}
                onChange={(e) => setServantSearch(e.target.value)}
                placeholder="بحث باسم الخادم..."
                className="w-full pl-3 pr-8 py-1.5 text-xs rounded-xl border border-gray-200 focus:outline-hidden focus:border-primary-500"
              />
            </div>
          )}
        </div>

        {/* Tab 1: Classes Breakdown Table */}
        {activeTab === 'classes' && !isPureServant && (
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                <tr>
                  <th className="py-2.5 px-3">الفصل</th>
                  <th className="py-2.5 px-3">إجمالي المخدومين</th>
                  <th className="py-2.5 px-3">تم افتقادهم</th>
                  <th className="py-2.5 px-3">نسبة الافتقاد</th>
                  <th className="py-2.5 px-3">حضور الاجتماع</th>
                  <th className="py-2.5 px-3">المؤشر العام</th>
                  <th className="py-2.5 px-3 text-center">إجراء</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {(ministryStats?.classesStats || []).length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center py-8 text-gray-400">
                      {selectedMinistryId
                        ? 'لا توجد فصول في هذه الخدمة'
                        : 'اختر خدمة من الأعلى لعرض تفاصيل فصولها'}
                    </td>
                  </tr>
                ) : (
                  ministryStats!.classesStats.map((cs) => (
                    <tr key={cs.classId} className="hover:bg-gray-50/50 transition">
                      <td className="py-2.5 px-3 font-bold text-gray-900">{cs.className}</td>
                      <td className="py-2.5 px-3 font-mono">{cs.totalStudents}</td>
                      <td className="py-2.5 px-3 font-mono text-emerald-600 font-bold">
                        {cs.visitedStudents}
                      </td>
                      <td className="py-2.5 px-3 font-mono font-bold">{cs.visitPercentage}%</td>
                      <td className="py-2.5 px-3 font-mono">
                        {cs.meetingAttendanceCount} ({cs.meetingAttendancePercentage}%)
                      </td>
                      <td className="py-2.5 px-3 font-mono font-bold text-primary-700">
                        {cs.overallFollowupIndex}%
                      </td>
                      <td className="py-2.5 px-3 text-center">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setSelectedClassId(cs.classId)}
                          className="text-xs h-8 text-primary-700 hover:bg-primary-50"
                        >
                          تحديد الفصل
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 2: Servants Breakdown Table */}
        {activeTab === 'servants' && !isPureServant && (
          <div className="space-y-4">
            {/* Aggregate Servants KPI Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
              {/* 1. إجمالي الخدام */}
              <Card className="p-3 bg-gray-50/50 flex flex-col justify-between">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-semibold text-gray-500">إجمالي الخدام</span>
                  <div className="w-6 h-6 rounded-lg bg-gray-100 text-gray-600 flex items-center justify-center">
                    <Users className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <div className="text-lg font-black text-gray-900 font-mono">
                    {isServantsLoading ? '...' : servantsPerformance?.totalServants ?? 0}
                  </div>
                  <p className="text-[10px] text-gray-400 mt-0.5">
                    {servantsPerformance?.recordedFollowUpCount ?? 0} سجلوا المتابعة ({servantsPerformance?.followUpSubmissionRate ?? 0}%)
                  </p>
                </div>
              </Card>

              {/* 2. متوسط النوتة الروحية */}
              <Card className="p-3 bg-purple-50/30 border-purple-100 flex flex-col justify-between">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-semibold text-gray-600">متوسط النوتة الروحية</span>
                  <div className="w-6 h-6 rounded-lg bg-purple-100 text-purple-600 flex items-center justify-center">
                    <BookOpen className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <div className="text-lg font-black text-purple-800 font-mono">
                    {isServantsLoading
                      ? '...'
                      : servantsPerformance?.averageNotePercentage !== null && servantsPerformance?.averageNotePercentage !== undefined
                      ? `${servantsPerformance.averageNotePercentage}%`
                      : '-'}
                  </div>
                  <p className="text-[10px] text-purple-600 mt-0.5">درجات النوتة للخدام</p>
                </div>
              </Card>

              {/* 3. حضور اجتماع الخدمة */}
              <Card className="p-3 bg-sky-50/30 border-sky-100 flex flex-col justify-between">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-semibold text-gray-600">حضور الاجتماع</span>
                  <div className="w-6 h-6 rounded-lg bg-sky-100 text-sky-600 flex items-center justify-center">
                    <Users className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <div className="text-lg font-black text-sky-800 font-mono">
                    {isServantsLoading
                      ? '...'
                      : servantsPerformance?.meetingAttendanceRate !== null && servantsPerformance?.meetingAttendanceRate !== undefined
                      ? `${servantsPerformance.meetingAttendanceRate}%`
                      : '-'}
                  </div>
                  <p className="text-[10px] text-sky-600 mt-0.5">نسبة الحضور للاجتماع</p>
                </div>
              </Card>

              {/* 4. حضور القداس الإلهي */}
              <Card className="p-3 bg-indigo-50/30 border-indigo-100 flex flex-col justify-between">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-semibold text-gray-600">حضور القداس</span>
                  <div className="w-6 h-6 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center">
                    <UserCheck className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <div className="text-lg font-black text-indigo-800 font-mono">
                    {isServantsLoading
                      ? '...'
                      : servantsPerformance?.massAttendanceRate !== null && servantsPerformance?.massAttendanceRate !== undefined
                      ? `${servantsPerformance.massAttendanceRate}%`
                      : '-'}
                  </div>
                  <p className="text-[10px] text-indigo-600 mt-0.5">نسبة الحضور للقداس</p>
                </div>
              </Card>

              {/* 5. نسبة إنجاز الافتقاد */}
              <Card className="p-3 bg-emerald-50/30 border-emerald-100 flex flex-col justify-between col-span-2 sm:col-span-1">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-semibold text-gray-600">نسبة الافتقاد</span>
                  <div className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-600 flex items-center justify-center">
                    <CalendarCheck className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <div className="text-lg font-black text-emerald-800 font-mono">
                    {isServantsLoading ? '...' : `${servantsPerformance?.overallVisitPercentage ?? 0}%`}
                  </div>
                  <p className="text-[10px] text-emerald-600 mt-0.5">إنجاز افتقاد المخدومين</p>
                </div>
              </Card>
            </div>

            {/* Servants Performance Table */}
            <div className="overflow-x-auto">
              {isServantsLoading ? (
                <div className="py-12 flex justify-center">
                  <Spinner size="md" />
                </div>
              ) : (
                <table className="w-full text-right text-xs">
                  <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                    <tr>
                      <th className="py-2.5 px-3">الخادم</th>
                      <th className="py-2.5 px-3">النوتة الروحية</th>
                      <th className="py-2.5 px-3">اجتماع الخدمة</th>
                      <th className="py-2.5 px-3">القداس الإلهي</th>
                      <th className="py-2.5 px-3">افتقاد المخدومين</th>
                      <th className="py-2.5 px-3 text-center">عرض الأداء</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {filteredServants.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center py-8 text-gray-400">
                          {servantSearch
                            ? 'لا يوجد خادم يطابق البحث'
                            : 'لا يوجد خدام مسجلين في هذا النطاق'}
                        </td>
                      </tr>
                    ) : (
                      filteredServants.map((ss) => (
                        <tr key={ss.servantId} className="hover:bg-gray-50/50 transition">
                          <td className="py-2.5 px-3">
                            <div className="font-bold text-gray-900">{ss.servantName}</div>
                            <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5">
                              {(ss.ministryName || ss.className) && (
                                <span>
                                  {ss.ministryName ? ss.ministryName : ''}
                                  {ss.ministryName && ss.className ? ' • ' : ''}
                                  {ss.className ? ss.className : ''}
                                </span>
                              )}
                              {ss.phone && (
                                <span dir="ltr" className="font-mono">
                                  {ss.phone}
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="py-2.5 px-3 font-mono">
                            {ss.recordedSelfFollowUp && ss.noteScore !== null && ss.maxNoteScore !== null ? (
                              <div className="flex items-center gap-2">
                                <span
                                  className={`inline-flex items-center px-2 py-0.5 rounded-md font-bold text-xs ${
                                    (ss.notePercentage ?? 0) >= 80
                                      ? 'bg-purple-100 text-purple-800'
                                      : (ss.notePercentage ?? 0) >= 50
                                      ? 'bg-yellow-100 text-yellow-800'
                                      : 'bg-red-100 text-red-800'
                                  }`}
                                >
                                  {ss.noteScore}/{ss.maxNoteScore} ({ss.notePercentage}%)
                                </span>
                              </div>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[11px] text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                                <Clock className="w-3 h-3" />
                                لم تسجل
                              </span>
                            )}
                          </td>
                          <td className="py-2.5 px-3">
                            {ss.attendedServiceMeeting === true ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                حاضر
                              </span>
                            ) : ss.attendedServiceMeeting === false ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-md border border-rose-200">
                                <XCircle className="w-3.5 h-3.5 text-rose-600" />
                                غائب
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[11px] text-gray-400 bg-gray-50 px-2 py-0.5 rounded-md border border-gray-200">
                                <Clock className="w-3 h-3 text-gray-400" />
                                غير مسجل
                              </span>
                            )}
                          </td>
                          <td className="py-2.5 px-3">
                            {ss.attendedMass === true ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                حاضر
                              </span>
                            ) : ss.attendedMass === false ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-md border border-rose-200">
                                <XCircle className="w-3.5 h-3.5 text-rose-600" />
                                غائب
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[11px] text-gray-400 bg-gray-50 px-2 py-0.5 rounded-md border border-gray-200">
                                <Clock className="w-3 h-3 text-gray-400" />
                                غير مسجل
                              </span>
                            )}
                          </td>
                          <td className="py-2.5 px-3">
                            <div className="flex items-center gap-2">
                              <span className="font-mono font-bold text-gray-800">
                                {ss.visitedCount}/{ss.assignedStudentsCount}
                              </span>
                              <span className="font-mono text-emerald-700 font-bold">
                                ({ss.visitPercentage}%)
                              </span>
                            </div>
                            <div className="w-24 bg-gray-100 h-1.5 rounded-full overflow-hidden mt-1">
                              <div
                                className="bg-emerald-600 h-full rounded-full"
                                style={{ width: `${Math.min(100, ss.visitPercentage)}%` }}
                              />
                            </div>
                          </td>
                          <td className="py-2.5 px-3 text-center">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setSelectedServantId(ss.servantId)}
                              className="text-xs h-8"
                            >
                              تفاصيل الأداء
                            </Button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}

        {/* Tab 3: Students Roster with Spiritual Drawer Drill-down */}
        {activeTab === 'students' && (
          <div className="overflow-x-auto">
            {isStudentsLoading ? (
              <div className="py-12 flex justify-center">
                <Spinner size="md" />
              </div>
            ) : (
              <table className="w-full text-right text-xs">
                <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                  <tr>
                    <th className="py-2.5 px-3">اسم المخدوم</th>
                    <th className="py-2.5 px-3">الخدمة والفصل</th>
                    {!isPureServant && <th className="py-2.5 px-3">الخادم المسؤول</th>}
                    <th className="py-2.5 px-3">الهاتف</th>
                    <th className="py-2.5 px-3">أب الاعتراف</th>
                    <th className="py-2.5 px-3 text-center">الملف الروحي</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {students.length === 0 ? (
                    <tr>
                      <td colSpan={isPureServant ? 5 : 6} className="text-center py-8 text-gray-400">
                        لا يوجد مخدومين يطابقون خيارات البحث
                      </td>
                    </tr>
                  ) : (
                    students.map((st: StudentResponse) => (
                      <tr key={st.id} className="hover:bg-gray-50/50 transition">
                        <td className="py-2.5 px-3 font-bold text-gray-900">{st.fullName}</td>
                        <td className="py-2.5 px-3 text-gray-500">
                          {st.ministryName} • {st.className}
                        </td>
                        {!isPureServant && (
                          <td className="py-2.5 px-3 text-primary-700 font-medium">
                            {st.responsibleServantName || st.servantName || 'غير مسكن لخادم'}
                          </td>
                        )}
                        <td className="py-2.5 px-3 text-gray-600 font-mono" dir="ltr">
                          {st.phone || '-'}
                        </td>
                        <td className="py-2.5 px-3 text-gray-500">
                          {st.confessionFather || '-'}
                        </td>
                        <td className="py-2.5 px-3 text-center">
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => setSelectedStudentId(st.id)}
                            className="text-xs h-8"
                          >
                            عرض الملف الروحي
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            )}
          </div>
        )}
      </Card>

      {/* Slide-out Drawers */}
      <StudentStatisticsDrawer
        isOpen={selectedStudentId !== null}
        onClose={() => setSelectedStudentId(null)}
        studentId={selectedStudentId}
      />

      <ServantStatisticsDrawer
        isOpen={selectedServantId !== null}
        onClose={() => setSelectedServantId(null)}
        servantId={selectedServantId}
        weekId={effectiveWeekId}
      />

      <CsvExportDrawer
        isOpen={isExportDrawerOpen}
        onClose={() => setIsExportDrawerOpen(false)}
        defaultWeekId={effectiveWeekId}
      />
    </div>
  );
};
