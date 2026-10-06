import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/users.api';
import { UserResponse } from '../../types/user.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Alert } from '../ui/Alert';

interface EditUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: UserResponse | null;
  onSuccess: () => void;
}

export const EditUserModal: React.FC<EditUserModalProps> = ({
  isOpen,
  onClose,
  user,
  onSuccess,
}) => {
  const queryClient = useQueryClient();

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE'>('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [confessionFather, setConfessionFather] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setPhone(user.phone || '');
      setGender(user.gender || 'MALE');
      setDateOfBirth(user.dateOfBirth || '');
      setAddress(user.address || '');
      setConfessionFather(user.confessionFather || '');
      setError(null);
    }
  }, [user, isOpen]);

  const updateMutation = useMutation({
    mutationFn: async () => {
      if (!user) return;
      if (!fullName.trim()) {
        throw new Error('الاسم الكامل مطلوب');
      }
      if (!phone.trim()) {
        throw new Error('رقم الهاتف مطلوب');
      }

      return usersApi.update(user.userId, {
        fullName: fullName.trim(),
        phone: phone.trim(),
        gender,
        dateOfBirth: dateOfBirth || undefined,
        address: address.trim() || undefined,
        confessionFather: confessionFather.trim() || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['servants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      onSuccess();
      onClose();
    },
    onError: (err: any) => {
      setError(err?.response?.data?.message || err?.message || 'فشل تحديث بيانات المستخدم');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate();
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={`تعديل بيانات المستخدم: ${user?.fullName || ''}`}
      footer={
        <div className="flex items-center justify-between w-full">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={updateMutation.isPending}
          >
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={updateMutation.isPending}
            className="font-bold"
          >
            حفظ التعديلات
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <Alert variant="error">{error}</Alert>}

        <div className="p-3 bg-blue-50/70 border border-blue-100 rounded-xl text-xs text-blue-900 leading-relaxed">
          <p className="font-bold mb-0.5">معلومات الحساب:</p>
          <p className="text-blue-700">
            تعديل البيانات هنا يحدّث الملف الشخصي للخادم ورقم الهاتف الذي يُستخدم كاسم مستخدم لتسجيل الدخول.
          </p>
        </div>

        <Input
          label="الاسم الكامل *"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          placeholder="مثال: يوسف ماهر ميخائيل"
          required
        />

        <Input
          label="رقم الهاتف (اسم المستخدم) *"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="01xxxxxxxxx"
          required
          dir="ltr"
        />

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
          label="تاريخ الميلاد"
          type="date"
          value={dateOfBirth}
          onChange={(e) => setDateOfBirth(e.target.value)}
        />

        <Input
          label="العنوان"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="المدينة، الشارع، رقم العقار..."
        />

        <Input
          label="أب الاعتراف"
          value={confessionFather}
          onChange={(e) => setConfessionFather(e.target.value)}
          placeholder="مثال: أبونا بيشوي كامل"
        />
      </form>
    </Drawer>
  );
};
