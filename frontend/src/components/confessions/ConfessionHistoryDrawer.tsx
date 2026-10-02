import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { confessionApi } from '../../api/confession.api';
import { ConfessionResponse } from '../../types/confession.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Spinner } from '../ui/Spinner';
import { formatDate } from '../../utils/date';
import {
  HeartHandshake,
  Plus,
  Calendar,
  User,
  FileText,
  Trash2,
  CheckCircle2,
} from 'lucide-react';

interface ConfessionHistoryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  student: {
    id: number;
    name: string;
    className?: string | null;
    confessionFather?: string | null;
  } | null;
  onOpenNewRecord: () => void;
  onRecordDeleted?: () => void;
}

export const ConfessionHistoryDrawer: React.FC<ConfessionHistoryDrawerProps> = ({
  isOpen,
  onClose,
  student,
  onOpenNewRecord,
  onRecordDeleted,
}) => {
  const queryClient = useQueryClient();
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const studentId = student?.id;

  const {
    data: records = [],
    isLoading,
    refetch,
  } = useQuery<ConfessionResponse[]>({
    queryKey: ['confessions', 'student', studentId],
    queryFn: () => confessionApi.findByStudent(studentId!),
    enabled: isOpen && !!studentId,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => confessionApi.delete(id),
    onSuccess: () => {
      setDeleteConfirmId(null);
      refetch();
      queryClient.invalidateQueries({ queryKey: ['confessions', 'overview'] });
      if (onRecordDeleted) onRecordDeleted();
    },
  });

  if (!student) return null;

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={`سجل اعترافات: ${student.name}`}
      subtitle={student.className ? `فصل ${student.className}` : 'سجل جلسات سر التوبة والاعتراف'}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" onClick={onClose}>
            إغلاق
          </Button>

          <Button
            variant="primary"
            onClick={onOpenNewRecord}
            className="font-bold bg-purple-600 hover:bg-purple-700"
          >
            <Plus className="w-4 h-4 ml-1.5" />
            تسجيل اعتراف جديد
          </Button>
        </div>
      }
    >
      <div className="space-y-5">
        {/* Student Summary Top Card */}
        <div className="bg-gradient-to-br from-purple-50/60 via-white to-purple-50/20 border border-purple-100 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center font-black text-sm">
              {student.name.charAt(0)}
            </div>
            <div>
              <h3 className="font-bold text-gray-900 text-sm">{student.name}</h3>
              <p className="text-xs text-purple-700 font-medium flex items-center gap-1 mt-0.5">
                <User className="w-3.5 h-3.5" />
                <span>أب الاعتراف: </span>
                <strong className="text-gray-900 font-bold">
                  {student.confessionFather || 'لم يُحدد بعد'}
                </strong>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-gray-600 bg-white px-3 py-1 rounded-xl border border-gray-100">
              إجمالي الجلسات: {records.length}
            </span>
          </div>
        </div>

        {/* History Timeline Stream */}
        {isLoading ? (
          <div className="py-16 text-center">
            <Spinner size="md" />
            <p className="text-xs text-gray-400 mt-2 font-medium">جاري تحميل سجل الاعترافات...</p>
          </div>
        ) : records.length === 0 ? (
          <div className="text-center py-16 px-4 bg-gray-50/60 rounded-2xl border border-gray-100">
            <div className="w-12 h-12 rounded-2xl bg-purple-100 text-purple-600 flex items-center justify-center mx-auto mb-3">
              <HeartHandshake className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-sm mb-1">لا توجد اعترافات مسجلة حتى الآن</h4>
            <p className="text-xs text-gray-500 max-w-sm mx-auto mb-4">
              لم يتم توثيق أي جلسة اعتراف سابقة لهذا المخدوم خلال العام الحالي.
            </p>
            <Button
              size="sm"
              variant="primary"
              onClick={onOpenNewRecord}
              className="bg-purple-600 hover:bg-purple-700 font-bold"
            >
              <Plus className="w-3.5 h-3.5 ml-1" />
              تسجيل أول اعتراف للمخدوم
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-gray-500 tracking-wider">
              الجلسات المسجلة ({records.length})
            </h4>

            {records.map((record, index) => {
              const isDeleting = deleteConfirmId === record.id;

              return (
                <div
                  key={record.id}
                  className="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm space-y-2.5 transition hover:border-purple-200"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="p-1.5 rounded-xl bg-purple-50 text-purple-700 font-bold">
                        <Calendar className="w-4 h-4" />
                      </span>
                      <span className="font-bold text-sm text-gray-900">
                        {formatDate(record.confessionDate)}
                      </span>
                      {index === 0 && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <CheckCircle2 className="w-3 h-3" />
                          أحدث اعتراف
                        </span>
                      )}
                      <span className="text-[10px] text-gray-400 font-mono">
                        ({record.academicYearName})
                      </span>
                    </div>

                    {/* Delete action */}
                    {!isDeleting ? (
                      <button
                        type="button"
                        onClick={() => setDeleteConfirmId(record.id)}
                        className="text-gray-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition"
                        title="حذف هذا السجل"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : (
                      <div className="flex items-center gap-1.5 bg-red-50 p-1 px-2 rounded-xl border border-red-200 text-xs">
                        <span className="text-red-700 font-bold text-[11px]">تأكيد الحذف؟</span>
                        <button
                          type="button"
                          onClick={() => deleteMutation.mutate(record.id)}
                          disabled={deleteMutation.isPending}
                          className="px-2 py-0.5 rounded-lg bg-red-600 text-white font-bold text-[10px] hover:bg-red-700"
                        >
                          نعم
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteConfirmId(null)}
                          className="px-2 py-0.5 rounded-lg bg-white text-gray-600 font-medium text-[10px] hover:bg-gray-100"
                        >
                          إلغاء
                        </button>
                      </div>
                    )}
                  </div>

                  {record.confessionFather && (
                    <p className="text-xs text-purple-800 font-medium flex items-center gap-1.5 bg-purple-50/40 p-2 rounded-xl">
                      <User className="w-3.5 h-3.5 text-purple-600" />
                      <span>أب الاعتراف: </span>
                      <strong className="font-bold">{record.confessionFather}</strong>
                    </p>
                  )}

                  {record.notes && (
                    <p className="text-xs text-gray-600 bg-gray-50/90 p-2.5 rounded-xl border border-gray-100 italic flex items-start gap-1.5">
                      <FileText className="w-3.5 h-3.5 text-gray-400 shrink-0 mt-0.5" />
                      <span>«{record.notes}»</span>
                    </p>
                  )}

                  <div className="flex items-center justify-between text-[10px] text-gray-400 pt-1 border-t border-gray-50 font-mono">
                    <span>سجل بواسطة: {record.recordedByName}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </Drawer>
  );
};
