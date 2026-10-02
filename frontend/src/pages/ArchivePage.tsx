import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { archiveApi, DeletedPersonItem } from '../api/archive.api';
import { usePermissions } from '../auth/usePermissions';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Spinner } from '../components/ui/Spinner';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import {
  Archive,
  UserX,
  RotateCcw,
  CheckCircle2,
  Lock,
  FileCheck2,
  Sparkles,
} from 'lucide-react';

export const ArchivePage: React.FC = () => {
  const { isAdmin } = usePermissions();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'deleted' | 'weeks'>('deleted');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Summary query
  const { data: summary, isLoading: isSummaryLoading } = useQuery({
    queryKey: ['archive', 'summary'],
    queryFn: archiveApi.getSummary,
  });

  // Deleted people query
  const { data: deletedPeople = [], isLoading: isPeopleLoading } = useQuery({
    queryKey: ['archive', 'deleted-people'],
    queryFn: archiveApi.getDeletedPeople,
    enabled: isAdmin && activeTab === 'deleted',
  });

  // Locked weeks query
  const { data: lockedWeeks = [], isLoading: isWeeksLoading } = useQuery({
    queryKey: ['archive', 'weeks'],
    queryFn: archiveApi.getLockedWeeks,
    enabled: activeTab === 'weeks',
  });

  // Restore person mutation
  const restoreMutation = useMutation({
    mutationFn: async (person: DeletedPersonItem) => {
      if (person.personType === 'SERVANT') {
        await archiveApi.restoreServant(person.id);
      } else {
        await archiveApi.restoreStudent(person.id);
      }
    },
    onSuccess: (_, person) => {
      setActionSuccess(`تم استرجاع "${person.fullName}" بنجاح إلى النظام.`);
      queryClient.invalidateQueries({ queryKey: ['archive'] });
      queryClient.invalidateQueries({ queryKey: ['students'] });
      queryClient.invalidateQueries({ queryKey: ['servants'] });
    },
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-gray-900 tracking-tight flex items-center gap-2.5">
          <Archive className="w-6 h-6 text-amber-600" />
          <span>الأرشيف والبيانات التاريخية</span>
        </h1>
      </div>

      {actionSuccess && (
        <Alert variant="success" className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{actionSuccess}</span>
        </Alert>
      )}

      {/* Summary KPI Cards Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-500">حسابات محذوفة</span>
            <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 flex items-center justify-center">
              <UserX className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900 font-mono">
              {isSummaryLoading ? '...' : summary?.deletedPeopleCount ?? 0}
            </div>
            <p className="text-[11px] text-gray-400 mt-0.5">قابلة للاسترجاع (Soft-deleted)</p>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-500">أسابيع مقفولة</span>
            <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
              <Lock className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-amber-700 font-mono">
              {isSummaryLoading ? '...' : summary?.lockedWeeksCount ?? 0}
            </div>
            <p className="text-[11px] text-gray-400 mt-0.5">تجاوزت 30 يوماً</p>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-500">إجمالي الافتقادات</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <FileCheck2 className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-emerald-700 font-mono">
              {isSummaryLoading ? '...' : summary?.totalVisitsCount ?? 0}
            </div>
            <p className="text-[11px] text-gray-400 mt-0.5">سجلات تاريخية محفوظة</p>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-500">سجلات الاعتراف</span>
            <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-purple-700 font-mono">
              {isSummaryLoading ? '...' : summary?.totalConfessionsCount ?? 0}
            </div>
            <p className="text-[11px] text-gray-400 mt-0.5">اعترافات موثقة</p>
          </div>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-gray-200 pb-2">
        {isAdmin && (
          <button
            type="button"
            onClick={() => setActiveTab('deleted')}
            className={`py-2 px-4 text-xs font-bold rounded-xl transition min-h-[40px] flex items-center gap-2 ${
              activeTab === 'deleted'
                ? 'bg-primary-600 text-white shadow-xs'
                : 'bg-white text-gray-600 hover:bg-gray-100'
            }`}
          >
            <UserX className="w-4 h-4" />
            <span>الأشخاص المحذوفين ({summary?.deletedPeopleCount ?? 0})</span>
          </button>
        )}

        <button
          type="button"
          onClick={() => setActiveTab('weeks')}
          className={`py-2 px-4 text-xs font-bold rounded-xl transition min-h-[40px] flex items-center gap-2 ${
            activeTab === 'weeks'
              ? 'bg-primary-600 text-white shadow-xs'
              : 'bg-white text-gray-600 hover:bg-gray-100'
          }`}
        >
          <Lock className="w-4 h-4" />
          <span>الأسابيع المقفولة ({summary?.lockedWeeksCount ?? 0})</span>
        </button>
      </div>

      {/* Tab 1: Deleted People (Admin only) */}
      {activeTab === 'deleted' && (
        <Card className="p-5">
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                <tr>
                  <th className="py-3 px-3">الاسم الكامل</th>
                  <th className="py-3 px-3">النوع / الدور</th>
                  <th className="py-3 px-3">الهاتف</th>
                  <th className="py-3 px-3">الخدمة والفصل السابق</th>
                  <th className="py-3 px-3">تاريخ الحذف</th>
                  <th className="py-3 px-3 text-center">إجراء الاسترجاع</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {isPeopleLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-12">
                      <Spinner size="md" />
                    </td>
                  </tr>
                ) : deletedPeople.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-gray-400">
                      سجل المحذوفين فارغ حالياً
                    </td>
                  </tr>
                ) : (
                  deletedPeople.map((person) => (
                    <tr key={person.id} className="hover:bg-gray-50/50 transition">
                      <td className="py-3 px-3 font-bold text-gray-900">{person.fullName}</td>
                      <td className="py-3 px-3">
                        <Badge variant={person.personType === 'SERVANT' ? 'primary' : 'neutral'}>
                          {person.personType === 'SERVANT' ? 'خادم' : 'مخدوم'}
                        </Badge>
                      </td>
                      <td className="py-3 px-3 font-mono text-gray-500" dir="ltr">
                        {person.phone}
                      </td>
                      <td className="py-3 px-3 text-gray-500">
                        {person.ministryName || '-'} {person.className ? `• ${person.className}` : ''}
                      </td>
                      <td className="py-3 px-3 font-mono text-[11px] text-gray-400">
                        {new Date(person.deletedAt).toLocaleDateString('ar-EG')}
                      </td>
                      <td className="py-3 px-3 text-center">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => restoreMutation.mutate(person)}
                          disabled={restoreMutation.isPending}
                          className="text-xs h-8 font-bold text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                        >
                          <RotateCcw className="w-3.5 h-3.5 ml-1" />
                          استرجاع الحساب
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Tab 2: Locked Weeks */}
      {activeTab === 'weeks' && (
        <Card className="p-5">
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                <tr>
                  <th className="py-3 px-3">كود الأسبوع</th>
                  <th className="py-3 px-3">تاريخ البداية (الجمعة)</th>
                  <th className="py-3 px-3">تاريخ النهاية (الخميس)</th>
                  <th className="py-3 px-3">حالة القفل</th>
                  <th className="py-3 px-3">ملاحظات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {isWeeksLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-12">
                      <Spinner size="md" />
                    </td>
                  </tr>
                ) : lockedWeeks.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center py-12 text-gray-400">
                      لا توجد أسابيع مقفولة بعد
                    </td>
                  </tr>
                ) : (
                  lockedWeeks.map((week) => (
                    <tr key={week.id} className="hover:bg-gray-50/50 transition">
                      <td className="py-3 px-3 font-mono font-bold text-gray-900">#{week.id}</td>
                      <td className="py-3 px-3 font-mono text-gray-700">{week.startDate}</td>
                      <td className="py-3 px-3 font-mono text-gray-700">{week.endDate}</td>
                      <td className="py-3 px-3">
                        <Badge variant="warning" className="flex items-center gap-1 w-fit">
                          <Lock className="w-3 h-3" />
                          <span>مقفول آلياً (&gt; 30 يوماً)</span>
                        </Badge>
                      </td>
                      <td className="py-3 px-3 text-gray-400 text-[11px]">
                        سجلات الافتقاد والحضور مقفولة للقراءة فقط للخدام، مع إمكانية تجاوز القفل للأمين العام
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};
