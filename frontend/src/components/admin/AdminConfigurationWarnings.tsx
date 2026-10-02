import React from 'react';
import { ConfigurationWarning } from '../../types/statistics.types';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle2 } from 'lucide-react';

interface AdminConfigurationWarningsProps {
  warnings: ConfigurationWarning[];
}

export const AdminConfigurationWarnings: React.FC<AdminConfigurationWarningsProps> = ({
  warnings,
}) => {
  if (warnings.length === 0) {
    return (
      <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <div>
            <h4 className="text-xs sm:text-sm font-bold text-emerald-900">
              الهيكل التنظيمي مكتمل بالكامل
            </h4>
            <p className="text-[11px] text-emerald-700">
              تم تعيين أمناء الخدمات والفصول وتسكين الخدام وتوزيع المخدومين وتفعيل الحسابات بنجاح.
            </p>
          </div>
        </div>
        <span className="text-xs font-bold text-emerald-800 bg-white px-3 py-1 rounded-full border border-emerald-200 shrink-0">
          جاهز للخدمة
        </span>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-black text-amber-900 uppercase tracking-wider flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-amber-600" />
          <span>تنبيهات التهيئة والإعداد التنظيمي ({warnings.length})</span>
        </h3>
        <span className="text-xs text-amber-700 font-medium">بحاجة لمعالجة لاستكمال ضبط النظام</span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {warnings.map((w, index) => (
          <Card
            key={index}
            className="p-4 border-amber-200 bg-gradient-to-br from-amber-50/50 to-amber-100/20 shadow-sm flex flex-col justify-between space-y-3"
          >
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold shrink-0 mt-0.5">
                <AlertTriangle className="w-4 h-4" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <h4 className="font-bold text-gray-900 text-sm">{w.title}</h4>
                  <span className="text-xs font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-md border border-red-100 shrink-0">
                    {w.count} معلق
                  </span>
                </div>
                <p className="text-xs text-gray-600 mt-1 leading-relaxed">{w.message}</p>
              </div>
            </div>

            <div className="pt-2 border-t border-amber-100 flex items-center justify-end">
              <Link to={w.actionUrl}>
                <Button size="sm" variant="primary" className="text-xs font-bold shadow-sm">
                  <span>{w.actionLabel}</span>
                  <ArrowLeft className="w-3.5 h-3.5 mr-1.5" />
                </Button>
              </Link>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};
