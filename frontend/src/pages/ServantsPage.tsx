import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { servantsApi } from '../api/servants.api';
import { ministriesApi } from '../api/ministries.api';
import { classesApi } from '../api/classes.api';
import { ServantResponse } from '../types/staff.types';
import { ServantDrawer } from '../components/servants/ServantDrawer';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Spinner } from '../components/ui/Spinner';
import { EmptyState } from '../components/ui/EmptyState';
import { usePermissions } from '../auth/usePermissions';
import { useDebounce } from '../hooks/useDebounce';
import {
  Search,
  UserPlus,
  Shield,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
} from 'lucide-react';

export const ServantsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const paramMinistryId = searchParams.get('ministryId') ? Number(searchParams.get('ministryId')) : null;
  const paramClassId = searchParams.get('classId') ? Number(searchParams.get('classId')) : null;

  const { isAdmin, managedMinistryId, managedClassId } = usePermissions();

  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [selectedMinistryId, setSelectedMinistryId] = useState<number | ''>(
    paramMinistryId || managedMinistryId || ''
  );
  const [selectedClassId, setSelectedClassId] = useState<number | ''>(
    paramClassId || managedClassId || ''
  );
  const [accountFilter, setAccountFilter] = useState<'ALL' | 'NO_ACCOUNT' | 'HAS_ACCOUNT'>('ALL');

  const [activeServant, setActiveServant] = useState<ServantResponse | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // Queries
  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
  });

  const { data: classes = [] } = useQuery({
    queryKey: ['classes', selectedMinistryId],
    queryFn: () => classesApi.findAll(selectedMinistryId ? Number(selectedMinistryId) : undefined),
    enabled: !!selectedMinistryId,
  });

  const {
    data: servants = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['servants', selectedMinistryId, selectedClassId, debouncedSearch],
    queryFn: () =>
      servantsApi.findAll({
        ministryId: selectedMinistryId ? Number(selectedMinistryId) : undefined,
        classId: selectedClassId ? Number(selectedClassId) : undefined,
        search: debouncedSearch || undefined,
      }),
  });

  const quickAccountMutation = useMutation({
    mutationFn: (personId: number) =>
      servantsApi.createAccount(
        personId,
        'Pass@' + Math.floor(100000 + Math.random() * 900000)
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      refetch();
    },
  });

  // KPI calculations
  const stats = useMemo(() => {
    const total = servants.length;
    const withAccount = servants.filter((s) => !!s.userId).length;
    const withoutAccount = total - withAccount;
    const assignedToClasses = servants.filter((s) => !!s.classId).length;

    return { total, withAccount, withoutAccount, assignedToClasses };
  }, [servants]);

  // Filtered by account status
  const filteredServants = useMemo(() => {
    return servants.filter((s) => {
      if (accountFilter === 'NO_ACCOUNT' && !!s.userId) return false;
      if (accountFilter === 'HAS_ACCOUNT' && !s.userId) return false;
      return true;
    });
  }, [servants, accountFilter]);

  const handleOpenDrawer = (servant: ServantResponse | null) => {
    setActiveServant(servant);
    setIsDrawerOpen(true);
  };

  const handleQuickCreateAccount = (e: React.MouseEvent, servant: ServantResponse) => {
    e.stopPropagation();
    const servantId = servant.id ?? servant.personId;
    if (servantId) {
      quickAccountMutation.mutate(servantId);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">إدارة الخدام</h1>
          <p className="text-xs text-gray-500 mt-1">
            إدارة بيانات الخدام وتسكينهم بالفصول ومتابعة تفعيل حسابات تسجيل الدخول
          </p>
        </div>

        <Button
          variant="primary"
          onClick={() => handleOpenDrawer(null)}
          className="shadow-sm font-bold self-start sm:self-auto"
        >
          <UserPlus className="w-4 h-4 ml-2" />
          إضافة خادم جديد
        </Button>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
          <span className="text-[11px] text-gray-400 font-bold block">إجمالي الخدام</span>
          <span className="text-xl font-black text-gray-900 mt-0.5 block">{stats.total}</span>
        </div>

        <div
          onClick={() => setAccountFilter('HAS_ACCOUNT')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-emerald-200 transition"
        >
          <span className="text-[11px] text-emerald-600 font-bold block">حسابات دخول مفعلة</span>
          <span className="text-xl font-black text-emerald-700 mt-0.5 block">{stats.withAccount}</span>
        </div>

        <div
          onClick={() => setAccountFilter('NO_ACCOUNT')}
          className={`p-3.5 rounded-xl border shadow-sm transition cursor-pointer ${
            stats.withoutAccount > 0
              ? 'bg-amber-50/70 border-amber-200 hover:bg-amber-100/60'
              : 'bg-white border-gray-100'
          }`}
        >
          <span className="text-[11px] text-amber-700 font-bold flex items-center gap-1">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
            بدون حساب دخول
          </span>
          <span className="text-xl font-black text-amber-800 mt-0.5 block">
            {stats.withoutAccount}
          </span>
        </div>

        <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
          <span className="text-[11px] text-blue-600 font-bold block">مسكنين بفصول</span>
          <span className="text-xl font-black text-blue-700 mt-0.5 block">
            {stats.assignedToClasses}
          </span>
        </div>
      </div>

      {/* Filters & Search Bar */}
      <div className="bg-white p-3.5 rounded-2xl border border-gray-100 shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Search */}
          <div className="relative w-full md:w-80">
            <Search className="w-4 h-4 text-gray-400 absolute right-3 top-2.5" />
            <Input
              placeholder="بحث بالاسم أو رقم الهاتف..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pr-9 text-xs"
            />
          </div>

          {/* Account Filter Pills */}
          <div className="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-100 text-xs w-full md:w-auto overflow-x-auto">
            <button
              onClick={() => setAccountFilter('ALL')}
              className={`px-3 py-1.5 rounded-lg font-bold transition whitespace-nowrap ${
                accountFilter === 'ALL'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              الكل ({stats.total})
            </button>
            <button
              onClick={() => setAccountFilter('NO_ACCOUNT')}
              className={`px-3 py-1.5 rounded-lg font-bold transition whitespace-nowrap ${
                accountFilter === 'NO_ACCOUNT'
                  ? 'bg-white text-amber-800 shadow-sm'
                  : 'text-gray-500 hover:text-amber-700'
              }`}
            >
              بدون حساب ({stats.withoutAccount})
            </button>
            <button
              onClick={() => setAccountFilter('HAS_ACCOUNT')}
              className={`px-3 py-1.5 rounded-lg font-bold transition whitespace-nowrap ${
                accountFilter === 'HAS_ACCOUNT'
                  ? 'bg-white text-emerald-700 shadow-sm'
                  : 'text-gray-500 hover:text-emerald-700'
              }`}
            >
              حسابات مفعلة ({stats.withAccount})
            </button>
          </div>
        </div>

        {/* Ministry & Class Pickers */}
        <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-gray-50">
          {isAdmin && (
            <div className="w-full sm:w-56">
              <Select
                value={selectedMinistryId}
                onChange={(e) => {
                  setSelectedMinistryId(e.target.value ? Number(e.target.value) : '');
                  setSelectedClassId('');
                }}
                options={ministries.map((m) => ({ value: m.id, label: m.name }))}
                placeholder="جميع الخدمات..."
              />
            </div>
          )}

          <div className="w-full sm:w-56">
            <Select
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value ? Number(e.target.value) : '')}
              options={classes.map((c) => ({ value: c.id, label: c.name }))}
              placeholder="جميع الفصول..."
              disabled={!selectedMinistryId && isAdmin}
            />
          </div>
        </div>
      </div>

      {/* Content Area */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل الخدام...</p>
        </div>
      ) : filteredServants.length === 0 ? (
        <EmptyState
          icon={Shield}
          title="لا يوجد خدام مطابقين"
          description="لم يتم العثور على أي خادم يطابق خيارات البحث والتصفية المحددة."
          actionLabel="إضافة خادم جديد"
          onAction={() => handleOpenDrawer(null)}
          actionIcon={UserPlus}
        />
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="hidden lg:block bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100 uppercase">
                <tr>
                  <th className="py-3.5 px-4">الخادم</th>
                  <th className="py-3.5 px-4">الهاتف (اسم المستخدم)</th>
                  <th className="py-3.5 px-4">الخدمة</th>
                  <th className="py-3.5 px-4">الفصل</th>
                  <th className="py-3.5 px-4 text-center">حساب تسجيل الدخول</th>
                  <th className="py-3.5 px-4 text-center">الإجراءات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredServants.map((s) => (
                  <tr
                    key={s.id ?? s.personId}
                    onClick={() => handleOpenDrawer(s)}
                    className="hover:bg-primary-50/40 cursor-pointer transition"
                  >
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-xs">
                          {s.fullName.charAt(0)}
                        </div>
                        <span className="font-bold text-gray-900 text-sm">{s.fullName}</span>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 font-mono font-bold text-gray-700" dir="ltr">
                      {s.phone}
                    </td>

                    <td className="py-3.5 px-4 text-gray-700 font-medium">
                      {s.ministryName || '—'}
                    </td>

                    <td className="py-3.5 px-4 text-gray-700 font-medium">
                      {s.className || '—'}
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      {s.userId ? (
                        <span className="inline-flex items-center gap-1 text-[11px] text-emerald-700 bg-emerald-50 border border-emerald-200 font-bold px-2.5 py-1 rounded-full">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          حساب مفعل
                        </span>
                      ) : (
                        <div className="inline-flex items-center gap-1.5">
                          <span className="inline-flex items-center gap-1 text-[11px] text-amber-800 bg-amber-50 border border-amber-200 font-bold px-2.5 py-1 rounded-full">
                            <AlertTriangle className="w-3 h-3 text-amber-600" />
                            لا يوجد حساب
                          </span>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={(e) => handleQuickCreateAccount(e, s)}
                            isLoading={quickAccountMutation.isPending}
                            className="text-[10px] font-bold py-0.5 px-2 h-auto text-primary-700 border-primary-200 hover:bg-primary-50"
                          >
                            <Sparkles className="w-2.5 h-2.5 ml-1 text-primary-600" />
                            تفعيل
                          </Button>
                        </div>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenDrawer(s);
                        }}
                        className="text-primary-700 hover:text-primary-900 font-bold text-xs"
                      >
                        عرض التفاصيل
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards View */}
          <div className="lg:hidden space-y-3">
            {filteredServants.map((s) => (
              <div
                key={s.id ?? s.personId}
                onClick={() => handleOpenDrawer(s)}
                className="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm active:bg-gray-50 transition cursor-pointer space-y-3"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-sm">
                      {s.fullName.charAt(0)}
                    </div>
                    <div>
                      <h4 className="font-bold text-gray-900 text-sm">{s.fullName}</h4>
                      <p className="text-xs text-gray-500 font-mono" dir="ltr">
                        {s.phone}
                      </p>
                    </div>
                  </div>

                  {s.userId ? (
                    <span className="inline-flex items-center text-[10px] text-emerald-700 bg-emerald-50 border border-emerald-200 font-bold px-2 py-0.5 rounded-md">
                      حساب مفعل
                    </span>
                  ) : (
                    <span className="inline-flex items-center text-[10px] text-amber-800 bg-amber-50 border border-amber-200 font-bold px-2 py-0.5 rounded-md">
                      بدون حساب
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 text-xs text-gray-600 bg-gray-50 p-2 rounded-xl">
                  <span>{s.ministryName || '—'}</span>
                  <span>•</span>
                  <span>{s.className || '—'}</span>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Drawer */}
      <ServantDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        servant={activeServant}
        onSaved={refetch}
      />
    </div>
  );
};
