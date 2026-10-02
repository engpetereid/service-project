import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { visitsApi } from '../../api/visits.api';
import { settingsApi } from '../../api/settings.api';
import { VisitMethod, VisitRecordResponse } from '../../types/visit.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { Home, PhoneCall, Award, Check } from 'lucide-react';

interface VisitDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  student: { id: number; name: string; phone?: string } | null;
  weekId: number;
  existingVisit?: VisitRecordResponse | null;
  isWeekLocked?: boolean;
  onSaved: () => void;
}

export const VisitDrawer: React.FC<VisitDrawerProps> = ({
  isOpen,
  onClose,
  student,
  weekId,
  existingVisit,
  isWeekLocked = false,
  onSaved,
}) => {
  const [method, setMethod] = useState<VisitMethod>('VISIT');
  const [noteScore, setNoteScore] = useState<number | ''>('');
  const [notes, setNotes] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch max note score setting
  const { data: publicSettings } = useQuery({
    queryKey: ['settings', 'public'],
    queryFn: settingsApi.getPublic,
    staleTime: 60000,
  });

  const maxNoteScore = publicSettings?.maxNoteScore ?? 21;

  useEffect(() => {
    if (existingVisit) {
      setMethod(existingVisit.method);
      setNoteScore(existingVisit.noteScore ?? '');
      setNotes(existingVisit.notes || '');
    } else {
      setMethod('VISIT');
      setNoteScore('');
      setNotes('');
    }
    setError(null);
  }, [existingVisit, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!student || !weekId) return;

    if (isWeekLocked) {
      setError('لا يمكن تسجيل أو تعديل افتقاد في أسبوع مقفول');
      return;
    }

    // Validate note score
    const nScore = noteScore === '' ? null : Number(noteScore);

    if (nScore !== null && (nScore < 0 || nScore > maxNoteScore)) {
      setError(`درجة النوتة يجب أن تكون بين 0 و ${maxNoteScore}`);
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      if (existingVisit) {
        await visitsApi.update(existingVisit.id, {
          method,
          prayerScore: null,
          readingScore: null,
          noteScore: nScore,
          notes: notes.trim() || null,
        });
      } else {
        await visitsApi.create({
          studentId: student.id,
          weekId,
          method,
          prayerScore: null,
          readingScore: null,
          noteScore: nScore,
          notes: notes.trim() || null,
        });
      }

      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل حفظ بيانات الافتقاد');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={existingVisit ? 'تعديل استمارة الافتقاد' : 'تسجيل افتقاد ومتابعة'}
      subtitle={student ? `المخدوم: ${student.name}` : undefined}
      size="md"
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isSubmitting}
            disabled={isWeekLocked}
          >
            {existingVisit ? 'حفظ التعديلات' : 'تسجيل الافتقاد'}
          </Button>
        </>
      }
    >
      {error && <Alert variant="error">{error}</Alert>}

      {isWeekLocked && (
        <Alert variant="warning">
          هذا الأسبوع مقفول لمرور أكثر من 30 يوماً على انتهائه. التعديل غير مصرح به.
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Method Selector */}
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-2 uppercase tracking-wider">
            طريقة الافتقاد *
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setMethod('VISIT')}
              className={`p-3.5 rounded-2xl border-2 transition flex items-center justify-center gap-2.5 min-h-[48px] font-bold text-sm ${
                method === 'VISIT'
                  ? 'border-primary-600 bg-primary-50 text-primary-800 shadow-sm'
                  : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Home className="w-5 h-5" />
              <span>افتقاد منزلي</span>
              {method === 'VISIT' && <Check className="w-4 h-4 mr-auto text-primary-600" />}
            </button>

            <button
              type="button"
              onClick={() => setMethod('CALL')}
              className={`p-3.5 rounded-2xl border-2 transition flex items-center justify-center gap-2.5 min-h-[48px] font-bold text-sm ${
                method === 'CALL'
                  ? 'border-primary-600 bg-primary-50 text-primary-800 shadow-sm'
                  : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              <PhoneCall className="w-5 h-5" />
              <span>مكالمة هاتفية</span>
              {method === 'CALL' && <Check className="w-4 h-4 mr-auto text-primary-600" />}
            </button>
          </div>
        </div>

        {/* Spiritual Evaluation (Note Score) */}
        <div className="space-y-3 pt-2 border-t border-gray-100">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              درجة النوتة الروحية
            </h4>
            <span className="text-[11px] text-gray-400">اختياري</span>
          </div>

          {/* Note Score (0..maxNoteScore) */}
          <div className="p-4 rounded-2xl bg-gray-50/75 border border-gray-100 space-y-2">
            <div className="flex items-center justify-between text-xs font-bold text-gray-700">
              <span className="flex items-center gap-1.5 text-purple-700">
                <Award className="w-4 h-4" />
                درجة النوتة الروحية (الحد الأقصى: {maxNoteScore})
              </span>
            </div>
            <Input
              type="number"
              min="0"
              max={maxNoteScore}
              value={noteScore}
              onChange={(e) => setNoteScore(e.target.value === '' ? '' : Number(e.target.value))}
              placeholder={`من 0 إلى ${maxNoteScore}`}
              className="bg-white"
            />
          </div>
        </div>

        {/* Notes Textarea */}
        <div className="space-y-2 text-right">
          <div className="flex items-center justify-between">
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider">
              ملاحظات الافتقاد واحتياجات المخدوم
            </label>
            <span className="text-[11px] text-gray-400">اختياري</span>
          </div>

          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full px-4 py-3 rounded-2xl border border-gray-200 text-sm transition focus:outline-none focus:ring-2 focus:border-primary-500 focus:ring-primary-100 bg-white min-h-[90px]"
            placeholder="اكتب أي تفاصيل خاصة بافتقاد هذا الأسبوع..."
          />
        </div>
      </form>
    </Drawer>
  );
};
