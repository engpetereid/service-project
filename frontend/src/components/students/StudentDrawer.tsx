import React, { useState, useEffect } from 'react';
import { useAuth } from '../../auth/useAuth';
import { usePermissions } from '../../auth/usePermissions';
import { studentsApi } from '../../api/students.api';
import { servantsApi } from '../../api/servants.api';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { StudentResponse } from '../../types/student.types';
import { ServantResponse } from '../../types/staff.types';
import { GradeClassResponse, MinistryResponse } from '../../types/ministry.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Badge } from '../ui/Badge';
import { Alert } from '../ui/Alert';
import { formatDate } from '../../utils/date';
import { GENDER_LABELS, STUDENT_STATUS_LABELS } from '../../utils/arabic';
import {
  Edit3,
  Trash2,
  Phone,
  MapPin,
  Calendar,
  Building,
  School,
  UserCheck,
  Sparkles,
  FileText,
  UserPlus,
  MessageCircle,
  Clock,
  Heart,
  UserX,
  XCircle,
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';

interface StudentDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  student: StudentResponse | null;
  onSaved: () => void;
  preselectedMinistryId?: number | null;
  preselectedClassId?: number | null;
}

export const StudentDrawer: React.FC<StudentDrawerProps> = ({
  isOpen,
  onClose,
  student,
  onSaved,
  preselectedMinistryId,
  preselectedClassId,
}) => {
  const { user } = useAuth();
  const { isAdmin, isServiceSecretary, isClassSecretary, managedMinistryId, managedClassId } =
    usePermissions();

  const isMinistryDisabled = !isAdmin && (!!managedMinistryId || !!student);
  const isClassDisabled = !isAdmin && (!!managedClassId || (!isServiceSecretary && !!student));

  const [activeTab, setActiveTab] = useState<'info' | 'placement' | 'notes'>('info');
  const [isEditMode, setIsEditMode] = useState(!student);
  const [isReassigning, setIsReassigning] = useState(false);
  const [selectedNewServantId, setSelectedNewServantId] = useState<number | '' | 'NONE'>('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE'>('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [confessionFather, setConfessionFather] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [talents, setTalents] = useState('');
  const [additionalDetails, setAdditionalDetails] = useState('');
  const [ministryId, setMinistryId] = useState<number | ''>('');
  const [classId, setClassId] = useState<number | ''>('');

  // Fetch ministries and classes for dropdowns
  const { data: ministries = [] } = useQuery<MinistryResponse[]>({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isOpen && (isEditMode || !student),
  });

  const { data: classes = [] } = useQuery<GradeClassResponse[]>({
    queryKey: ['classes', ministryId],
    queryFn: () => classesApi.findAll(ministryId ? Number(ministryId) : undefined),
    enabled: isOpen && !!ministryId && (isEditMode || !student),
  });

  // Fetch available servants in the target class
  const targetClassId = student ? student.classId : classId;
  const { data: classServants = [] } = useQuery<ServantResponse[]>({
    queryKey: ['servants', 'by-class', targetClassId],
    queryFn: () => servantsApi.findAll({ classId: Number(targetClassId) }),
    enabled: isOpen && !!targetClassId && (isReassigning || !student),
  });

  useEffect(() => {
    if (student) {
      setFullName(student.fullName);
      setPhone(student.phone);
      setGender(student.gender);
      setDateOfBirth(student.dateOfBirth || '');
      setAddress(student.address || '');
      setConfessionFather(student.confessionFather || '');
      setGuardianPhone(student.guardianPhone || '');
      setTalents(student.talents || '');
      setAdditionalDetails(student.additionalDetails || '');
      setMinistryId(student.ministryId || '');
      setClassId(student.classId || '');
      setIsEditMode(false);
      setIsReassigning(false);
      setSelectedNewServantId(student.responsibleServantId ?? student.servantId ?? '');
      setActiveTab('info');
    } else {
      setFullName('');
      setPhone('');
      setGender('MALE');
      setDateOfBirth('');
      setAddress('');
      setConfessionFather('');
      setGuardianPhone('');
      setTalents('');
      setAdditionalDetails('');
      setMinistryId(preselectedMinistryId || managedMinistryId || '');
      setClassId(preselectedClassId || managedClassId || '');
      setIsEditMode(true);
      setIsReassigning(false);
      setSelectedNewServantId('');
      setActiveTab('info');
    }
    setError(null);
    setShowDeleteConfirm(false);
  }, [student, isOpen, preselectedMinistryId, preselectedClassId, managedMinistryId, managedClassId]);

  const canAssignServant = isAdmin || isServiceSecretary || isClassSecretary;
  const canDelete = isAdmin || isServiceSecretary || isClassSecretary;

  // Calculate age if date of birth exists
  const calculateAge = (dobString?: string) => {
    if (!dobString) return null;
    const dob = new Date(dobString);
    if (isNaN(dob.getTime())) return null;
    const diffMs = Date.now() - dob.getTime();
    const ageDate = new Date(diffMs);
    return Math.abs(ageDate.getUTCFullYear() - 1970);
  };

  const studentAge = calculateAge(student?.dateOfBirth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!fullName.trim() || !phone.trim() || !ministryId || !classId) {
      setError('يرجى ملء جميع الحقول المطلوبة (الاسم، الهاتف، الخدمة، والفصل). لا يُسمح بإضافة مخدوم غير مسكن بفصل.');
      return;
    }

    try {
      setIsSubmitting(true);
      if (student) {
        await studentsApi.update(student.id, {
          fullName: fullName.trim(),
          phone: phone.trim(),
          gender,
          dateOfBirth: dateOfBirth || undefined,
          address: address.trim() || undefined,
          confessionFather: confessionFather.trim() || undefined,
          ministryId: isAdmin ? Number(ministryId) : undefined,
          classId: isAdmin ? Number(classId) : undefined,
          guardianPhone: guardianPhone.trim() || undefined,
          talents: talents.trim() || undefined,
          additionalDetails: additionalDetails.trim() || undefined,
        });
      } else {
        const assignedServant = selectedNewServantId && selectedNewServantId !== 'NONE'
          ? Number(selectedNewServantId)
          : null;

        await studentsApi.create({
          fullName: fullName.trim(),
          phone: phone.trim(),
          gender,
          dateOfBirth: dateOfBirth || undefined,
          address: address.trim() || undefined,
          confessionFather: confessionFather.trim() || undefined,
          ministryId: Number(ministryId),
          classId: Number(classId),
          servantId: assignedServant,
          guardianPhone: guardianPhone.trim() || undefined,
          talents: talents.trim() || undefined,
          additionalDetails: additionalDetails.trim() || undefined,
        });
      }

      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'حدث خطأ أثناء حفظ البيانات');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReassignServant = async () => {
    if (!student) return;
    try {
      setIsSubmitting(true);
      const servantToSet = selectedNewServantId === 'NONE' || selectedNewServantId === ''
        ? null
        : Number(selectedNewServantId);

      const studentId = student.id ?? student.personId!;
      await studentsApi.changeAssignment(studentId, {
        servantId: servantToSet,
      });
      setIsReassigning(false);
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل تغيير الخادم المسؤول');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!student) return;
    try {
      setIsDeleting(true);
      const studentId = student.id ?? student.personId!;
      await studentsApi.softDelete(studentId);
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل أرشفة المخدوم');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={student ? (isEditMode ? 'تعديل بيانات مخدوم' : student.fullName) : 'إضافة مخدوم جديد'}
      subtitle={
        student?.className
          ? `${student.ministryName} • ${student.className}`
          : preselectedClassId
          ? 'إضافة إلى الفصل المحدد'
          : undefined
      }
      size="lg"
      footer={
        isEditMode ? (
          <div className="w-full flex items-center justify-end gap-3">
            <Button
              variant="outline"
              type="button"
              onClick={() => {
                if (student) setIsEditMode(false);
                else onClose();
              }}
              disabled={isSubmitting}
            >
              إلغاء
            </Button>
            <Button
              variant="primary"
              type="button"
              onClick={handleSubmit}
              isLoading={isSubmitting}
              className="font-bold"
            >
              {student ? 'حفظ التعديلات' : 'إضافة المخدوم'}
            </Button>
          </div>
        ) : (
          <div className="w-full flex items-center justify-between">
            {canDelete && (
              <Button
                variant="danger"
                size="sm"
                onClick={() => setShowDeleteConfirm(true)}
                className="bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 font-medium"
              >
                <Trash2 className="w-4 h-4 ml-1.5" />
                أرشفة المخدوم
              </Button>
            )}
            <div className="flex gap-2">
              <Button variant="outline" onClick={onClose}>
                إغلاق
              </Button>
              <Button variant="primary" onClick={() => setIsEditMode(true)} className="font-bold">
                <Edit3 className="w-4 h-4 ml-1.5" />
                تعديل البيانات
              </Button>
            </div>
          </div>
        )
      }
    >
      <div className="space-y-5 text-right">
        {error && <Alert variant="error">{error}</Alert>}

        {/* Delete Confirmation */}
        {showDeleteConfirm && (
          <div className="p-4 rounded-2xl bg-red-50 border border-red-200 space-y-3">
            <h4 className="font-bold text-red-900">تأكيد أرشفة المخدوم</h4>
            <p className="text-xs text-red-700 leading-relaxed">
              سيتم نقل المخدوم إلى الأرشيف وإيقاف ظهوره في قوائم الافتقاد الأسبوعي، مع الحفاظ التام
              على كامل سجلات الافتقاد والحضور والاعتراف السابقة.
            </p>
            <div className="flex gap-2 justify-end pt-1">
              <Button
                size="sm"
                variant="outline"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={isDeleting}
              >
                تراجع
              </Button>
              <Button
                size="sm"
                variant="danger"
                onClick={handleDelete}
                isLoading={isDeleting}
                className="font-bold"
              >
                نعم، نقل للأرشيف
              </Button>
            </div>
          </div>
        )}

        {isEditMode ? (
          /* Edit / Create Form */
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Section 1: Basic Info */}
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider pb-1 border-b border-gray-100">
                1. البيانات الشخصية
              </h4>

              <Input
                label="الاسم بالكامل *"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                placeholder="اسم المخدوم ثلاثي أو رباعي"
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="رقم هاتف المخدوم *"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  dir="ltr"
                  placeholder="010XXXXXXXX"
                  className="text-left font-mono"
                />

                <Input
                  label="رقم هاتف ولي الأمر"
                  value={guardianPhone}
                  onChange={(e) => setGuardianPhone(e.target.value)}
                  dir="ltr"
                  placeholder="010XXXXXXXX"
                  className="text-left font-mono"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Select
                  label="النوع *"
                  value={gender}
                  onChange={(e) => setGender(e.target.value as 'MALE' | 'FEMALE')}
                  options={[
                    { value: 'MALE', label: 'ذكر' },
                    { value: 'FEMALE', label: 'أنثى' },
                  ]}
                />

                <Input
                  type="date"
                  label="تاريخ الميلاد"
                  value={dateOfBirth}
                  onChange={(e) => setDateOfBirth(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="أب الاعتراف"
                  value={confessionFather}
                  onChange={(e) => setConfessionFather(e.target.value)}
                  placeholder="اسم أب الاعتراف"
                />

                <Input
                  label="العنوان ومحل السكن"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="المنطقة / الشارع"
                />
              </div>
            </div>

            {/* Section 2: Placement */}
            <div className="space-y-4 pt-2">
              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider pb-1 border-b border-gray-100">
                2. التسكين والخدمة والفصل
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Select
                  label="الخدمة *"
                  value={ministryId}
                  onChange={(e) => {
                    setMinistryId(e.target.value ? Number(e.target.value) : '');
                    setClassId('');
                  }}
                  disabled={isMinistryDisabled}
                  options={
                    ministries.length > 0
                      ? ministries.map((m) => ({ value: m.id, label: m.name }))
                      : (ministryId ? [{ value: Number(ministryId), label: student?.ministryName || 'الخدمة المصرح بها' }] : [])
                  }
                  placeholder="اختر الخدمة..."
                />

                <Select
                  label="الفصل *"
                  value={classId}
                  onChange={(e) => setClassId(e.target.value ? Number(e.target.value) : '')}
                  disabled={isClassDisabled || !ministryId}
                  options={
                    classes.length > 0
                      ? classes.map((c) => ({ value: c.id, label: c.name }))
                      : (classId ? [{ value: Number(classId), label: student?.className || 'الفصل المصرح به' }] : [])
                  }
                  placeholder={
                    ministryId
                      ? (classes.length > 0 ? 'اختر الفصل *' : 'لا توجد فصول معرفة في هذه الخدمة')
                      : 'اختر الخدمة أولاً'
                  }
                  required
                />
              </div>

              {ministryId && classes.length === 0 && (
                <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800">
                  تنبيه: لا توجد فصول دراسية معرفة في هذه الخدمة حتى الآن. يجب إضافة فصل دراسي أولاً في شاشة الخدمات قبل إضافة مخدومين.
                </div>
              )}

              {!student && canAssignServant && classId && (
                <div>
                  <label className="text-xs font-bold text-gray-700 block mb-1.5">
                    الخادم المسؤول المبدئي (اختياري):
                  </label>
                  <Select
                    value={selectedNewServantId}
                    onChange={(e) =>
                      setSelectedNewServantId(
                        e.target.value ? (e.target.value as unknown as number) : ''
                      )
                    }
                    options={classServants.map((s) => {
                      const sid = s.personId ?? s.id;
                      const isSecretary = s.isClassSecretary;
                      const isMe = user?.personId === sid;
                      let label = s.fullName;
                      if (isSecretary) {
                        label += isMe ? ' (أمين الفصل - أنت)' : ' (أمين الفصل)';
                      } else if (isMe) {
                        label += ' (أنت)';
                      }
                      return { value: sid, label };
                    })}
                    placeholder="بدون تعيين خادم حالياً (يمكن التعيين لاحقاً)"
                  />
                  <p className="text-[11px] text-gray-400 mt-1">
                    يتم عرض الخدام المسكنين في هذا الفصل فقط.
                  </p>
                </div>
              )}
            </div>

            {/* Section 3: Talents & Notes */}
            <div className="space-y-4 pt-2">
              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider pb-1 border-b border-gray-100">
                3. المواهب والملاحظات
              </h4>

              <Input
                label="المواهب والاهتمامات"
                value={talents}
                onChange={(e) => setTalents(e.target.value)}
                placeholder="ألحان، ترانيم، تمثيل، كشافة، رسم..."
              />

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1.5">
                  ملاحظات إضافية عن المخدوم
                </label>
                <textarea
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm transition focus:outline-none focus:ring-2 focus:border-primary-500 focus:ring-primary-100 bg-white min-h-[80px]"
                  value={additionalDetails}
                  onChange={(e) => setAdditionalDetails(e.target.value)}
                  placeholder="أي تفاصيل صحية أو دراسية أو اجتماعية هامة..."
                />
              </div>
            </div>
          </form>
        ) : (
          /* View Mode with Tabs */
          <div className="space-y-5">
            {/* Student Header Card */}
            <div className="bg-gradient-to-l from-purple-50 via-indigo-50/40 to-white rounded-2xl p-4 border border-purple-100 flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-2xl bg-purple-600 text-white flex items-center justify-center font-black text-lg shadow-sm">
                  {student?.fullName?.charAt(0) || 'م'}
                </div>
                <div>
                  <h3 className="font-black text-gray-900 text-base">{student?.fullName}</h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-gray-500 font-mono" dir="ltr">
                      {student?.phone}
                    </span>
                    {studentAge !== null && (
                      <span className="text-[11px] font-bold text-purple-700 bg-purple-100/70 px-2 py-0.5 rounded-md">
                        {studentAge} سنة
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex flex-col items-end gap-1.5">
                <Badge variant={student?.status === 'ACTIVE' ? 'success' : 'warning'}>
                  {student ? STUDENT_STATUS_LABELS[student.status] : '—'}
                </Badge>
                {!student?.active && (
                  <Badge variant="danger" className="text-[10px]">
                    مؤرشف
                  </Badge>
                )}
              </div>
            </div>

            {/* Tab Navigation */}
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <button
                type="button"
                onClick={() => setActiveTab('info')}
                className={`text-xs font-bold px-3 py-2 rounded-xl transition ${
                  activeTab === 'info'
                    ? 'bg-primary-600 text-white shadow-sm'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                البيانات والتواصل
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('placement')}
                className={`text-xs font-bold px-3 py-2 rounded-xl transition flex items-center gap-1.5 ${
                  activeTab === 'placement'
                    ? 'bg-primary-600 text-white shadow-sm'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                <span>التسكين والخدمة</span>
                {!(student?.responsibleServantId || student?.servantId) && (
                  <span className="w-2 h-2 rounded-full bg-amber-500" />
                )}
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('notes')}
                className={`text-xs font-bold px-3 py-2 rounded-xl transition ${
                  activeTab === 'notes'
                    ? 'bg-primary-600 text-white shadow-sm'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                المواهب والملاحظات
              </button>
            </div>

            {/* Tab 1: Personal Info & Contact */}
            {activeTab === 'info' && (
              <div className="space-y-4">
                {/* Fast Contact Action Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {/* Student Phone */}
                  <div className="p-3 bg-white rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
                    <div>
                      <span className="text-[11px] font-bold text-gray-400 block">هاتف المخدوم</span>
                      <span className="font-mono text-xs font-bold text-gray-900" dir="ltr">
                        {student?.phone}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <a
                        href={`tel:${student?.phone}`}
                        className="p-2 rounded-lg bg-gray-50 hover:bg-gray-100 text-gray-600 transition"
                        title="اتصال هاتفي"
                      >
                        <Phone className="w-3.5 h-3.5" />
                      </a>
                      <a
                        href={`https://wa.me/2${student?.phone?.replace(/\D/g, '')}`}
                        target="_blank"
                        rel="noreferrer"
                        className="p-2 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-600 transition"
                        title="محادثة واتساب"
                      >
                        <MessageCircle className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  </div>

                  {/* Guardian Phone */}
                  <div className="p-3 bg-white rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
                    <div>
                      <span className="text-[11px] font-bold text-gray-400 block">هاتف ولي الأمر</span>
                      <span className="font-mono text-xs font-bold text-gray-900" dir="ltr">
                        {student?.guardianPhone || 'غير مسجل'}
                      </span>
                    </div>
                    {student?.guardianPhone && (
                      <div className="flex items-center gap-1.5">
                        <a
                          href={`tel:${student?.guardianPhone}`}
                          className="p-2 rounded-lg bg-gray-50 hover:bg-gray-100 text-gray-600 transition"
                          title="اتصال بولي الأمر"
                        >
                          <Phone className="w-3.5 h-3.5" />
                        </a>
                        <a
                          href={`https://wa.me/2${student?.guardianPhone?.replace(/\D/g, '')}`}
                          target="_blank"
                          rel="noreferrer"
                          className="p-2 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-600 transition"
                          title="محادثة ولي الأمر واتساب"
                        >
                          <MessageCircle className="w-3.5 h-3.5" />
                        </a>
                      </div>
                    )}
                  </div>
                </div>

                {/* Details List */}
                <div className="divide-y divide-gray-100 rounded-2xl border border-gray-100 bg-white overflow-hidden text-xs">
                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-gray-500">النوع</span>
                    <span className="font-bold text-gray-900">
                      {student ? GENDER_LABELS[student.gender] : '—'}
                    </span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-gray-400" />
                      تاريخ الميلاد
                    </span>
                    <span className="font-bold text-gray-900">
                      {student?.dateOfBirth ? formatDate(student.dateOfBirth) : 'غير مسجل'}
                    </span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <Heart className="w-3.5 h-3.5 text-gray-400" />
                      أب الاعتراف
                    </span>
                    <span className="font-bold text-gray-900">
                      {student?.confessionFather || 'غير مسجل'}
                    </span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-gray-400" />
                      العنوان
                    </span>
                    <span className="font-bold text-gray-900">{student?.address || 'غير مسجل'}</span>
                  </div>
                </div>
              </div>
            )}

            {/* Tab 2: Placement & Responsible Servant */}
            {activeTab === 'placement' && (
              <div className="space-y-4">
                {/* Service & Class info */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm flex items-center gap-3">
                    <Building className="w-5 h-5 text-gray-400 shrink-0" />
                    <div>
                      <span className="block text-[11px] text-gray-400">الخدمة</span>
                      <span className="text-xs font-bold text-gray-900">
                        {student?.ministryName || '—'}
                      </span>
                    </div>
                  </div>

                  <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm flex items-center gap-3">
                    <School className="w-5 h-5 text-gray-400 shrink-0" />
                    <div>
                      <span className="block text-[11px] text-gray-400">الفصل</span>
                      <span className="text-xs font-bold text-gray-900">
                        {student?.className || '-'}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Responsible Servant Card */}
                <div className="bg-primary-50/50 rounded-2xl p-4 border border-primary-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-primary-900 flex items-center gap-1.5">
                      <UserCheck className="w-4 h-4 text-primary-600" />
                      الخادم المسؤول عن الافتقاد والمتابعة
                    </span>
                    {canAssignServant && !isReassigning && (
                      <button
                        type="button"
                        onClick={() => setIsReassigning(true)}
                        className="text-xs text-primary-700 hover:text-primary-900 font-bold flex items-center gap-1"
                      >
                        <UserPlus className="w-3.5 h-3.5" />
                        {(student?.responsibleServantName || student?.servantName) ? 'تغيير الخادم' : 'تعيين خادم'}
                      </button>
                    )}
                  </div>

                  {isReassigning ? (
                    <div className="space-y-3 pt-1">
                      <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                        {/* Option: Clear Servant */}
                        <label
                          className={`flex items-center justify-between p-3 rounded-xl border text-xs cursor-pointer transition ${
                            selectedNewServantId === 'NONE'
                              ? 'border-primary-600 bg-white ring-1 ring-primary-600'
                              : 'border-gray-200 bg-white hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <input
                              type="radio"
                              name="servantDrawerAssign"
                              checked={selectedNewServantId === 'NONE'}
                              onChange={() => setSelectedNewServantId('NONE')}
                              className="text-primary-600"
                            />
                            <span className="font-bold text-gray-700">
                              بدون خادم مسؤول (إلغاء التعيين)
                            </span>
                          </div>
                          <UserX className="w-4 h-4 text-gray-400" />
                        </label>

                        {/* Servants in this class */}
                        {classServants.map((s) => {
                          const servantId = s.personId ?? s.id;
                          const isSecretary = s.isClassSecretary;
                          const isMe = user?.personId === servantId;
                          return (
                            <label
                              key={servantId}
                              className={`flex items-center justify-between p-3 rounded-xl border text-xs cursor-pointer transition ${
                                selectedNewServantId === servantId
                                  ? 'border-primary-600 bg-white ring-1 ring-primary-600'
                                  : 'border-gray-200 bg-white hover:bg-gray-50'
                              }`}
                            >
                              <div className="flex items-center gap-2">
                                <input
                                  type="radio"
                                  name="servantDrawerAssign"
                                  checked={selectedNewServantId === servantId}
                                  onChange={() => setSelectedNewServantId(servantId!)}
                                  className="text-primary-600"
                                />
                                <div>
                                  <div className="flex items-center gap-1.5">
                                    <span className="font-bold text-gray-900 block">{s.fullName}</span>
                                    {isSecretary && (
                                      <span className="px-1.5 py-0.5 text-[9px] font-bold bg-purple-100 text-purple-700 rounded-md">
                                        {isMe ? 'أمين الفصل (أنت)' : 'أمين الفصل'}
                                      </span>
                                    )}
                                    {!isSecretary && isMe && (
                                      <span className="px-1.5 py-0.5 text-[9px] font-medium bg-blue-100 text-blue-700 rounded-md">
                                        (أنت)
                                      </span>
                                    )}
                                  </div>
                                  <span className="text-[10px] text-gray-500 font-mono" dir="ltr">
                                    {s.phone}
                                  </span>
                                </div>
                              </div>
                              <UserCheck className="w-4 h-4 text-primary-600" />
                            </label>
                          );
                        })}
                      </div>

                      <div className="flex justify-end gap-2 pt-1">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setIsReassigning(false)}
                          disabled={isSubmitting}
                        >
                          إلغاء
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={handleReassignServant}
                          isLoading={isSubmitting}
                          className="font-bold"
                        >
                          حفظ التعيين
                        </Button>
                      </div>
                    </div>
                  ) : (student?.responsibleServantName || student?.servantName) ? (
                    <div className="bg-white p-3 rounded-xl border border-primary-100 flex items-center justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-xs">
                          {(student.responsibleServantName || student.servantName)?.charAt(0)}
                        </div>
                        <div>
                          <span className="font-bold text-gray-900 text-xs block">
                            {student.responsibleServantName || student.servantName}
                          </span>
                          {student.assignedAt && (
                            <span className="text-[10px] text-gray-400 flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              تم الإسناد: {formatDate(student.assignedAt)}
                            </span>
                          )}
                        </div>
                      </div>
                      <Badge variant="primary">متابع معتمد</Badge>
                    </div>
                  ) : (
                    <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <XCircle className="w-4 h-4 text-amber-600 shrink-0" />
                        <span>لم يتم إسناد هذا المخدوم لأي خادم مسؤول بعد.</span>
                      </div>
                      {canAssignServant && (
                        <button
                          type="button"
                          onClick={() => setIsReassigning(true)}
                          className="text-xs font-bold text-amber-900 underline hover:no-underline shrink-0"
                        >
                          تعيين الآن
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Tab 3: Talents & Notes */}
            {activeTab === 'notes' && (
              <div className="space-y-4 text-xs">
                <div className="p-4 bg-white rounded-2xl border border-gray-100 shadow-sm space-y-2">
                  <div className="flex items-center gap-2 text-amber-600 font-bold">
                    <Sparkles className="w-4 h-4" />
                    <span>المواهب والاهتمامات</span>
                  </div>
                  <p className="text-gray-700 leading-relaxed">
                    {student?.talents || 'لا توجد مواهب مسجلة لهذا المخدوم.'}
                  </p>
                </div>

                <div className="p-4 bg-white rounded-2xl border border-gray-100 shadow-sm space-y-2">
                  <div className="flex items-center gap-2 text-gray-600 font-bold">
                    <FileText className="w-4 h-4" />
                    <span>ملاحظات عامة وإضافية</span>
                  </div>
                  <p className="text-gray-700 leading-relaxed">
                    {student?.additionalDetails || 'لا توجد ملاحظات إضافية مسجلة.'}
                  </p>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Drawer>
  );
};
