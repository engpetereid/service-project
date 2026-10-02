import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { selfFollowupApi } from '../api/selfFollowup.api';
import { visitsApi } from '../api/visits.api';
import { WeekResponse } from '../types/visit.types';
import { SelfFollowUpRequest, SelfFollowUpResponse } from '../types/selfFollowup.types';
import { WeekTimelineBanner } from '../components/attendance/WeekTimelineBanner';
import { Card } from '../components/ui/Card';
import { Spinner } from '../components/ui/Spinner';
import { usePermissions } from '../auth/usePermissions';
import { formatDate } from '../utils/date';
import {
  ClipboardCheck,
  Sparkles,
  Lock,
  Church,
  BookOpen,
  Music,
  Users2,
  Flame,
  History,
  TrendingUp,
  Check,
  AlertCircle,
  Calendar,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

export const SelfFollowUpPage: React.FC = () => {
  const { isAdmin } = usePermissions();
  const queryClient = useQueryClient();

  // Selected week ID (null means current week)
  const [selectedWeekId, setSelectedWeekId] = useState<number | null>(null);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const [showHistory, setShowHistory] = useState(false);

  // Fetch all weeks to populate timeline banner
  const { data: allWeeks = [] } = useQuery<WeekResponse[]>({
    queryKey: ['weeks'],
    queryFn: visitsApi.getWeeks,
  });

  // Current week self-follow-up query
  const {
    data: currentWeekData,
    isLoading: isCurrentLoading,
  } = useQuery({
    queryKey: ['self-followup', 'current-week'],
    queryFn: selfFollowupApi.getCurrentWeek,
  });

  const currentWeek = currentWeekData?.week || (allWeeks.find((w) => w.active) ?? allWeeks[0] ?? null);

  // Active week resolution
  const activeWeek: WeekResponse | null = useMemo(() => {
    if (!selectedWeekId) return currentWeek;
    return allWeeks.find((w) => w.id === selectedWeekId) || currentWeek;
  }, [selectedWeekId, currentWeek, allWeeks]);

  const isViewingCurrent = !selectedWeekId || selectedWeekId === currentWeek?.id;

  // Query specific week record if not viewing current week
  const {
    data: historicalRecord,
    isLoading: isHistoricalLoading,
  } = useQuery({
    queryKey: ['self-followup', 'by-week', activeWeek?.id],
    queryFn: () => selfFollowupApi.getByWeek(activeWeek!.id),
    enabled: !!activeWeek?.id && !isViewingCurrent,
    retry: false, // 404 is normal if no record exists for that past week
  });

  // Query personal history for current academic year
  const { data: historyRecords = [] } = useQuery<SelfFollowUpResponse[]>({
    queryKey: ['self-followup', 'history'],
    queryFn: selfFollowupApi.getHistory,
  });

  // Query personal statistics
  const { data: stats } = useQuery({
    queryKey: ['self-followup', 'statistics'],
    queryFn: selfFollowupApi.getStatistics,
  });

  // Active record resolution
  const activeRecord: SelfFollowUpResponse | null = useMemo(() => {
    if (isViewingCurrent) {
      return currentWeekData?.record ?? null;
    }
    return historicalRecord ?? null;
  }, [isViewingCurrent, currentWeekData, historicalRecord]);

  const maxNoteScore = activeRecord?.maxNoteScoreSnapshot ?? currentWeekData?.maxNoteScore ?? 21;
  const isWeekLocked = !!activeWeek?.locked && !isAdmin;

  // Local state for auto-save fields
  const [attendedMass, setAttendedMass] = useState<boolean | null>(null);
  const [attendedServiceMeeting, setAttendedServiceMeeting] = useState<boolean | null>(null);
  const [attendedTasbeha, setAttendedTasbeha] = useState<boolean | null>(null);
  const [attendedManagementMeeting, setAttendedManagementMeeting] = useState<boolean | null>(null);
  const [noteScore, setNoteScore] = useState<number | null>(null);

  // Sync state whenever activeRecord changes
  useEffect(() => {
    if (activeRecord) {
      setAttendedMass(activeRecord.attendedMass);
      setAttendedServiceMeeting(activeRecord.attendedServiceMeeting);
      setAttendedTasbeha(activeRecord.attendedTasbeha);
      setAttendedManagementMeeting(activeRecord.attendedManagementMeeting);
      setNoteScore(activeRecord.noteScore);
    } else {
      setAttendedMass(null);
      setAttendedServiceMeeting(null);
      setAttendedTasbeha(null);
      setAttendedManagementMeeting(null);
      setNoteScore(null);
    }
  }, [activeRecord, activeWeek?.id]);

  // Upsert mutation
  const upsertMutation = useMutation({
    mutationFn: (payload: SelfFollowUpRequest) => selfFollowupApi.upsert(payload),
    onMutate: () => {
      setSaveStatus('saving');
    },
    onSuccess: (data) => {
      setSaveStatus('saved');
      queryClient.setQueryData(['self-followup', 'current-week'], (old: any) => {
        if (!old) return old;
        if (old.week?.id === data.weekId) {
          return { ...old, record: data };
        }
        return old;
      });
      if (!isViewingCurrent && activeWeek?.id) {
        queryClient.setQueryData(['self-followup', 'by-week', activeWeek.id], data);
      }
      queryClient.invalidateQueries({ queryKey: ['self-followup', 'history'] });
      queryClient.invalidateQueries({ queryKey: ['self-followup', 'statistics'] });
      setTimeout(() => {
        setSaveStatus((prev) => (prev === 'saved' ? 'idle' : prev));
      }, 2000);
    },
    onError: () => {
      setSaveStatus('error');
    },
  });

  // Auto-save trigger
  const triggerSave = useCallback(
    (overrides?: Partial<SelfFollowUpRequest>) => {
      if (!activeWeek?.id || isWeekLocked) return;

      const payload: SelfFollowUpRequest = {
        weekId: activeWeek.id,
        noteScore: overrides && 'noteScore' in overrides ? (overrides.noteScore ?? null) : noteScore,
        attendedMass: overrides && 'attendedMass' in overrides ? (overrides.attendedMass ?? null) : attendedMass,
        attendedServiceMeeting:
          overrides && 'attendedServiceMeeting' in overrides
            ? (overrides.attendedServiceMeeting ?? null)
            : attendedServiceMeeting,
        attendedTasbeha:
          overrides && 'attendedTasbeha' in overrides ? (overrides.attendedTasbeha ?? null) : attendedTasbeha,
        attendedManagementMeeting:
          overrides && 'attendedManagementMeeting' in overrides
            ? (overrides.attendedManagementMeeting ?? null)
            : attendedManagementMeeting,
      };

      upsertMutation.mutate(payload);
    },
    [
      activeWeek?.id,
      isWeekLocked,
      noteScore,
      attendedMass,
      attendedServiceMeeting,
      attendedTasbeha,
      attendedManagementMeeting,
      upsertMutation,
    ]
  );

  // Toggle handler
  const handleToggle = (
    field: 'attendedMass' | 'attendedServiceMeeting' | 'attendedTasbeha' | 'attendedManagementMeeting',
    currentVal: boolean | null
  ) => {
    if (isWeekLocked) return;
    // Cycling: null -> true -> false -> true
    const nextVal = currentVal === true ? false : true;

    if (field === 'attendedMass') {
      setAttendedMass(nextVal);
      triggerSave({ attendedMass: nextVal });
    } else if (field === 'attendedServiceMeeting') {
      setAttendedServiceMeeting(nextVal);
      triggerSave({ attendedServiceMeeting: nextVal });
    } else if (field === 'attendedTasbeha') {
      setAttendedTasbeha(nextVal);
      triggerSave({ attendedTasbeha: nextVal });
    } else if (field === 'attendedManagementMeeting') {
      setAttendedManagementMeeting(nextVal);
      triggerSave({ attendedManagementMeeting: nextVal });
    }
  };

  // Note score change handler with instant state and debounced trigger
  const handleScoreChange = (newScore: number) => {
    if (isWeekLocked) return;
    const clamped = Math.max(0, Math.min(maxNoteScore, newScore));
    setNoteScore(clamped);
    triggerSave({ noteScore: clamped });
  };

  // Live calculation of overall percentage
  const liveOverallPercentage = useMemo(() => {
    const filled: number[] = [];
    if (attendedMass !== null) filled.push(attendedMass ? 100 : 0);
    if (attendedServiceMeeting !== null) filled.push(attendedServiceMeeting ? 100 : 0);
    if (attendedTasbeha !== null) filled.push(attendedTasbeha ? 100 : 0);
    if (attendedManagementMeeting !== null) filled.push(attendedManagementMeeting ? 100 : 0);
    if (noteScore !== null) {
      filled.push(Math.min(100, (noteScore * 100) / maxNoteScore));
    }
    if (filled.length === 0) return 0;
    const avg = filled.reduce((a, b) => a + b, 0) / filled.length;
    return Math.round(avg * 10) / 10;
  }, [attendedMass, attendedServiceMeeting, attendedTasbeha, attendedManagementMeeting, noteScore, maxNoteScore]);

  const isLoading = isCurrentLoading || (!isViewingCurrent && isHistoricalLoading);

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-16 px-1 sm:px-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight flex items-center gap-2">
              <ClipboardCheck className="w-6 h-6 text-primary-600" />
              <span>متابعتي الأسبوعية</span>
            </h1>

          </div>
          <p className="text-xs text-gray-500 mt-1">
            سجل شخصي خاص بك لتوثيق حضورك ومشاركتك الروحية والخدمية أسبوعياً
          </p>
        </div>

        {/* Auto-save notification pill */}
        <div className="flex items-center gap-2">
          {saveStatus === 'saving' && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200 animate-pulse">
              <Spinner size="sm" />
              جاري الحفظ...
            </span>
          )}
          {saveStatus === 'saved' && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 animate-in fade-in">
              <Check className="w-3.5 h-3.5" />
              تم الحفظ تلقائياً
            </span>
          )}
          {saveStatus === 'error' && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-red-50 text-red-700 border border-red-200">
              <AlertCircle className="w-3.5 h-3.5" />
              تعذر الحفظ
            </span>
          )}
        </div>
      </div>

      {/* Week Timeline Banner */}
      <WeekTimelineBanner
        currentWeek={currentWeek}
        selectedWeek={activeWeek}
        allWeeks={allWeeks}
        onSelectWeek={(w) => setSelectedWeekId(w.id)}
        isAdmin={isAdmin}
      />

      {/* Week Locked Banner */}
      {isWeekLocked && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 flex items-center gap-3 text-amber-900 text-xs font-medium">
          <Lock className="w-5 h-5 text-amber-600 shrink-0" />
          <span>
            هذا الأسبوع مغلق تاريخياً (مضى عليه أكثر من 30 يوماً). لا يمكن تسجيل أو تعديل متابعتك الشخصية لهذا الأسبوع.
          </span>
        </div>
      )}

      {/* Main Checklist Card */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل متابعتك الأسبوعية...</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Real-time Overall Score Card */}
          <Card className="p-5 bg-gradient-to-br from-white via-primary-50/20 to-white border-primary-100 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <span className="text-[11px] font-bold text-primary-600 uppercase tracking-wider block mb-1">
                  المعدل العام للأسبوع المختار
                </span>
                <div className="flex items-center gap-3">
                  <span className="text-3xl sm:text-4xl font-black text-gray-900 font-mono">
                    {liveOverallPercentage}%
                  </span>
                  <div className="text-xs text-gray-500">
                    <p className="font-bold text-gray-700">
                      {liveOverallPercentage >= 85
                        ? '🌟 ممتاز جداً!'
                        : liveOverallPercentage >= 70
                        ? '👍 جيد جداً!'
                        : liveOverallPercentage >= 50
                        ? '👍 جيد ونسعى لمزيد من النمو الاسبوع القادم'
                        : '🤍 ابدأ بتسجيل حضورك ونوتتك لهذا الأسبوع'}
                    </p>
                    <p className="text-[11px] text-gray-400 mt-0.5">
                      يُحسب بمتوسط الأنشطة المسجلة (القداس، الاجتماع، التسبحة، النوتة، المجلس)
                    </p>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-center">
                <span className="px-3 py-1.5 rounded-xl bg-primary-100/70 text-primary-800 text-xs font-bold font-mono">
                  {noteScore !== null ? `${noteScore} / ${maxNoteScore} نوتة` : 'النوتة لم تُحدد'}
                </span>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="space-y-1">
              <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 ease-out ${
                    liveOverallPercentage === 100
                      ? 'bg-emerald-500'
                      : liveOverallPercentage >= 70
                      ? 'bg-gradient-to-r from-primary-600 to-indigo-600'
                      : 'bg-gradient-to-r from-amber-500 to-primary-600'
                  }`}
                  style={{ width: `${liveOverallPercentage}%` }}
                />
              </div>
            </div>
          </Card>

          {/* Section 1: Note Score Slider */}
          <Card className="p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center font-bold">
                  <BookOpen className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-sm sm:text-base">
                    درجة النوتة الروحية (الكتاب المقدس والصلاة)
                  </h3>
                  <p className="text-xs text-gray-500">
                    الحد الأقصى للنوتة لهذا الأسبوع: {maxNoteScore} درجة
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-1">
                <span className="text-2xl font-black font-mono text-purple-700">
                  {noteScore ?? 0}
                </span>
                <span className="text-xs font-bold text-gray-400 font-mono">/ {maxNoteScore}</span>
              </div>
            </div>

            {/* Slider with quick buttons */}
            <div className="space-y-3 pt-2">
              <input
                type="range"
                min={0}
                max={maxNoteScore}
                value={noteScore ?? 0}
                disabled={isWeekLocked}
                onChange={(e) => handleScoreChange(Number(e.target.value))}
                className="w-full h-2.5 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-purple-600 disabled:opacity-50 disabled:cursor-not-allowed"
              />

              <div className="flex items-center justify-between text-xs text-gray-400 font-mono">
                <span>0</span>
                <span>منتصف: {Math.round(maxNoteScore / 2)}</span>
                <span>الحد الأقصى: {maxNoteScore}</span>
              </div>

              {/* Quick Stepper Buttons */}
              <div className="flex items-center justify-center gap-2 pt-1 flex-wrap">
                {[0, Math.round(maxNoteScore / 2), maxNoteScore].map((quickVal) => (
                  <button
                    key={quickVal}
                    type="button"
                    disabled={isWeekLocked}
                    onClick={() => handleScoreChange(quickVal)}
                    className={`px-3 py-1 rounded-lg text-xs font-bold transition border ${
                      noteScore === quickVal
                        ? 'bg-purple-600 text-white border-purple-600 shadow-sm'
                        : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    {quickVal === 0 ? '0' : quickVal === maxNoteScore ? `الكاملة (${maxNoteScore})` : `${quickVal}`}
                  </button>
                ))}
              </div>
            </div>
          </Card>

          {/* Section 2: Four Attendance Toggles (Checklist feel) */}
          <div className="space-y-3">
            <h3 className="text-sm font-black text-gray-900 tracking-tight flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-primary-600" />
              <span>مشاركتي وحضوري الأسبوعي (قائمة المتابعة)</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
              {/* 1. Mass Toggle */}
              <button
                type="button"
                disabled={isWeekLocked}
                onClick={() => handleToggle('attendedMass', attendedMass)}
                className={`p-4 rounded-2xl border text-right transition flex items-center justify-between gap-3 shadow-sm min-h-[76px] ${
                  attendedMass === true
                    ? 'bg-emerald-50/80 border-emerald-300 ring-2 ring-emerald-500/20'
                    : attendedMass === false
                    ? 'bg-gray-50/80 border-gray-200 opacity-80'
                    : 'bg-white border-gray-200 hover:border-primary-200'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      attendedMass === true
                        ? 'bg-emerald-600 text-white'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    <Church className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">القداس</h4>
                  </div>
                </div>

                <div
                  className={`w-7 h-7 rounded-xl flex items-center justify-center transition border ${
                    attendedMass === true
                      ? 'bg-emerald-600 border-emerald-600 text-white shadow-sm'
                      : 'border-gray-300 bg-white text-transparent'
                  }`}
                >
                  <Check className="w-4 h-4 stroke-[3]" />
                </div>
              </button>

              {/* 2. Service Meeting Toggle */}
              <button
                type="button"
                disabled={isWeekLocked}
                onClick={() => handleToggle('attendedServiceMeeting', attendedServiceMeeting)}
                className={`p-4 rounded-2xl border text-right transition flex items-center justify-between gap-3 shadow-sm min-h-[76px] ${
                  attendedServiceMeeting === true
                    ? 'bg-emerald-50/80 border-emerald-300 ring-2 ring-emerald-500/20'
                    : attendedServiceMeeting === false
                    ? 'bg-gray-50/80 border-gray-200 opacity-80'
                    : 'bg-white border-gray-200 hover:border-primary-200'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      attendedServiceMeeting === true
                        ? 'bg-emerald-600 text-white'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    <Flame className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">اجتماع الخدام</h4>
                  </div>
                </div>

                <div
                  className={`w-7 h-7 rounded-xl flex items-center justify-center transition border ${
                    attendedServiceMeeting === true
                      ? 'bg-emerald-600 border-emerald-600 text-white shadow-sm'
                      : 'border-gray-300 bg-white text-transparent'
                  }`}
                >
                  <Check className="w-4 h-4 stroke-[3]" />
                </div>
              </button>

              {/* 3. Tasbeha Toggle */}
              <button
                type="button"
                disabled={isWeekLocked}
                onClick={() => handleToggle('attendedTasbeha', attendedTasbeha)}
                className={`p-4 rounded-2xl border text-right transition flex items-center justify-between gap-3 shadow-sm min-h-[76px] ${
                  attendedTasbeha === true
                    ? 'bg-emerald-50/80 border-emerald-300 ring-2 ring-emerald-500/20'
                    : attendedTasbeha === false
                    ? 'bg-gray-50/80 border-gray-200 opacity-80'
                    : 'bg-white border-gray-200 hover:border-primary-200'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      attendedTasbeha === true
                        ? 'bg-emerald-600 text-white'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    <Music className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">التسبحة</h4>
                  </div>
                </div>

                <div
                  className={`w-7 h-7 rounded-xl flex items-center justify-center transition border ${
                    attendedTasbeha === true
                      ? 'bg-emerald-600 border-emerald-600 text-white shadow-sm'
                      : 'border-gray-300 bg-white text-transparent'
                  }`}
                >
                  <Check className="w-4 h-4 stroke-[3]" />
                </div>
              </button>

              {/* 4. Management Meeting Toggle */}
              <button
                type="button"
                disabled={isWeekLocked}
                onClick={() => handleToggle('attendedManagementMeeting', attendedManagementMeeting)}
                className={`p-4 rounded-2xl border text-right transition flex items-center justify-between gap-3 shadow-sm min-h-[76px] ${
                  attendedManagementMeeting === true
                    ? 'bg-emerald-50/80 border-emerald-300 ring-2 ring-emerald-500/20'
                    : attendedManagementMeeting === false
                    ? 'bg-gray-50/80 border-gray-200 opacity-80'
                    : 'bg-white border-gray-200 hover:border-primary-200'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      attendedManagementMeeting === true
                        ? 'bg-emerald-600 text-white'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    <Users2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">الاجتماع التدبيري</h4>
                  </div>
                </div>

                <div
                  className={`w-7 h-7 rounded-xl flex items-center justify-center transition border ${
                    attendedManagementMeeting === true
                      ? 'bg-emerald-600 border-emerald-600 text-white shadow-sm'
                      : 'border-gray-300 bg-white text-transparent'
                  }`}
                >
                  <Check className="w-4 h-4 stroke-[3]" />
                </div>
              </button>
            </div>
          </div>

          {/* Section 3: Personal Statistics Summary */}
          {stats && (
            <Card className="p-5 space-y-4 bg-gray-50/60 border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-gray-900 text-sm flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-primary-600" />
                  <span>إحصائيات انضباطي السنوي لهذا العام</span>
                </h3>
                <span className="text-xs text-gray-500 font-mono">
                  {stats.recordedWeeks} من أصل {stats.totalWeeks} أسابيع مسجلة ({stats.recordingRate}%)
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                <div className="p-3 bg-white rounded-xl border border-gray-100 text-center">
                  <span className="text-xs text-gray-500 block mb-1">القداس الإلهي</span>
                  <strong className="text-lg font-black text-gray-900 font-mono">
                    {stats.avgMassRate !== null ? `${stats.avgMassRate}%` : '—'}
                  </strong>
                </div>

                <div className="p-3 bg-white rounded-xl border border-gray-100 text-center">
                  <span className="text-xs text-gray-500 block mb-1">اجتماع الخدمة</span>
                  <strong className="text-lg font-black text-gray-900 font-mono">
                    {stats.avgServiceMeetingRate !== null ? `${stats.avgServiceMeetingRate}%` : '—'}
                  </strong>
                </div>

                <div className="p-3 bg-white rounded-xl border border-gray-100 text-center">
                  <span className="text-xs text-gray-500 block mb-1">التسبحة</span>
                  <strong className="text-lg font-black text-gray-900 font-mono">
                    {stats.avgTasbehaRate !== null ? `${stats.avgTasbehaRate}%` : '—'}
                  </strong>
                </div>

                <div className="p-3 bg-white rounded-xl border border-gray-100 text-center">
                  <span className="text-xs text-gray-500 block mb-1">متوسط النوتة</span>
                  <strong className="text-lg font-black text-purple-700 font-mono">
                    {stats.avgNotePercentage !== null ? `${stats.avgNotePercentage}%` : '—'}
                  </strong>
                </div>

                <div className="p-3 bg-white rounded-xl border border-gray-100 text-center col-span-2 sm:col-span-1">
                  <span className="text-xs text-gray-500 block mb-1">المعدل العام</span>
                  <strong className="text-lg font-black text-emerald-600 font-mono">
                    {stats.avgOverallPercentage !== null ? `${stats.avgOverallPercentage}%` : '—'}
                  </strong>
                </div>
              </div>
            </Card>
          )}

          {/* Section 4: History / Previous Weeks Accordion */}
          <div className="space-y-3">
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              className="w-full flex items-center justify-between p-4 bg-white rounded-2xl border border-gray-200 hover:border-primary-200 transition font-bold text-sm text-gray-900 shadow-sm"
            >
              <div className="flex items-center gap-2">
                <History className="w-4 h-4 text-primary-600" />
                <span>سجل الأسابيع السابقة ({historyRecords.length} أسبوع مسجل)</span>
              </div>
              {showHistory ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>

            {showHistory && (
              <div className="space-y-2 pt-1 animate-in fade-in">
                {historyRecords.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center py-4">لا توجد أسابيع مسجلة سابقة بعد</p>
                ) : (
                  historyRecords.map((r) => (
                    <div
                      key={r.id}
                      onClick={() => setSelectedWeekId(r.weekId)}
                      className={`p-3.5 rounded-xl border cursor-pointer transition flex items-center justify-between gap-3 text-xs ${
                        activeWeek?.id === r.weekId
                          ? 'bg-primary-50/60 border-primary-300'
                          : 'bg-white border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-gray-400" />
                        <span className="font-bold text-gray-800">
                          {formatDate(r.weekStartDate)} — {formatDate(r.weekEndDate)}
                        </span>
                        {r.weekLocked && (
                          <span className="px-1.5 py-0.5 rounded bg-gray-100 text-[10px] text-gray-500 font-bold flex items-center gap-1">
                            <Lock className="w-3 h-3" />
                            مغلق
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="font-mono text-purple-700 font-bold">
                          نوتة: {r.noteScore ?? '—'}/{r.maxNoteScoreSnapshot}
                        </span>
                        <span
                          className={`font-black font-mono px-2 py-0.5 rounded-md ${
                            r.overallPercentage >= 70
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-amber-50 text-amber-700'
                          }`}
                        >
                          {r.overallPercentage}%
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
