import React, { useState, useMemo, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { attendanceApi } from '../api/attendance.api';
import { weeksApi } from '../api/weeks.api';
import { studentsApi } from '../api/students.api';
import { classesApi } from '../api/classes.api';
import { servantsApi } from '../api/servants.api';
import { ActivityType, AttendanceSessionResponse } from '../types/attendance.types';
import { WeekResponse } from '../types/visit.types';
import { StudentResponse } from '../types/student.types';
import { AttendanceToggleCard } from '../components/attendance/AttendanceToggleCard';
import { WeekTimelineBanner } from '../components/attendance/WeekTimelineBanner';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { Alert } from '../components/ui/Alert';
import { EmptyState } from '../components/ui/EmptyState';
import { usePermissions } from '../auth/usePermissions';
import { useDebounce } from '../hooks/useDebounce';
import { formatDate } from '../utils/date';
import {
  Church,
  Users,
  Music,
  HeartHandshake,
  Search,
  Plus,
  Calendar,
  CheckCircle2,
  XCircle,
  CheckCheck,
  Ban,
  Lock,
  ArrowRight,
} from 'lucide-react';

type PresenceFilter = 'ALL' | 'PRESENT' | 'ABSENT';

export const AttendancePage: React.FC = () => {
  const queryClient = useQueryClient();
  const { isAdmin, isServiceSecretary, isClassSecretary, managedMinistryId, managedClassId } = usePermissions();
  const isSecretaryOrAdmin = isAdmin || isServiceSecretary || isClassSecretary;

  const todayStr = new Date().toISOString().split('T')[0];

  const [activeActivity, setActiveActivity] = useState<ActivityType>('MASS');
  const [selectedWeek, setSelectedWeek] = useState<WeekResponse | null>(null);

  // Filters
  const [nameSearch, setNameSearch] = useState('');
  const debouncedNameSearch = useDebounce(nameSearch, 250);
  const [selectedClassId, setSelectedClassId] = useState<number | ''>(managedClassId || '');
  const [selectedServantId, setSelectedServantId] = useState<number | ''>('');
  const [presenceFilter, setPresenceFilter] = useState<PresenceFilter>('ALL');

  // Session state
  const [sessionDate, setSessionDate] = useState<string>(todayStr);
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [batchActionLoading, setBatchActionLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 1. Fetch current week
  const { data: currentWeek } = useQuery<WeekResponse>({
    queryKey: ['weeks', 'current'],
    queryFn: weeksApi.getCurrentWeek,
  });

  // 2. Fetch all weeks
  const { data: allWeeks = [] } = useQuery<WeekResponse[]>({
    queryKey: ['weeks', 'all'],
    queryFn: () => weeksApi.getAll(false),
  });

  // Initialize selectedWeek to currentWeek if not set
  useEffect(() => {
    if (!selectedWeek && currentWeek) {
      setSelectedWeek(currentWeek);
      setSessionDate(currentWeek.startDate);
    }
  }, [currentWeek, selectedWeek]);

  // When selectedWeek changes, update default sessionDate to week startDate
  useEffect(() => {
    if (selectedWeek) {
      setSessionDate(selectedWeek.startDate);
      setSelectedSessionId(null);
    }
  }, [selectedWeek]);

  // 3. Fetch attendance sessions for selected week
  const { data: weekSessions = [], isLoading: isSessionsLoading } = useQuery<AttendanceSessionResponse[]>({
    queryKey: ['attendance', 'sessions', selectedWeek?.id],
    queryFn: () => attendanceApi.getSessionsByWeek(selectedWeek!.id),
    enabled: !!selectedWeek?.id,
  });

  // Filter sessions matching active activity
  const matchingSessions = useMemo(() => {
    return weekSessions.filter((s) => s.activityType === activeActivity);
  }, [weekSessions, activeActivity]);

  // Active session
  const currentSession = useMemo(() => {
    if (selectedSessionId) {
      return matchingSessions.find((s) => s.id === selectedSessionId) || matchingSessions[0] || null;
    }
    return matchingSessions[0] || null;
  }, [matchingSessions, selectedSessionId]);

  // 4. Fetch session details (with attendance records)
  const { data: sessionDetails, refetch: refetchDetails } = useQuery({
    queryKey: ['attendance', 'session-details', currentSession?.id],
    queryFn: () => attendanceApi.getSessionDetails(currentSession!.id),
    enabled: !!currentSession?.id,
  });

  // 5. Fetch all active students
  const { data: allStudents = [], isLoading: isStudentsLoading } = useQuery<StudentResponse[]>({
    queryKey: ['students', 'attendance-roster'],
    queryFn: () => studentsApi.findAll(),
  });

  // 6. Fetch classes for filter
  const { data: allClasses = [] } = useQuery({
    queryKey: ['classes', 'all'],
    queryFn: () => classesApi.findAll(managedMinistryId || undefined),
  });

  // 7. Fetch servants for filter
  const { data: servants = [] } = useQuery({
    queryKey: ['servants', selectedClassId],
    queryFn: () => servantsApi.findAll({ classId: selectedClassId ? Number(selectedClassId) : undefined }),
    enabled: !!selectedClassId,
  });

  // Create session mutation
  const createSessionMutation = useMutation({
    mutationFn: async () => {
      setErrorMessage(null);
      if (!selectedWeek) return;
      return attendanceApi.createSession({
        weekId: selectedWeek.id,
        activityType: activeActivity,
        sessionDate,
      });
    },
    onSuccess: (newSession) => {
      if (newSession) {
        queryClient.invalidateQueries({ queryKey: ['attendance', 'sessions', selectedWeek?.id] });
        setSelectedSessionId(newSession.id);
      }
    },
    onError: (err: Error) => {
      setErrorMessage(err.message || 'فشل إنشاء جلسة الحضور');
    },
  });

  // Toggle record mutation with optimistic updates
  const toggleMutation = useMutation({
    mutationFn: async ({ studentId, present }: { studentId: number; present: boolean }) => {
      setErrorMessage(null);
      if (!currentSession) throw new Error('لا توجد جلسة محددة');
      return attendanceApi.toggleAttendance({
        sessionId: currentSession.id,
        studentId,
        present,
      });
    },
    onSuccess: () => {
      refetchDetails();
      queryClient.invalidateQueries({ queryKey: ['attendance', 'sessions'] });
    },
    onError: (err: Error) => {
      setErrorMessage(err.message || 'فشل تحديث حالة الحضور');
    },
  });

  // Check if editing is locked
  const isLockedForUser = selectedWeek ? selectedWeek.locked && !isAdmin : false;

  // Roster composition
  const roster = useMemo(() => {
    const recordsMap = new Map<number, { present: boolean; recordedByName?: string; recordedAt?: string }>();
    sessionDetails?.records.forEach((r) => {
      recordsMap.set(r.studentId, {
        present: r.present,
        recordedByName: r.recordedByName,
        recordedAt: r.recordedAt,
      });
    });

    // Sort alphabetically in Arabic
    const sorted = [...allStudents].sort((a, b) => a.fullName.localeCompare(b.fullName, 'ar'));

    return sorted
      .filter((s) => {
        // Name / phone search
        if (debouncedNameSearch.trim()) {
          const query = debouncedNameSearch.toLowerCase().trim();
          const matchesName = s.fullName.toLowerCase().includes(query);
          const matchesPhone = s.phone.includes(query);
          if (!matchesName && !matchesPhone) return false;
        }

        // Class filter
        if (selectedClassId && s.classId !== Number(selectedClassId)) {
          return false;
        }

        // Servant filter
        if (selectedServantId && (s.responsibleServantId ?? s.servantId) !== Number(selectedServantId)) {
          return false;
        }

        // Presence filter
        const record = recordsMap.get(s.id);
        const isPresent = record?.present ?? false;
        if (presenceFilter === 'PRESENT' && !isPresent) return false;
        if (presenceFilter === 'ABSENT' && isPresent) return false;

        return true;
      })
      .map((s) => {
        const record = recordsMap.get(s.id);
        return {
          student: s,
          isPresent: record?.present ?? false,
          recordedByInfo: record?.recordedByName
            ? `سجله: ${record.recordedByName} (${formatDate(record.recordedAt)})`
            : undefined,
        };
      });
  }, [allStudents, sessionDetails, debouncedNameSearch, selectedClassId, selectedServantId, presenceFilter]);

  // Attendance stats for current filtered roster
  const totalRosterCount = roster.length;
  const presentCount = roster.filter((r) => r.isPresent).length;
  const absentCount = totalRosterCount - presentCount;
  const presentPercent = totalRosterCount > 0 ? Math.round((presentCount / totalRosterCount) * 100) : 0;

  const handleToggle = (studentId: number, newPresent: boolean) => {
    if (isLockedForUser) return;
    toggleMutation.mutate({ studentId, present: newPresent });
  };

  // Bulk actions
  const handleBulkMark = async (present: boolean) => {
    if (!currentSession || isLockedForUser || roster.length === 0) return;
    try {
      setBatchActionLoading(true);
      setErrorMessage(null);
      const studentIds = roster.map((r) => r.student.id);

      await attendanceApi.batchToggleAttendance({
        sessionId: currentSession.id,
        studentIds,
        present,
      });

      await refetchDetails();
      queryClient.invalidateQueries({ queryKey: ['attendance', 'sessions'] });
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'فشل تسجيل الحضور الجماعي');
    } finally {
      setBatchActionLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">تسجيل حضور الأنشطة والخدمات</h1>
          <p className="text-xs text-gray-500 mt-1">
            تسجيل الحضور الأسبوعي للقداسات واجتماعات الخدمة والتسبحة ومتابعة التفاعل
          </p>
        </div>

        {/* Link to Confessions page */}
        <Link
          to="/confessions"
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-bold bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 transition shrink-0"
        >
          <HeartHandshake className="w-4 h-4 text-purple-600" />
          <span>سجل الاعترافات </span>
          <ArrowRight className="w-3.5 h-3.5 rotate-180" />
        </Link>
      </div>

      {/* Week Timeline & Navigation Banner */}
      <WeekTimelineBanner
        currentWeek={currentWeek || null}
        selectedWeek={selectedWeek}
        allWeeks={allWeeks}
        onSelectWeek={(w) => setSelectedWeek(w)}
        isAdmin={isAdmin}
      />

      {/* Error Banner */}
      {errorMessage && (
        <Alert variant="error">
          <div className="flex items-center justify-between">
            <span>{errorMessage}</span>
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="text-xs font-bold underline"
            >
              إغلاق
            </button>
          </div>
        </Alert>
      )}

      {/* Locked Week Warning Banner */}
      {isLockedForUser && (
        <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 flex items-center gap-3 text-amber-900 text-xs">
          <Lock className="w-5 h-5 text-amber-600 shrink-0" />
          <div>
            <span className="font-bold block">هذا الأسبوع مغلق تاريخياً (للقراءة فقط)</span>
            <span>
              نظراً لمرور أكثر من 30 يوماً على نهاية هذا الأسبوع، تم إيقاف تعديل الحضور للحفاظ على السجلات.
              صلاحية التعديل الاستثنائي متاحة للأمين العام فقط.
            </span>
          </div>
        </div>
      )}

      {/* Activity Tabs */}
      <div className="flex items-center gap-2 border-b border-gray-200 overflow-x-auto pb-2">
        <button
          type="button"
          onClick={() => {
            setActiveActivity('MASS');
            setSelectedSessionId(null);
          }}
          className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition flex items-center gap-2 whitespace-nowrap min-h-[44px] ${
            activeActivity === 'MASS'
              ? 'bg-primary-600 text-white shadow-md shadow-primary-500/10'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-100'
          }`}
        >
          <Church className="w-4 h-4" />
          <span>القداس الإلهي</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveActivity('MEETING');
            setSelectedSessionId(null);
          }}
          className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition flex items-center gap-2 whitespace-nowrap min-h-[44px] ${
            activeActivity === 'MEETING'
              ? 'bg-primary-600 text-white shadow-md shadow-primary-500/10'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-100'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>اجتماع الخدمة</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveActivity('TASBEHA');
            setSelectedSessionId(null);
          }}
          className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition flex items-center gap-2 whitespace-nowrap min-h-[44px] ${
            activeActivity === 'TASBEHA'
              ? 'bg-primary-600 text-white shadow-md shadow-primary-500/10'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-100'
          }`}
        >
          <Music className="w-4 h-4" />
          <span>التسبحة</span>
        </button>
      </div>

      {/* Session Selector / Creator Bar */}
      <Card className="p-4 bg-white flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Sessions list chips */}
        <div className="flex items-center gap-2.5 overflow-x-auto pb-1 md:pb-0">
          <span className="text-xs font-bold text-gray-400 shrink-0">جلسات الأسبوع:</span>
          {isSessionsLoading ? (
            <div className="flex items-center gap-2 text-xs text-gray-400">
              <Spinner size="sm" />
              <span>جاري التحميل...</span>
            </div>
          ) : matchingSessions.length > 0 ? (
            matchingSessions.map((session) => (
              <button
                key={session.id}
                type="button"
                onClick={() => setSelectedSessionId(session.id)}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 whitespace-nowrap min-h-[40px] border ${
                  currentSession?.id === session.id
                    ? 'bg-primary-50 border-primary-500 text-primary-800 ring-1 ring-primary-500'
                    : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
                }`}
              >
                <Calendar className="w-3.5 h-3.5 text-primary-600" />
                <span>جلسة {formatDate(session.sessionDate)}</span>
                <Badge variant="success" className="text-[10px] py-0 px-1.5 font-mono">
                  {session.presentCount} حاضر
                </Badge>
              </button>
            ))
          ) : (
            <span className="text-xs text-amber-700 bg-amber-50 px-2.5 py-1 rounded-lg">
              لا توجد جلسة مسجلة بعد لهذا النشاط في هذا الأسبوع
            </span>
          )}
        </div>

        {/* Create new session inline (if not locked) */}
        {!isLockedForUser && (
          <div className="flex items-center gap-2 shrink-0 pt-2 md:pt-0 border-t md:border-t-0 border-gray-100">
            <Input
              type="date"
              value={sessionDate}
              onChange={(e) => setSessionDate(e.target.value)}
              className="w-36 py-1.5 text-xs font-mono"
            />
            <Button
              variant="primary"
              size="sm"
              onClick={() => createSessionMutation.mutate()}
              isLoading={createSessionMutation.isPending}
              className="font-bold whitespace-nowrap text-xs shadow-sm"
            >
              <Plus className="w-3.5 h-3.5 ml-1" />
              إنشاء جلسة
            </Button>
          </div>
        )}
      </Card>

      {/* Main Workspace */}
      {!currentSession ? (
        <Card className="text-center py-16">
          <div className="w-14 h-14 rounded-2xl bg-primary-50 text-primary-600 flex items-center justify-center mx-auto mb-3">
            <Church className="w-7 h-7" />
          </div>
          <h3 className="text-base font-bold text-gray-900 mb-1">
            لا توجد جلسة حضور مختارة لهذا الأسبوع
          </h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto mb-4">
            قم باختيار تاريخ الجلسة واضغط «إنشاء جلسة» للبدء بتسجيل الحضور الفوري لمخدومي الكنيسة.
          </p>
          {!isLockedForUser && (
            <Button
              variant="primary"
              onClick={() => createSessionMutation.mutate()}
              isLoading={createSessionMutation.isPending}
              className="font-bold text-xs shadow-sm"
            >
              <Plus className="w-4 h-4 ml-1.5" />
              إنشاء جلسة بتاريخ {formatDate(sessionDate)}
            </Button>
          )}
        </Card>
      ) : (
        <div className="space-y-4">
          {/* Live KPI & Progress Bar */}
          <div className="bg-gradient-to-l from-emerald-50 via-teal-50/30 to-white rounded-2xl border border-emerald-100 p-4 shadow-sm">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <span className="text-[11px] font-bold text-emerald-800 block">
                  إحصائية حضور الجلسة: {formatDate(currentSession.sessionDate)}
                </span>
                <div className="flex items-center gap-3 mt-1">
                  <span className="text-2xl font-black text-gray-900 font-mono">
                    {presentCount} / {totalRosterCount}
                  </span>
                  <span className="text-xs font-bold text-emerald-700 bg-emerald-100/80 px-2 py-0.5 rounded-lg font-mono">
                    {presentPercent}% نسبة الحضور
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-3 text-xs">
                <div className="flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-xl border border-emerald-200">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span className="font-bold text-gray-900">{presentCount}</span>
                  <span className="text-gray-400">حاضر</span>
                </div>

                <div className="flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-xl border border-gray-200">
                  <XCircle className="w-4 h-4 text-gray-400" />
                  <span className="font-bold text-gray-900">{absentCount}</span>
                  <span className="text-gray-400">غائب</span>
                </div>
              </div>
            </div>

            {/* Visual Progress Bar */}
            <div className="mt-3 w-full bg-gray-200 rounded-full h-2 overflow-hidden">
              <div
                className="bg-emerald-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${presentPercent}%` }}
              />
            </div>
          </div>

          {/* Search, Class, and Servant Filters */}
          <Card className="p-4 bg-white space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center gap-3">
              {/* Student Search */}
              <div className="relative flex-1">
                <Input
                  placeholder="بحث باسم المخدوم أو هاتفه..."
                  value={nameSearch}
                  onChange={(e) => setNameSearch(e.target.value)}
                  className="pr-10"
                />
                <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-gray-400">
                  <Search className="w-4 h-4" />
                </div>
              </div>

              {/* Class Filter */}
              <div className="w-full sm:w-52">
                <Select
                  value={selectedClassId}
                  onChange={(e) => {
                    setSelectedClassId(e.target.value ? Number(e.target.value) : '');
                    setSelectedServantId('');
                  }}
                  options={allClasses.map((c) => ({
                    value: c.id,
                    label: `${c.ministryName} — ${c.name}`,
                  }))}
                  placeholder="كل الفصول..."
                />
              </div>

              {/* Servant Filter */}
              {isSecretaryOrAdmin && (
                <div className="w-full sm:w-48">
                  <Select
                    value={selectedServantId}
                    onChange={(e) => setSelectedServantId(e.target.value ? Number(e.target.value) : '')}
                    options={servants.map((s) => ({
                      value: s.personId ?? s.id,
                      label: s.fullName + (s.isClassSecretary ? ' (أمين الفصل)' : ''),
                    }))}
                    placeholder="كل الخدام المسؤولين..."
                    disabled={!selectedClassId}
                  />
                </div>
              )}
            </div>

            {/* Quick Filter Tabs & Bulk Action Buttons */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 border-t border-gray-100">
              {/* Presence filter tabs */}
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
                <button
                  type="button"
                  onClick={() => setPresenceFilter('ALL')}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                    presenceFilter === 'ALL'
                      ? 'bg-gray-900 text-white shadow-sm'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  الكل ({totalRosterCount})
                </button>

                <button
                  type="button"
                  onClick={() => setPresenceFilter('PRESENT')}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition flex items-center gap-1 ${
                    presenceFilter === 'PRESENT'
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100'
                  }`}
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>الحاضرون ({presentCount})</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPresenceFilter('ABSENT')}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition flex items-center gap-1 ${
                    presenceFilter === 'ABSENT'
                      ? 'bg-gray-700 text-white shadow-sm'
                      : 'text-gray-600 bg-gray-100 hover:bg-gray-200'
                  }`}
                >
                  <XCircle className="w-3.5 h-3.5" />
                  <span>الغائبون ({absentCount})</span>
                </button>
              </div>

              {/* Bulk Actions for Filtered Students */}
              {!isLockedForUser && roster.length > 0 && (
                <div className="flex items-center gap-2 shrink-0">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleBulkMark(true)}
                    disabled={batchActionLoading}
                    className="text-xs font-bold text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                  >
                    <CheckCheck className="w-3.5 h-3.5 ml-1" />
                    تسجيل الكل حضور
                  </Button>

                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleBulkMark(false)}
                    disabled={batchActionLoading}
                    className="text-xs font-bold text-gray-600 border-gray-200 hover:bg-gray-50"
                  >
                    <Ban className="w-3.5 h-3.5 ml-1" />
                    تسجيل الكل غياب
                  </Button>
                </div>
              )}
            </div>
          </Card>

          {/* Roster Cards List */}
          {isStudentsLoading ? (
            <div className="py-20 flex flex-col items-center justify-center">
              <Spinner size="lg" />
              <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل قائمة المخدومين...</p>
            </div>
          ) : roster.length === 0 ? (
            <EmptyState
              title="لا توجد نتائج مطابقة"
              description="لم يتم العثور على مخدومين يطابقون خيارات البحث أو التصفية المختارة."
              icon={Users}
            />
          ) : (
            <div className="space-y-2.5">
              {roster.map(({ student, isPresent, recordedByInfo }) => (
                <AttendanceToggleCard
                  key={student.id}
                  studentId={student.id}
                  studentName={student.fullName}
                  className={student.className}
                  ministryName={student.ministryName}
                  servantName={student.responsibleServantName || student.servantName}
                  recordedByInfo={recordedByInfo}
                  isPresent={isPresent}
                  disabled={isLockedForUser}
                  isToggling={
                    (toggleMutation.isPending && toggleMutation.variables?.studentId === student.id) ||
                    batchActionLoading
                  }
                  onToggle={handleToggle}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
