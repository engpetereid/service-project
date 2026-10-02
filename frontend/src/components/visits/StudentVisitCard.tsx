import React from 'react';
import { StudentVisitStatusResponse, VisitRecordResponse } from '../../types/visit.types';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { VISIT_METHOD_LABELS } from '../../utils/arabic';
import { formatDateTime } from '../../utils/date';
import {
  Phone,
  PhoneCall,
  Home,
  CheckCircle2,
  Clock,
  Edit3,
  Award,
  MessageCircle,
  Sparkles,
} from 'lucide-react';

interface StudentVisitCardProps {
  item: StudentVisitStatusResponse;
  isWeekLocked?: boolean;
  onRecordVisit: (item: StudentVisitStatusResponse) => void;
  onEditVisit: (item: StudentVisitStatusResponse, record: VisitRecordResponse) => void;
}

export const StudentVisitCard: React.FC<StudentVisitCardProps> = ({
  item,
  isWeekLocked = false,
  onRecordVisit,
  onEditVisit,
}) => {
  const isVisited = item.visited;
  const record = item.visitRecord;

  const studentCleanPhone = item.phone.replace(/\D/g, '');
  const guardianCleanPhone = item.guardianPhone?.replace(/\D/g, '');

  const studentWaMsg = encodeURIComponent(
    ` يا ${item.studentName}، بنطمن عليك`
  );
  const studentWaUrl = `https://wa.me/2${studentCleanPhone}?text=${studentWaMsg}`;

  const guardianWaMsg = encodeURIComponent(
    `سلام ونعمة لحضرتك،انا خادم ${item.studentName}`
  );
  const guardianWaUrl = guardianCleanPhone
    ? `https://wa.me/2${guardianCleanPhone}?text=${guardianWaMsg}`
    : null;

  return (
    <div
      className={`rounded-3xl p-5 border transition shadow-sm ${
        isVisited
          ? 'bg-gradient-to-r from-emerald-50/40 via-white to-white border-emerald-200/80 shadow-emerald-500/5'
          : 'bg-white border-gray-100 hover:border-gray-200'
      }`}
    >
      {/* Top Header: Name, Address, Status Badge */}
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="flex items-center gap-3">
          <div
            className={`w-11 h-11 rounded-2xl flex items-center justify-center font-black text-sm shrink-0 ${
              isVisited
                ? 'bg-emerald-100 text-emerald-800'
                : 'bg-gray-100 text-gray-700'
            }`}
          >
            {item.studentName.charAt(0)}
          </div>
          <div>
            <h3 className="font-bold text-gray-900 text-base leading-tight">
              {item.studentName}
            </h3>
            {item.address && (
              <span className="text-[11px] text-gray-400 block mt-0.5 truncate max-w-[200px] sm:max-w-xs">
                {item.address}
              </span>
            )}
          </div>
        </div>

        <div>
          {isVisited ? (
            <Badge variant="success" className="gap-1 font-bold">
              <CheckCircle2 className="w-3.5 h-3.5" />
              تم الافتقاد
            </Badge>
          ) : (
            <Badge variant="warning" className="gap-1 font-bold">
              <Clock className="w-3.5 h-3.5" />
              لم يفتقد بعد
            </Badge>
          )}
        </div>
      </div>

      {/* Middle Section: Direct Communication Buttons (WhatsApp, Call) */}
      <div className="flex flex-wrap items-center justify-between gap-3 py-3 border-y border-gray-50 text-xs">
        {/* Student and Guardian Contact Buttons */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Student Phone */}
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
              href={studentWaUrl}
              target="_blank"
              rel="noreferrer"
              className="p-1.5 rounded-lg hover:bg-emerald-100 text-emerald-600 transition"
              title="محادثة واتساب سريعة"
            >
              <MessageCircle className="w-3.5 h-3.5" />
            </a>
          </div>

          {/* Guardian Phone */}
          {item.guardianPhone && (
            <div className="flex items-center gap-1 bg-gray-50/70 p-1 px-2 rounded-xl border border-gray-100 text-gray-600">
              <span className="text-[10px] text-gray-400">ولي الأمر:</span>
              <span className="font-mono text-[11px] font-medium" dir="ltr">
                {item.guardianPhone}
              </span>
              <a
                href={`tel:${item.guardianPhone}`}
                className="p-1 rounded hover:bg-gray-200 text-gray-500 transition"
                title="اتصال بولي الأمر"
              >
                <Phone className="w-3 h-3 text-primary-600" />
              </a>
              {guardianWaUrl && (
                <a
                  href={guardianWaUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="p-1 rounded hover:bg-emerald-100 text-emerald-600 transition"
                  title="محادثة ولي الأمر واتساب"
                >
                  <MessageCircle className="w-3 h-3" />
                </a>
              )}
            </div>
          )}
        </div>

        {/* Visit Method Badge if Visited */}
        {isVisited && record && (
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-100/60 text-emerald-800 font-bold text-[11px]">
              {record.method === 'VISIT' ? (
                <Home className="w-3.5 h-3.5 text-emerald-700" />
              ) : (
                <PhoneCall className="w-3.5 h-3.5 text-emerald-700" />
              )}
              {VISIT_METHOD_LABELS[record.method]}
            </span>
          </div>
        )}
      </div>

      {/* Bottom Section: Visited Details vs Action Button */}
      {isVisited && record ? (
        <div className="pt-3 space-y-2.5">
          {/* Scores Pills */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            {record.noteScore !== null && record.noteScore !== undefined && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-purple-50 text-purple-800 font-semibold border border-purple-100">
                <Award className="w-3.5 h-3.5 text-purple-600" />
                نوتة: {record.noteScore}
              </span>
            )}

            <span className="text-[11px] text-gray-400 mr-auto">
              سُجل: {formatDateTime(record.recordedAt)}
            </span>
          </div>

          {/* Notes Snippet if present */}
          {record.notes && (
            <p className="text-xs text-gray-600 bg-gray-50/80 p-2.5 rounded-xl border border-gray-100 italic">
              «{record.notes}»
            </p>
          )}

          {/* Edit Button */}
          {!isWeekLocked && (
            <div className="flex justify-end pt-1">
              <button
                type="button"
                onClick={() => onEditVisit(item, record)}
                className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-primary-700 font-bold p-1 transition min-h-[36px]"
              >
                <Edit3 className="w-3.5 h-3.5" />
                تعديل بيانات الافتقاد
              </button>
            </div>
          )}
        </div>
      ) : (
        /* Action to record visit */
        <div className="pt-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <span className="text-xs text-amber-700 flex items-center gap-1.5 font-medium">
            <Sparkles className="w-3.5 h-3.5 text-amber-500 shrink-0" />
            <span>يمكنك الاطمئنان عليه عبر الهاتف أو الزيارة وتوثيقها فوراً</span>
          </span>

          <Button
            variant="primary"
            size="md"
            onClick={() => onRecordVisit(item)}
            disabled={isWeekLocked}
            className="font-bold shadow-md shadow-primary-500/10 min-h-[44px] w-full sm:w-auto shrink-0"
          >
            تسجيل الافتقاد
          </Button>
        </div>
      )}
    </div>
  );
};
