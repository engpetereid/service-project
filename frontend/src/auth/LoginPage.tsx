import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from './useAuth';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { Phone, Eye, EyeOff } from 'lucide-react';
import waznaLogo from '../assets/waznaLogo.png';

export const LoginPage: React.FC = () => {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanPhone = phone.trim();
    if (!cleanPhone) {
      setError('يرجى إدخال رقم الهاتف');
      return;
    }

    if (!password) {
      setError('يرجى إدخال كلمة المرور');
      return;
    }

    try {
      setIsSubmitting(true);
      await login({ phone: cleanPhone, password });
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'فشل تسجيل الدخول، يرجى المحاولة لاحقاً');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary-50/50 via-white to-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Header / Brand */}
        <div className="text-center mb-8">
          <img
            src={waznaLogo}
            alt="شعار وزنة"
            className="w-20 h-20 rounded-3xl object-cover shadow-xl shadow-primary-500/20 mb-4 border-2 border-white ring-4 ring-primary-50 mx-auto"
          />
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">
            وزنة
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            نظام إدارة وخدمة ومتابعة الكنيسة الأسبوعية
          </p>
        </div>

        {/* Card Form */}
        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-xl shadow-gray-200/50">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && <Alert variant="error">{error}</Alert>}

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5 text-right">
                رقم الهاتف (اسم المستخدم)
              </label>
              <div className="relative">
                <Input
                  type="tel"
                  dir="ltr"
                  placeholder="010XXXXXXXX"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  autoComplete="username"
                  className="pl-10 text-left font-mono"
                  required
                />
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Phone className="w-5 h-5" />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5 text-right">
                كلمة المرور
              </label>
              <div className="relative">
                <Input
                  type={showPassword ? 'text' : 'password'}
                  dir="ltr"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  className="pl-10 text-left font-mono"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400 hover:text-gray-600 focus:outline-none"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full text-base font-bold shadow-md shadow-primary-500/20"
              isLoading={isSubmitting}
            >
              تسجيل الدخول
            </Button>
          </form>

          <div className="mt-8 pt-6 border-t border-gray-100 text-center space-y-2.5">
            <p className="text-xs text-gray-400">
              في حالة نسيان كلمة المرور، يرجى مراجعة أمين الخدمة أو الأمين العام
            </p>
            <div className="pt-1">
              <Link
                to="/about"
                className="inline-flex items-center gap-1 text-xs font-bold text-primary-600 hover:text-primary-800 hover:underline transition"
              >
                <span>برنامج وزنة • عن النظام والمطور</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
