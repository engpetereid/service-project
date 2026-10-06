import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams, useLocation } from 'react-router-dom';
import { studentsApi } from '../api/students.api';
import { ministriesApi } from '../api/ministries.api';
import { classesApi } from '../api/classes.api';
import { servantsApi } from '../api/servants.api';
import { StudentResponse } from '../types/student.types';
import { StudentDrawer } from '../components/students/StudentDrawer';
import { QuickAssignModal } from '../components/students/QuickAssignModal';
import { BatchMoveModal } from '../components/students/BatchMoveModal';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { EmptyState } from '../components/ui/EmptyState';
import { usePermissions } from '../auth/usePermissions';
import { useDebounce } from '../hooks/useDebounce';
import { GENDER_LABELS, STUDENT_STATUS_LABELS } from '../utils/arabic';
import {
  Search,
  UserPlus,
  Users,
  School,
  UserCheck,
  UserX,
  ArrowRightLeft,
  CheckSquare,
  Square,
  X,
} from 'lucide-react';

type FilterTab = 'ALL' | 'WITHOUT_SERVANT' | 'ACTIVE' | 'GRADUATED';

export const StudentsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const isAllStudentsView = location.pathname === '/all-students' || searchParams.get('scope') === 'class';
  const paramMinistryId = searchParams.get('ministryId') ? Number(searchParams.get('ministryId')) : null;
  const paramClassId = searchParams.get('classId') ? Number(searchParams.get('classId')) : null;

  const { isAdmin, isServiceSecretary, isClassSecretary, isServant, managedMinistryId, managedClassId } =
    usePermissions();
  const isSecretaryOrAdmin = isAdmin || isServiceSecretary || isClassSecretary;

  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [selectedMinistryId, setSelectedMinistryId] = useState<number | ''>(
    paramMinistryId || managedMinistryId || ''
  );
  const [selectedClassId, setSelectedClassId] = useState<number | ''>(
    paramClassId || managedClassId || ''
  );
  const [selectedServantId, setSelectedServantId] = useState<number | ''>('');
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');

  // Selection state for batch operations
  const [selectedStudentIds, setSelectedStudentIds] = useState<number[]>([]);

  // Modals / Drawers state
  const [activeStudent, setActiveStudent] = useState<StudentResponse | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isQuickAssignOpen, setIsQuickAssignOpen] = useState(false);
  const [quickAssignTargetStudents, setQuickAssignTargetStudents] = useState<StudentResponse[]>([]);
  const [isBatchMoveOpen, setIsBatchMoveOpen] = useState(false);

  // Queries
  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries', 'all-for-students-filter'],
    queryFn: () => ministriesApi.findAll(),
  });

  const { data: classes = [] } = useQuery({
    queryKey: ['classes', selectedMinistryId],
    queryFn: () => classesApi.findAll(selectedMinistryId ? Number(selectedMinistryId) : undefined),
    enabled: !!selectedMinistryId,
  });

  const { data: servants = [] } = useQuery({
    queryKey: ['servants', selectedMinistryId, selectedClassId],
    queryFn: () =>
      servantsApi.findAll({
        ministryId: selectedMinistryId ? Number(selectedMinistryId) : undefined,
        classId: selectedClassId ? Number(selectedClassId) : undefined,
      }),
    enabled: isSecretaryOrAdmin && !!selectedClassId,
  });

  const {
    data: students = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['students', selectedMinistryId, selectedClassId, selectedServantId, debouncedSearch, isAllStudentsView],
    queryFn: () =>
      studentsApi.findAll({
        ministryId: selectedMinistryId ? Number(selectedMinistryId) : undefined,
        classId: selectedClassId ? Number(selectedClassId) : undefined,
        servantId: selectedServantId ? Number(selectedServantId) : undefined,
        scope: isAllStudentsView ? 'class' : undefined,
        search: debouncedSearch.trim() || undefined,
      }),
  });

  // Calculate KPIs
  const stats = useMemo(() => {
    const total = students.length;
    const withoutServant = students.filter((s) => !(s.responsibleServantId || s.servantId)).length;
    const active = students.filter((s) => s.status === 'ACTIVE').length;
    const graduated = students.filter((s) => s.status === 'GRADUATED').length;
    return { total, withoutServant, active, graduated };
  }, [students]);

  // Tab Filtering
  const filteredStudents = useMemo(() => {
    return students.filter((s) => {
      if (activeTab === 'WITHOUT_SERVANT' && (!!s.responsibleServantId || !!s.servantId)) return false;
      if (activeTab === 'ACTIVE' && s.status !== 'ACTIVE') return false;
      if (activeTab === 'GRADUATED' && s.status !== 'GRADUATED') return false;
      return true;
    });
  }, [students, activeTab]);

  // Selection handlers
  const handleToggleSelect = (id: number) => {
    setSelectedStudentIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleSelectAll = () => {
    if (selectedStudentIds.length === filteredStudents.length) {
      setSelectedStudentIds([]);
    } else {
      setSelectedStudentIds(filteredStudents.map((s) => s.id));
    }
  };

  // Open modals
  const handleOpenDrawer = (student: StudentResponse | null) => {
    setActiveStudent(student);
    setIsDrawerOpen(true);
  };

  const handleOpenQuickAssign = (target: StudentResponse[]) => {
    setQuickAssignTargetStudents(target);
    setIsQuickAssignOpen(true);
  };

  const handleOpenBatchMove = () => {
    const targets = students.filter((s) => selectedStudentIds.includes(s.id));
    setQuickAssignTargetStudents(targets);
    setIsBatchMoveOpen(true);
  };

  const handleClearContextFilter = () => {
    setSelectedMinistryId(managedMinistryId || '');
    setSelectedClassId(managedClassId || '');
    setSearchParams({});
  };

  const canManagePlacements = isAdmin || isServiceSecretary || isClassSecretary;

  const pageTitle = isServant && !isSecretaryOrAdmin
    ? (isAllStudentsView ? 'جميع مخدومي الفصل' : 'قائمة مخدومي')
    : 'إدارة وتسكين المخدومين';

  const pageSubtitle = isServant && !isSecretaryOrAdmin
    ? (isAllStudentsView
        ? 'عرض ومتابعة وتعديل بيانات جميع مخدومي الفصل الدراسي'
        : 'متابعة المخدومين المسندين إليك للافتقاد والتواصل')
    : 'متابعة قوائم المخدومين، ربطهم بالخدام المسؤولين، وتسكينهم بالفصول الدراسية';

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight">{pageTitle}</h1>
            <span className="text-xs font-bold text-primary-700 bg-primary-50 px-2.5 py-1 rounded-lg border border-primary-100">
              {stats.total} مخدوم
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            {pageSubtitle}
          </p>
        </div>

        <Button
          variant="primary"
          onClick={() => handleOpenDrawer(null)}
          className="shadow-sm font-bold shrink-0 w-full sm:w-auto"
        >
          <UserPlus className="w-4 h-4 ml-2" />
          إضافة مخدوم جديد
        </Button>
      </div>

      {/* Contextual Filter Notice (if opened from class/ministry link) */}
      {(paramClassId || paramMinistryId) && (
        <div className="p-3.5 bg-blue-50/80 rounded-2xl border border-blue-200 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-blue-900">
            <School className="w-4 h-4 text-blue-600" />
            <span>
              يتم الآن عرض مخدومي الفصل المحدد تلقائياً.{' '}
              {selectedClassId && (
                <span className="font-bold">
                  ({classes.find((c) => c.id === selectedClassId)?.name || 'فصل مختار'})
                </span>
              )}
            </span>
          </div>
          <button
            type="button"
            onClick={handleClearContextFilter}
            className="flex items-center gap-1 text-blue-700 hover:text-blue-900 font-bold underline"
          >
            <X className="w-3.5 h-3.5" />
            عرض جميع الفصول
          </button>
        </div>
      )}

      {/* KPI Stats Bar */}
      <div className={`grid ${canManagePlacements ? 'grid-cols-3' : 'grid-cols-2'} gap-2 sm:gap-4`}>
        <div
          onClick={() => setActiveTab('ALL')}
          className={`p-3 sm:p-4 rounded-2xl border transition cursor-pointer ${
            activeTab === 'ALL'
              ? 'bg-white border-primary-500 ring-2 ring-primary-100 shadow-sm'
              : 'bg-white border-gray-100 hover:border-gray-200'
          }`}
        >
          <div className="flex items-center justify-between gap-1">
            <span className="text-[11px] sm:text-xs font-bold text-gray-500 truncate">إجمالي المخدومين</span>
            <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-xl bg-gray-100 text-gray-600 flex items-center justify-center shrink-0">
              <Users className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            </div>
          </div>
          <div className="mt-1.5 sm:mt-2 flex items-baseline gap-1.5">
            <span className="text-xl sm:text-2xl font-black text-gray-900">{stats.total}</span>
            <span className="text-[10px] sm:text-[11px] text-gray-400 font-medium hidden xs:inline">مخدوم</span>
          </div>
        </div>

        {canManagePlacements && (
          <div
            onClick={() => setActiveTab('WITHOUT_SERVANT')}
            className={`p-3 sm:p-4 rounded-2xl border transition cursor-pointer ${
              activeTab === 'WITHOUT_SERVANT'
                ? 'bg-white border-amber-500 ring-2 ring-amber-100 shadow-sm'
                : 'bg-white border-gray-100 hover:border-gray-200'
            }`}
          >
            <div className="flex items-center justify-between gap-1">
              <span className="text-[11px] sm:text-xs font-bold text-amber-700 truncate">بدون خادم</span>
              <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                <UserX className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </div>
            </div>
            <div className="mt-1.5 sm:mt-2 flex items-baseline gap-1.5">
              <span className="text-xl sm:text-2xl font-black text-amber-600">{stats.withoutServant}</span>
              <span className="text-[10px] sm:text-[11px] text-amber-600 font-medium hidden xs:inline">يحتاج تعيين</span>
            </div>
          </div>
        )}

        <div
          onClick={() => setActiveTab('ACTIVE')}
          className={`p-3 sm:p-4 rounded-2xl border transition cursor-pointer ${
            activeTab === 'ACTIVE'
              ? 'bg-white border-emerald-500 ring-2 ring-emerald-100 shadow-sm'
              : 'bg-white border-gray-100 hover:border-gray-200'
          }`}
        >
          <div className="flex items-center justify-between gap-1">
            <span className="text-[11px] sm:text-xs font-bold text-emerald-700 truncate">منتظمون</span>
            <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
              <UserCheck className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            </div>
          </div>
          <div className="mt-1.5 sm:mt-2 flex items-baseline gap-1.5">
            <span className="text-xl sm:text-2xl font-black text-emerald-600">{stats.active}</span>
            <span className="text-[10px] sm:text-[11px] text-emerald-600 font-medium hidden xs:inline">نشط</span>
          </div>
        </div>
      </div>

      {/* Filter Tabs & Search Controls */}
      <div className="bg-white rounded-2xl border border-gray-100 p-4 shadow-sm space-y-4">
        {/* Quick Filter Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 border-b border-gray-100">
          <button
            type="button"
            onClick={() => setActiveTab('ALL')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition shrink-0 ${
              activeTab === 'ALL'
                ? 'bg-primary-600 text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            الكل ({stats.total})
          </button>
          {canManagePlacements && (
            <button
              type="button"
              onClick={() => setActiveTab('WITHOUT_SERVANT')}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1.5 shrink-0 ${
                activeTab === 'WITHOUT_SERVANT'
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'text-amber-700 bg-amber-50 hover:bg-amber-100'
              }`}
            >
              <span>بدون خادم مسؤول</span>
              <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-amber-200/80 text-amber-900">
                {stats.withoutServant}
              </span>
            </button>
          )}
          <button
            type="button"
            onClick={() => setActiveTab('ACTIVE')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition shrink-0 ${
              activeTab === 'ACTIVE'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            منتظمون ({stats.active})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('GRADUATED')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition shrink-0 ${
              activeTab === 'GRADUATED'
                ? 'bg-gray-800 text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            خريجون ({stats.graduated})
          </button>
        </div>

        {/* Dropdowns & Search */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-3">
          <div className="relative flex-1">
            <Input
              placeholder="بحث بالاسم أو هاتف المخدوم أو ولي الأمر..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pr-10"
            />
            <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-gray-400">
              <Search className="w-4 h-4" />
            </div>
          </div>

          {isAdmin && (
            <div className="w-full sm:w-44">
              <Select
                value={selectedMinistryId}
                onChange={(e) => {
                  setSelectedMinistryId(e.target.value ? Number(e.target.value) : '');
                  setSelectedClassId('');
                  setSelectedServantId('');
                }}
                options={ministries.map((m) => ({ value: m.id, label: m.name }))}
                placeholder="كل الخدمات"
              />
            </div>
          )}

          {(isAdmin || isServiceSecretary) && (
            <div className="w-full sm:w-44">
              <Select
                value={selectedClassId}
                onChange={(e) => {
                  setSelectedClassId(e.target.value ? Number(e.target.value) : '');
                  setSelectedServantId('');
                }}
                options={classes.map((c) => ({ value: c.id, label: c.name }))}
                placeholder="كل الفصول"
                disabled={!selectedMinistryId && isAdmin}
              />
            </div>
          )}

          {isSecretaryOrAdmin && (
            <div className="w-full sm:w-48">
              <Select
                value={selectedServantId}
                onChange={(e) => setSelectedServantId(e.target.value ? Number(e.target.value) : '')}
                options={servants.map((s) => ({
                  value: s.personId ?? s.id,
                  label: s.fullName + (s.isClassSecretary ? ' (أمين الفصل)' : ''),
                }))}
                placeholder="كل الخدام"
                disabled={!selectedClassId}
              />
            </div>
          )}

          {(search || selectedMinistryId || selectedClassId || selectedServantId) && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setSearch('');
                setSelectedMinistryId(managedMinistryId || '');
                setSelectedClassId(managedClassId || '');
                setSelectedServantId('');
                setActiveTab('ALL');
              }}
              className="shrink-0 text-gray-500"
            >
              إعادة ضبط
            </Button>
          )}
        </div>
      </div>

      {/* Floating Batch Action Bar (Only for managers and secretaries) */}
      {canManagePlacements && selectedStudentIds.length > 0 && (
        <div className="sticky top-4 z-20 bg-gray-900 text-white p-3.5 rounded-2xl shadow-xl flex items-center justify-between gap-4 animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <span className="w-7 h-7 rounded-xl bg-primary-500 text-white flex items-center justify-center font-bold text-xs">
              {selectedStudentIds.length}
            </span>
            <span className="text-xs font-bold">تم تحديد {selectedStudentIds.length} مخدومين</span>
          </div>

          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="primary"
              onClick={() => {
                const targets = students.filter((s) => selectedStudentIds.includes(s.id));
                handleOpenQuickAssign(targets);
              }}
              className="font-bold text-xs shadow-sm"
            >
              <UserCheck className="w-3.5 h-3.5 ml-1.5" />
              تعيين خادم مسؤول
            </Button>

            {(isAdmin || isServiceSecretary) && (
              <Button
                size="sm"
                variant="outline"
                onClick={handleOpenBatchMove}
                className="font-bold text-xs text-white border-gray-700 hover:bg-gray-800"
              >
                <ArrowRightLeft className="w-3.5 h-3.5 ml-1.5" />
                نقل إلى فصل
              </Button>
            )}

            <button
              type="button"
              onClick={() => setSelectedStudentIds([])}
              className="p-1.5 rounded-lg hover:bg-gray-800 text-gray-400 hover:text-white transition"
              title="إلغاء التحديد"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Content: Table for Desktop, Cards for Mobile */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل بيانات المخدومين...</p>
        </div>
      ) : filteredStudents.length === 0 ? (
        <EmptyState
          title="لا يوجد مخدومين مطابقين"
          description="لم يتم العثور على أي مخدوم مطابق لخيارات البحث أو التصفية الحالية."
          actionLabel={canManagePlacements ? 'إضافة مخدوم جديد' : undefined}
          onAction={canManagePlacements ? () => handleOpenDrawer(null) : undefined}
          icon={Users}
        />
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="hidden lg:block bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
            <table className="w-full text-right divide-y divide-gray-100 text-sm">
              <thead className="bg-gray-50/75 text-gray-500 font-bold text-xs uppercase">
                <tr>
                  {canManagePlacements && (
                    <th className="px-4 py-4 w-10 text-center">
                      <button
                        type="button"
                        onClick={handleSelectAll}
                        className="text-gray-400 hover:text-primary-600 transition"
                        title={
                          selectedStudentIds.length === filteredStudents.length
                            ? 'إلغاء تحديد الكل'
                            : 'تحديد الكل'
                        }
                      >
                        {selectedStudentIds.length === filteredStudents.length &&
                        filteredStudents.length > 0 ? (
                          <CheckSquare className="w-4 h-4 text-primary-600" />
                        ) : (
                          <Square className="w-4 h-4" />
                        )}
                      </button>
                    </th>
                  )}
                  <th className="px-5 py-4">المخدوم</th>
                  <th className="px-5 py-4">الهاتف وولي الأمر</th>
                  <th className="px-5 py-4">الخدمة والفصل</th>
                  <th className="px-5 py-4">الخادم المسؤول عن الافتقاد</th>
                  <th className="px-5 py-4">الحالة</th>
                  <th className="px-5 py-4 text-center">الإجراءات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredStudents.map((st) => {
                  const isSelected = selectedStudentIds.includes(st.id);
                  return (
                    <tr
                      key={st.id}
                      onClick={() => handleOpenDrawer(st)}
                      className={`cursor-pointer transition ${
                        isSelected ? 'bg-primary-50/40' : 'hover:bg-gray-50/70'
                      }`}
                    >
                      {canManagePlacements && (
                        <td
                          className="px-4 py-4 text-center"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleSelect(st.id);
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => handleToggleSelect(st.id)}
                            className="rounded text-primary-600 focus:ring-primary-500 cursor-pointer"
                          />
                        </td>
                      )}

                      <td className="px-5 py-4 font-bold text-gray-900 flex items-center gap-3">
                        <div className="w-8 h-8 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs shrink-0">
                          {st.fullName.charAt(0)}
                        </div>
                        <div>
                          <span className="block">{st.fullName}</span>
                          <span className="text-[11px] text-gray-400 font-normal">
                            {GENDER_LABELS[st.gender]}
                            {st.confessionFather ? ` &bull; أب الاعتراف: ${st.confessionFather}` : ''}
                          </span>
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span className="font-mono text-xs font-semibold text-gray-800 block" dir="ltr">
                          {st.phone}
                        </span>
                        {st.guardianPhone && (
                          <span className="font-mono text-[11px] text-gray-400 block" dir="ltr">
                            ولي الأمر: {st.guardianPhone}
                          </span>
                        )}
                      </td>

                      <td className="px-5 py-4">
                        <div>
                          <span className="font-bold text-gray-800 block text-xs">{st.className || '-'}</span>
                          <span className="text-[11px] text-gray-400">{st.ministryName}</span>
                        </div>
                      </td>

                      <td
                        className="px-5 py-4"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {(st.responsibleServantName || st.servantName) ? (
                          <div className="flex items-center justify-between gap-2 bg-gray-50 hover:bg-primary-50/50 p-1.5 px-2.5 rounded-xl border border-gray-100 transition">
                            <span className="text-xs font-bold text-gray-800 flex items-center gap-1.5 truncate">
                              <UserCheck className="w-3.5 h-3.5 text-primary-600 shrink-0" />
                              {st.responsibleServantName || st.servantName}
                            </span>
                            {canManagePlacements && (
                              <button
                                type="button"
                                onClick={() => handleOpenQuickAssign([st])}
                                className="text-[11px] font-bold text-primary-600 hover:text-primary-800 p-1 hover:bg-white rounded-lg transition"
                                title="تغيير الخادم المسؤول"
                              >
                                تغيير
                              </button>
                            )}
                          </div>
                        ) : (
                          <div>
                            {canManagePlacements ? (
                              <button
                                type="button"
                                onClick={() => handleOpenQuickAssign([st])}
                                className="px-2.5 py-1 rounded-xl text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 hover:bg-amber-100 transition flex items-center gap-1"
                              >
                                <UserPlus className="w-3.5 h-3.5 text-amber-600" />
                                <span>+ تعيين خادم مسؤول</span>
                              </button>
                            ) : (
                              <span className="text-amber-600 bg-amber-50 px-2 py-0.5 rounded-lg text-xs font-semibold">
                                بدون خادم
                              </span>
                            )}
                          </div>
                        )}
                      </td>

                      <td className="px-5 py-4">
                        <Badge variant={st.status === 'ACTIVE' ? 'success' : 'warning'}>
                          {STUDENT_STATUS_LABELS[st.status]}
                        </Badge>
                      </td>

                      <td className="px-5 py-4 text-center">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenDrawer(st);
                          }}
                          className="text-primary-600 hover:text-primary-800 font-bold text-xs"
                        >
                          عرض وتعديل
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards List */}
          <div className="lg:hidden space-y-3">
            {filteredStudents.map((st) => {
              const isSelected = selectedStudentIds.includes(st.id);
              return (
                <div
                  key={st.id}
                  onClick={() => handleOpenDrawer(st)}
                  className={`bg-white rounded-2xl p-4 border transition cursor-pointer ${
                    isSelected
                      ? 'border-primary-500 bg-primary-50/20 shadow-sm'
                      : 'border-gray-100 shadow-sm active:bg-gray-50'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3">
                      {canManagePlacements && (
                        <div
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleSelect(st.id);
                          }}
                          className="p-1"
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => handleToggleSelect(st.id)}
                            className="rounded text-primary-600 focus:ring-primary-500 cursor-pointer"
                          />
                        </div>
                      )}
                      <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-sm">
                        {st.fullName.charAt(0)}
                      </div>
                      <div>
                        <h4 className="font-bold text-gray-900 text-sm">{st.fullName}</h4>
                        <span className="text-xs text-gray-500 font-mono" dir="ltr">
                          {st.phone}
                        </span>
                      </div>
                    </div>
                    <Badge variant={st.status === 'ACTIVE' ? 'success' : 'warning'}>
                      {STUDENT_STATUS_LABELS[st.status]}
                    </Badge>
                  </div>

                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-50 text-xs text-gray-600">
                    <div className="flex items-center gap-1.5 truncate">
                      <School className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{st.className || '-'}</span>
                    </div>
                    <div className="flex items-center gap-1.5 truncate">
                      <UserCheck className="w-3.5 h-3.5 text-primary-500 shrink-0" />
                      <span>{st.responsibleServantName || st.servantName || 'بدون خادم'}</span>
                    </div>
                  </div>

                  {canManagePlacements && !(st.responsibleServantName || st.servantName) && (
                    <div className="mt-2 pt-2 border-t border-gray-50 flex justify-end">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenQuickAssign([st]);
                        }}
                        className="text-xs font-bold text-amber-700 bg-amber-50 px-2.5 py-1 rounded-lg border border-amber-200"
                      >
                        + تعيين خادم مسؤول
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="text-xs text-gray-400 text-left pt-2 font-medium">
            يتم عرض {filteredStudents.length} من إجمالي {stats.total} مخدوم
          </div>
        </>
      )}

      {/* Student Drawer */}
      <StudentDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        student={activeStudent}
        onSaved={refetch}
        preselectedMinistryId={selectedMinistryId ? Number(selectedMinistryId) : null}
        preselectedClassId={selectedClassId ? Number(selectedClassId) : null}
      />

      {/* Quick Assign Modal (single or batch) */}
      <QuickAssignModal
        isOpen={isQuickAssignOpen}
        onClose={() => {
          setIsQuickAssignOpen(false);
          setQuickAssignTargetStudents([]);
        }}
        students={quickAssignTargetStudents}
        onSuccess={() => {
          setSelectedStudentIds([]);
          refetch();
        }}
      />

      {/* Batch Move Modal */}
      <BatchMoveModal
        isOpen={isBatchMoveOpen}
        onClose={() => {
          setIsBatchMoveOpen(false);
          setQuickAssignTargetStudents([]);
        }}
        students={quickAssignTargetStudents}
        onSuccess={() => {
          setSelectedStudentIds([]);
          refetch();
        }}
      />
    </div>
  );
};
