import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../ui/Card';
import {
  Building2,
  School,
  UserPlus,
  Users,
  Shield,
  UserCheck,
  PlusCircle,
} from 'lucide-react';

export const AdminQuickActions: React.FC = () => {
  const actions = [
    {
      label: 'إضافة خدمة',
      description: 'إنشاء مرحلة أو خدمة جديدة',
      icon: Building2,
      path: '/ministries',
      color: 'bg-blue-50 text-blue-700 hover:border-blue-300 hover:bg-blue-100/50',
      iconBg: 'bg-blue-600 text-white',
    },
    {
      label: 'إضافة فصل',
      description: 'إنشاء فصل دراسي تابع لخدمة',
      icon: School,
      path: '/ministries',
      color: 'bg-indigo-50 text-indigo-700 hover:border-indigo-300 hover:bg-indigo-100/50',
      iconBg: 'bg-indigo-600 text-white',
    },
    {
      label: 'إضافة خادم',
      description: 'تسجيل خادم وتسكينه وتفعيل حسابه',
      icon: UserPlus,
      path: '/servants',
      color: 'bg-emerald-50 text-emerald-700 hover:border-emerald-300 hover:bg-emerald-100/50',
      iconBg: 'bg-emerald-600 text-white',
    },
    {
      label: 'إضافة مخدوم',
      description: 'تسجيل مخدوم وتسكينه بالفصل',
      icon: Users,
      path: '/students',
      color: 'bg-purple-50 text-purple-700 hover:border-purple-300 hover:bg-purple-100/50',
      iconBg: 'bg-purple-600 text-white',
    },
    {
      label: 'إضافة مستخدم',
      description: 'إنشاء حساب دخول جديد',
      icon: Shield,
      path: '/users',
      color: 'bg-amber-50 text-amber-700 hover:border-amber-300 hover:bg-amber-100/50',
      iconBg: 'bg-amber-600 text-white',
    },
    {
      label: 'تعيين أمين خدمة',
      description: 'إسناد مسؤولية خدمة لأمين',
      icon: Shield,
      path: '/ministries',
      color: 'bg-teal-50 text-teal-700 hover:border-teal-300 hover:bg-teal-100/50',
      iconBg: 'bg-teal-600 text-white',
    },
    {
      label: 'تعيين أمين فصل',
      description: 'إسناد مسؤولية فصل لأمين',
      icon: UserCheck,
      path: '/ministries',
      color: 'bg-rose-50 text-rose-700 hover:border-rose-300 hover:bg-rose-100/50',
      iconBg: 'bg-rose-600 text-white',
    },
  ];

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-black text-gray-900 uppercase tracking-wider flex items-center gap-2">
          <PlusCircle className="w-4 h-4 text-primary-600" />
          <span>الإجراءات السريعة</span>
        </h3>
        <span className="text-xs text-gray-400 font-medium">الوصول المباشر للعمليات الإدارية</span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-3">
        {actions.map((act, index) => {
          const Icon = act.icon;
          return (
            <Link key={index} to={act.path} className="group">
              <Card
                className={`p-3.5 border transition-all duration-150 flex flex-col justify-between h-full group-hover:shadow-md group-hover:-translate-y-0.5 ${act.color}`}
              >
                <div className="flex items-center justify-between mb-2">
                  <div
                    className={`w-8 h-8 rounded-xl flex items-center justify-center shadow-sm ${act.iconBg}`}
                  >
                    <Icon className="w-4 h-4" />
                  </div>
                  <span className="text-xs font-bold text-gray-400 group-hover:text-gray-700 transition">
                    +
                  </span>
                </div>
                <div>
                  <h4 className="font-bold text-gray-900 text-xs sm:text-sm leading-tight">
                    {act.label}
                  </h4>
                  <p className="text-[10px] text-gray-500 mt-1 line-clamp-1">
                    {act.description}
                  </p>
                </div>
              </Card>
            </Link>
          );
        })}
      </div>
    </div>
  );
};
