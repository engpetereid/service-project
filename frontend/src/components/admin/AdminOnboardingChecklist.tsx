import React, { useState } from 'react';
import { AdminSetupResponse } from '../../types/statistics.types';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Link } from 'react-router-dom';
import {
  CheckCircle2,
  Circle,
  ChevronDown,
  ChevronUp,
  Sparkles,
  ArrowLeft,
  Building2,
  School,
  UserCheck,
  Users,
  Shield,
} from 'lucide-react';

interface AdminOnboardingChecklistProps {
  setupData: AdminSetupResponse;
}

export const AdminOnboardingChecklist: React.FC<AdminOnboardingChecklistProps> = ({
  setupData,
}) => {
  const [isExpanded, setIsExpanded] = useState(!setupData.setupComplete);

  const steps = [
    {
      id: 1,
      title: 'إنشاء أول خدمة كنسية',
      description: 'إضافة المراحل والخدمات الأساسية (مثل: ابتدائي، إعدادي، ثانوي)',
      isDone: setupData.hasMinistry,
      actionLabel: 'إنشاء أول خدمة',
      actionUrl: '/ministries',
      icon: Building2,
    },
    {
      id: 2,
      title: 'إنشاء الفصول الدراسية',
      description: 'إضافة فصول كل خدمة (مثل: أولى ابتدائي، ثانية ابتدائي)',
      isDone: setupData.hasClasses,
      actionLabel: 'إضافة الفصول',
      actionUrl: '/ministries',
      icon: School,
    },
    {
      id: 3,
      title: 'تعيين أمين الخدمة',
      description: 'تحديد الخادم المسؤول عن كل خدمة',
      isDone: setupData.hasServiceSecretary,
      actionLabel: 'تعيين أمين الخدمة',
      actionUrl: '/ministries',
      icon: Shield,
    },
    {
      id: 4,
      title: 'تعيين أمين كل فصل',
      description: 'تحديد الخادم المسؤول عن متابعة كل فصل دراسي',
      isDone: setupData.hasClassSecretary,
      actionLabel: 'تعيين أمين الفصل',
      actionUrl: '/ministries',
      icon: UserCheck,
    },
    {
      id: 5,
      title: 'إضافة الخدام وتفعيل حسابات الدخول',
      description: 'تسجيل الخدام وإعطاؤهم صلاحيات وحسابات للدخول',
      isDone: setupData.hasServants,
      actionLabel: 'إضافة الخدام',
      actionUrl: '/servants',
      icon: Users,
    },
    {
      id: 6,
      title: 'إضافة المخدومين',
      description: 'تسجيل بيانات المخدومين وتسكينهم في الفصول الدراسية',
      isDone: setupData.hasStudents,
      actionLabel: 'إضافة مخدومين',
      actionUrl: '/students',
      icon: Users,
    },
    {
      id: 7,
      title: 'توزيع المخدومين على الخدام',
      description: 'ربط كل مخدوم بخادم مسؤول عن افتقاده ومتابعته الروحية',
      isDone: setupData.hasAssignments,
      actionLabel: 'توزيع المخدومين',
      actionUrl: '/students',
      icon: UserCheck,
    },
  ];

  const completedCount = steps.filter((s) => s.isDone).length;
  const progressPercent = Math.round((completedCount / steps.length) * 100);

  // Hide the onboarding guide completely once 100% completed
  if (progressPercent >= 100 || completedCount === steps.length) {
    return null;
  }

  return (
    <Card className="p-6 bg-gradient-to-br from-white via-primary-50/20 to-primary-100/10 border-primary-200 shadow-md">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-primary-600 text-white flex items-center justify-center font-bold shadow-md shadow-primary-500/20 shrink-0">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base sm:text-lg font-black text-gray-900 leading-tight">
              دليل تأسيس وإعداد نظام الخدمة
            </h2>
            <p className="text-xs text-gray-500 mt-0.5 font-medium">
              أكمل هذه الخطوات لتهيئة الهيكل التنظيمي وتمكين الخدام من متابعة الافتقاد
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-primary-700 bg-primary-100/80 px-3 py-1 rounded-full">
            {completedCount} من {steps.length} مكتمل ({progressPercent}%)
          </span>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-1.5 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-white/80 transition"
            aria-label={isExpanded ? 'طي الدليل' : 'توسيع الدليل'}
          >
            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-gray-200/80 h-2.5 rounded-full overflow-hidden mt-4 p-0.5">
        <div
          className="bg-primary-600 h-full rounded-full transition-all duration-500 ease-out"
          style={{ width: `${progressPercent}%` }}
        />
      </div>

      {/* Steps List */}
      {isExpanded && (
        <div className="mt-6 space-y-3 divide-y divide-gray-100/80">
          {steps.map((step) => {
            return (
              <div
                key={step.id}
                className="pt-3 first:pt-0 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="flex items-start sm:items-center gap-3">
                  <div className="mt-0.5 sm:mt-0">
                    {step.isDone ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                    ) : (
                      <Circle className="w-5 h-5 text-gray-300 shrink-0" />
                    )}
                  </div>
                  <div>
                    <h3
                      className={`text-sm font-bold ${
                        step.isDone ? 'text-gray-800 line-through/50' : 'text-gray-900'
                      }`}
                    >
                      {step.id}. {step.title}
                    </h3>
                    <p className="text-xs text-gray-500 mt-0.5">{step.description}</p>
                  </div>
                </div>

                <div className="self-end sm:self-center shrink-0">
                  {step.isDone ? (
                    <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-lg flex items-center gap-1 border border-emerald-100">
                      ✓ تم الإنجاز
                    </span>
                  ) : (
                    <Link to={step.actionUrl}>
                      <Button size="sm" variant="primary" className="text-xs font-bold shadow-sm">
                        <span>{step.actionLabel}</span>
                        <ArrowLeft className="w-3.5 h-3.5 mr-1.5" />
                      </Button>
                    </Link>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </Card>
  );
};
