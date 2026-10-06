import React from 'react';
import { StudentConfessionSummary } from '../../types/confession.types';
import { Button } from '../ui/Button';
import { formatDate } from '../../utils/date';
import {
  Calendar,
  User,
  Clock,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  Phone,
  MessageCircle,
  History,
  Plus,
} from 'lucide-react';

interface StudentConfessionCardProps {
  item: StudentConfessionSummary;
  onRecordConfession: (item: StudentConfessionSummary) => void;
  onViewHistory: (item: StudentConfessionSummary) => void;
}

export const StudentConfessionCard: React.FC<StudentConfessionCardProps> = ({
  item,
  onRecordConfession,
  onViewHistory,
}) => {
  const cleanPhone = item.phone ? item.phone.replace(/\D/g, '') : '';

  // Pre-filled WhatsApp message encouraging confession
  const waReminderMsg = encodeURIComponent(
    item.confessionFather
      ? ` يا ${item.studentName}، حابب أطمن عليك وأفكرك بميعاد الاعتراف مع ${item.confessionFather}. تحب أنسق معاك ميعاد؟`
      : ` يا ${item.studentName}، حابب أطمن عليك وأفكرك بالاعتراف. كلمني لو تحب نرتب ميعاد مع أبونا.`
  );
  const waUrl = `https://wa.me/2${cleanPhone}?text=${waReminderMsg}`;

  // Status configuration
  const getStatusBadge = () => {
    switch (item.status) {
      case 'UP_TO_DATE':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>منتظم حديثاً ({item.daysSinceLastConfession} يوم)</span>
          </span>
        );
      case 'OVERDUE':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            <span>متأخر ({item.daysSinceLastConfession} يوم)</span>
          </span>
        );
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>منقطع ({item.daysSinceLastConfession} يوم)</span>
          </span>
        );
      case 'NEVER':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-700 border border-gray-200">
            <AlertCircle className="w-3.5 h-3.5 text-gray-400" />
            <span>لم يُسجل اعتراف هذا العام</span>
          </span>
        );
    }
  };

  const getBorderAndBg = () => {
    switch (item.status) {
      case 'UP_TO_DATE':
        return 'bg-white border-gray-100 hover:border-emerald-200';
      case 'OVERDUE':
        return 'bg-gradient-to-r from-amber-50/20 via-white to-white border-amber-200/80 shadow-amber-500/5';
      case 'CRITICAL':
        return 'bg-gradient-to-r from-rose-50/25 via-white to-white border-rose-200/80 shadow-rose-500/5';
      case 'NEVER':
      default:
        return 'bg-white border-gray-100 hover:border-gray-200';
    }
  };

  return (
    <div
      className={`rounded-3xl p-5 border transition shadow-sm space-y-3.5 ${getBorderAndBg()}`}
    >
      {/* Top Header: Student Info, Class, Status Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div
            className={`w-11 h-11 rounded-2xl flex items-center justify-center font-black text-sm shrink-0 ${
              item.status === 'UP_TO_DATE'
                ? 'bg-emerald-100 text-emerald-800'
                : item.status === 'OVERDUE'
                ? 'bg-amber-100 text-amber-800'
                : item.status === 'CRITICAL'
                ? 'bg-rose-100 text-rose-800'
                : 'bg-gray-100 text-gray-700'
            }`}
          >
            {item.studentName.charAt(0)}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-gray-900 text-base leading-tight">
                {item.studentName}
              </h3>
              {item.className && (
                <span className="text-[11px] font-bold px-2 py-0.5 rounded-lg bg-gray-100 text-gray-700">
                  {item.className}
                </span>
              )}
            </div>

            {item.servantName && (
              <span className="text-[11px] text-gray-400 block mt-0.5">
                الخادم المسؤول: {item.servantName}
              </span>
            )}
          </div>
        </div>

        <div>{getStatusBadge()}</div>
      </div>

      {/* Middle Row: Confession Father & Last Confession Info */}
      <div className="flex flex-wrap items-center justify-between gap-3 py-3 border-y border-gray-50 text-xs">
        {/* Confession Father info */}
        <div className="flex items-center gap-2 flex-wrap">
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-50/70 border border-purple-100 text-purple-900 font-medium">
            <User className="w-3.5 h-3.5 text-purple-600" />
            <span className="text-[11px] text-purple-600">أب الاعتراف:</span>
            <strong className="font-bold">
              {item.confessionFather || 'لم يُحدد بعد'}
            </strong>
          </div>

          {/* Direct Phone & WhatsApp buttons */}
          {item.phone && (
            <div className="flex items-center gap-1 bg-gray-50 p-1 px-2 rounded-xl border border-gray-100">
              <span className="font-mono text-xs font-semibold text-gray-800" dir="ltr">
                {item.phone}
              </span>
              <a
                href={`tel:${item.phone}`}
                className="p-1.5 rounded-lg hover:bg-gray-200 text-gray-600 transition"
                title="اتصال هاتفي بالمخدوم"
              >
                <Phone className="w-3.5 h-3.5 text-primary-600" />
              </a>
              <a
                href={waUrl}
                target="_blank"
                rel="noreferrer"
                className="p-1.5 rounded-lg hover:bg-emerald-100 text-emerald-600 transition"
                title="إرسال تذكير بالاعتراف عبر واتساب"
              >
                <MessageCircle className="w-3.5 h-3.5" />
              </a>
            </div>
          )}
        </div>

        {/* Last Confession Date */}
        <div className="text-xs text-gray-500 font-medium flex items-center gap-1.5">
          <Calendar className="w-3.5 h-3.5 text-gray-400" />
          {item.lastConfessionDate ? (
            <span>
              آخر جلسة: <strong className="text-gray-900">{formatDate(item.lastConfessionDate)}</strong>
            </span>
          ) : (
            <span className="text-gray-400">لا يوجد تاريخ مسجل</span>
          )}
        </div>
      </div>

      {/* Bottom Row: Actions */}
      <div className="flex items-center justify-between gap-3 pt-1">
        <span className="text-xs text-gray-500">
          عدد جلسات الاعتراف هذا العام:{' '}
          <strong className="font-mono text-purple-700">{item.totalConfessionsThisYear}</strong>
        </span>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewHistory(item)}
            className="font-bold text-xs gap-1 min-h-[38px]"
          >
            <History className="w-3.5 h-3.5 text-purple-600" />
            <span>سجل الاعترافات</span>
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => onRecordConfession(item)}
            className="font-bold text-xs bg-purple-600 hover:bg-purple-700 gap-1 shadow-sm min-h-[38px]"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>تسجيل اعتراف</span>
          </Button>
        </div>
      </div>
    </div>
  );
};
