import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { confessionApi } from '../api/confession.api';
import { StudentConfessionSummary, ConfessionSessionDto } from '../types/confession.types';
import { StudentConfessionCard } from '../components/confessions/StudentConfessionCard';
import { ConfessionDrawer } from '../components/confessions/ConfessionDrawer';
import { ConfessionHistoryDrawer } from '../components/confessions/ConfessionHistoryDrawer';
import { ConfessionSessionDrawer } from '../components/confessions/ConfessionSessionDrawer';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { useDebounce } from '../hooks/useDebounce';
import { formatDate } from '../utils/date';
import {
  HeartHandshake,
  Plus,
  Search,
  Users,
  Clock,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  RotateCcw,
  Sparkles,
  Calendar,
  User,
  ChevronDown,
  ChevronUp,
  MessageCircle,
  History,
  School,
  FileText,
} from 'lucide-react';

type FilterTab = 'ALL' | 'OVERDUE' | 'CRITICAL' | 'NEVER' | 'UP_TO_DATE';
type MainView = 'sessions' | 'students';

export const ConfessionsPage: React.FC = () => {
  const [mainView, setMainView] = useState<MainView>('sessions');

  // Sessions state
  const [sessionSearch, setSessionSearch] = useState('');
  const [expandedSessions, setExpandedSessions] = useState<Record<string, boolean>>({});
  const [isSessionDrawerOpen, setIsSessionDrawerOpen] = useState(false);

  // Student overview state
  const [filterTab, setFilterTab] = useState<FilterTab>('ALL');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);

  // Dropdown filters
  const [selectedClassId, setSelectedClassId] = useState<string>('ALL');
  const [selectedServantId, setSelectedServantId] = useState<string>('ALL');
  const [selectedFather, setSelectedFather] = useState<string>('ALL');

  // Single record drawers
  const [isNewDrawerOpen, setIsNewDrawerOpen] = useState(false);
  const [activeStudentForNew, setActiveStudentForNew] = useState<{
    id: number;
    name: string;
    confessionFather?: string | null;
  } | null>(null);

  const [isHistoryDrawerOpen, setIsHistoryDrawerOpen] = useState(false);
  const [activeStudentForHistory, setActiveStudentForHistory] = useState<{
    id: number;
    name: string;
    className?: string | null;
    confessionFather?: string | null;
  } | null>(null);

  // Fetch overview data
  const {
    data: summaries = [],
    isLoading: isSummariesLoading,
    refetch: refetchSummaries,
  } = useQuery<StudentConfessionSummary[]>({
    queryKey: ['confessions', 'overview'],
    queryFn: confessionApi.getOverview,
  });

  // Fetch sessions data
  const {
    data: sessions = [],
    isLoading: isSessionsLoading,
    refetch: refetchSessions,
  } = useQuery<ConfessionSessionDto[]>({
    queryKey: ['confessions', 'sessions'],
    queryFn: confessionApi.getSessions,
  });

  const refetchAll = () => {
    refetchSummaries();
    refetchSessions();
  };

  // Extract unique classes, servants, and fathers for filters
  const uniqueClasses = useMemo(() => {
    const map = new Map<number, string>();
    summaries.forEach((s) => {
      if (s.classId && s.className) {
        map.set(s.classId, s.className);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [summaries]);

  const uniqueServants = useMemo(() => {
    const map = new Map<number, string>();
    summaries.forEach((s) => {
      if (s.servantId && s.servantName) {
        map.set(s.servantId, s.servantName);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [summaries]);

  const uniqueFathers = useMemo(() => {
    const set = new Set<string>();
    summaries.forEach((s) => {
      if (s.confessionFather && s.confessionFather.trim()) {
        set.add(s.confessionFather.trim());
      }
    });
    sessions.forEach((s) => {
      if (s.confessionFather && s.confessionFather.trim()) {
        set.add(s.confessionFather.trim());
      }
    });
    return Array.from(set).sort();
  }, [summaries, sessions]);

  // Metrics
  const totalStudents = summaries.length;
  const upToDateCount = summaries.filter((s) => s.status === 'UP_TO_DATE').length;
  const overdueCount = summaries.filter((s) => s.status === 'OVERDUE').length;
  const criticalCount = summaries.filter((s) => s.status === 'CRITICAL').length;
  const neverCount = summaries.filter((s) => s.status === 'NEVER').length;

  const regularityRate =
    totalStudents > 0 ? Math.round((upToDateCount / totalStudents) * 100) : 0;

  // Filtered sessions
  const filteredSessions = useMemo(() => {
    if (!sessionSearch.trim()) return sessions;
    const q = sessionSearch.toLowerCase().trim();
    return sessions.filter((s) => {
      const matchFather = s.confessionFather.toLowerCase().includes(q);
      const matchDate = s.sessionDate.includes(q);
      const matchNotes = s.notes?.toLowerCase().includes(q) ?? false;
      const matchRecorder = s.recordedByName?.toLowerCase().includes(q) ?? false;
      const matchStudent = s.students.some(
        (st) =>
          st.studentName.toLowerCase().includes(q) ||
          st.phone.includes(q) ||
          (st.className && st.className.toLowerCase().includes(q))
      );
      return matchFather || matchDate || matchNotes || matchRecorder || matchStudent;
    });
  }, [sessions, sessionSearch]);

  // Filtered students for roster tab
  const filteredStudents = useMemo(() => {
    return summaries.filter((item) => {
      if (filterTab !== 'ALL' && item.status !== filterTab) return false;
      if (selectedClassId !== 'ALL' && item.classId !== Number(selectedClassId)) return false;
      if (selectedServantId !== 'ALL' && item.servantId !== Number(selectedServantId)) return false;

      if (selectedFather !== 'ALL') {
        if (selectedFather === 'UNASSIGNED') {
          if (item.confessionFather && item.confessionFather.trim()) return false;
        } else if (item.confessionFather !== selectedFather) {
          return false;
        }
      }

      if (debouncedSearch.trim()) {
        const q = debouncedSearch.toLowerCase().trim();
        const matchesName = item.studentName.toLowerCase().includes(q);
        const matchesPhone = item.phone.includes(q);
        const matchesFather = item.confessionFather?.toLowerCase().includes(q) ?? false;
        if (!matchesName && !matchesPhone && !matchesFather) return false;
      }

      return true;
    });
  }, [
    summaries,
    filterTab,
    selectedClassId,
    selectedServantId,
    selectedFather,
    debouncedSearch,
  ]);

  const toggleSessionExpand = (sessionId: string) => {
    setExpandedSessions((prev) => ({
      ...prev,
      [sessionId]: !prev[sessionId],
    }));
  };

  const handleOpenRecordForStudent = (item: StudentConfessionSummary) => {
    setActiveStudentForNew({
      id: item.studentId,
      name: item.studentName,
      confessionFather: item.confessionFather,
    });
    setIsNewDrawerOpen(true);
  };

  const handleOpenGeneralNewRecord = () => {
    setActiveStudentForNew(null);
    setIsNewDrawerOpen(true);
  };

  const handleViewHistory = (student: {
    id?: number;
    studentId?: number;
    name?: string;
    studentName?: string;
    className?: string | null;
    confessionFather?: string | null;
  }) => {
    setActiveStudentForHistory({
      id: student.id ?? student.studentId ?? 0,
      name: student.name ?? student.studentName ?? '',
      className: student.className,
      confessionFather: student.confessionFather,
    });
    setIsHistoryDrawerOpen(true);
  };

  const handleResetFilters = () => {
    setFilterTab('ALL');
    setSearch('');
    setSelectedClassId('ALL');
    setSelectedServantId('ALL');
    setSelectedFather('ALL');
  };

  const hasActiveFilters =
    filterTab !== 'ALL' ||
    search.trim() !== '' ||
    selectedClassId !== 'ALL' ||
    selectedServantId !== 'ALL' ||
    selectedFather !== 'ALL';

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight">
              سجل وجلسات سر الاعتراف
            </h1>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            تسجيل جلسات الاعترافات مع الآباء الكهنة ومتابعة انتظام المخدومين بالبحث
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={handleOpenGeneralNewRecord}
            className="text-xs h-10 px-3 border-purple-200 text-purple-800 hover:bg-purple-50"
          >
            <Plus className="w-3.5 h-3.5 ml-1" />
            تسجيل فردي
          </Button>

          <Button
            variant="primary"
            onClick={() => setIsSessionDrawerOpen(true)}
            className="shadow-sm font-bold bg-purple-600 hover:bg-purple-700 min-h-[42px]"
          >
            <Sparkles className="w-4 h-4 ml-1.5" />
            + تسجيل جلسة اعترافات جديدة
          </Button>
        </div>
      </div>

      {/* Main View Mode Selector (جلسات الاعترافات vs متابعة المخدومين) */}
      <div className="flex items-center gap-2 border-b border-gray-200 pb-2">
        <button
          type="button"
          onClick={() => setMainView('sessions')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition flex items-center gap-2 ${
            mainView === 'sessions'
              ? 'bg-purple-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>جلسات الاعترافات المسجلة</span>
          <span
            className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              mainView === 'sessions' ? 'bg-purple-700 text-white' : 'bg-gray-200 text-gray-800'
            }`}
          >
            {sessions.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setMainView('students')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition flex items-center gap-2 ${
            mainView === 'students'
              ? 'bg-purple-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>متابعة حالة المخدومين</span>
          <span
            className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              mainView === 'students' ? 'bg-purple-700 text-white' : 'bg-gray-200 text-gray-800'
            }`}
          >
            {totalStudents}
          </span>
        </button>
      </div>


      {/* ========================================================================= */}
      {/* VIEW 1: CONFESSION SESSIONS LOG                                           */}
      {/* ========================================================================= */}
      {mainView === 'sessions' && (
        <div className="space-y-4">
          {/* Sessions Search and Actions */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="relative flex-1 max-w-md">
              <Input
                placeholder="ابحث باسم الكاهن، التاريخ، أو اسم المخدوم في الجلسات..."
                value={sessionSearch}
                onChange={(e) => setSessionSearch(e.target.value)}
                className="pr-10 bg-white"
              />
              <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-gray-400">
                <Search className="w-4 h-4" />
              </div>
              {sessionSearch && (
                <button
                  type="button"
                  onClick={() => setSessionSearch('')}
                  className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="text-xs text-gray-500 font-medium">
              عرض {filteredSessions.length} من أصل {sessions.length} جلسة
            </div>
          </div>

          {/* Sessions Cards List */}
          {isSessionsLoading ? (
            <div className="py-20 flex flex-col items-center justify-center">
              <Spinner size="lg" />
              <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل جلسات الاعترافات...</p>
            </div>
          ) : filteredSessions.length === 0 ? (
            <Card className="text-center py-16 px-4">
              <div className="w-16 h-16 rounded-3xl bg-purple-50 text-purple-600 flex items-center justify-center mx-auto mb-4 border border-purple-100">
                <Calendar className="w-8 h-8" />
              </div>
              <h3 className="text-base font-bold text-gray-900 mb-1">
                {sessionSearch.trim() ? 'لا توجد جلسات تطابق البحث' : 'لم يتم تسجيل أي جلسة اعترافات بعد'}
              </h3>
              <p className="text-xs text-gray-500 max-w-md mx-auto mb-5">
                {sessionSearch.trim()
                  ? 'جرّب البحث باسم كاهن آخر أو تاريخ مختلف.'
                  : 'يمكنك إنشاء جلسة اعتراف وتحديد اسم الكاهن ثم اختيار المخدومين الحاضرين عبر البحث.'}
              </p>
              <Button
                variant="primary"
                onClick={() => setIsSessionDrawerOpen(true)}
                className="font-bold bg-purple-600 hover:bg-purple-700"
              >
                <Plus className="w-4 h-4 ml-1.5" />
                تسجيل جلسة اعترافات الآن
              </Button>
            </Card>
          ) : (
            <div className="space-y-3.5">
              {filteredSessions.map((session) => {
                const isExpanded = !!expandedSessions[session.sessionId];

                return (
                  <Card
                    key={session.sessionId}
                    className="border-gray-200 hover:border-purple-300 transition shadow-sm overflow-hidden"
                  >
                    {/* Session Header Card */}
                    <div className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-gradient-to-r from-purple-50/30 to-white">
                      <div className="space-y-1.5">
                        <div className="flex items-center gap-2.5 flex-wrap">
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-bold bg-purple-100 text-purple-900 border border-purple-200">
                            <User className="w-3.5 h-3.5 text-purple-700" />
                            {session.confessionFather}
                          </span>

                          <span className="text-xs font-bold text-gray-700 flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5 text-gray-400" />
                            {formatDate(session.sessionDate)}
                          </span>

                          <Badge variant="success">
                            {session.studentCount} مخدوم حاضر
                          </Badge>
                        </div>

                        {session.notes && (
                          <p className="text-xs text-purple-950 font-medium flex items-center gap-1.5 mt-1">
                            <FileText className="w-3 h-3 text-purple-500" />
                            <span>{session.notes}</span>
                          </p>
                        )}

                        {session.recordedByName && (
                          <p className="text-[11px] text-gray-400">
                            سجلت بواسطة: <span className="text-gray-600 font-medium">{session.recordedByName}</span>
                          </p>
                        )}
                      </div>

                      {/* Expand / Collapse Button */}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => toggleSessionExpand(session.sessionId)}
                        className="text-xs font-bold gap-1 self-start sm:self-auto border-purple-200 text-purple-900 hover:bg-purple-100"
                      >
                        <span>{isExpanded ? 'إخفاء المخدومين' : `عرض المخدومين (${session.studentCount})`}</span>
                        {isExpanded ? (
                          <ChevronUp className="w-3.5 h-3.5" />
                        ) : (
                          <ChevronDown className="w-3.5 h-3.5" />
                        )}
                      </Button>
                    </div>

                    {/* Expanded Attendees List */}
                    {isExpanded && (
                      <div className="border-t border-gray-100 p-4 bg-gray-50/50 space-y-2">
                        <h5 className="text-xs font-bold text-gray-700 flex items-center gap-1.5 mb-3">
                          <Users className="w-3.5 h-3.5 text-purple-600" />
                          قائمة المخدومين الذين اعترفوا في هذه الجلسة ({session.students.length}):
                        </h5>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                          {session.students.map((st, idx) => {
                            const cleanPhone = st.phone ? st.phone.replace(/\D/g, '') : '';
                            const waUrl = cleanPhone
                              ? `https://wa.me/2${cleanPhone}?text=${encodeURIComponent(
                                  `سلام ونعمة يا ${st.studentName}، بنفكرك بمتابعة وتوجيهات أبونا ${session.confessionFather}.`
                                )}`
                              : null;

                            return (
                              <div
                                key={st.studentId || idx}
                                className="flex items-center justify-between p-2.5 rounded-xl border border-gray-200 bg-white text-xs hover:border-purple-200 transition"
                              >
                                <div className="flex items-center gap-2">
                                  <div className="w-6 h-6 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-[11px] shrink-0">
                                    {idx + 1}
                                  </div>
                                  <div>
                                    <p className="font-bold text-gray-900">{st.studentName}</p>
                                    <div className="flex items-center gap-2 text-[11px] text-gray-500 mt-0.5">
                                      {st.className && (
                                        <span className="flex items-center gap-0.5">
                                          <School className="w-3 h-3 text-gray-400" />
                                          {st.className}
                                        </span>
                                      )}
                                      <span className="font-mono text-gray-400" dir="ltr">
                                        {st.phone}
                                      </span>
                                    </div>
                                  </div>
                                </div>

                                <div className="flex items-center gap-1">
                                  {waUrl && (
                                    <a
                                      href={waUrl}
                                      target="_blank"
                                      rel="noopener noreferrer"
                                      className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-50 transition"
                                      title="مراسلة واتساب"
                                    >
                                      <MessageCircle className="w-3.5 h-3.5" />
                                    </a>
                                  )}

                                  <button
                                    type="button"
                                    onClick={() =>
                                      handleViewHistory({
                                        id: st.studentId,
                                        name: st.studentName,
                                        className: st.className,
                                        confessionFather: session.confessionFather,
                                      })
                                    }
                                    className="p-1.5 rounded-lg text-purple-600 hover:bg-purple-50 transition"
                                    title="عرض سجل الاعترافات"
                                  >
                                    <History className="w-3.5 h-3.5" />
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* VIEW 2: STUDENTS CONFESSION TRACKER / ROSTER                              */}
      {/* ========================================================================= */}
      {mainView === 'students' && (
        <div className="space-y-6">
          {/* Progress & KPI Metrics Card */}
          <Card className="p-5 bg-gradient-to-br from-white via-purple-50/15 to-white border-purple-100 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-purple-600" />
                  <h3 className="font-black text-gray-900 text-base">
                    نسبة الانتظام بالاعتراف: {regularityRate}%
                  </h3>
                </div>
                <p className="text-xs text-gray-500 mt-0.5">
                  {upToDateCount} مخدوم واظبوا على الاعتراف خلال الـ 30 يوماً الماضية من أصل {totalStudents} مخدوم
                </p>
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                <div className="flex items-center gap-1.5 bg-emerald-50 text-emerald-800 border border-emerald-200 px-3 py-1.5 rounded-xl text-xs font-bold">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>منتظم: {upToDateCount}</span>
                </div>
                <div className="flex items-center gap-1.5 bg-amber-50 text-amber-800 border border-amber-200 px-3 py-1.5 rounded-xl text-xs font-bold">
                  <Clock className="w-3.5 h-3.5 text-amber-600" />
                  <span>متأخر (&gt; 30 يوم): {overdueCount}</span>
                </div>
                <div className="flex items-center gap-1.5 bg-rose-50 text-rose-800 border border-rose-200 px-3 py-1.5 rounded-xl text-xs font-bold">
                  <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                  <span>منقطع (&gt; 60 يوم): {criticalCount}</span>
                </div>
              </div>
            </div>

            {/* Regularity Progress Bar */}
            <div className="space-y-1.5">
              <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 ease-out ${
                    regularityRate >= 70
                      ? 'bg-emerald-500'
                      : regularityRate >= 40
                      ? 'bg-purple-600'
                      : 'bg-amber-500'
                  }`}
                  style={{ width: `${regularityRate}%` }}
                />
              </div>
              <div className="flex items-center justify-between text-[11px] text-gray-400 font-mono">
                <span>0%</span>
                <span>الهدف الرعوي: ممارسة شهرية منتظمة لكل مخدوم</span>
                <span>100%</span>
              </div>
            </div>
          </Card>

          {/* Filter and Search Bar */}
          <div className="space-y-3">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              {/* Status Filter Tabs */}
              <div className="flex items-center gap-1.5 p-1 bg-gray-100/90 rounded-2xl w-fit overflow-x-auto max-w-full pb-1 sm:pb-1">
                <button
                  type="button"
                  onClick={() => setFilterTab('ALL')}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] whitespace-nowrap ${
                    filterTab === 'ALL'
                      ? 'bg-white text-gray-900 shadow-sm'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  <span>الكل</span>
                  <span className="px-1.5 py-0.5 rounded-md bg-gray-100 text-[10px] font-mono">
                    {totalStudents}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setFilterTab('OVERDUE')}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] whitespace-nowrap ${
                    filterTab === 'OVERDUE'
                      ? 'bg-amber-500 text-white shadow-sm'
                      : 'text-gray-600 hover:text-amber-800'
                  }`}
                >
                  <Clock className="w-3.5 h-3.5" />
                  <span>متأخر (&gt; 30 يوم)</span>
                  <span
                    className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                      filterTab === 'OVERDUE'
                        ? 'bg-amber-600 text-white'
                        : 'bg-amber-100 text-amber-900'
                    }`}
                  >
                    {overdueCount}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setFilterTab('CRITICAL')}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] whitespace-nowrap ${
                    filterTab === 'CRITICAL'
                      ? 'bg-rose-600 text-white shadow-sm'
                      : 'text-gray-600 hover:text-rose-800'
                  }`}
                >
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>منقطع (&gt; 60 يوم)</span>
                  <span
                    className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                      filterTab === 'CRITICAL'
                        ? 'bg-rose-700 text-white'
                        : 'bg-rose-100 text-rose-900'
                    }`}
                  >
                    {criticalCount}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setFilterTab('NEVER')}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] whitespace-nowrap ${
                    filterTab === 'NEVER'
                      ? 'bg-gray-700 text-white shadow-sm'
                      : 'text-gray-500 hover:text-gray-800'
                  }`}
                >
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>لم يسجل</span>
                  <span
                    className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                      filterTab === 'NEVER' ? 'bg-gray-800 text-white' : 'bg-gray-200 text-gray-800'
                    }`}
                  >
                    {neverCount}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setFilterTab('UP_TO_DATE')}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] whitespace-nowrap ${
                    filterTab === 'UP_TO_DATE'
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-gray-600 hover:text-emerald-800'
                  }`}
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>منتظم حديثاً</span>
                  <span
                    className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                      filterTab === 'UP_TO_DATE'
                        ? 'bg-emerald-700 text-white'
                        : 'bg-emerald-100 text-emerald-900'
                    }`}
                  >
                    {upToDateCount}
                  </span>
                </button>
              </div>

              {/* Search Input */}
              <div className="relative w-full md:w-80">
                <Input
                  placeholder="بحث باسم المخدوم، الهاتف، أب الاعتراف..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pr-10 bg-white"
                />
                <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-gray-400">
                  <Search className="w-4 h-4" />
                </div>
                {search && (
                  <button
                    type="button"
                    onClick={() => setSearch('')}
                    className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400 hover:text-gray-600"
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>

            {/* Secondary Filter Dropdowns Bar */}
            {(uniqueClasses.length > 1 ||
              uniqueServants.length > 1 ||
              uniqueFathers.length > 0 ||
              hasActiveFilters) && (
              <div className="flex items-center gap-2 flex-wrap pt-1 text-xs">
                {uniqueClasses.length > 1 && (
                  <select
                    value={selectedClassId}
                    onChange={(e) => setSelectedClassId(e.target.value)}
                    className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-purple-500 min-h-[36px]"
                  >
                    <option value="ALL">جميع الفصول ({uniqueClasses.length})</option>
                    {uniqueClasses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                )}

                {uniqueServants.length > 1 && (
                  <select
                    value={selectedServantId}
                    onChange={(e) => setSelectedServantId(e.target.value)}
                    className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-purple-500 min-h-[36px]"
                  >
                    <option value="ALL">جميع الخدام ({uniqueServants.length})</option>
                    {uniqueServants.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                )}

                {uniqueFathers.length > 0 && (
                  <select
                    value={selectedFather}
                    onChange={(e) => setSelectedFather(e.target.value)}
                    className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-purple-500 min-h-[36px]"
                  >
                    <option value="ALL">كل آباء الاعتراف ({uniqueFathers.length})</option>
                    <option value="UNASSIGNED">غير محدد له أب اعتراف</option>
                    {uniqueFathers.map((f) => (
                      <option key={f} value={f}>
                        {f}
                      </option>
                    ))}
                  </select>
                )}

                {hasActiveFilters && (
                  <button
                    type="button"
                    onClick={handleResetFilters}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-gray-500 hover:text-red-600 hover:bg-red-50 transition font-medium min-h-[36px]"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>إلغاء التصفية</span>
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Stream of Student Cards */}
          {isSummariesLoading ? (
            <div className="py-20 flex flex-col items-center justify-center">
              <Spinner size="lg" />
              <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل متابعة المخدومين...</p>
            </div>
          ) : filteredStudents.length === 0 ? (
            <Card className="text-center py-16 px-4">
              <div className="w-16 h-16 rounded-3xl bg-purple-50 text-purple-600 flex items-center justify-center mx-auto mb-4 border border-purple-100">
                <HeartHandshake className="w-8 h-8" />
              </div>

              <h3 className="text-base font-bold text-gray-900 mb-1">
                {filterTab === 'CRITICAL'
                  ? 'ممتاز! لا يوجد مخدومون منقطعون عن الاعتراف'
                  : filterTab === 'OVERDUE'
                  ? 'رائع! لا يوجد مخدومون متأخرون عن موعد الاعتراف'
                  : 'لا توجد بطاقات اعتراف مطابقة'}
              </h3>

              <p className="text-xs text-gray-500 max-w-md mx-auto mb-5">
                {search.trim()
                  ? 'لم يتم العثور على أي مخدوم يطابق كلمات البحث المحددة.'
                  : filterTab === 'CRITICAL'
                  ? 'جميع مخدوميك يمارسون سر الاعتراف بانتظام خلال الشهرين الماضيين.'
                  : filterTab === 'OVERDUE'
                  ? 'جميع مخدوميك منتظمون في اعترافاتهم خلال الـ 30 يوماً الماضية.'
                  : 'لا يوجد مخدومين مسندين إليك حالياً في هذا النطاق.'}
              </p>

              {hasActiveFilters && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleResetFilters}
                  className="gap-1.5 font-bold"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  عرض جميع المخدومين
                </Button>
              )}
            </Card>
          ) : (
            <div className="space-y-4">
              {filteredStudents.map((item) => (
                <StudentConfessionCard
                  key={item.studentId}
                  item={item}
                  onRecordConfession={handleOpenRecordForStudent}
                  onViewHistory={handleViewHistory}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* DRAWERS                                                                   */}
      {/* ========================================================================= */}

      {/* Session Confession Drawer */}
      <ConfessionSessionDrawer
        isOpen={isSessionDrawerOpen}
        onClose={() => setIsSessionDrawerOpen(false)}
        onSaved={refetchAll}
        existingFathers={uniqueFathers}
      />

      {/* Single Record Confession Drawer */}
      <ConfessionDrawer
        isOpen={isNewDrawerOpen}
        onClose={() => {
          setIsNewDrawerOpen(false);
          setActiveStudentForNew(null);
        }}
        student={activeStudentForNew}
        onSaved={refetchAll}
      />

      {/* History Drawer */}
      <ConfessionHistoryDrawer
        isOpen={isHistoryDrawerOpen}
        onClose={() => {
          setIsHistoryDrawerOpen(false);
          setActiveStudentForHistory(null);
        }}
        student={activeStudentForHistory}
        onOpenNewRecord={() => {
          if (activeStudentForHistory) {
            setActiveStudentForNew({
              id: activeStudentForHistory.id,
              name: activeStudentForHistory.name,
              confessionFather: activeStudentForHistory.confessionFather,
            });
            setIsHistoryDrawerOpen(false);
            setIsNewDrawerOpen(true);
          }
        }}
        onRecordDeleted={refetchAll}
      />
    </div>
  );
};
