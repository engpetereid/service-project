import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Select } from '../ui/Select';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { Spinner } from '../ui/Spinner';
import { reportsApi } from '../../api/reports.api';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { weeksApi } from '../../api/weeks.api';
import { GradeClassResponse } from '../../types/ministry.types';
import { FileDown, FileSpreadsheet, CheckCircle2, Printer } from 'lucide-react';

interface CsvExportDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  defaultWeekId?: number;
}

type ReportType = 'students' | 'visits' | 'attendance' | 'confessions';

export const CsvExportDrawer: React.FC<CsvExportDrawerProps> = ({
  isOpen,
  onClose,
  defaultWeekId,
}) => {
  const [reportType, setReportType] = useState<ReportType>('visits');
  const [selectedMinistryId, setSelectedMinistryId] = useState<string>('');
  const [selectedClassId, setSelectedClassId] = useState<string>('');
  const [selectedWeekId, setSelectedWeekId] = useState<string>(
    defaultWeekId ? String(defaultWeekId) : ''
  );
  const [selectedActivity, setSelectedActivity] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportSuccess, setExportSuccess] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load ministries for filters
  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isOpen,
  });

  // Load weeks for filters
  const { data: weeks = [] } = useQuery({
    queryKey: ['weeks'],
    queryFn: () => weeksApi.getAll(false),
    enabled: isOpen,
  });

  // Available classes based on selected ministry
  const { data: availableClasses = [] } = useQuery<GradeClassResponse[]>({
    queryKey: ['classes', selectedMinistryId],
    queryFn: () => classesApi.findAll(selectedMinistryId ? Number(selectedMinistryId) : undefined),
    enabled: isOpen && !!selectedMinistryId,
  });

  const handleExport = async () => {
    setIsExporting(true);
    setErrorMessage(null);
    setExportSuccess(false);

    try {
      const minId = selectedMinistryId ? Number(selectedMinistryId) : undefined;
      const clsId = selectedClassId ? Number(selectedClassId) : undefined;
      const wkId = selectedWeekId ? Number(selectedWeekId) : undefined;

      switch (reportType) {
        case 'students':
          await reportsApi.exportStudents({ ministryId: minId, classId: clsId });
          break;
        case 'visits':
          await reportsApi.exportVisits({ weekId: wkId, ministryId: minId, classId: clsId });
          break;
        case 'attendance':
          await reportsApi.exportAttendance({
            weekId: wkId,
            activityType: selectedActivity || undefined,
            ministryId: minId,
            classId: clsId,
          });
          break;
        case 'confessions':
          await reportsApi.exportConfessions({
            ministryId: minId,
            classId: clsId,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          });
          break;
      }

      setExportSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'فشل تحميل التقرير. يرجى المحاولة لاحقاً.');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title="تصدير تقارير الإحصائيات والمتابعة (CSV)"
      size="md"
    >
      <div className="space-y-6">
        {/* Intro */}
        <div className="p-4 bg-primary-50/40 rounded-2xl border border-primary-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary-600 text-white flex items-center justify-center shrink-0">
            <FileSpreadsheet className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-gray-900 text-sm">تصدير متوافق مع Excel</h4>
            <p className="text-xs text-gray-500 mt-0.5">
              الملفات الناتجة تدعم اللغة العربية بالكامل بترميز UTF-8 مع BOM.
            </p>
          </div>
        </div>

        {/* Report Type Selector Tabs */}
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-2">نوع التقرير المراد تصديره</label>
          <div className="grid grid-cols-2 gap-2">
            {[
              { id: 'visits', title: 'سجل الافتقاد الأسبوعي' },
              { id: 'attendance', title: 'سجل الحضور والغياب' },
              { id: 'students', title: 'سجل المخدومين الشامل' },
              { id: 'confessions', title: 'سجل الاعترافات' },
            ].map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setReportType(tab.id as ReportType);
                  setExportSuccess(false);
                }}
                className={`py-3 px-3 text-xs font-bold rounded-xl border text-center transition min-h-[44px] ${
                  reportType === tab.id
                    ? 'bg-primary-600 text-white border-primary-600 shadow-sm'
                    : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {tab.title}
              </button>
            ))}
          </div>
        </div>

        {/* Optional Filters */}
        <div className="space-y-4 pt-2 border-t border-gray-100">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider">
            خيارات التصفية والنطاق
          </h4>

          {/* Week Filter (for visits and attendance) */}
          {(reportType === 'visits' || reportType === 'attendance') && (
            <Select
              label="الأسبوع المستهدف"
              value={selectedWeekId}
              onChange={(e) => setSelectedWeekId(e.target.value)}
              options={[
                { value: '', label: 'الأسبوع الحالي (افتراضي)' },
                ...weeks.map((w) => ({
                  value: String(w.id),
                  label: `أسبوع ${w.startDate} إلى ${w.endDate}`,
                })),
              ]}
            />
          )}

          {/* Activity Type (for attendance) */}
          {reportType === 'attendance' && (
            <Select
              label="نوع النشاط"
              value={selectedActivity}
              onChange={(e) => setSelectedActivity(e.target.value)}
              options={[
                { value: '', label: 'كافة الأنشطة (قداس، اجتماع، تسبحة)' },
                { value: 'MASS', label: 'القداس الإلهي' },
                { value: 'MEETING', label: 'اجتماع الخدمة' },
                { value: 'TASBEHA', label: 'التسبحة' },
              ]}
            />
          )}

          {/* Ministry Filter */}
          <Select
            label="الخدمة (اختياري)"
            value={selectedMinistryId}
            onChange={(e) => {
              setSelectedMinistryId(e.target.value);
              setSelectedClassId('');
            }}
            options={[
              { value: '', label: 'كافة الخدمات المصرح بها' },
              ...ministries.map((m) => ({
                value: String(m.id),
                label: m.name,
              })),
            ]}
          />

          {/* Class Filter */}
          {selectedMinistryId && availableClasses.length > 0 && (
            <Select
              label="الفصل (اختياري)"
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
              options={[
                { value: '', label: 'كافة فصول الخدمة' },
                ...availableClasses.map((c) => ({
                  value: String(c.id),
                  label: c.name,
                })),
              ]}
            />
          )}

          {/* Date range for confessions */}
          {reportType === 'confessions' && (
            <div className="grid grid-cols-2 gap-2 pt-1">
              <Input
                label="من تاريخ"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
              <Input
                label="إلى تاريخ"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          )}
        </div>

        {/* Feedback alerts */}
        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}

        {exportSuccess && (
          <Alert variant="success" className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>تم تصدير وتحميل الملف بنجاح! تفقد مجلد التنزيلات.</span>
          </Alert>
        )}

        {/* Action Buttons */}
        <div className="pt-4 border-t border-gray-100 space-y-2">
          <div className="flex gap-2">
            <Button
              variant="primary"
              className="flex-1 font-bold h-11"
              disabled={isExporting}
              onClick={handleExport}
            >
              {isExporting ? (
                <span className="flex items-center justify-center gap-2">
                  <Spinner size="sm" /> جاري تجهيز الملف...
                </span>
              ) : (
                <span className="flex items-center justify-center gap-2">
                  <FileDown className="w-4 h-4" />
                  تصدير وتحميل CSV (Excel)
                </span>
              )}
            </Button>

            <Button
              variant="outline"
              onClick={() => window.print()}
              className="font-bold h-11 gap-1.5 px-4"
              title="طباعة تقرير الشاشة الحالي"
            >
              <Printer className="w-4 h-4 text-gray-600" />
              <span>طباعة</span>
            </Button>
          </div>

          <Button variant="ghost" onClick={onClose} disabled={isExporting} className="w-full text-xs text-gray-500">
            إغلاق النافذة
          </Button>
        </div>
      </div>
    </Drawer>
  );
};
