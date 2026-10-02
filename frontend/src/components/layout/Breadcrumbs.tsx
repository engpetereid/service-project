import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronLeft, Home } from 'lucide-react';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

const ROUTE_MAP: Record<string, { category?: string; label: string }> = {
  '/': { label: 'الرئيسية' },
  '/ministries': { category: 'الإدارة التنظيمية', label: 'الخدمات والفصول' },
  '/servants': { category: 'الإدارة التنظيمية', label: 'الخدام' },
  '/students': { category: 'الإدارة التنظيمية', label: 'المخدومين' },
  '/users': { category: 'الإدارة التنظيمية', label: 'حسابات المستخدمين' },
  '/visits': { category: 'المتابعة والافتقاد', label: 'الأسبوع الحالي' },
  '/attendance': { category: 'المتابعة والافتقاد', label: 'تسجيل الحضور' },
  '/confessions': { category: 'المتابعة والافتقاد', label: 'سجل الاعترافات' },
  '/archive': { category: 'المتابعة والافتقاد', label: 'الأرشيف' },
  '/statistics': { category: 'التحليلات', label: 'الإحصائيات والتقارير' },
  '/notifications': { category: 'النظام', label: 'الإشعارات' },
  '/settings': { category: 'النظام', label: 'إعدادات النظام' },
  '/audit-logs': { category: 'النظام', label: 'سجل العمليات' },
};

export const Breadcrumbs: React.FC = () => {
  const location = useLocation();
  const currentPath = location.pathname;

  const currentRoute = ROUTE_MAP[currentPath] || { label: 'الصفحة الحالية' };

  if (currentPath === '/') {
    return null;
  }

  const items: BreadcrumbItem[] = [
    { label: 'الرئيسية', path: '/' },
  ];

  if (currentRoute.category) {
    items.push({ label: currentRoute.category });
  }

  items.push({ label: currentRoute.label });

  return (
    <nav aria-label="مسار التنقل" className="flex items-center text-xs text-gray-500 py-1">
      <ol className="flex items-center flex-wrap gap-1.5">
        {items.map((item, index) => {
          const isLast = index === index && index === items.length - 1;
          const isFirst = index === 0;

          return (
            <li key={index} className="flex items-center gap-1.5">
              {index > 0 && (
                <ChevronLeft className="w-3.5 h-3.5 text-gray-400 shrink-0" aria-hidden="true" />
              )}
              {isLast ? (
                <span className="font-bold text-gray-900 truncate max-w-[200px] sm:max-w-none">
                  {item.label}
                </span>
              ) : item.path ? (
                <Link
                  to={item.path}
                  className="hover:text-primary-600 transition flex items-center gap-1 text-gray-500"
                >
                  {isFirst && <Home className="w-3.5 h-3.5 shrink-0" />}
                  <span>{item.label}</span>
                </Link>
              ) : (
                <span className="text-gray-400">{item.label}</span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
};
