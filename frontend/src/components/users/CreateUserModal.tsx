import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/users.api';
import { servantsApi } from '../../api/servants.api';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { RoleAssignment } from '../../types/user.types';
import { Role } from '../../types/auth.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Alert } from '../ui/Alert';
import { Spinner } from '../ui/Spinner';
import {
  UserPlus,
  Shield,
  Sparkles,
  Trash2,
} from 'lucide-react';

interface CreateUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateUserModal: React.FC<CreateUserModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const queryClient = useQueryClient();
  const [creationMode, setCreationMode] = useState<'existingServant' | 'brandNew'>('existingServant');

  // Mode 1: From Existing Servant
  const [selectedServantId, setSelectedServantId] = useState<number | ''>('');
  const [servantCustomPassword, setServantCustomPassword] = useState('');

  // Mode 2: Brand New User
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE'>('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [confessionFather, setConfessionFather] = useState('');
  const [password, setPassword] = useState('');

  // Common: Roles
  const [roles, setRoles] = useState<RoleAssignment[]>([{ role: 'SERVANT', ministryId: null, classId: null }]);
  const [error, setError] = useState<string | null>(null);

  // Queries
  const { data: servants = [], isLoading: isLoadingServants } = useQuery({
    queryKey: ['servants', 'all'],
    queryFn: () => servantsApi.findAll(),
    enabled: isOpen && creationMode === 'existingServant',
  });

  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isOpen,
  });

  const { data: allClasses = [] } = useQuery({
    queryKey: ['classes', 'all'],
    queryFn: () => classesApi.findAll(),
    enabled: isOpen,
  });

  // Filter servants without user accounts
  const servantsWithoutAccount = servants.filter((s) => !s.userId);

  const selectedServant = servants.find((s) => s.id === Number(selectedServantId));

  // Reset form
  const handleClose = () => {
    setSelectedServantId('');
    setServantCustomPassword('');
    setFullName('');
    setPhone('');
    setGender('MALE');
    setDateOfBirth('');
    setAddress('');
    setConfessionFather('');
    setPassword('');
    setRoles([{ role: 'SERVANT', ministryId: null, classId: null }]);
    setError(null);
    onClose();
  };

  const handleGeneratePassword = (target: 'servant' | 'brandNew') => {
    const randomPass = 'User@' + Math.floor(100000 + Math.random() * 900000);
    if (target === 'servant') {
      setServantCustomPassword(randomPass);
    } else {
      setPassword(randomPass);
    }
  };

  // Role helpers
  const handleAddRole = (role: Role) => {
    if (roles.some((r) => r.role === role)) return;
    setRoles([...roles, { role, ministryId: null, classId: null }]);
  };

  const handleRemoveRole = (index: number) => {
    setRoles(roles.filter((_, i) => i !== index));
  };

  const handleUpdateRoleScope = (
    index: number,
    field: 'ministryId' | 'classId',
    value: number | null
  ) => {
    const updated = [...roles];
    updated[index] = { ...updated[index], [field]: value };
    setRoles(updated);
  };

  // Submit
  const createMutation = useMutation({
    mutationFn: async () => {
      setError(null);

      if (creationMode === 'existingServant') {
        if (!selectedServantId) {
          throw new Error('يرجى اختيار خادم لإنشاء الحساب له');
        }
        const finalPass = servantCustomPassword.trim();
        if (!finalPass) {
          throw new Error('كلمة المرور مطلوبة لإنشاء الحساب');
        }
        if (selectedServant && finalPass === selectedServant.phone) {
          throw new Error('لا يمكن استخدام رقم الهاتف ككلمة مرور');
        }
        if (finalPass.length < 6) {
          throw new Error('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
        }

        return usersApi.createForPerson(Number(selectedServantId), {
          password: finalPass,
          roles,
        });
      } else {
        if (!fullName.trim()) throw new Error('الاسم مطلوب');
        if (!phone.trim()) throw new Error('رقم الهاتف مطلوب');

        const finalPass = password.trim();
        if (!finalPass) {
          throw new Error('كلمة المرور مطلوبة');
        }
        if (finalPass === phone.trim()) {
          throw new Error('لا يمكن استخدام رقم الهاتف ككلمة مرور');
        }
        if (finalPass.length < 6) {
          throw new Error('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
        }

        const res = await usersApi.create({
          fullName: fullName.trim(),
          phone: phone.trim(),
          password: finalPass,
          gender,
          dateOfBirth: dateOfBirth || undefined,
          address: address.trim() || undefined,
          confessionFather: confessionFather.trim() || undefined,
        });

        // Assign roles if different than default
        if (roles.length > 0) {
          await usersApi.assignRoles(res.userId, { roles });
        }

        return res;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل إنشاء الحساب');
    },
  });

  return (
    <Drawer
      isOpen={isOpen}
      onClose={handleClose}
      title="إضافة حساب مستخدم جديد"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" onClick={handleClose} disabled={createMutation.isPending}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={() => createMutation.mutate()}
            isLoading={createMutation.isPending}
            className="font-bold"
          >
            <UserPlus className="w-4 h-4 ml-1.5" />
            إنشاء الحساب
          </Button>
        </div>
      }
    >
      <div className="space-y-5">
        {error && <Alert variant="error">{error}</Alert>}

        {/* Concept Guidance Box */}
        <div className="p-3 bg-blue-50/70 border border-blue-100 rounded-xl text-xs text-blue-900 space-y-1">
          <p className="font-bold flex items-center gap-1.5">
            <Sparkles className="w-4 h-4 text-blue-600" />
            كيف يعمل حساب تسجيل الدخول؟
          </p>
          <p className="text-[11px] text-blue-700 leading-relaxed">
            اسم المستخدم لتسجيل الدخول دائماً هو <strong>رقم الهاتف</strong>. يمكنك إنشاء الحساب فوراً لخادم مسجل، أو إنشاء مستخدم جديد بالكامل.
          </p>
        </div>

        {/* Mode Selector Tabs */}
        <div className="grid grid-cols-2 gap-2 p-1 bg-gray-100 rounded-xl">
          <button
            type="button"
            onClick={() => setCreationMode('existingServant')}
            className={`py-2 px-3 rounded-lg text-xs font-bold transition ${
              creationMode === 'existingServant'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            خادم مسجل حالياً ({servantsWithoutAccount.length})
          </button>
          <button
            type="button"
            onClick={() => setCreationMode('brandNew')}
            className={`py-2 px-3 rounded-lg text-xs font-bold transition ${
              creationMode === 'brandNew'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            مستخدم جديد بالكامل
          </button>
        </div>

        {/* Tab 1: From Existing Servant */}
        {creationMode === 'existingServant' ? (
          <div className="space-y-4">
            {isLoadingServants ? (
              <div className="py-6 text-center">
                <Spinner size="md" />
                <p className="text-xs text-gray-400 mt-2">جاري تحميل الخدام...</p>
              </div>
            ) : servantsWithoutAccount.length === 0 ? (
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-center text-xs text-emerald-800">
                🎉 رائع! جميع الخدام المسجلين لديهم حسابات دخول مفعلة بالفعل.
              </div>
            ) : (
              <div>
                <Select
                  label="اختر الخادم المراد تفعيل حسابه *"
                  value={selectedServantId}
                  onChange={(e) => setSelectedServantId(e.target.value ? Number(e.target.value) : '')}
                  options={servantsWithoutAccount.map((s) => ({
                    value: s.id,
                    label: `${s.fullName} (${s.phone}) - ${s.className || s.ministryName || 'غير مسكن'}`,
                  }))}
                  placeholder="اختر خادماً من القائمة..."
                />
              </div>
            )}

            {selectedServant && (
              <div className="p-3.5 bg-gray-50 rounded-xl border border-gray-100 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-gray-500 font-bold">اسم المستخدم:</span>
                  <span className="font-mono font-bold text-gray-900" dir="ltr">
                    {selectedServant.phone}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-gray-500 font-bold">الخدمة والفصل:</span>
                  <span className="text-gray-700">
                    {selectedServant.ministryName || '—'} / {selectedServant.className || '—'}
                  </span>
                </div>
              </div>
            )}

            {/* Password Field for Existing Servant */}
            <div className="space-y-2 p-3 bg-gray-50 rounded-xl border border-gray-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-gray-700 block">كلمة المرور للحساب *</label>
                <button
                  type="button"
                  onClick={() => handleGeneratePassword('servant')}
                  className="text-xs font-bold text-primary-700 hover:text-primary-800 underline cursor-pointer"
                >
                  توليد كلمة مرور
                </button>
              </div>
              <Input
                type="text"
                value={servantCustomPassword}
                onChange={(e) => setServantCustomPassword(e.target.value)}
                placeholder="6 أحرف أو أرقام على الأقل"
                required
                dir="ltr"
                className="font-mono"
              />
              <p className="text-[11px] text-gray-400">
                ملاحظة: تم إلغاء استخدام رقم الهاتف ككلمة مرور لضمان أمان النظام.
              </p>
            </div>
          </div>
        ) : (
          /* Tab 2: Brand New User */
          <div className="space-y-3.5">
            <Input
              label="الاسم بالكامل *"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="مثال: بيتر سمير رزق"
              required
            />

            <Input
              label="رقم الهاتف (اسم المستخدم) *"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="010XXXXXXXX"
              required
              dir="ltr"
            />

            <div className="grid grid-cols-2 gap-3">
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

            <div className="grid grid-cols-2 gap-3">
              <Input
                label="العنوان"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="المنطقة أو الحي"
              />

              <Input
                label="أب الاعتراف"
                value={confessionFather}
                onChange={(e) => setConfessionFather(e.target.value)}
                placeholder="اسم أب الاعتراف"
              />
            </div>

            {/* Password Section for New User */}
            <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-gray-700 block">كلمة المرور *</label>
                <button
                  type="button"
                  onClick={() => handleGeneratePassword('brandNew')}
                  className="text-xs font-bold text-primary-700 hover:text-primary-800 underline cursor-pointer"
                >
                  توليد كلمة مرور
                </button>
              </div>

              <Input
                type="text"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="6 أحرف أو أرقام على الأقل"
                required
                dir="ltr"
                className="font-mono"
              />
              <p className="text-[11px] text-gray-400">
                ملاحظة: يجب ألا تطابق كلمة المرور رقم الهاتف.
              </p>
            </div>
          </div>
        )}

        {/* Roles & Permissions Section */}
        <div className="pt-3 border-t border-gray-100 space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
              <Shield className="w-4 h-4 text-primary-600" />
              الأدوار والصلاحيات الممنوحة:
            </label>
            <div className="flex items-center gap-1">
              {(['SERVANT', 'SERVICE_SECRETARY', 'CLASS_SECRETARY', 'GENERAL_ADMIN'] as Role[]).map(
                (r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => handleAddRole(r)}
                    disabled={roles.some((ro) => ro.role === r)}
                    className="text-[10px] font-bold text-primary-700 bg-primary-50 hover:bg-primary-100 disabled:opacity-40 disabled:cursor-not-allowed px-2 py-0.5 rounded-md"
                  >
                    + {r === 'SERVANT' ? 'خادم' : r === 'SERVICE_SECRETARY' ? 'أمين خدمة' : r === 'CLASS_SECRETARY' ? 'أمين فصل' : 'أدمن'}
                  </button>
                )
              )}
            </div>
          </div>

          <div className="space-y-2">
            {roles.map((r, index) => (
              <div
                key={index}
                className="p-3 bg-gray-50 rounded-xl border border-gray-100 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-gray-900">
                    {r.role === 'SERVANT' && 'خادم (SERVANT)'}
                    {r.role === 'SERVICE_SECRETARY' && 'أمين خدمة (SERVICE_SECRETARY)'}
                    {r.role === 'CLASS_SECRETARY' && 'أمين فصل (CLASS_SECRETARY)'}
                    {r.role === 'GENERAL_ADMIN' && 'مدير نظام (GENERAL_ADMIN)'}
                  </span>
                  {roles.length > 1 && (
                    <button
                      type="button"
                      onClick={() => handleRemoveRole(index)}
                      className="text-gray-400 hover:text-danger-600 p-1"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                {r.role === 'SERVICE_SECRETARY' && (
                  <Select
                    label="الخدمة التابعة *"
                    value={r.ministryId || ''}
                    onChange={(e) =>
                      handleUpdateRoleScope(
                        index,
                        'ministryId',
                        e.target.value ? Number(e.target.value) : null
                      )
                    }
                    options={ministries.map((m) => ({ value: m.id, label: m.name }))}
                    placeholder="اختر الخدمة..."
                  />
                )}

                {r.role === 'CLASS_SECRETARY' && (
                  <Select
                    label="الفصل التابع *"
                    value={r.classId || ''}
                    onChange={(e) =>
                      handleUpdateRoleScope(
                        index,
                        'classId',
                        e.target.value ? Number(e.target.value) : null
                      )
                    }
                    options={allClasses.map((c) => ({
                      value: c.id,
                      label: `${c.name} (${c.ministryName})`,
                    }))}
                    placeholder="اختر الفصل..."
                  />
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </Drawer>
  );
};
