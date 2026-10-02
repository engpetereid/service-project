import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { servantsApi } from '../../api/servants.api';
import { studentsApi } from '../../api/students.api';
import { StudentResponse } from '../../types/student.types';
import { ServantResponse } from '../../types/staff.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Alert } from '../ui/Alert';
import { Spinner } from '../ui/Spinner';
import { UserCheck, Users, AlertTriangle, Sparkles, XCircle } from 'lucide-react';
import { useAuth } from '../../auth/useAuth';

interface QuickAssignModalProps {
  isOpen: boolean;
  onClose: () => void;
  students: StudentResponse[];
  onSuccess: () => void;
}

export const QuickAssignModal: React.FC<QuickAssignModalProps> = ({
  isOpen,
  onClose,
  students,
  onSuccess,
}) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedServantId, setSelectedServantId] = useState<number | '' | 'NONE'>('');
  const [error, setError] = useState<string | null>(null);

  // Determine target class id if all students are in the same class
  const classIds = Array.from(new Set(students.map((s) => s.classId).filter(Boolean)));
  const isSingleClass = classIds.length === 1;
  const commonClassId = isSingleClass ? classIds[0] : null;

  // Initialize selected servant for single student
  React.useEffect(() => {
    if (isOpen && students.length === 1) {
      const currentServantId = students[0].responsibleServantId ?? students[0].servantId;
      setSelectedServantId(currentServantId || '');
    } else if (isOpen) {
      setSelectedServantId('');
    }
  }, [isOpen, students]);

  // Fetch servants for that class
  const { data: servants = [], isLoading: loadingServants } = useQuery<ServantResponse[]>({
    queryKey: ['servants', 'by-class', commonClassId],
    queryFn: () => servantsApi.findAll({ classId: Number(commonClassId) }),
    enabled: isOpen && !!commonClassId,
  });

  const mutation = useMutation({
    mutationFn: async () => {
      setError(null);
      if (selectedServantId === '') {
        throw new Error('يرجى اختيار خادم مسؤول أو اختيار إلغاء التعيين');
      }

      const servantIdToAssign = selectedServantId === 'NONE' ? null : Number(selectedServantId);

      if (students.length === 1) {
        const studentId = students[0].id ?? students[0].personId;
        if (!studentId) {
          throw new Error('معرف المخدوم غير صالح');
        }
        await studentsApi.changeAssignment(studentId, {
          servantId: servantIdToAssign,
        });
      } else {
        const studentIds = students.map((s) => s.id ?? s.personId).filter((id): id is number => typeof id === 'number');
        if (studentIds.length === 0) {
          throw new Error('لم يتم تحديد أي مخدومين صالحين للتعيين');
        }
        await studentsApi.batchAssignServant({
          studentIds,
          servantId: servantIdToAssign,
        });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      queryClient.invalidateQueries({ queryKey: ['statistics'] });
      onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setError(err.message || 'حدث خطأ أثناء حفظ التعيين');
    },
  });

  const handleClose = () => {
    setSelectedServantId('');
    setError(null);
    onClose();
  };

  if (!isOpen || students.length === 0) return null;

  const isSingleStudent = students.length === 1;
  const singleStudent = isSingleStudent ? students[0] : null;

  return (
    <Drawer
      isOpen={isOpen}
      onClose={handleClose}
      title={
        isSingleStudent
          ? `تعيين الخادم المسؤول: ${singleStudent?.fullName}`
          : `تعيين خادم مسؤول لـ (${students.length}) مخدومين`
      }
      size="md"
      footer={
        <div className="flex items-center justify-end gap-3 w-full">
          <Button variant="outline" onClick={handleClose} disabled={mutation.isPending}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending || selectedServantId === ''}
            className="font-bold"
          >
            {mutation.isPending ? 'جاري الحفظ...' : 'حفظ التعيين'}
          </Button>
        </div>
      }
    >
      <div className="space-y-5 text-right">
        {error && <Alert variant="error">{error}</Alert>}

        {/* Informative Header / Selected Summary */}
        {isSingleStudent && singleStudent ? (
          <div className="p-4 bg-primary-50/50 rounded-2xl border border-primary-100 flex items-center justify-between">
            <div>
              <h4 className="font-bold text-gray-900 text-sm">{singleStudent.fullName}</h4>
              <p className="text-xs text-gray-500 mt-0.5">
                {singleStudent.ministryName} &bull; {singleStudent.className || '-'}
              </p>
            </div>
            <div className="text-left">
              <span className="text-[11px] font-bold text-gray-500 block">الخادم الحالي:</span>
              <span className="text-xs font-bold text-primary-700">
                {singleStudent.responsibleServantName || singleStudent.servantName || 'لا يوجد خادم مسند'}
              </span>
            </div>
          </div>
        ) : (
          <div className="p-4 bg-purple-50 rounded-2xl border border-purple-100 space-y-2">
            <div className="flex items-center gap-2 text-purple-800 font-bold text-sm">
              <Users className="w-4 h-4 text-purple-600" />
              <span>تم تحديد {students.length} مخدومين للتعيين الجماعي</span>
            </div>
            <p className="text-xs text-purple-700 leading-relaxed">
              سيتم تطبيق الخادم المسؤول المختار على جميع المخدومين المحددين دفعة واحدة.
            </p>
          </div>
        )}

        {/* Warning if student has no class */}
        {isSingleClass && !commonClassId && (
          <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div className="text-xs text-amber-800 space-y-1">
              <span className="font-bold block">تنبيه: المخدوم غير مسكن في أي فصل حالياً</span>
              <span>
                يجب تسكين المخدوم في خدمة وفصل أولاً حتى تتمكن من إسناد خادم مسؤول له من نفس الفصل.
              </span>
            </div>
          </div>
        )}

        {/* Warning if selected students belong to multiple classes */}
        {!isSingleClass && (
          <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div className="text-xs text-amber-800 space-y-1">
              <span className="font-bold block">تنبيه: المخدومون المختارون يتبعون فصولاً مختلفة</span>
              <span>
                وفقاً لقواعد النظام، يجب أن يكون الخادم المسؤول مسكناً في نفس فصل المخدوم. يرجى تصفية
                الطلاب بحسب الفصل أولاً لإجراء تعيين سليم.
              </span>
            </div>
          </div>
        )}

        {/* Servant Selection */}
        {loadingServants ? (
          <div className="py-8 flex flex-col items-center justify-center">
            <Spinner size="md" />
            <p className="text-xs text-gray-500 mt-2">جاري جلب خدام الفصل المتاحين...</p>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="text-xs font-bold text-gray-700 block mb-2">
                اختر الخادم المسؤول:
              </label>

              {servants.length === 0 ? (
                <div className="p-4 bg-gray-50 rounded-2xl border border-gray-200 text-center text-xs text-gray-500">
                  <p className="font-bold text-gray-700 mb-1">لا يوجد خدام مسكنين في هذا الفصل حالياً</p>
                  <p className="text-[11px] text-gray-400">
                    يمكنك تسكين خدام في هذا الفصل من شاشة الخدام أولاً لتتمكن من إسناد المخدومين إليهم.
                  </p>
                </div>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                  {/* Option: Unassign Servant */}
                  <label
                    className={`flex items-center justify-between p-3.5 rounded-xl border cursor-pointer transition ${
                      selectedServantId === 'NONE'
                        ? 'border-primary-600 bg-primary-50/60 ring-1 ring-primary-600'
                        : 'border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="servantSelection"
                        checked={selectedServantId === 'NONE'}
                        onChange={() => setSelectedServantId('NONE')}
                        className="text-primary-600 focus:ring-primary-500"
                      />
                      <div>
                        <span className="text-xs font-bold text-gray-800 block">
                          بدون خادم مسؤول (إلغاء التعيين)
                        </span>
                        <span className="text-[11px] text-gray-400">
                          إزالة أي خادم مسؤول حالي وترك المخدوم للمتابعة العامة
                        </span>
                      </div>
                    </div>
                    <XCircle className="w-4 h-4 text-gray-400" />
                  </label>

                  {/* Servants list */}
                  {servants.map((s) => {
                    const servantId = s.personId ?? s.id;
                    const isSecretary = s.isClassSecretary;
                    const isMe = user?.personId === servantId;
                    return (
                      <label
                        key={servantId}
                        className={`flex items-center justify-between p-3.5 rounded-xl border cursor-pointer transition ${
                          selectedServantId === servantId
                            ? 'border-primary-600 bg-primary-50/60 ring-1 ring-primary-600'
                            : 'border-gray-200 hover:bg-gray-50'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <input
                            type="radio"
                            name="servantSelection"
                            checked={selectedServantId === servantId}
                            onChange={() => setSelectedServantId(servantId!)}
                            className="text-primary-600 focus:ring-primary-500"
                          />
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-gray-900 block">{s.fullName}</span>
                              {isSecretary && (
                                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-purple-100 text-purple-700 rounded-md">
                                  {isMe ? 'أمين الفصل (أنت)' : 'أمين الفصل'}
                                </span>
                              )}
                              {!isSecretary && isMe && (
                                <span className="px-1.5 py-0.5 text-[10px] font-medium bg-blue-100 text-blue-700 rounded-md">
                                  (أنت)
                                </span>
                              )}
                            </div>
                            <span className="text-[11px] text-gray-500 font-mono" dir="ltr">
                              {s.phone}
                            </span>
                          </div>
                        </div>
                        <UserCheck className="w-4 h-4 text-primary-600" />
                      </label>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Quick helper note */}
            <div className="p-3 bg-blue-50/50 rounded-xl border border-blue-100 flex items-center gap-2 text-xs text-blue-700">
              <Sparkles className="w-4 h-4 text-blue-600 shrink-0" />
              <span>
                بمجرد حفظ التعيين، سيظهر هؤلاء المخدومون في قائمة المتابعة والافتقاد الخاصة بالخادم المحدد فوراً.
              </span>
            </div>
          </div>
        )}
      </div>
    </Drawer>
  );
};
