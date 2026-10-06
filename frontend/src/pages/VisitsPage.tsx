import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { visitsApi } from '../api/visits.api';
import {
  StudentVisitStatusResponse,
  VisitRecordResponse,
  WeekResponse,
} from '../types/visit.types';
import { StudentVisitCard } from '../components/visits/StudentVisitCard';
import { VisitDrawer } from '../components/visits/VisitDrawer';
import { WeekTimelineBanner } from '../components/attendance/WeekTimelineBanner';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Spinner } from '../components/ui/Spinner';
import { Button } from '../components/ui/Button';
import { useDebounce } from '../hooks/useDebounce';
import { usePermissions } from '../auth/usePermissions';
import {
  CalendarCheck,
  Search,
  CheckCircle2,
  Clock,
  Home,
  PhoneCall,
  Users,
  RotateCcw,
  Sparkles,
  Award,
} from 'lucide-react';

type FilterTab = 'ALL' | 'UNVISITED' | 'VISITED';

export const VisitsPage: React.FC = () => {
  const { isAdmin } = usePermissions();

  const [filterTab, setFilterTab] = useState<FilterTab>('ALL');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);

  // Selected week ID (null means current week)
  const [selectedWeekId, setSelectedWeekId] = useState<number | null>(null);

  // Filter dropdowns
  const [selectedClassId, setSelectedClassId] = useState<string>('ALL');
  const [selectedServantId, setSelectedServantId] = useState<string>('ALL');
  const [selectedMethod, setSelectedMethod] = useState<string>('ALL');

  // Drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [activeStudent, setActiveStudent] = useState<{ id: number; name: string } | null>(null);
  const [activeRecord, setActiveRecord] = useState<VisitRecordResponse | null>(null);

  // Fetch all weeks to populate timeline banner
  const { data: allWeeks = [] } = useQuery<WeekResponse[]>({
    queryKey: ['weeks'],
    queryFn: visitsApi.getWeeks,
  });

  // Current week workflow query
  const {
    data: currentWeekData,
    isLoading: isCurrentLoading,
    refetch: refetchCurrentWeek,
  } = useQuery({
    queryKey: ['visits', 'current-week'],
    queryFn: visitsApi.getCurrentWeek,
  });

  const currentWeek = currentWeekData?.week || (allWeeks.find((w) => w.active) ?? allWeeks[0] ?? null);

  // Active week resolution
  const activeWeek: WeekResponse | null = useMemo(() => {
    if (!selectedWeekId) return currentWeek;
    return allWeeks.find((w) => w.id === selectedWeekId) || currentWeek;
  }, [selectedWeekId, currentWeek, allWeeks]);

  const isViewingCurrent = !selectedWeekId || selectedWeekId === currentWeek?.id;

  // Historical visits query if viewing a past week
  const {
    data: historicalVisits = [],
    isLoading: isHistoricalLoading,
    refetch: refetchHistorical,
  } = useQuery({
    queryKey: ['visits', 'by-week', activeWeek?.id],
    queryFn: () => visitsApi.findByWeek(activeWeek!.id),
    enabled: !!activeWeek?.id && !isViewingCurrent,
  });

  // Compose student list with normalized visit statuses
  const rawStudentList: StudentVisitStatusResponse[] = useMemo(() => {
    const baseStudents = currentWeekData?.students || [];

    return (baseStudents as any[]).map((st) => {
      let isVisited = false;
      let record: VisitRecordResponse | null = null;

      if (isViewingCurrent) {
        isVisited = !!st.visit || !!st.visited;
        if (st.visit) {
          record = {
            id: st.visit.id,
            studentId: st.studentId,
            studentName: st.fullName || st.studentName || '',
            weekId: currentWeek?.id || 0,
            weekStartDate: currentWeek?.startDate || '',
            weekEndDate: currentWeek?.endDate || '',
            academicYearId: 0,
            academicYearName: '',
            ministryId: 0,
            ministryName: '',
            classId: st.classId || 0,
            className: st.className || '',
            servantId: st.servantId,
            servantName: st.servantName,
            method: st.visit.method,
            prayerScore: st.visit.prayerScore,
            readingScore: st.visit.readingScore,
            noteScore: st.visit.noteScore,
            notes: st.visit.notes,
            recordedById: 0,
            recordedByName: '',
            recordedAt: st.visit.recordedAt,
          };
        } else if (st.visitRecord) {
          record = st.visitRecord;
        }
      } else {
        const hist = historicalVisits.find((v) => v.studentId === st.studentId);
        isVisited = !!hist;
        record = hist || null;
      }

      return {
        studentId: st.studentId,
        studentName: st.fullName || st.studentName || '',
        phone: st.phone || '',
        gender: st.gender || 'MALE',
        address: st.address,
        guardianPhone: st.guardianPhone,
        classId: st.classId || record?.classId,
        className: st.className || record?.className,
        servantId: st.servantId || record?.servantId,
        servantName: st.servantName || record?.servantName,
        visited: isVisited,
        visitRecord: record,
      };
    });
  }, [isViewingCurrent, currentWeekData, historicalVisits, currentWeek]);

  // Extract unique classes & servants for filters
  const uniqueClasses = useMemo(() => {
    const map = new Map<number, string>();
    rawStudentList.forEach((s) => {
      if (s.classId && s.className) {
        map.set(s.classId, s.className);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [rawStudentList]);

  const uniqueServants = useMemo(() => {
    const map = new Map<number, string>();
    rawStudentList.forEach((s) => {
      if (s.servantId && s.servantName) {
        map.set(s.servantId, s.servantName);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [rawStudentList]);

  // Filter and search
  const filteredStudents = useMemo(() => {
    return rawStudentList.filter((item) => {
      // Tab filter
      if (filterTab === 'VISITED' && !item.visited) return false;
      if (filterTab === 'UNVISITED' && item.visited) return false;

      // Class filter
      if (selectedClassId !== 'ALL' && item.classId !== Number(selectedClassId)) {
        return false;
      }

      // Servant filter
      if (selectedServantId !== 'ALL' && item.servantId !== Number(selectedServantId)) {
        return false;
      }

      // Method filter
      if (
        selectedMethod !== 'ALL' &&
        (!item.visitRecord || item.visitRecord.method !== selectedMethod)
      ) {
        return false;
      }

      // Search filter
      if (debouncedSearch.trim()) {
        const q = debouncedSearch.toLowerCase().trim();
        const matchesName = item.studentName.toLowerCase().includes(q);
        const matchesPhone = Boolean(item.phone && item.phone.includes(q));
        const matchesGuardian = Boolean(item.guardianPhone && item.guardianPhone.includes(q));
        const matchesAddress = item.address?.toLowerCase().includes(q) ?? false;
        if (!matchesName && !matchesPhone && !matchesGuardian && !matchesAddress) return false;
      }

      return true;
    });
  }, [
    rawStudentList,
    filterTab,
    selectedClassId,
    selectedServantId,
    selectedMethod,
    debouncedSearch,
  ]);

  // Metrics
  const totalCount = rawStudentList.length;
  const visitedCount = rawStudentList.filter((s) => s.visited).length;
  const unvisitedCount = totalCount - visitedCount;
  const progressPercent = totalCount > 0 ? Math.round((visitedCount / totalCount) * 100) : 0;

  const homeVisitCount = rawStudentList.filter(
    (s) => s.visited && s.visitRecord?.method === 'VISIT'
  ).length;
  const callVisitCount = rawStudentList.filter(
    (s) => s.visited && s.visitRecord?.method === 'CALL'
  ).length;

  const isWeekLocked = !!activeWeek?.locked && !isAdmin;

  // Drawer handlers
  const handleOpenRecord = (item: StudentVisitStatusResponse) => {
    setActiveStudent({ id: item.studentId, name: item.studentName });
    setActiveRecord(null);
    setIsDrawerOpen(true);
  };

  const handleOpenEdit = (item: StudentVisitStatusResponse, record: VisitRecordResponse) => {
    setActiveStudent({ id: item.studentId, name: item.studentName });
    setActiveRecord(record);
    setIsDrawerOpen(true);
  };

  const handleSaved = () => {
    if (isViewingCurrent) {
      refetchCurrentWeek();
    } else {
      refetchHistorical();
    }
  };

  const handleResetFilters = () => {
    setFilterTab('ALL');
    setSearch('');
    setSelectedClassId('ALL');
    setSelectedServantId('ALL');
    setSelectedMethod('ALL');
  };

  const hasActiveFilters =
    filterTab !== 'ALL' ||
    search.trim() !== '' ||
    selectedClassId !== 'ALL' ||
    selectedServantId !== 'ALL' ||
    selectedMethod !== 'ALL';

  const isLoading = isCurrentLoading || (!isViewingCurrent && isHistoricalLoading);

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight">
              المتابعة الأسبوعية والافتقاد
            </h1>
            <span className="bg-primary-50 text-primary-700 text-xs font-bold px-2.5 py-0.5 rounded-full border border-primary-200">
              سير العمل الدوري
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            سير العمل الأسبوعي لرعاية وافتقاد المخدومين — تواصل مباشر ومتابعة للنمو الروحي
          </p>
        </div>
      </div>

      {/* Week Timeline Banner */}
      <WeekTimelineBanner
        currentWeek={currentWeek}
        selectedWeek={activeWeek}
        allWeeks={allWeeks}
        onSelectWeek={(w) => setSelectedWeekId(w.id)}
        isAdmin={isAdmin}
      />

      {/* Weekly Visitation Progress KPI Card */}
      <Card className="p-5 bg-gradient-to-br from-white via-primary-50/20 to-white border-primary-100 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-primary-600" />
              <h3 className="font-black text-gray-900 text-base">
                نسبة إنجاز الافتقاد: {progressPercent}%
              </h3>
            </div>
            <p className="text-xs text-gray-500 mt-0.5">
              تم افتقاد {visitedCount} من أصل {totalCount} مخدوم خلال هذا الأسبوع
            </p>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <div className="flex items-center gap-1.5 bg-emerald-50 text-emerald-800 border border-emerald-200 px-3 py-1.5 rounded-xl text-xs font-bold">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>تم الافتقاد: {visitedCount}</span>
            </div>
            <div className="flex items-center gap-1.5 bg-amber-50 text-amber-800 border border-amber-200 px-3 py-1.5 rounded-xl text-xs font-bold">
              <Clock className="w-3.5 h-3.5 text-amber-600" />
              <span>المتبقي: {unvisitedCount}</span>
            </div>
          </div>
        </div>

        {/* Animated Progress Bar */}
        <div className="space-y-1.5">
          <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden p-0.5">
            <div
              className={`h-full rounded-full transition-all duration-500 ease-out ${
                progressPercent === 100
                  ? 'bg-emerald-500'
                  : 'bg-gradient-to-r from-primary-600 to-indigo-600'
              }`}
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[11px] text-gray-400 font-mono">
            <span>0%</span>
            <span>الهدف: 100% افتقاد شامل</span>
            <span>100%</span>
          </div>
        </div>

        {/* Quick Method Breakdown */}
        {visitedCount > 0 && (
          <div className="pt-2 border-t border-gray-100 flex items-center gap-4 text-xs text-gray-600">
            <span className="text-[11px] font-bold text-gray-400">طريقة الافتقاد:</span>
            <span className="inline-flex items-center gap-1">
              <Home className="w-3.5 h-3.5 text-primary-600" />
              <span>زيارة منزلية:</span>
              <strong className="font-mono text-gray-900">{homeVisitCount}</strong>
            </span>
            <span className="inline-flex items-center gap-1">
              <PhoneCall className="w-3.5 h-3.5 text-emerald-600" />
              <span>مكالمة هاتفية:</span>
              <strong className="font-mono text-gray-900">{callVisitCount}</strong>
            </span>
          </div>
        )}
      </Card>

      {/* Filter and Search Bar */}
      <div className="space-y-3">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          {/* Status Tabs with prominent "لم يفتقد بعد" */}
          <div className="flex items-center gap-1.5 p-1 bg-gray-100/90 rounded-2xl w-fit">
            <button
              type="button"
              onClick={() => setFilterTab('ALL')}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] ${
                filterTab === 'ALL'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-500 hover:text-gray-900'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>الكل</span>
              <span className="px-1.5 py-0.5 rounded-md bg-gray-100 text-[10px] font-mono">
                {totalCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setFilterTab('UNVISITED')}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] ${
                filterTab === 'UNVISITED'
                  ? 'bg-amber-500 text-white shadow-sm'
                  : 'text-gray-600 hover:text-amber-800'
              }`}
            >
              <Clock className="w-3.5 h-3.5" />
              <span>لم يفتقد بعد</span>
              <span
                className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                  filterTab === 'UNVISITED'
                    ? 'bg-amber-600 text-white'
                    : 'bg-amber-100 text-amber-900'
                }`}
              >
                {unvisitedCount}
              </span>
              {unvisitedCount > 0 && filterTab !== 'UNVISITED' && (
                <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
              )}
            </button>

            <button
              type="button"
              onClick={() => setFilterTab('VISITED')}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 min-h-[38px] ${
                filterTab === 'VISITED'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-gray-600 hover:text-emerald-800'
              }`}
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>تم الافتقاد</span>
              <span
                className={`px-1.5 py-0.5 rounded-md text-[10px] font-mono ${
                  filterTab === 'VISITED'
                    ? 'bg-emerald-700 text-white'
                    : 'bg-emerald-100 text-emerald-900'
                }`}
              >
                {visitedCount}
              </span>
            </button>
          </div>

          {/* Search Input */}
          <div className="relative w-full md:w-80">
            <Input
              placeholder="بحث باسم المخدوم، الهاتف، العنوان..."
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

        {/* Secondary Filters Bar (Class, Servant, Method, Reset) */}
        {(uniqueClasses.length > 1 || uniqueServants.length > 1 || hasActiveFilters) && (
          <div className="flex items-center gap-2 flex-wrap pt-1 text-xs">
            {/* Class Dropdown */}
            {uniqueClasses.length > 1 && (
              <select
                value={selectedClassId}
                onChange={(e) => setSelectedClassId(e.target.value)}
                className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-primary-500 min-h-[36px]"
              >
                <option value="ALL">جميع الفصول ({uniqueClasses.length})</option>
                {uniqueClasses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            )}

            {/* Servant Dropdown */}
            {uniqueServants.length > 1 && (
              <select
                value={selectedServantId}
                onChange={(e) => setSelectedServantId(e.target.value)}
                className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-primary-500 min-h-[36px]"
              >
                <option value="ALL">جميع الخدام ({uniqueServants.length})</option>
                {uniqueServants.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            )}

            {/* Method Dropdown */}
            {filterTab !== 'UNVISITED' && (
              <select
                value={selectedMethod}
                onChange={(e) => setSelectedMethod(e.target.value)}
                className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium focus:ring-1 focus:ring-primary-500 min-h-[36px]"
              >
                <option value="ALL">كل طرق الافتقاد</option>
                <option value="VISIT">زيارة منزلية فقط</option>
                <option value="CALL">مكالمة هاتفية فقط</option>
              </select>
            )}

            {/* Reset Filters button */}
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

      {/* Cards Stream */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل بطاقات الافتقاد...</p>
        </div>
      ) : filteredStudents.length === 0 ? (
        <Card className="text-center py-16 px-4">
          <div className="w-16 h-16 rounded-3xl bg-gray-50 text-gray-400 flex items-center justify-center mx-auto mb-4 border border-gray-100">
            {filterTab === 'UNVISITED' ? (
              <Award className="w-8 h-8 text-amber-500" />
            ) : filterTab === 'VISITED' ? (
              <Clock className="w-8 h-8 text-gray-400" />
            ) : (
              <CalendarCheck className="w-8 h-8 text-primary-500" />
            )}
          </div>

          <h3 className="text-base font-bold text-gray-900 mb-1">
            {filterTab === 'UNVISITED' && totalCount > 0
              ? 'ممتاز! تم افتقاد جميع المخدومين لهذا الأسبوع'
              : 'لا توجد بطاقات افتقاد مطابقة'}
          </h3>

          <p className="text-xs text-gray-500 max-w-md mx-auto mb-5">
            {search.trim()
              ? 'لم يتم العثور على أي مخدوم يطابق كلمات البحث المحددة.'
              : filterTab === 'UNVISITED' && totalCount > 0
              ? 'لقد اكتمل افتقاد جميع المخدومين المسندين بالكامل! يمكنك مراجعة بطاقاتهم في تبويب "تم الافتقاد".'
              : filterTab === 'VISITED'
              ? 'لم يتم تسجيل افتقاد لأي مخدوم بعد خلال هذا الأسبوع. اختر "لم يفتقد بعد" لبدء الافتقاد.'
              : 'لا يوجد مخدومين مسندين إليك حالياً في هذا الأسبوع الخدمي.'}
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
            <StudentVisitCard
              key={item.studentId}
              item={item}
              isWeekLocked={isWeekLocked}
              onRecordVisit={handleOpenRecord}
              onEditVisit={handleOpenEdit}
            />
          ))}
        </div>
      )}

      {/* Visit Record & Edit Drawer */}
      {activeWeek && (
        <VisitDrawer
          isOpen={isDrawerOpen}
          onClose={() => setIsDrawerOpen(false)}
          student={activeStudent}
          weekId={activeWeek.id}
          existingVisit={activeRecord}
          isWeekLocked={isWeekLocked}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
};

