import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { auditApi, AuditLogItem } from '../api/audit.api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Select } from '../components/ui/Select';
import { Input } from '../components/ui/Input';
import { Spinner } from '../components/ui/Spinner';
import { Badge } from '../components/ui/Badge';
import { Drawer } from '../components/ui/Drawer';
import {
  FileClock,
  ChevronRight,
  ChevronLeft,
  RotateCcw,
  Shield,
  Search,
  Eye,
  ArrowRightLeft,
  CheckCircle2,
} from 'lucide-react';

export const AuditLogsPage: React.FC = () => {
  const [actionFilter, setActionFilter] = useState<string>('');
  const [entityFilter, setEntityFilter] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [actorSearch, setActorSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [selectedLog, setSelectedLog] = useState<AuditLogItem | null>(null);

  const PAGE_SIZE = 20;

  const { data: pageData, isLoading } = useQuery({
    queryKey: ['audit-logs', actionFilter, entityFilter, startDate, endDate, page],
    queryFn: () =>
      auditApi.getLogs({
        action: actionFilter || undefined,
        entityType: entityFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: PAGE_SIZE,
      }),
  });

  const logs = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  // Filter in-memory for actor name search if provided
  const displayedLogs = actorSearch.trim()
    ? logs.filter(
        (l) =>
          l.actorName.toLowerCase().includes(actorSearch.toLowerCase().trim()) ||
          String(l.entityId).includes(actorSearch.trim())
      )
    : logs;

  const getActionBadge = (action: string) => {
    switch (action) {
      case 'CREATE':
        return <Badge variant="success">إنشاء (CREATE)</Badge>;
      case 'UPDATE':
        return <Badge variant="warning">تعديل (UPDATE)</Badge>;
      case 'DELETE':
        return <Badge variant="danger">حذف (DELETE)</Badge>;
      case 'RESTORE':
        return <Badge variant="primary">استعادة (RESTORE)</Badge>;
      case 'WEEK_LOCK_OVERRIDE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-50 text-purple-800 border border-purple-200">
            تجاوز القفل
          </span>
        );
      case 'PROMOTION_RUN':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-800 border border-blue-200">
            ترفيع سنوي
          </span>
        );
      default:
        return <Badge variant="neutral">{action}</Badge>;
    }
  };

  const handleResetFilters = () => {
    setActionFilter('');
    setEntityFilter('');
    setStartDate('');
    setEndDate('');
    setActorSearch('');
    setPage(0);
  };

  const hasActiveFilters =
    actionFilter !== '' ||
    entityFilter !== '' ||
    startDate !== '' ||
    endDate !== '' ||
    actorSearch !== '';

  const formatJson = (val?: string | null) => {
    if (!val) return null;
    try {
      const parsed = JSON.parse(val);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return val;
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight flex items-center gap-2.5">
              <FileClock className="w-6 h-6 text-primary-600" />
              <span>سجل التدقيق والعمليات</span>
            </h1>
            <span className="bg-primary-50 text-primary-700 text-xs font-bold px-2.5 py-0.5 rounded-full border border-primary-200">
              عام ومحمي
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            سجل توثيقي دائم وغير قابل للتعديل لكافة العمليات والتغييرات الحساسة بالنظام (صلاحية أمين عام)
          </p>
        </div>

        {totalElements > 0 && (
          <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 px-3.5 py-1.5 rounded-2xl text-xs">
            <Shield className="w-4 h-4 text-emerald-600" />
            <span className="text-gray-600">إجمالي العمليات الموثقة:</span>
            <strong className="font-mono font-black text-gray-900">{totalElements}</strong>
          </div>
        )}
      </div>

      {/* Filter Bar */}
      <Card className="p-4 bg-white border-gray-200 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <Select
            label="نوع العملية"
            value={actionFilter}
            onChange={(e) => {
              setActionFilter(e.target.value);
              setPage(0);
            }}
            options={[
              { value: '', label: 'كافة العمليات' },
              { value: 'CREATE', label: 'إنشاء (CREATE)' },
              { value: 'UPDATE', label: 'تعديل (UPDATE)' },
              { value: 'DELETE', label: 'حذف (DELETE)' },
              { value: 'RESTORE', label: 'استعادة (RESTORE)' },
              { value: 'WEEK_LOCK_OVERRIDE', label: 'تجاوز قفل الأسبوع' },
              { value: 'PROMOTION_RUN', label: 'عملية الترفيع الآلي' },
            ]}
          />

          <Select
            label="نوع الكيان المستهدف"
            value={entityFilter}
            onChange={(e) => {
              setEntityFilter(e.target.value);
              setPage(0);
            }}
            options={[
              { value: '', label: 'كافة الكيانات' },
              { value: 'Student', label: 'مخدوم (Student)' },
              { value: 'StaffPlacement', label: 'تسكين خادم (Staff)' },
              { value: 'VisitRecord', label: 'سجل افتقاد (Visit)' },
              { value: 'AttendanceRecord', label: 'سجل حضور (Attendance)' },
              { value: 'ConfessionRecord', label: 'سجل اعتراف (Confession)' },
              { value: 'Week', label: 'أسبوع (Week)' },
              { value: 'PromotionRun', label: 'سجل ترفيع (Promotion)' },
            ]}
          />

          <Input
            label="من تاريخ"
            type="date"
            value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value);
              setPage(0);
            }}
          />

          <Input
            label="إلى تاريخ"
            type="date"
            value={endDate}
            onChange={(e) => {
              setEndDate(e.target.value);
              setPage(0);
            }}
          />
        </div>

        {/* Secondary row: Search & Reset */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1 border-t border-gray-100">
          <div className="relative w-full sm:w-72">
            <Input
              placeholder="بحث باسم المستخدم أو كود الكيان..."
              value={actorSearch}
              onChange={(e) => setActorSearch(e.target.value)}
              className="pr-9 bg-white text-xs h-9"
            />
            <Search className="w-3.5 h-3.5 absolute right-3 top-3 text-gray-400 pointer-events-none" />
          </div>

          {hasActiveFilters && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              className="text-xs font-bold gap-1 min-h-[36px]"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>إعادة تعيين الفلاتر</span>
            </Button>
          )}
        </div>
      </Card>

      {/* Audit Logs Table */}
      <Card className="p-0 overflow-hidden border-gray-200">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-gray-50/90 text-gray-600 font-semibold border-b border-gray-100">
              <tr>
                <th className="py-3.5 px-4">التوقيت</th>
                <th className="py-3.5 px-4">المستخدم (الفاعل)</th>
                <th className="py-3.5 px-4">نوع العملية</th>
                <th className="py-3.5 px-4">الكيان المستهدف</th>
                <th className="py-3.5 px-4">عنوان IP</th>
                <th className="py-3.5 px-4 text-center">التفاصيل</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50 bg-white">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-16">
                    <Spinner size="md" />
                    <p className="text-xs text-gray-400 mt-2 font-medium">جاري استرجاع سجل العمليات...</p>
                  </td>
                </tr>
              ) : displayedLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-16 text-gray-400">
                    <FileClock className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                    <p className="font-bold text-gray-600">لا توجد سجلات تدقيق تطابق معايير البحث</p>
                    {hasActiveFilters && (
                      <p className="text-xs text-gray-400 mt-1">
                        جرّب تعديل الفلاتر أو إعادة تعيينها لعرض المزيد من العمليات.
                      </p>
                    )}
                  </td>
                </tr>
              ) : (
                displayedLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/70 transition">
                    <td className="py-3.5 px-4 text-gray-500 font-mono text-[11px]">
                      {new Date(log.timestamp).toLocaleString('ar-EG')}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-gray-900">{log.actorName}</div>
                      <div className="text-[10px] text-gray-400 font-mono">
                        معرف الحساب: #{log.actorUserId}
                      </div>
                    </td>
                    <td className="py-3.5 px-4">{getActionBadge(log.action)}</td>
                    <td className="py-3.5 px-4">
                      <span className="font-semibold text-gray-800">{log.entityType}</span>
                      <span className="text-[10px] text-gray-400 font-mono block">
                        كود: #{log.entityId}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-gray-500 font-mono text-[11px]" dir="ltr">
                      {log.clientIp || '-'}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setSelectedLog(log)}
                        className="text-xs h-8 text-primary-700 hover:bg-primary-50 font-bold gap-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>عرض القيم</span>
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="p-3.5 bg-gray-50/70 border-t border-gray-100 flex items-center justify-between text-xs text-gray-600">
            <div>
              عرض صفحة <strong className="font-mono text-gray-900">{page + 1}</strong> من{' '}
              <strong className="font-mono text-gray-900">{totalPages}</strong> (إجمالي{' '}
              <span className="font-mono">{totalElements}</span> عملية)
            </div>

            <div className="flex items-center gap-1.5">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
                className="gap-1 font-bold text-xs h-8"
              >
                <ChevronRight className="w-4 h-4" />
                <span>السابق</span>
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || isLoading}
                className="gap-1 font-bold text-xs h-8"
              >
                <span>التالي</span>
                <ChevronLeft className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Log Detail Drawer */}
      <Drawer
        isOpen={selectedLog !== null}
        onClose={() => setSelectedLog(null)}
        title="تفاصيل العملية وبيانات التغيير"
        subtitle={selectedLog ? `عملية رقم #${selectedLog.id}` : ''}
        size="lg"
      >
        {selectedLog && (
          <div className="space-y-5">
            {/* Metadata Summary Card */}
            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 space-y-2.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-gray-500">كود العملية:</span>
                <span className="font-mono font-bold text-gray-900">#{selectedLog.id}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-500">المستخدم المنفذ:</span>
                <span className="font-bold text-gray-900">
                  {selectedLog.actorName} (معرف: #{selectedLog.actorUserId})
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-500">نوع العملية:</span>
                <span>{getActionBadge(selectedLog.action)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-500">الكيان المستهدف:</span>
                <span className="font-mono font-bold text-gray-900">
                  {selectedLog.entityType} #{selectedLog.entityId}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-500">التوقيت:</span>
                <span className="font-mono text-gray-700">
                  {new Date(selectedLog.timestamp).toLocaleString('ar-EG')}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-500">عنوان IP:</span>
                <span className="font-mono text-gray-700" dir="ltr">
                  {selectedLog.clientIp || 'غير مسجل'}
                </span>
              </div>
            </div>

            {/* Old Values JSON */}
            {selectedLog.oldValues && (
              <div className="space-y-1.5">
                <h4 className="text-xs font-bold text-rose-700 flex items-center gap-1.5">
                  <ArrowRightLeft className="w-3.5 h-3.5 text-rose-500" />
                  <span>القيم السابقة (Before Change):</span>
                </h4>
                <pre
                  className="p-3.5 bg-rose-50/40 border border-rose-200/80 rounded-2xl text-[11px] font-mono text-rose-950 overflow-x-auto text-left leading-relaxed max-h-60"
                  dir="ltr"
                >
                  {formatJson(selectedLog.oldValues)}
                </pre>
              </div>
            )}

            {/* New Values JSON */}
            {selectedLog.newValues && (
              <div className="space-y-1.5">
                <h4 className="text-xs font-bold text-emerald-700 flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>القيم الجديدة (After Change):</span>
                </h4>
                <pre
                  className="p-3.5 bg-emerald-50/40 border border-emerald-200/80 rounded-2xl text-[11px] font-mono text-emerald-950 overflow-x-auto text-left leading-relaxed max-h-60"
                  dir="ltr"
                >
                  {formatJson(selectedLog.newValues)}
                </pre>
              </div>
            )}

            {!selectedLog.oldValues && !selectedLog.newValues && (
              <p className="text-center py-6 text-xs text-gray-400 bg-gray-50 rounded-2xl border border-gray-100">
                لا توجد فروق قيم إضافية مسجلة لهذه العملية.
              </p>
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
};
