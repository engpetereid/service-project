import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { studentsApi } from '../../api/students.api';
import { confessionApi } from '../../api/confession.api';
import { StudentResponse } from '../../types/student.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { Badge } from '../ui/Badge';
import { Spinner } from '../ui/Spinner';
import { useDebounce } from '../../hooks/useDebounce';
import {
  Calendar,
  User,
  Search,
  Plus,
  Trash2,
  Check,
  CheckCircle2,
  Users,
  FileText,
  HeartHandshake,
  School,
} from 'lucide-react';

interface ConfessionSessionDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  existingFathers?: string[];
}

interface SelectedStudentItem {
  id: number;
  fullName: string;
  phone?: string;
  className?: string | null;
  confessionFather?: string | null;
}

export const ConfessionSessionDrawer: React.FC<ConfessionSessionDrawerProps> = ({
  isOpen,
  onClose,
  onSaved,
  existingFathers = [],
}) => {
  const todayStr = new Date().toISOString().split('T')[0];

  const [sessionDate, setSessionDate] = useState<string>(todayStr);
  const [confessionFather, setConfessionFather] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [selectedStudents, setSelectedStudents] = useState<SelectedStudentItem[]>([]);

  // Search state for selecting students
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 250);

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Live student search query (strictly search-based)
  const { data: searchResults = [], isLoading: isSearching } = useQuery<StudentResponse[]>({
    queryKey: ['students-session-search', debouncedSearch],
    queryFn: () => studentsApi.findAll({ search: debouncedSearch.trim() }),
    enabled: isOpen && debouncedSearch.trim().length >= 2,
  });

  // Reset or initialize state when opening
  useEffect(() => {
    if (isOpen) {
      setSessionDate(todayStr);
      setConfessionFather('');
      setNotes('');
      setSelectedStudents([]);
      setSearchQuery('');
      setError(null);
    }
  }, [isOpen, todayStr]);

  const handleAddStudent = (student: StudentResponse) => {
    if (selectedStudents.some((s) => s.id === student.id)) return;

    setSelectedStudents((prev) => [
      ...prev,
      {
        id: student.id,
        fullName: student.fullName,
        phone: student.phone,
        className: student.className,
        confessionFather: student.confessionFather,
      },
    ]);

    // If father is not yet entered and student has a confession father, suggest it
    if (!confessionFather.trim() && student.confessionFather) {
      setConfessionFather(student.confessionFather);
    }
  };

  const handleRemoveStudent = (studentId: number) => {
    setSelectedStudents((prev) => prev.filter((s) => s.id !== studentId));
  };

  const handleSetQuickDate = (daysAgo: number) => {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    setSessionDate(d.toISOString().split('T')[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!sessionDate) {
      setError('يرجى تحديد تاريخ الجلسة');
      return;
    }

    if (!confessionFather.trim()) {
      setError('يرجى إدخال اسم الأب الكاهن');
      return;
    }

    if (selectedStudents.length === 0) {
      setError('يرجى اختيار مخدوم واحد على الأقل عبر البحث لإضافته في الجلسة');
      return;
    }

    try {
      setIsSubmitting(true);

      await confessionApi.createSession({
        sessionDate,
        confessionFather: confessionFather.trim(),
        studentIds: selectedStudents.map((s) => s.id),
        notes: notes.trim() || undefined,
      });

      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل حفظ وتسجيل الجلسة');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title="تسجيل جلسة اعترافات جماعية"
      subtitle="إنشاء جلسة سر الاعتراف وتحديد الكاهن وإضافة المخدومين بالبحث"
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <div className="text-xs text-gray-500 font-medium flex items-center gap-1.5">
            <Users className="w-4 h-4 text-purple-600" />
            <span>إجمالي المخدومين في الجلسة:</span>
            <strong className="text-purple-900 font-bold bg-purple-100 px-2 py-0.5 rounded-full">
              {selectedStudents.length}
            </strong>
          </div>

          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
              إلغاء
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={isSubmitting}
              disabled={selectedStudents.length === 0 || !confessionFather.trim()}
              className="font-bold bg-purple-600 hover:bg-purple-700 min-h-[44px]"
            >
              <HeartHandshake className="w-4 h-4 ml-1.5" />
              حفظ وتسجيل الجلسة ({selectedStudents.length})
            </Button>
          </div>
        </div>
      }
    >
      {error && <Alert variant="error">{error}</Alert>}

      {/* Confidentiality Reminder */}

      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Section 1: Session Details */}
        <div className="bg-gray-50/80 border border-gray-100 rounded-2xl p-4 space-y-4">
          <h4 className="font-bold text-xs text-gray-700 uppercase tracking-wider flex items-center gap-1.5">
            <Calendar className="w-4 h-4 text-purple-600" />
            بيانات الجلسة وأب الاعتراف
          </h4>

          {/* Date with quick shortcuts */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-gray-700">تاريخ الجلسة *</label>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => handleSetQuickDate(0)}
                  className="text-[11px] px-2 py-0.5 rounded-lg bg-white border border-gray-200 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
                >
                  اليوم
                </button>
                <button
                  type="button"
                  onClick={() => handleSetQuickDate(1)}
                  className="text-[11px] px-2 py-0.5 rounded-lg bg-white border border-gray-200 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
                >
                  أمس
                </button>
                <button
                  type="button"
                  onClick={() => handleSetQuickDate(7)}
                  className="text-[11px] px-2 py-0.5 rounded-lg bg-white border border-gray-200 hover:bg-purple-100 hover:text-purple-700 transition font-bold"
                >
                  قبل أسبوع
                </button>
              </div>
            </div>
            <Input
              type="date"
              value={sessionDate}
              onChange={(e) => setSessionDate(e.target.value)}
              required
              className="bg-white"
            />
          </div>

          {/* Priest Name Input with suggestions */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-gray-700 flex items-center gap-1.5">
              <User className="w-3.5 h-3.5 text-purple-600" />
              اسم الأب الكاهن (أب الاعتراف) *
            </label>
            <Input
              value={confessionFather}
              onChange={(e) => setConfessionFather(e.target.value)}
              placeholder="مثال: أبونا بيشوي كامل"
              required
              className="bg-white font-medium"
            />

            {/* Existing Father Chips */}
            {existingFathers.length > 0 && (
              <div className="flex items-center gap-1.5 flex-wrap pt-1">
                <span className="text-[11px] text-gray-400">اختيار سريع:</span>
                {existingFathers.slice(0, 5).map((f) => (
                  <button
                    key={f}
                    type="button"
                    onClick={() => setConfessionFather(f)}
                    className="text-[11px] px-2 py-0.5 rounded-lg bg-purple-50 text-purple-800 border border-purple-200 hover:bg-purple-100 transition"
                  >
                    {f}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Optional Notes */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-gray-700 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-purple-600" />
                ملاحظات الجلسة / المناسبة (اختياري)
              </label>
            </div>
            <Input
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="مثال: جلسة اعترافات نهضة العذراء"
              className="bg-white text-xs"
            />
          </div>
        </div>

        {/* Section 2: Student Search Only (اختيار المخدومين عبر البحث فقط) */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="font-bold text-xs text-gray-700 uppercase tracking-wider flex items-center gap-1.5">
              <Search className="w-4 h-4 text-purple-600" />
              اختيار المخدومين عبر البحث فقط
            </h4>
            <span className="text-[11px] text-gray-400">
              اكتب اسم المخدوم أو هاتفه للإضافة
            </span>
          </div>

          {/* Search Box */}
          <div className="relative">
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="ابحث باسم المخدوم أو رقم الهاتف لإضافته للجلسة..."
              className="pr-10 bg-white shadow-sm border-purple-200 focus:border-purple-500 focus:ring-purple-100"
            />
            <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-purple-500">
              {isSearching ? <Spinner size="sm" /> : <Search className="w-4 h-4" />}
            </div>
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            )}
          </div>

          {/* Live Search Results Dropdown/Box */}
          {debouncedSearch.trim().length >= 2 && (
            <div className="bg-white rounded-2xl border border-purple-200 shadow-md p-2 space-y-1 max-h-56 overflow-y-auto">
              {isSearching ? (
                <div className="py-4 text-center text-xs text-gray-400 flex items-center justify-center gap-2">
                  <Spinner size="sm" />
                  <span>جاري البحث في قاعدة البيانات...</span>
                </div>
              ) : searchResults.length === 0 ? (
                <div className="py-4 text-center text-xs text-gray-400">
                  لا توجد نتائج بحث مطابقة لـ «{debouncedSearch}»
                </div>
              ) : (
                searchResults.map((st) => {
                  const isAlreadySelected = selectedStudents.some((s) => s.id === st.id);
                  return (
                    <div
                      key={st.id}
                      className="flex items-center justify-between p-2.5 rounded-xl hover:bg-purple-50/60 transition text-xs"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs shrink-0">
                          {st.fullName.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-gray-900">{st.fullName}</p>
                          <div className="flex items-center gap-2 text-[11px] text-gray-500 mt-0.5">
                            {st.className && (
                              <span className="flex items-center gap-0.5">
                                <School className="w-3 h-3 text-gray-400" />
                                {st.className}
                              </span>
                            )}
                            <span className="font-mono text-gray-400" dir="ltr">
                              {st.phone}
                            </span>
                            {st.confessionFather && (
                              <span className="text-purple-600">
                                ({st.confessionFather})
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {isAlreadySelected ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-1 rounded-lg">
                          <Check className="w-3 h-3 text-emerald-600" />
                          مضاف بالجلسة
                        </span>
                      ) : (
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={() => handleAddStudent(st)}
                          className="h-8 text-xs font-bold text-purple-700 border-purple-300 hover:bg-purple-100 hover:border-purple-400"
                        >
                          <Plus className="w-3 h-3 ml-1" />
                          إضافة
                        </Button>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* Section 3: Selected Students in this Session */}
          <div className="pt-2 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-gray-800 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                المخدومين المضافين في الجلسة:
                <Badge variant={selectedStudents.length > 0 ? 'success' : 'neutral'}>
                  {selectedStudents.length} مخدوم
                </Badge>
              </span>

              {selectedStudents.length > 0 && (
                <button
                  type="button"
                  onClick={() => setSelectedStudents([])}
                  className="text-[11px] text-red-500 hover:text-red-700 font-medium"
                >
                  إفراغ القائمة
                </button>
              )}
            </div>

            {selectedStudents.length === 0 ? (
              <div className="p-6 text-center rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50/50 space-y-1">
                <Users className="w-8 h-8 text-gray-300 mx-auto mb-1" />
                <p className="text-xs font-bold text-gray-600">لم يتم اختيار أي مخدوم بعد</p>
                <p className="text-[11px] text-gray-400 max-w-sm mx-auto">
                  استخدم خانة البحث أعلاه للبحث عن المخدوم بالاسم أو رقم الهاتف وإضافته لهذه الجلسة.
                </p>
              </div>
            ) : (
              <div className="space-y-1.5 max-h-64 overflow-y-auto pr-1">
                {selectedStudents.map((item, index) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-2.5 rounded-xl border border-gray-200 bg-white hover:border-purple-200 transition shadow-xs text-xs"
                  >
                    <div className="flex items-center gap-2.5">
                      <span className="w-5 h-5 rounded-full bg-gray-100 text-gray-500 text-[10px] font-bold flex items-center justify-center font-mono">
                        {index + 1}
                      </span>
                      <div>
                        <p className="font-bold text-gray-900">{item.fullName}</p>
                        <div className="flex items-center gap-2 text-[11px] text-gray-500 mt-0.5">
                          {item.className && (
                            <span className="bg-gray-100 text-gray-700 px-1.5 py-0.2 rounded">
                              {item.className}
                            </span>
                          )}
                          <span className="font-mono text-gray-400" dir="ltr">
                            {item.phone}
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleRemoveStudent(item.id)}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition"
                      title="حذف من الجلسة"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </form>
    </Drawer>
  );
};
