import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { servantsApi } from '../../api/servants.api';
import { ServantResponse } from '../../types/staff.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Badge } from '../ui/Badge';
import { Alert } from '../ui/Alert';
import { usePermissions } from '../../auth/usePermissions';
import {
  User,
  Phone,
  Building,
  School,
  Calendar,
  Heart,
  Trash2,
  Edit2,
  KeyRound,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
} from 'lucide-react';

interface ServantDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  servant: ServantResponse | null;
  onSaved: () => void;
  initialEditMode?: boolean;
}

export const ServantDrawer: React.FC<ServantDrawerProps> = ({
  isOpen,
  onClose,
  servant,
  onSaved,
  initialEditMode,
}) => {
  const queryClient = useQueryClient();
  const { isAdmin, isServiceSecretary, isClassSecretary, managedMinistryId, managedClassId } =
    usePermissions();

  const isMinistryDisabled = !isAdmin && (!!managedMinistryId || !!servant);
  const isClassDisabled = !isAdmin && (!!managedClassId || (!isServiceSecretary && !!servant));

  const [isEditMode, setIsEditMode] = useState(!servant);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Form state
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE'>('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [confessionFather, setConfessionFather] = useState('');
  const [ministryId, setMinistryId] = useState<number | ''>('');
  const [classId, setClassId] = useState<number | ''>('');

  // Account password for new servants (always auto-activated)
  const [customPassword, setCustomPassword] = useState('');

  // Account creation / reset for existing servants
  const [showAccountActivation, setShowAccountActivation] = useState(false);
  const [accountActivationPassword, setAccountActivationPassword] = useState('');
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [resetPasswordValue, setResetPasswordValue] = useState('');

  // Queries
  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isOpen && (isEditMode || !servant),
  });

  const { data: classes = [] } = useQuery({
    queryKey: ['classes', ministryId],
    queryFn: () => classesApi.findAll(ministryId ? Number(ministryId) : undefined),
    enabled: isOpen && !!ministryId && (isEditMode || !servant),
  });

  useEffect(() => {
    if (servant) {
      setFullName(servant.fullName);
      setPhone(servant.phone);
      setGender(servant.gender);
      setDateOfBirth(servant.dateOfBirth || '');
      setAddress(servant.address || '');
      setConfessionFather(servant.confessionFather || '');
      setMinistryId(servant.ministryId || '');
      setClassId(servant.classId || '');
      setIsEditMode(initialEditMode ?? false);
    } else {
      setFullName('');
      setPhone('');
      setGender('MALE');
      setDateOfBirth('');
      setAddress('');
      setConfessionFather('');
      setMinistryId(managedMinistryId || '');
      setClassId(managedClassId || '');
      setCustomPassword('Pass@' + Math.floor(100000 + Math.random() * 900000));
      setIsEditMode(true);
    }
    setShowAccountActivation(false);
    setAccountActivationPassword('');
    setShowResetPassword(false);
    setResetPasswordValue('');
    setError(null);
    setSuccessMsg(null);
    setShowDeleteConfirm(false);
  }, [servant, isOpen, managedMinistryId, managedClassId, initialEditMode]);

  const canEdit = isAdmin || isServiceSecretary || isClassSecretary;

  // Create Account for existing servant
  const createAccountMutation = useMutation({
    mutationFn: async (pwd: string) => {
      const servantId = servant?.id ?? servant?.personId;
      if (!servantId) return;
      return servantsApi.createAccount(servantId, pwd);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      setSuccessMsg('تم تفعيل حساب تسجيل الدخول للخادم بنجاح!');
      setShowAccountActivation(false);
      setAccountActivationPassword('');
      onSaved();
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل تفعيل الحساب');
    },
  });

  // Reset password for existing servant
  const resetPasswordMutation = useMutation({
    mutationFn: async (pwd: string) => {
      const servantId = servant?.id ?? servant?.personId;
      if (!servantId) return;
      return servantsApi.resetPassword(servantId, pwd);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      setSuccessMsg('تمت إعادة تعيين كلمة المرور بنجاح.');
      setShowResetPassword(false);
      setResetPasswordValue('');
      onSaved();
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل إعادة تعيين كلمة المرور');
    },
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    if (!fullName.trim() || !phone.trim() || !ministryId || !classId) {
      setError('يرجى ملء جميع الحقول المطلوبة');
      return;
    }

    try {
      setIsSubmitting(true);
      const servantId = servant?.id ?? servant?.personId;
      if (servant && servantId) {
        await servantsApi.update(servantId, {
          fullName: fullName.trim(),
          phone: phone.trim(),
          gender,
          dateOfBirth: dateOfBirth || undefined,
          address: address.trim() || undefined,
          confessionFather: confessionFather.trim() || undefined,
          ministryId: Number(ministryId),
          classId: Number(classId),
        });
      } else {
        const finalPass = customPassword.trim();

        if (!finalPass || finalPass.length < 6) {
          setError('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
          setIsSubmitting(false);
          return;
        }
        if (finalPass === phone.trim()) {
          setError('لا يمكن استخدام رقم الهاتف ككلمة مرور لدواعي الأمان');
          setIsSubmitting(false);
          return;
        }

        await servantsApi.create({
          fullName: fullName.trim(),
          phone: phone.trim(),
          gender,
          dateOfBirth: dateOfBirth || undefined,
          address: address.trim() || undefined,
          confessionFather: confessionFather.trim() || undefined,
          ministryId: Number(ministryId),
          classId: Number(classId),
          password: finalPass,
        });
      }

      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'حدث خطأ أثناء حفظ البيانات');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    const servantId = servant?.id ?? servant?.personId;
    if (!servant || !servantId) return;
    try {
      setIsDeleting(true);
      await servantsApi.softDelete(servantId);
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل حذف الخادم');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={
        servant
          ? isEditMode
            ? 'تعديل بيانات الخادم'
            : 'تفاصيل الخادم'
          : 'إضافة خادم جديد'
      }
      footer={
        <div className="flex items-center justify-between w-full">
          {servant && !isEditMode && canEdit ? (
            <>
              <div className="flex items-center gap-2">
                {showDeleteConfirm ? (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-danger-600 font-bold">تأكيد الحذف؟</span>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={handleDelete}
                      isLoading={isDeleting}
                    >
                      نعم، حذف
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setShowDeleteConfirm(false)}
                    >
                      إلغاء
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="outline"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="text-danger-600 border-danger-200 hover:bg-danger-50"
                  >
                    <Trash2 className="w-4 h-4 ml-1.5" />
                    حذف
                  </Button>
                )}
              </div>
              <Button variant="primary" onClick={() => setIsEditMode(true)}>
                <Edit2 className="w-4 h-4 ml-1.5" />
                تعديل البيانات
              </Button>
            </>
          ) : isEditMode ? (
            <>
              <Button
                variant="outline"
                onClick={() => {
                  if (servant) setIsEditMode(false);
                  else onClose();
                }}
                disabled={isSubmitting}
              >
                إلغاء
              </Button>
              <Button
                variant="primary"
                onClick={handleSubmit}
                isLoading={isSubmitting}
                className="font-bold"
              >
                {servant ? 'حفظ التعديلات' : 'إضافة الخادم'}
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={onClose} className="w-full">
              إغلاق
            </Button>
          )}
        </div>
      }
    >
      {error && <Alert variant="error" className="mb-4">{error}</Alert>}
      {successMsg && <Alert variant="success" className="mb-4">{successMsg}</Alert>}

      {isEditMode ? (
        /* Edit / Create Form */
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="اسم الخادم بالكامل *"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            placeholder="مثال: بيشوي عادل فهمي"
          />

          <Input
            label="رقم الهاتف (سيكون اسم المستخدم) *"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
            dir="ltr"
            placeholder="010XXXXXXXX"
            className="text-left font-mono"
          />

          <div className="grid grid-cols-2 gap-4">
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

          <div className="grid grid-cols-2 gap-4">
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
                  : (ministryId ? [{ value: Number(ministryId), label: servant?.ministryName || 'الخدمة المصرح بها' }] : [])
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
                  : (classId ? [{ value: Number(classId), label: servant?.className || 'الفصل المصرح به' }] : [])
              }
              placeholder={ministryId ? 'اختر الفصل...' : 'اختر الخدمة أولاً'}
            />
          </div>

          <Input
            label="العنوان"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="المنطقة / الشارع"
          />

          <Input
            label="أب الاعتراف"
            value={confessionFather}
            onChange={(e) => setConfessionFather(e.target.value)}
            placeholder="اسم أب الاعتراف"
          />

          {/* Automatic User Account Notice & Password (When creating new servant) */}
          {!servant && (
            <div className="p-4 bg-emerald-50/60 border border-emerald-200 rounded-2xl space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-emerald-950">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>حساب تسجيل الدخول مفعل تلقائياً لهذا الخادم فور الحفظ</span>
              </div>

              <div className="space-y-2 pt-1 border-t border-emerald-100">
                <p className="text-[11px] text-emerald-800">
                  اسم المستخدم لتسجيل الدخول: <strong dir="ltr">{phone || 'رقم الهاتف'}</strong>
                </p>

                <div className="flex gap-2 items-end">
                  <div className="flex-1">
                    <Input
                      type="text"
                      label="كلمة المرور لحساب الخادم *"
                      value={customPassword}
                      onChange={(e) => setCustomPassword(e.target.value)}
                      placeholder="6 أحرف أو أرقام على الأقل"
                      required
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-10 text-xs text-emerald-800 border-emerald-300 whitespace-nowrap"
                    onClick={() => setCustomPassword('Pass@' + Math.floor(100000 + Math.random() * 900000))}
                  >
                    توليد تلقائي
                  </Button>
                </div>
                <p className="text-[11px] text-gray-500">
                  تم توليد كلمة مرور أولية قوية تلقائياً، يمكنك تعديلها أو استخدامها مباشرة.
                </p>
              </div>
            </div>
          )}
        </form>
      ) : (
        /* Details View */
        <div className="space-y-5">
          {/* Header Card */}
          <div className="bg-gray-50 rounded-2xl p-4 border border-gray-100 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-lg">
                {servant?.fullName?.charAt(0) || 'خ'}
              </div>
              <div>
                <h4 className="font-bold text-gray-900">{servant?.fullName}</h4>
                <p className="text-xs text-gray-500 font-mono mt-0.5" dir="ltr">
                  {servant?.phone}
                </p>
              </div>
            </div>
            <Badge variant={servant?.active ? 'success' : 'danger'}>
              {servant?.active ? 'نشط' : 'محذوف'}
            </Badge>
          </div>

          {/* User Account Status Box */}
          <div
            className={`p-4 rounded-2xl border ${
              servant?.userId
                ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                : 'bg-amber-50/70 border-amber-200 text-amber-900'
            } space-y-3`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {servant?.userId ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
                ) : (
                  <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0" />
                )}
                <div>
                  <h5 className="font-bold text-xs">
                    {servant?.userId ? 'حساب تسجيل الدخول مفعل' : 'لا يوجد حساب تسجيل دخول لهذا الخادم'}
                  </h5>
                  <p className="text-[11px] opacity-80 mt-0.5">
                    {servant?.userId
                      ? `اسم المستخدم: ${servant.phone}`
                      : 'الخادم مسجل بالخدمة لكنه لا يستطيع تسجيل الدخول ومتابعة الافتقاد.'}
                  </p>
                </div>
              </div>

              {servant?.userId ? (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setShowResetPassword(!showResetPassword);
                    if (!resetPasswordValue) {
                      setResetPasswordValue('Pass@' + Math.floor(100000 + Math.random() * 900000));
                    }
                  }}
                  className="text-[11px] font-bold py-1 px-2.5 h-auto border-emerald-300 hover:bg-emerald-100 text-emerald-900"
                >
                  <KeyRound className="w-3.5 h-3.5 ml-1" />
                  إعادة تعيين كلمة المرور
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => {
                    setShowAccountActivation(!showAccountActivation);
                    if (!accountActivationPassword) {
                      setAccountActivationPassword('Pass@' + Math.floor(100000 + Math.random() * 900000));
                    }
                  }}
                  className="text-[11px] font-bold py-1 px-2.5 h-auto bg-amber-600 hover:bg-amber-700"
                >
                  <Sparkles className="w-3.5 h-3.5 ml-1" />
                  + تفعيل الحساب الآن
                </Button>
              )}
            </div>

            {/* Inline Account Activation Box */}
            {showAccountActivation && !servant?.userId && (
              <div className="pt-3 border-t border-amber-200/70 space-y-3">
                <div className="flex gap-2 items-end">
                  <div className="flex-1">
                    <Input
                      type="text"
                      label="كلمة المرور الجديدة للحساب"
                      value={accountActivationPassword}
                      onChange={(e) => setAccountActivationPassword(e.target.value)}
                      placeholder="6 أحرف على الأقل"
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-10 text-xs text-amber-800 border-amber-300 whitespace-nowrap"
                    onClick={() => setAccountActivationPassword('Pass@' + Math.floor(100000 + Math.random() * 900000))}
                  >
                    توليد عشوائي
                  </Button>
                </div>
                <div className="flex justify-end gap-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setShowAccountActivation(false)}
                  >
                    إلغاء
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    isLoading={createAccountMutation.isPending}
                    onClick={() => {
                      if (!accountActivationPassword.trim() || accountActivationPassword.trim().length < 6) {
                        setError('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
                        return;
                      }
                      if (servant && accountActivationPassword.trim() === servant.phone.trim()) {
                        setError('لا يمكن استخدام رقم الهاتف ككلمة مرور');
                        return;
                      }
                      createAccountMutation.mutate(accountActivationPassword.trim());
                    }}
                  >
                    تأكيد وتفعيل الحساب
                  </Button>
                </div>
              </div>
            )}

            {/* Inline Reset Password Box */}
            {showResetPassword && servant?.userId && (
              <div className="pt-3 border-t border-emerald-200/70 space-y-3">
                <div className="flex gap-2 items-end">
                  <div className="flex-1">
                    <Input
                      type="text"
                      label="كلمة المرور الجديدة"
                      value={resetPasswordValue}
                      onChange={(e) => setResetPasswordValue(e.target.value)}
                      placeholder="6 أحرف على الأقل"
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-10 text-xs text-emerald-800 border-emerald-300 whitespace-nowrap"
                    onClick={() => setResetPasswordValue('Pass@' + Math.floor(100000 + Math.random() * 900000))}
                  >
                    توليد عشوائي
                  </Button>
                </div>
                <div className="flex justify-end gap-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setShowResetPassword(false)}
                  >
                    إلغاء
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    isLoading={resetPasswordMutation.isPending}
                    onClick={() => {
                      if (!resetPasswordValue.trim() || resetPasswordValue.trim().length < 6) {
                        setError('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
                        return;
                      }
                      if (servant && resetPasswordValue.trim() === servant.phone.trim()) {
                        setError('لا يمكن استخدام رقم الهاتف ككلمة مرور');
                        return;
                      }
                      resetPasswordMutation.mutate(resetPasswordValue.trim());
                    }}
                  >
                    تأكيد حفظ كلمة المرور
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Quick info grid */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white p-3.5 rounded-xl border border-gray-100 flex items-center gap-3">
              <Building className="w-5 h-5 text-gray-400 shrink-0" />
              <div>
                <span className="block text-[11px] text-gray-400">الخدمة</span>
                <span className="text-sm font-bold text-gray-800">
                  {servant?.ministryName || '—'}
                </span>
              </div>
            </div>

            <div className="bg-white p-3.5 rounded-xl border border-gray-100 flex items-center gap-3">
              <School className="w-5 h-5 text-gray-400 shrink-0" />
              <div>
                <span className="block text-[11px] text-gray-400">الفصل</span>
                <span className="text-sm font-bold text-gray-800">
                  {servant?.className || '—'}
                </span>
              </div>
            </div>

            <div className="bg-white p-3.5 rounded-xl border border-gray-100 flex items-center gap-3">
              <User className="w-5 h-5 text-gray-400 shrink-0" />
              <div>
                <span className="block text-[11px] text-gray-400">النوع</span>
                <span className="text-sm font-bold text-gray-800">
                  {servant?.gender === 'MALE' ? 'ذكر' : 'أنثى'}
                </span>
              </div>
            </div>

            <div className="bg-white p-3.5 rounded-xl border border-gray-100 flex items-center gap-3">
              <Calendar className="w-5 h-5 text-gray-400 shrink-0" />
              <div>
                <span className="block text-[11px] text-gray-400">تاريخ الميلاد</span>
                <span className="text-sm font-bold text-gray-800 font-mono">
                  {servant?.dateOfBirth || '—'}
                </span>
              </div>
            </div>
          </div>

          {/* Additional details */}
          <div className="bg-white p-4 rounded-xl border border-gray-100 space-y-3">
            <div className="flex items-center gap-3 text-xs">
              <Heart className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-gray-500">أب الاعتراف:</span>
              <span className="font-bold text-gray-800">
                {servant?.confessionFather || 'غير محدد'}
              </span>
            </div>

            <div className="flex items-center gap-3 text-xs">
              <Phone className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-gray-500">العنوان:</span>
              <span className="font-bold text-gray-800">
                {servant?.address || 'غير محدد'}
              </span>
            </div>
          </div>
        </div>
      )}
    </Drawer>
  );
};
