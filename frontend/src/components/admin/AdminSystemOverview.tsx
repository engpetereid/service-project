import React from 'react';
import { AdminSetupResponse } from '../../types/statistics.types';
import { Card } from '../ui/Card';
import { Link } from 'react-router-dom';
import {
  Building2,
  School,
  ShieldCheck,
  Users,
  KeyRound,
  UserX,
  AlertCircle,
  ChevronLeft,
} from 'lucide-react';

interface AdminSystemOverviewProps {
  setupData: AdminSetupResponse;
}

export const AdminSystemOverview: React.FC<AdminSystemOverviewProps> = ({ setupData }) => {
  const stats = [
    {
      title: 'إجمالي الخدمات',
      value: setupData.totalMinistries,
      subtitle: setupData.ministriesWithoutSecretary > 0
        ? `منها ${setupData.ministriesWithoutSecretary} بدون أمين`
        : 'كافة الخدمات لها أمناء',
      isWarning: setupData.ministriesWithoutSecretary > 0,
      icon: Building2,
      path: '/ministries',
      color: 'text-blue-600 bg-blue-50',
    },
    {
      title: 'إجمالي الفصول',
      value: setupData.totalClasses,
      subtitle: setupData.classesWithoutSecretary > 0
        ? `منها ${setupData.classesWithoutSecretary} بدون أمين`
        : 'كافة الفصول لها أمناء',
      isWarning: setupData.classesWithoutSecretary > 0,
      icon: School,
      path: '/ministries',
      color: 'text-indigo-600 bg-indigo-50',
    },
    {
      title: 'إجمالي الخدام',
      value: setupData.totalServants,
      subtitle: `${setupData.servantsWithAccount} لديهم حسابات دخول`,
      isWarning: false,
      icon: ShieldCheck,
      path: '/servants',
      color: 'text-emerald-600 bg-emerald-50',
    },
    {
      title: 'إجمالي المخدومين',
      value: setupData.totalStudents,
      subtitle: setupData.studentsWithoutServant > 0
        ? `يوجد ${setupData.studentsWithoutServant} بدون خادم مسؤول`
        : 'جميع المخدومين موزعون على خدام',
      isWarning: setupData.studentsWithoutServant > 0,
      icon: Users,
      path: '/students',
      color: 'text-purple-600 bg-purple-50',
    },
    {
      title: 'خدام بدون حساب دخول',
      value: setupData.servantsWithoutAccount,
      subtitle: setupData.servantsWithoutAccount > 0
        ? 'يحتاجون تفعيل حسابات للافتقاد'
        : 'جميع الخدام يملكون حسابات',
      isWarning: setupData.servantsWithoutAccount > 0,
      icon: UserX,
      path: '/users',
      color: setupData.servantsWithoutAccount > 0 ? 'text-amber-600 bg-amber-50' : 'text-gray-600 bg-gray-50',
    },
    {
      title: 'فصول بدون أمين فصل',
      value: setupData.classesWithoutSecretary,
      subtitle: setupData.classesWithoutSecretary > 0
        ? 'بحاجة لتعيين أمين فصل'
        : 'مكتمل التعيين',
      isWarning: setupData.classesWithoutSecretary > 0,
      icon: AlertCircle,
      path: '/ministries',
      color: setupData.classesWithoutSecretary > 0 ? 'text-rose-600 bg-rose-50' : 'text-gray-600 bg-gray-50',
    },
    {
      title: 'مخدومين بدون خادم',
      value: setupData.studentsWithoutServant,
      subtitle: setupData.studentsWithoutServant > 0
        ? 'بحاجة لإسناد لخادم متابع'
        : 'مكتمل التوزيع',
      isWarning: setupData.studentsWithoutServant > 0,
      icon: Users,
      path: '/students',
      color: setupData.studentsWithoutServant > 0 ? 'text-amber-600 bg-amber-50' : 'text-gray-600 bg-gray-50',
    },
    {
      title: 'حسابات الدخول المفعلة',
      value: setupData.servantsWithAccount,
      subtitle: 'حسابات نشطة بالخدمة',
      isWarning: false,
      icon: KeyRound,
      path: '/users',
      color: 'text-teal-600 bg-teal-50',
    },
  ];

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-black text-gray-900 uppercase tracking-wider flex items-center gap-2">
          <Building2 className="w-4 h-4 text-primary-600" />
          <span>الهيكل التنظيمي العام للنظام</span>
        </h3>
        <span className="text-xs text-gray-400 font-medium">مؤشرات الجاهزية والتغطية</span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {stats.map((st, idx) => {
          const Icon = st.icon;
          return (
            <Link key={idx} to={st.path} className="group">
              <Card
                className={`p-4 transition hover:shadow-md flex flex-col justify-between h-full ${
                  st.isWarning ? 'border-amber-300 bg-amber-50/20' : 'bg-white'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-gray-600">{st.title}</span>
                  <div
                    className={`w-8 h-8 rounded-xl flex items-center justify-center font-bold ${st.color}`}
                  >
                    <Icon className="w-4 h-4" />
                  </div>
                </div>

                <div>
                  <div className="flex items-baseline gap-2">
                    <span
                      className={`text-2xl font-black ${
                        st.isWarning ? 'text-amber-700' : 'text-gray-900'
                      }`}
                    >
                      {st.value}
                    </span>
                    {st.isWarning && (
                      <span className="text-[10px] font-bold text-red-600 bg-red-50 px-1.5 py-0.5 rounded">
                        تنبيه
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] text-gray-400 mt-1 leading-snug">{st.subtitle}</p>
                </div>

                <div className="pt-2 mt-2 border-t border-gray-50 flex items-center justify-between text-[11px] font-bold text-primary-600 group-hover:text-primary-800">
                  <span>فتح القائمة</span>
                  <ChevronLeft className="w-3.5 h-3.5 transition group-hover:-translate-x-0.5" />
                </div>
              </Card>
            </Link>
          );
        })}
      </div>
    </div>
  );
};
