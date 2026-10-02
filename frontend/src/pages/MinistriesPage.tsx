import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ministriesApi } from '../api/ministries.api';
import { classesApi } from '../api/classes.api';
import { MinistryResponse, GradeClassResponse } from '../types/ministry.types';
import { usePermissions } from '../auth/usePermissions';
import { AssignSecretaryModal } from '../components/ministries/AssignSecretaryModal';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Drawer } from '../components/ui/Drawer';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { Spinner } from '../components/ui/Spinner';
import { EmptyState } from '../components/ui/EmptyState';
import {
  Building2,
  Plus,
  Edit3,
  School,
  ChevronDown,
  ChevronUp,
  UserCheck,
  Phone,
  AlertTriangle,
  Search,
  ExternalLink,
  Table as TableIcon,
  LayoutGrid,
  Layers,
} from 'lucide-react';

export const MinistriesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { isAdmin, isServiceSecretary, managedMinistryId } = usePermissions();

  const [activeTab, setActiveTab] = useState<'cards' | 'classesTable'>('cards');
  const [filterType, setFilterType] = useState<'all' | 'needsSecretary' | 'active'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedMinistryIds, setExpandedMinistryIds] = useState<number[]>([]);

  // Ministry modal states
  const [isMinistryDrawerOpen, setIsMinistryDrawerOpen] = useState(false);
  const [editingMinistry, setEditingMinistry] = useState<MinistryResponse | null>(null);
  const [ministryName, setMinistryName] = useState('');
  const [ministryError, setMinistryError] = useState<string | null>(null);

  // Contextual Class modal states
  const [isClassDrawerOpen, setIsClassDrawerOpen] = useState(false);
  const [editingClass, setEditingClass] = useState<GradeClassResponse | null>(null);
  const [targetMinistryForClass, setTargetMinistryForClass] = useState<MinistryResponse | null>(null);
  const [className, setClassName] = useState('');
  const [classSortOrder, setClassSortOrder] = useState<number>(0);
  const [classError, setClassError] = useState<string | null>(null);

  // Secretary Assignment Modal states
  const [secretaryModalState, setSecretaryModalState] = useState<{
    isOpen: boolean;
    targetType: 'ministry' | 'class';
    targetId: number;
    targetName: string;
    currentSecretary: { id?: number | null; name?: string | null; phone?: string | null } | null;
    ministryIdForClass?: number;
  }>({
    isOpen: false,
    targetType: 'ministry',
    targetId: 0,
    targetName: '',
    currentSecretary: null,
  });

  // Query ministries
  const { data: allMinistries = [], isLoading: isMinistriesLoading } = useQuery({
    queryKey: ['ministries', 'all'],
    queryFn: () => ministriesApi.findAll(true),
  });

  // Query all classes
  const { data: allClasses = [], isLoading: isClassesLoading } = useQuery({
    queryKey: ['classes', 'all'],
    queryFn: () => classesApi.findAll(),
  });

  // Filter ministries for role and search
  const visibleMinistries = useMemo(() => {
    let list = allMinistries;

    // Scope for Service Secretary
    if (isServiceSecretary && !isAdmin && managedMinistryId) {
      list = list.filter((m) => m.id === managedMinistryId);
    }

    // Filter by type
    if (filterType === 'needsSecretary') {
      list = list.filter((m) => !m.secretaryName);
    } else if (filterType === 'active') {
      list = list.filter((m) => m.active);
    }

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      list = list.filter((m) => m.name.toLowerCase().includes(q));
    }

    return list;
  }, [allMinistries, isServiceSecretary, isAdmin, managedMinistryId, filterType, searchQuery]);

  // Overall counts for summary
  const summaryStats = useMemo(() => {
    const totalMinistries = allMinistries.length;
    const withSecretary = allMinistries.filter((m) => !!m.secretaryName).length;
    const withoutSecretary = totalMinistries - withSecretary;
    const totalClasses = allClasses.length;
    const totalServants = allMinistries.reduce((sum, m) => sum + (m.servantsCount || 0), 0);
    const totalStudents = allMinistries.reduce((sum, m) => sum + (m.studentsCount || 0), 0);

    return {
      totalMinistries,
      withSecretary,
      withoutSecretary,
      totalClasses,
      totalServants,
      totalStudents,
    };
  }, [allMinistries, allClasses]);

  // Expand all by default when service secretary
  React.useEffect(() => {
    if (isServiceSecretary && !isAdmin && managedMinistryId) {
      setExpandedMinistryIds([managedMinistryId]);
    }
  }, [isServiceSecretary, isAdmin, managedMinistryId]);

  const toggleExpandMinistry = (id: number) => {
    setExpandedMinistryIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  // Ministry Mutations
  const ministryMutation = useMutation({
    mutationFn: async () => {
      if (editingMinistry) {
        return ministriesApi.update(editingMinistry.id, { name: ministryName.trim() });
      } else {
        return ministriesApi.create({ name: ministryName.trim() });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ministries'] });
      setIsMinistryDrawerOpen(false);
    },
    onError: (err: Error) => {
      setMinistryError(err.message || 'فشل حفظ الخدمة');
    },
  });

  // Class Mutations
  const classMutation = useMutation({
    mutationFn: async () => {
      if (editingClass) {
        return classesApi.update(editingClass.id, {
          name: className.trim(),
          ministryId: editingClass.ministryId,
          sortOrder: classSortOrder,
        });
      } else {
        return classesApi.create({
          name: className.trim(),
          ministryId: targetMinistryForClass!.id,
          sortOrder: classSortOrder,
        });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      queryClient.invalidateQueries({ queryKey: ['ministries'] });
      setIsClassDrawerOpen(false);
    },
    onError: (err: Error) => {
      setClassError(err.message || 'فشل حفظ الفصل');
    },
  });

  const handleOpenMinistryDrawer = (ministry: MinistryResponse | null) => {
    setEditingMinistry(ministry);
    setMinistryName(ministry ? ministry.name : '');
    setMinistryError(null);
    setIsMinistryDrawerOpen(true);
  };

  const handleOpenClassDrawer = (
    ministry: MinistryResponse,
    gradeClass: GradeClassResponse | null
  ) => {
    setTargetMinistryForClass(ministry);
    setEditingClass(gradeClass);
    setClassName(gradeClass ? gradeClass.name : '');
    setClassSortOrder(gradeClass ? gradeClass.sortOrder : 0);
    setClassError(null);
    setIsClassDrawerOpen(true);
  };

  const handleOpenAssignSecretaryModal = (
    targetType: 'ministry' | 'class',
    targetId: number,
    targetName: string,
    currentSecretary: { id?: number | null; name?: string | null; phone?: string | null } | null,
    ministryIdForClass?: number
  ) => {
    setSecretaryModalState({
      isOpen: true,
      targetType,
      targetId,
      targetName,
      currentSecretary,
      ministryIdForClass,
    });
  };

  const handleSecretarySuccess = () => {
    queryClient.invalidateQueries({ queryKey: ['ministries'] });
    queryClient.invalidateQueries({ queryKey: ['classes'] });
    queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
  };

  // Filter classes for the flat table
  const tableFilteredClasses = useMemo(() => {
    let list = allClasses;
    if (isServiceSecretary && !isAdmin && managedMinistryId) {
      list = list.filter((c) => c.ministryId === managedMinistryId);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      list = list.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          c.ministryName.toLowerCase().includes(q) ||
          (c.secretaryName && c.secretaryName.toLowerCase().includes(q))
      );
    }
    return list;
  }, [allClasses, isServiceSecretary, isAdmin, managedMinistryId, searchQuery]);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight">
              {isServiceSecretary && !isAdmin ? 'خدمتي وفصولها' : 'الخدمات والفصول'}
            </h1>
            {isServiceSecretary && !isAdmin && (
              <Badge variant="primary" className="font-bold">
                أمين خدمة
              </Badge>
            )}
          </div>
          <p className="text-xs text-gray-500 mt-1">
            {isServiceSecretary && !isAdmin
              ? 'إدارة فصول ومساعدي خدمتك ومتابعة تسكين الخدام والمخدومين'
              : 'إدارة الهيكل التنظيمي للخدمات الكنسية وتعيين أمناء الخدمات والفصول'}
          </p>
        </div>

        {isAdmin && (
          <Button
            variant="primary"
            onClick={() => handleOpenMinistryDrawer(null)}
            className="shadow-sm font-bold self-start sm:self-auto"
          >
            <Plus className="w-4 h-4 ml-2" />
            إضافة خدمة جديدة
          </Button>
        )}
      </div>

      {/* Admin KPI Summary Bar */}
      {isAdmin && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
            <span className="text-[11px] text-gray-400 font-bold block">إجمالي الخدمات</span>
            <span className="text-xl font-black text-gray-900 mt-0.5 block">
              {summaryStats.totalMinistries}
            </span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
            <span className="text-[11px] text-emerald-600 font-bold block">مكتملة بأمين خدمة</span>
            <span className="text-xl font-black text-emerald-700 mt-0.5 block">
              {summaryStats.withSecretary}
            </span>
          </div>

          <div
            onClick={() => setFilterType('needsSecretary')}
            className={`p-3.5 rounded-xl border shadow-sm transition cursor-pointer ${
              summaryStats.withoutSecretary > 0
                ? 'bg-amber-50/70 border-amber-200 hover:bg-amber-100/60'
                : 'bg-white border-gray-100'
            }`}
          >
            <span className="text-[11px] text-amber-700 font-bold flex items-center gap-1">
              <AlertTriangle className="w-3 h-3" />
              تحتاج أمين خدمة
            </span>
            <span className="text-xl font-black text-amber-800 mt-0.5 block">
              {summaryStats.withoutSecretary}
            </span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
            <span className="text-[11px] text-gray-400 font-bold block">إجمالي الفصول</span>
            <span className="text-xl font-black text-gray-900 mt-0.5 block">
              {summaryStats.totalClasses}
            </span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
            <span className="text-[11px] text-blue-600 font-bold block">إجمالي الخدام</span>
            <span className="text-xl font-black text-blue-700 mt-0.5 block">
              {summaryStats.totalServants}
            </span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
            <span className="text-[11px] text-purple-600 font-bold block">إجمالي المخدومين</span>
            <span className="text-xl font-black text-purple-700 mt-0.5 block">
              {summaryStats.totalStudents}
            </span>
          </div>
        </div>
      )}

      {/* Controls: Search, Filter Tabs & View Mode */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-gray-100 shadow-sm">
        {/* Search */}
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-gray-400 absolute right-3 top-2.5" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="بحث عن خدمة أو فصل أو أمين..."
            className="pr-9 text-xs"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Filter Pills (for Admins) */}
          {isAdmin && (
            <div className="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-100">
              <button
                onClick={() => setFilterType('all')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                  filterType === 'all'
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                الكل ({allMinistries.length})
              </button>
              <button
                onClick={() => setFilterType('needsSecretary')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                  filterType === 'needsSecretary'
                    ? 'bg-white text-amber-700 shadow-sm'
                    : 'text-gray-500 hover:text-amber-700'
                }`}
              >
                بدون أمين ({summaryStats.withoutSecretary})
              </button>
              <button
                onClick={() => setFilterType('active')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                  filterType === 'active'
                    ? 'bg-white text-emerald-700 shadow-sm'
                    : 'text-gray-500 hover:text-emerald-700'
                }`}
              >
                النشطة
              </button>
            </div>
          )}

          {/* View Mode Toggle */}
          <div className="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-100">
            <button
              onClick={() => setActiveTab('cards')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                activeTab === 'cards'
                  ? 'bg-white text-primary-700 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              عرض الخدمات
            </button>
            <button
              onClick={() => setActiveTab('classesTable')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                activeTab === 'classesTable'
                  ? 'bg-white text-primary-700 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <TableIcon className="w-3.5 h-3.5" />
              جدول الفصول
            </button>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      {isMinistriesLoading || isClassesLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل بيانات الخدمات والفصول...</p>
        </div>
      ) : activeTab === 'cards' ? (
        /* Card View */
        visibleMinistries.length === 0 ? (
          <EmptyState
            icon={Building2}
            title="لا توجد خدمات مطابقة"
            description="لم يتم العثور على أي خدمات مسجلة مطابقة لمعايير البحث أو التصفية الحالية."
            actionLabel={isAdmin ? 'إضافة خدمة جديدة' : undefined}
            onAction={isAdmin ? () => handleOpenMinistryDrawer(null) : undefined}
            actionIcon={Plus}
          />
        ) : (
          <div className="space-y-4">
            {visibleMinistries.map((m) => {
              const isExpanded = expandedMinistryIds.includes(m.id);
              const ministryClasses = allClasses.filter((c) => c.ministryId === m.id);

              return (
                <Card
                  key={m.id}
                  className="p-0 overflow-hidden border-gray-200/80 shadow-sm hover:border-gray-300 transition"
                >
                  {/* Ministry Header Card */}
                  <div className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white">
                    {/* Right: Service Identity & Secretary Status */}
                    <div className="flex items-start gap-4">
                      <div className="w-12 h-12 rounded-2xl bg-primary-50 text-primary-700 flex items-center justify-center font-black text-lg flex-shrink-0">
                        <Building2 className="w-6 h-6" />
                      </div>

                      <div className="space-y-2">
                        <div className="flex items-center gap-2.5">
                          <h3 className="font-black text-gray-900 text-lg">{m.name}</h3>
                          <Badge variant={m.active ? 'success' : 'neutral'}>
                            {m.active ? 'نشطة' : 'غير نشطة'}
                          </Badge>
                        </div>

                        {/* Service Secretary Box */}
                        <div className="flex items-center gap-3">
                          {m.secretaryName ? (
                            <div className="inline-flex items-center gap-2 bg-emerald-50 border border-emerald-100 px-3 py-1.5 rounded-xl">
                              <UserCheck className="w-4 h-4 text-emerald-700" />
                              <span className="text-xs font-bold text-emerald-900">
                                أمين الخدمة: {m.secretaryName}
                              </span>
                              {m.secretaryPhone && (
                                <a
                                  href={`tel:${m.secretaryPhone}`}
                                  className="text-[11px] text-emerald-700 font-mono hover:underline flex items-center gap-1 mr-1"
                                  dir="ltr"
                                >
                                  <Phone className="w-3 h-3" />
                                  {m.secretaryPhone}
                                </a>
                              )}
                              {isAdmin && (
                                <button
                                  onClick={() =>
                                    handleOpenAssignSecretaryModal(
                                      'ministry',
                                      m.id,
                                      m.name,
                                      {
                                        id: m.secretaryPersonId,
                                        name: m.secretaryName,
                                        phone: m.secretaryPhone,
                                      }
                                    )
                                  }
                                  className="text-[10px] text-emerald-800 hover:text-emerald-950 font-bold underline mr-2"
                                >
                                  تغيير
                                </button>
                              )}
                            </div>
                          ) : (
                            <div className="inline-flex items-center gap-2 bg-amber-50 border border-amber-200 px-3 py-1.5 rounded-xl">
                              <AlertTriangle className="w-4 h-4 text-amber-600" />
                              <span className="text-xs font-bold text-amber-800">
                                لا يوجد أمين خدمة معين
                              </span>
                              {isAdmin && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() =>
                                    handleOpenAssignSecretaryModal(
                                      'ministry',
                                      m.id,
                                      m.name,
                                      null
                                    )
                                  }
                                  className="text-xs font-bold text-amber-800 border-amber-300 hover:bg-amber-100 py-0.5 px-2 h-auto"
                                >
                                  + تعيين أمين خدمة الآن
                                </Button>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Left: Metrics & Action Buttons */}
                    <div className="flex flex-wrap items-center gap-3">
                      {/* KPI Counters */}
                      <div className="flex items-center gap-2 bg-gray-50 p-1.5 rounded-xl border border-gray-100 text-xs">
                        <span className="px-2.5 py-1 rounded-lg bg-white font-bold text-gray-700 shadow-2xs">
                          {m.classesCount || ministryClasses.length} فصول
                        </span>
                        <span className="px-2.5 py-1 rounded-lg bg-white font-bold text-blue-700 shadow-2xs">
                          {m.servantsCount || 0} خدام
                        </span>
                        <span className="px-2.5 py-1 rounded-lg bg-white font-bold text-purple-700 shadow-2xs">
                          {m.studentsCount || 0} مخدومين
                        </span>
                      </div>

                      {/* Direct Contextual Class Creation */}
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleOpenClassDrawer(m, null)}
                        className="font-bold text-xs"
                      >
                        <Plus className="w-3.5 h-3.5 ml-1" />
                        إضافة فصل
                      </Button>

                      {isAdmin && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenMinistryDrawer(m)}
                        >
                          <Edit3 className="w-3.5 h-3.5 ml-1" />
                          تعديل
                        </Button>
                      )}

                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => toggleExpandMinistry(m.id)}
                        className="text-gray-500 hover:text-gray-800 font-bold text-xs"
                      >
                        {isExpanded ? (
                          <>
                            طي الفصول
                            <ChevronUp className="w-4 h-4 mr-1" />
                          </>
                        ) : (
                          <>
                            عرض الفصول ({m.classesCount || ministryClasses.length})
                            <ChevronDown className="w-4 h-4 mr-1" />
                          </>
                        )}
                      </Button>
                    </div>
                  </div>

                  {/* Nested Classes Section */}
                  {isExpanded && (
                    <div className="border-t border-gray-100 bg-gray-50/50 p-5 space-y-4">
                      <div className="flex items-center justify-between">
                        <h4 className="text-xs font-black text-gray-600 flex items-center gap-1.5">
                          <School className="w-4 h-4 text-primary-600" />
                          فصول خدمة {m.name} ({ministryClasses.length})
                        </h4>
                        <span className="text-[11px] text-gray-400">
                          انقر على أي فصل لاستعراض المخدومين أو الخدام أو تعيين أمين الفصل
                        </span>
                      </div>

                      {ministryClasses.length === 0 ? (
                        <div className="text-center py-8 bg-white rounded-2xl border border-dashed border-gray-200">
                          <School className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                          <p className="text-xs font-bold text-gray-700">لا توجد فصول مسجلة في هذه الخدمة بعد</p>
                          <p className="text-[11px] text-gray-400 mt-0.5 mb-3">
                            أضف أول فصل دراسي لهذه الخدمة (مثل: أولى ابتدائي، ثانية ابتدائي)
                          </p>
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => handleOpenClassDrawer(m, null)}
                          >
                            <Plus className="w-3.5 h-3.5 ml-1" />
                            إضافة أول فصل
                          </Button>
                        </div>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
                          {ministryClasses.map((c) => (
                            <div
                              key={c.id}
                              className="bg-white p-4 rounded-2xl border border-gray-100 shadow-2xs hover:shadow-sm transition space-y-3"
                            >
                              {/* Class Title & Edit */}
                              <div className="flex items-start justify-between">
                                <div>
                                  <h5 className="font-bold text-gray-900 text-sm">{c.name}</h5>
                                  <span className="text-[10px] text-gray-400 font-mono">
                                    الترتيب: {c.sortOrder}
                                  </span>
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleOpenClassDrawer(m, c)}
                                  className="text-gray-400 hover:text-gray-700 p-1 h-auto"
                                >
                                  <Edit3 className="w-3.5 h-3.5" />
                                </Button>
                              </div>

                              {/* Class Secretary Info */}
                              <div className="p-2.5 rounded-xl bg-gray-50 border border-gray-100 flex items-center justify-between">
                                {c.secretaryName ? (
                                  <div className="flex items-center gap-2">
                                    <div className="w-6 h-6 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold text-[10px]">
                                      {c.secretaryName.charAt(0)}
                                    </div>
                                    <div>
                                      <span className="text-[11px] font-bold text-gray-800 block">
                                        {c.secretaryName}
                                      </span>
                                      {c.secretaryPhone && (
                                        <span className="text-[10px] text-gray-500 font-mono" dir="ltr">
                                          {c.secretaryPhone}
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                ) : (
                                  <span className="text-[11px] font-bold text-amber-700 flex items-center gap-1">
                                    <AlertTriangle className="w-3 h-3 text-amber-500" />
                                    بدون أمين فصل
                                  </span>
                                )}

                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() =>
                                    handleOpenAssignSecretaryModal(
                                      'class',
                                      c.id,
                                      c.name,
                                      {
                                        id: c.secretaryPersonId,
                                        name: c.secretaryName,
                                        phone: c.secretaryPhone,
                                      },
                                      m.id
                                    )
                                  }
                                  className="text-[10px] font-bold py-0.5 px-2 h-auto"
                                >
                                  {c.secretaryName ? 'تغيير' : '+ تعيين أمين'}
                                </Button>
                              </div>

                              {/* Class Metrics & Navigation Buttons */}
                              <div className="flex items-center justify-between pt-1 border-t border-gray-50">
                                <div className="flex items-center gap-2 text-[11px] font-semibold text-gray-500">
                                  <span>{c.servantsCount || 0} خدام</span>
                                  <span>•</span>
                                  <span>{c.studentsCount || 0} مخدومين</span>
                                </div>

                                <div className="flex items-center gap-1.5">
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={() => navigate(`/students?ministryId=${m.id}&classId=${c.id}`)}
                                    className="text-[11px] font-bold text-primary-700 hover:text-primary-900 py-1 px-2 h-auto"
                                  >
                                    المخدومين
                                    <ExternalLink className="w-3 h-3 mr-1" />
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={() => navigate(`/servants?ministryId=${m.id}&classId=${c.id}`)}
                                    className="text-[11px] font-bold text-blue-700 hover:text-blue-900 py-1 px-2 h-auto"
                                  >
                                    الخدام
                                    <ExternalLink className="w-3 h-3 mr-1" />
                                  </Button>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </Card>
              );
            })}
          </div>
        )
      ) : (
        /* Standalone Flat Classes Table View */
        <Card className="p-0 overflow-hidden border-gray-200 shadow-sm">
          <div className="p-4 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
            <h3 className="font-bold text-gray-900 text-sm flex items-center gap-2">
              <Layers className="w-4 h-4 text-primary-600" />
              جميع الفصول المسجلة في النظام ({tableFilteredClasses.length})
            </h3>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100">
                <tr>
                  <th className="py-3 px-4">اسم الفصل</th>
                  <th className="py-3 px-4">الخدمة التابعة</th>
                  <th className="py-3 px-4">أمين الفصل</th>
                  <th className="py-3 px-4 text-center">الخدام</th>
                  <th className="py-3 px-4 text-center">المخدومين</th>
                  <th className="py-3 px-4 text-center">الإجراءات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {tableFilteredClasses.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-gray-400">
                      لا توجد فصول مطابقة لمعايير البحث
                    </td>
                  </tr>
                ) : (
                  tableFilteredClasses.map((c) => (
                    <tr key={c.id} className="hover:bg-gray-50/60 transition">
                      <td className="py-3.5 px-4 font-bold text-gray-900">{c.name}</td>
                      <td className="py-3.5 px-4">
                        <Badge variant="primary" className="text-[11px]">
                          {c.ministryName}
                        </Badge>
                      </td>
                      <td className="py-3.5 px-4">
                        {c.secretaryName ? (
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-gray-800">{c.secretaryName}</span>
                            {c.secretaryPhone && (
                              <span className="text-[10px] text-gray-400 font-mono" dir="ltr">
                                ({c.secretaryPhone})
                              </span>
                            )}
                            <button
                              onClick={() =>
                                handleOpenAssignSecretaryModal(
                                  'class',
                                  c.id,
                                  c.name,
                                  {
                                    id: c.secretaryPersonId,
                                    name: c.secretaryName,
                                    phone: c.secretaryPhone,
                                  },
                                  c.ministryId
                                )
                              }
                              className="text-[10px] text-primary-600 hover:underline font-bold"
                            >
                              تغيير
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() =>
                              handleOpenAssignSecretaryModal(
                                'class',
                                c.id,
                                c.name,
                                null,
                                c.ministryId
                              )
                            }
                            className="text-[11px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200 px-2 py-0.5 rounded-md"
                          >
                            + تعيين أمين فصل
                          </button>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-center font-bold text-blue-700">
                        {c.servantsCount || 0}
                      </td>
                      <td className="py-3.5 px-4 text-center font-bold text-purple-700">
                        {c.studentsCount || 0}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              navigate(`/students?ministryId=${c.ministryId}&classId=${c.id}`)
                            }
                            className="text-[11px] font-bold text-primary-700 hover:text-primary-900 py-1 px-2 h-auto"
                          >
                            المخدومين
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              navigate(`/servants?ministryId=${c.ministryId}&classId=${c.id}`)
                            }
                            className="text-[11px] font-bold text-blue-700 hover:text-blue-900 py-1 px-2 h-auto"
                          >
                            الخدام
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Ministry Drawer (Create/Edit) */}
      <Drawer
        isOpen={isMinistryDrawerOpen}
        onClose={() => setIsMinistryDrawerOpen(false)}
        title={editingMinistry ? 'تعديل اسم الخدمة' : 'إضافة خدمة كنسية جديدة'}
        footer={
          <div className="flex items-center justify-between w-full">
            <Button
              variant="outline"
              onClick={() => setIsMinistryDrawerOpen(false)}
              disabled={ministryMutation.isPending}
            >
              إلغاء
            </Button>
            <Button
              variant="primary"
              onClick={() => ministryMutation.mutate()}
              isLoading={ministryMutation.isPending}
              className="font-bold"
            >
              حفظ الخدمة
            </Button>
          </div>
        }
      >
        {ministryError && <Alert variant="error">{ministryError}</Alert>}
        <div className="space-y-4">
          <Input
            label="اسم الخدمة *"
            value={ministryName}
            onChange={(e) => setMinistryName(e.target.value)}
            placeholder="مثال: ابتدائي، إعدادي، ثانوي، خريجين..."
            required
          />
          <p className="text-[11px] text-gray-500">
            يمثل اسم الخدمة المرحلة الكنسية العامة. بعد إنشاء الخدمة، يمكنك إضافة الفصول والأسر التابعة لها وتعيين أمين الخدمة.
          </p>
        </div>
      </Drawer>

      {/* Contextual Class Drawer (Create/Edit) */}
      <Drawer
        isOpen={isClassDrawerOpen}
        onClose={() => setIsClassDrawerOpen(false)}
        title={
          editingClass
            ? `تعديل بيانات فصل: ${editingClass.name}`
            : `إضافة فصل جديد لخدمة: ${targetMinistryForClass?.name || ''}`
        }
        footer={
          <div className="flex items-center justify-between w-full">
            <Button
              variant="outline"
              onClick={() => setIsClassDrawerOpen(false)}
              disabled={classMutation.isPending}
            >
              إلغاء
            </Button>
            <Button
              variant="primary"
              onClick={() => classMutation.mutate()}
              isLoading={classMutation.isPending}
              className="font-bold"
            >
              حفظ الفصل
            </Button>
          </div>
        }
      >
        {classError && <Alert variant="error">{classError}</Alert>}
        <div className="space-y-4">
          {/* Target Ministry Badge */}
          <div className="p-3 bg-primary-50/70 border border-primary-100 rounded-xl flex items-center justify-between">
            <span className="text-xs font-bold text-primary-900">الخدمة التابعة:</span>
            <Badge variant="primary" className="font-bold text-xs">
              {targetMinistryForClass?.name || editingClass?.ministryName}
            </Badge>
          </div>

          <Input
            label="اسم الفصل الدراسي أو الأسرة *"
            value={className}
            onChange={(e) => setClassName(e.target.value)}
            placeholder="مثال: أولى ابتدائي (بنين)، ثانية إعدادي..."
            required
          />

          <Input
            type="number"
            label="ترتيب العرض (Sort Order)"
            value={classSortOrder}
            onChange={(e) => setClassSortOrder(Number(e.target.value))}
            placeholder="0, 1, 2..."
          />

          <p className="text-[11px] text-gray-500">
            سيتم ربط هذا الفصل تلقائياً بهذه الخدمة، ويمكنك تعيين أمين الفصل وتسكين الخدام والمخدومين فيه بعد الحفظ.
          </p>
        </div>
      </Drawer>

      {/* Assign Secretary Modal */}
      <AssignSecretaryModal
        isOpen={secretaryModalState.isOpen}
        onClose={() =>
          setSecretaryModalState((prev) => ({
            ...prev,
            isOpen: false,
          }))
        }
        targetType={secretaryModalState.targetType}
        targetId={secretaryModalState.targetId}
        targetName={secretaryModalState.targetName}
        currentSecretary={secretaryModalState.currentSecretary}
        ministryIdForClass={secretaryModalState.ministryIdForClass}
        onSuccess={handleSecretarySuccess}
      />
    </div>
  );
};
