import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { studentsApi } from '../../api/students.api';
import { confessionApi } from '../../api/confession.api';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Alert } from '../ui/Alert';
import { Calendar, User, FileText, HeartHandshake } from 'lucide-react';

interface ConfessionDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  student?: { id: number; name: string; confessionFather?: string | null } | null;
  preselectedStudentId?: number | null;
  onSaved: () => void;
}

export const ConfessionDrawer: React.FC<ConfessionDrawerProps> = ({
  isOpen,
  onClose,
  student,
  preselectedStudentId,
  onSaved,
}) => {
  const todayStr = new Date().toISOString().split('T')[0];

  const [studentId, setStudentId] = useState<number | ''>('');
  const [confessionDate, setConfessionDate] = useState<string>(todayStr);
  const [confessionFather, setConfessionFather] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Query students for selection if no fixed student is provided
  const { data: students = [] } = useQuery({
    queryKey: ['students', 'all-active-confessions'],
    queryFn: () => studentsApi.findAll(),
    enabled: isOpen && !student,
  });

  const effectiveStudentId = student?.id || preselectedStudentId || studentId;

  useEffect(() => {
    if (student) {
      setStudentId(student.id);
      setConfessionFather(student.confessionFather || '');
    } else if (preselectedStudentId) {
      setStudentId(preselectedStudentId);
      const matched = students.find((s) => s.id === preselectedStudentId);
      if (matched?.confessionFather) {
        setConfessionFather(matched.confessionFather);
      }
    } else {
      setStudentId('');
      setConfessionFather('');
    }
    setConfessionDate(todayStr);
    setNotes('');
    setError(null);
  }, [student, preselectedStudentId, isOpen, students, todayStr]);

  const handleStudentChange = (id: number | '') => {
    setStudentId(id);
    if (id) {
      const matched = students.find((s) => s.id === id);
      if (matched?.confessionFather) {
        setConfessionFather(matched.confessionFather);
      }
    }
  };

  // Quick date shortcuts
  const handleSetQuickDate = (daysAgo: number) => {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    setConfessionDate(d.toISOString().split('T')[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const finalStudentId = Number(effectiveStudentId);
    if (!finalStudentId || !confessionDate) {
      setError('يرجى تحديد المخدوم وتاريخ الاعتراف');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      await confessionApi.create({
        studentId: finalStudentId,
        confessionDate,
        confessionFather: confessionFather.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل تسجيل الاعتراف');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={student ? `تسجيل اعتراف: ${student.name}` : 'تسجيل اعتراف مخدوم'}
      subtitle="توثيق جلسة سر التوبة والاعتراف لمتابعة الرعاية الروحية"
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
            className="font-bold bg-purple-600 hover:bg-purple-700 min-h-[44px]"
          >
            <HeartHandshake className="w-4 h-4 ml-1.5" />
            حفظ جلسة الاعتراف
          </Button>
        </>
      }
    >
      {error && <Alert variant="error">{error}</Alert>}

      {/* Confidentiality Reminder */}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Student Selector or Readonly Banner */}
        {student ? (
          <div className="bg-gray-50 border border-gray-100 rounded-2xl p-3 flex items-center justify-between">
            <div>
              <span className="text-[11px] text-gray-400 block">المخدوم:</span>
              <strong className="text-sm text-gray-900">{student.name}</strong>
            </div>
            <span className="text-xs bg-purple-100 text-purple-800 font-bold px-2.5 py-1 rounded-xl">
              تحديد تلقائي
            </span>
          </div>
        ) : (
          <Select
            label="المخدوم *"
            value={studentId}
            onChange={(e) => handleStudentChange(e.target.value ? Number(e.target.value) : '')}
            options={students.map((s) => ({
              value: s.id,
              label: `${s.fullName} (${s.className || 'بدون فصل'})`,
            }))}
            placeholder="اختر المخدوم..."
            required
          />
        )}

        {/* Confession Date with Quick Shortcuts */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-sm font-semibold text-gray-700 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-purple-600" />
              تاريخ الاعتراف *
            </label>
            {/* Quick shortcuts */}
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => handleSetQuickDate(0)}
                className="text-[11px] px-2 py-0.5 rounded-lg bg-gray-100 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
              >
                اليوم
              </button>
              <button
                type="button"
                onClick={() => handleSetQuickDate(1)}
                className="text-[11px] px-2 py-0.5 rounded-lg bg-gray-100 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
              >
                أمس
              </button>
              <button
                type="button"
                onClick={() => handleSetQuickDate(7)}
                className="text-[11px] px-2 py-0.5 rounded-lg bg-gray-100 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
              >
                قبل أسبوع
              </button>
            </div>
          </div>

          <Input
            type="date"
            value={confessionDate}
            onChange={(e) => setConfessionDate(e.target.value)}
            required
            className="bg-white"
          />
        </div>

        {/* Confession Father */}
        <div className="space-y-1">
          <label className="text-sm font-semibold text-gray-700 flex items-center gap-1.5">
            <User className="w-4 h-4 text-purple-600" />
            أب الاعتراف
          </label>
          <Input
            value={confessionFather}
            onChange={(e) => setConfessionFather(e.target.value)}
            placeholder="مثال: أبونا بيشوي كامل"
            helperText="اسم الكاهن أب اعتراف المخدوم (سيتم حفظه كأب اعتراف افتراضي له)"
            className="bg-white"
          />
        </div>

        {/* Pastoral Notes */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-sm font-semibold text-gray-700 flex items-center gap-1.5">
              <FileText className="w-4 h-4 text-purple-600" />
              ملاحظات وتوجيهات المتابعة (اختياري)
            </label>
            <span className="text-[11px] text-gray-400">اختياري</span>
          </div>

          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full px-4 py-3 rounded-2xl border border-gray-200 text-sm transition focus:outline-none focus:ring-2 focus:border-purple-500 focus:ring-purple-100 bg-white min-h-[90px]"
            placeholder="أي ملاحظات عامة حول موعد الاعتراف القادم أو المتابعة الروحية..."
          />
        </div>
      </form>
    </Drawer>
  );
};
