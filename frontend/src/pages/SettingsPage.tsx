import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '../api/settings.api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { Spinner } from '../components/ui/Spinner';
import { formatDate } from '../utils/date';
import { Sliders, Calendar, Lock, CheckCircle2 } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [maxNoteScore, setMaxNoteScore] = useState<string>('21');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Queries
  const { data: settings = [], isLoading: isSettingsLoading } = useQuery({
    queryKey: ['settings'],
    queryFn: () => settingsApi.findAll(),
  });

  const { data: academicYears = [], isLoading: isYearsLoading } = useQuery({
    queryKey: ['academic-years'],
    queryFn: () => settingsApi.getAcademicYears(),
  });

  useEffect(() => {
    const setting = settings.find((s) => s.settingKey === 'MAX_NOTE_SCORE');
    if (setting) {
      setMaxNoteScore(setting.settingValue);
    }
  }, [settings]);

  // Mutation
  const updateSettingMutation = useMutation({
    mutationFn: (newValue: string) =>
      settingsApi.update({
        settingKey: 'MAX_NOTE_SCORE',
        settingValue: newValue,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      setSuccessMessage('تم حفظ إعدادات النظام بنجاح');
      setError(null);
      setTimeout(() => setSuccessMessage(null), 4000);
    },
    onError: (err: Error) => {
      setError(err.message || 'فشل تحديث الإعدادات');
      setSuccessMessage(null);
    },
  });

  const handleSaveScore = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = parseInt(maxNoteScore, 10);
    if (isNaN(parsed) || parsed <= 0 || parsed > 100) {
      setError('يجب أن تكون القيمة رقماً صحيحاً موجباً بين 1 و 100');
      return;
    }
    updateSettingMutation.mutate(maxNoteScore);
  };

  const currentYear = academicYears.find((y) => y.current);

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-gray-900 tracking-tight">إعدادات النظام</h1>
        <p className="text-xs text-gray-500 mt-1">
          إدارة المتغيرات الحاكمة للنظام وقواعد القفل التلقائي للأسابيع والأعوام الدراسية
        </p>
      </div>

      {successMessage && (
        <Alert variant="success" className="flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>{successMessage}</span>
        </Alert>
      )}

      {error && <Alert variant="error">{error}</Alert>}

      {/* General Settings Card */}
      <Card className="p-6 space-y-5 bg-white">
        <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
          <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-600 flex items-center justify-center font-bold">
            <Sliders className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-gray-900 text-base">درجات المتابعة والافتقاد</h3>
            <p className="text-xs text-gray-500 mt-0.5">
              تحديد الحدود القصوى للدرجات المحسوبة في استمارة الافتقاد الأسبوعي
            </p>
          </div>
        </div>

        {isSettingsLoading ? (
          <div className="py-6 text-center">
            <Spinner size="sm" />
          </div>
        ) : (
          <form onSubmit={handleSaveScore} className="space-y-4">
            <div className="max-w-xs">
              <Input
                label="الحد الأقصى لنقاط النوتة (MAX_NOTE_SCORE) *"
                type="number"
                min="1"
                max="100"
                value={maxNoteScore}
                onChange={(e) => setMaxNoteScore(e.target.value)}
                helperText="القيمة الافتراضية 21 درجة"
                required
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              isLoading={updateSettingMutation.isPending}
              className="font-bold"
            >
              حفظ التعديل
            </Button>
          </form>
        )}
      </Card>

      {/* Week Locking Policy Card */}
      <Card className="p-6 space-y-4 bg-white">
        <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-gray-900 text-base">سياسة قفل الأسابيع التلقائي</h3>
            <p className="text-xs text-gray-500 mt-0.5">قاعدة الـ 30 يوماً لحماية دقة البيانات التاريخية</p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 text-xs text-gray-600 leading-relaxed space-y-2">
          <p>
            • يتم قفل أي أسبوع تلقائياً بعد مرور <strong>30 يوماً</strong> من تاريخ انتهائه لحماية السجلات التاريخية.
          </p>
          <p>
            • لا يمكن للخادم أو أمين الفصل تعديل افتقادات أو حضور أسبوع مقفول.
          </p>
          <p>
            • يمتلك <strong>الأمين العام</strong> صلاحية استثنائية للتعديل في الأسابيع المقفولة عند الضرورة مع توثيق العملية فورياً في سجل التدقيق.
          </p>
        </div>
      </Card>

      {/* Academic Years Card */}
      <Card className="p-6 space-y-4 bg-white">
        <div className="flex items-center gap-3 pb-4 border-b border-gray-100">
          <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-gray-900 text-base">الأعوام الدراسية والترفيع الآلي</h3>
            <p className="text-xs text-gray-500 mt-0.5">تاريخ الأعوام السابقة والعام الحالي المفعل</p>
          </div>
        </div>

        {isYearsLoading ? (
          <div className="py-6 text-center">
            <Spinner size="sm" />
          </div>
        ) : (
          <div className="space-y-4">
            {currentYear && (
              <div className="p-4 rounded-2xl bg-purple-50/60 border border-purple-200 flex items-center justify-between">
                <div>
                  <span className="text-[11px] font-bold text-purple-700 block">العام الدراسي الحالي</span>
                  <h4 className="text-lg font-black text-purple-900">{currentYear.name}</h4>
                  <p className="text-xs text-purple-600 font-mono mt-0.5">
                    {formatDate(currentYear.startDate)} — {formatDate(currentYear.endDate)}
                  </p>
                </div>
                <Badge variant="primary" className="bg-purple-600 text-white border-transparent">
                  مفعل حالياً
                </Badge>
              </div>
            )}

            <div className="divide-y divide-gray-100 rounded-2xl border border-gray-100 overflow-hidden bg-white text-sm">
              {academicYears.map((y) => (
                <div key={y.id} className="p-4 flex items-center justify-between">
                  <div>
                    <span className="font-bold text-gray-900 block">{y.name}</span>
                    <span className="text-xs text-gray-400 font-mono">
                      {formatDate(y.startDate)} إلى {formatDate(y.endDate)}
                    </span>
                  </div>
                  <Badge variant={y.current ? 'success' : 'neutral'}>
                    {y.current ? 'حالي' : 'مؤرشف'}
                  </Badge>
                </div>
              ))}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};
