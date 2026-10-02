import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { studentsApi } from '../../api/students.api';
import { StudentResponse } from '../../types/student.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Select } from '../ui/Select';
import { Alert } from '../ui/Alert';
import { usePermissions } from '../../auth/usePermissions';
import { ArrowRightLeft, AlertTriangle } from 'lucide-react';

interface BatchMoveModalProps {
  isOpen: boolean;
  onClose: () => void;
  students: StudentResponse[];
  onSuccess: () => void;
}

export const BatchMoveModal: React.FC<BatchMoveModalProps> = ({
  isOpen,
  onClose,
  students,
  onSuccess,
}) => {
  const queryClient = useQueryClient();
  const { isAdmin, managedMinistryId } = usePermissions();

  const [targetMinistryId, setTargetMinistryId] = useState<number | ''>(managedMinistryId || '');
  const [targetClassId, setTargetClassId] = useState<number | ''>('');
  const [error, setError] = useState<string | null>(null);

  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isOpen && isAdmin,
  });

  const { data: classes = [] } = useQuery({
    queryKey: ['classes', targetMinistryId],
    queryFn: () => classesApi.findAll(targetMinistryId ? Number(targetMinistryId) : undefined),
    enabled: isOpen && !!targetMinistryId,
  });

  useEffect(() => {
    if (managedMinistryId) {
      setTargetMinistryId(managedMinistryId);
    }
  }, [managedMinistryId, isOpen]);

  const mutation = useMutation({
    mutationFn: async () => {
      setError(null);
      if (!targetMinistryId || !targetClassId) {
        throw new Error('يرجى تحديد الخدمة والفصل المراد النقل إليهما');
      }

      await studentsApi.batchMoveClass({
        studentIds: students.map((s) => s.id),
        ministryId: Number(targetMinistryId),
        classId: Number(targetClassId),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      queryClient.invalidateQueries({ queryKey: ['statistics'] });
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل نقل المخدومين إلى الفصل الجديد');
    },
  });

  const handleClose = () => {
    setTargetClassId('');
    setError(null);
    onClose();
  };

  if (!isOpen || students.length === 0) return null;

  return (
    <Drawer
      isOpen={isOpen}
      onClose={handleClose}
      title={`نقل (${students.length}) مخدومين إلى فصل آخر`}
      size="md"
      footer={
        <div className="flex items-center justify-end gap-3 w-full">
          <Button variant="outline" onClick={handleClose} disabled={mutation.isPending}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending || !targetClassId}
            className="font-bold"
          >
            {mutation.isPending ? 'جاري النقل...' : 'تأكيد النقل'}
          </Button>
        </div>
      }
    >
      <div className="space-y-5 text-right">
        {error && <Alert variant="error">{error}</Alert>}

        <div className="p-4 bg-purple-50 rounded-2xl border border-purple-100 flex items-center gap-3 text-purple-800">
          <ArrowRightLeft className="w-5 h-5 text-purple-600 shrink-0" />
          <div className="text-xs">
            <span className="font-bold block">نقل {students.length} مخدومين دفعة واحدة</span>
            <span className="text-purple-600">
              سيتم نقل سجلات تسكين هؤلاء المخدومين في العام الدراسي الحالي إلى الفصل الجديد.
            </span>
          </div>
        </div>

        <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="text-xs text-amber-800 space-y-1">
            <span className="font-bold block">ملاحظة هامة بشأن الخدام المسؤولين:</span>
            <span>
              نظراً لأن الخدام مخصصون لفصول محددة، فإنه عند نقل مخدوم إلى فصل جديد يتم إلغاء تعيين الخادم المسؤول تلقائياً،
              لتتمكن من إسناد خادم مسؤول جديد من خدام الفصل المنقول إليه.
            </span>
          </div>
        </div>

        <div className="space-y-4 pt-2">
          {isAdmin && (
            <div>
              <label className="text-xs font-bold text-gray-700 block mb-1.5">الخدمة المستهدفة:</label>
              <Select
                value={targetMinistryId}
                onChange={(e) => {
                  setTargetMinistryId(e.target.value ? Number(e.target.value) : '');
                  setTargetClassId('');
                }}
                options={ministries.map((m) => ({ value: m.id, label: m.name }))}
                placeholder="اختر الخدمة..."
              />
            </div>
          )}

          <div>
            <label className="text-xs font-bold text-gray-700 block mb-1.5">الفصل المستهدف الجديد:</label>
            <Select
              value={targetClassId}
              onChange={(e) => setTargetClassId(e.target.value ? Number(e.target.value) : '')}
              options={classes.map((c) => ({ value: c.id, label: c.name }))}
              placeholder="اختر الفصل..."
              disabled={!targetMinistryId}
            />
          </div>
        </div>
      </div>
    </Drawer>
  );
};
