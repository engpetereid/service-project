import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/users.api';
import { UserResponse } from '../../types/user.types';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { KeyRound } from 'lucide-react';

interface ResetPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: UserResponse | null;
  onSuccess?: () => void;
}

export const ResetPasswordModal: React.FC<ResetPasswordModalProps> = ({
  isOpen,
  onClose,
  user,
  onSuccess,
}) => {
  const queryClient = useQueryClient();
  const [customPassword, setCustomPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleClose = () => {
    setCustomPassword('');
    setError(null);
    onClose();
  };

  const handleGeneratePassword = () => {
    const randomPass = 'Pass@' + Math.floor(100000 + Math.random() * 900000);
    setCustomPassword(randomPass);
  };

  const mutation = useMutation({
    mutationFn: async () => {
      if (!user) return;
      setError(null);

      const pass = customPassword.trim();
      if (!pass) {
        throw new Error('يرجى إدخال كلمة المرور الجديدة');
      }
      if (pass === user.phone) {
        throw new Error('لا يمكن استخدام رقم الهاتف ككلمة مرور');
      }
      if (pass.length < 6) {
        throw new Error('كلمة المرور يجب أن تكون 6 أحرف على الأقل');
      }

      await usersApi.resetPassword(user.userId, { newPassword: pass });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      if (onSuccess) onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل إعادة تعيين كلمة المرور');
    },
  });

  if (!user) return null;

  return (
    <Drawer
      isOpen={isOpen}
      onClose={handleClose}
      title={`إعادة تعيين كلمة المرور — ${user.fullName}`}
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" onClick={handleClose} disabled={mutation.isPending}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={() => mutation.mutate()}
            isLoading={mutation.isPending}
            className="font-bold"
          >
            <KeyRound className="w-4 h-4 ml-1.5" />
            تأكيد إعادة التعيين
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        {error && <Alert variant="error">{error}</Alert>}

        <div className="p-3.5 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-gray-500 block">اسم المستخدم:</span>
            <span className="text-xs text-gray-400">هو نفسه رقم الهاتف المسجل</span>
          </div>
          <span className="font-mono font-bold text-sm text-gray-900" dir="ltr">
            {user.phone}
          </span>
        </div>

        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-gray-700 block">كلمة المرور الجديدة *</label>
            <button
              type="button"
              onClick={handleGeneratePassword}
              className="text-xs font-bold text-primary-700 hover:text-primary-800 underline cursor-pointer"
            >
              توليد كلمة مرور تلقائية
            </button>
          </div>

          <Input
            type="text"
            value={customPassword}
            onChange={(e) => setCustomPassword(e.target.value)}
            placeholder="أدخل كلمة مرور قوية (6 أحرف أو أرقام على الأقل)"
            required
            dir="ltr"
            className="font-mono"
          />
          <p className="text-[11px] text-gray-400">
            ملاحظة أمنية: تم إلغاء استخدام رقم الهاتف ككلمة مرور لضمان سرية حسابات الخدام والمستخدمين.
          </p>
        </div>

        <p className="text-[11px] text-gray-400 leading-relaxed pt-2">
          ملاحظة: سيتم إبطال جميع جلسات تسجيل الدخول السابقة لهذا الحساب فور تغيير كلمة المرور.
        </p>
      </div>
    </Drawer>
  );
};
