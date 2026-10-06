import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { usePermissions } from '../auth/usePermissions';
import { useAuth } from '../auth/useAuth';
import { ROLE_LABELS, getRoleBadgeClass } from '../utils/arabic';
import {
  LayoutDashboard,
  CalendarCheck,
  UserCheck,
  HeartHandshake,
  Users,
  ShieldCheck,
  Building2,
  BarChart3,
  FileClock,
  Archive,
  Settings,
  Bell,
  X,
  Shield,
  Info,
  ClipboardCheck,
} from 'lucide-react';
import { clsx } from 'clsx';
import waznaLogo from '../assets/waznaLogo.png';
import { PWAInstallSidebarButton } from '../components/pwa/PWAInstallPrompt';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

interface NavItem {
  name: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  show: boolean;
  badge?: string | number;
}

interface NavGroup {
  title?: string;
  items: NavItem[];
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const { isAdmin, isServiceSecretary, isClassSecretary, isServant } = usePermissions();

  const isSecretaryOrAdmin = isAdmin || isServiceSecretary || isClassSecretary;
  const roles = user?.roles || [];

  // Grouped Navigation structure tailored to permissions
  const navGroups: NavGroup[] = [
    {
      items: [
        {
          name: isServant && !isSecretaryOrAdmin ? 'الأسبوع الحالي (الرئيسية)' : 'الرئيسية',
          path: '/',
          icon: LayoutDashboard,
          show: true,
        },
      ],
    },
    {
      title: 'الإدارة التنظيمية',
      items: [
        {
          name: isServiceSecretary && !isAdmin ? 'خدمتي وفصولها' : 'الخدمات والفصول',
          path: '/ministries',
          icon: Building2,
          show: isAdmin || isServiceSecretary,
        },
        {
          name: isSecretaryOrAdmin ? 'إدارة الخدام' : 'الخدام',
          path: '/servants',
          icon: ShieldCheck,
          show: isSecretaryOrAdmin,
        },
        {
          name: isServant && !isSecretaryOrAdmin ? 'مخدومي' : 'المخدومين',
          path: '/students',
          icon: isServant && !isSecretaryOrAdmin ? UserCheck : Users,
          show: true,
        },
        {
          name: 'جميع المخدومين',
          path: '/all-students',
          icon: Users,
          show: isServant && !isSecretaryOrAdmin,
        },
        {
          name: 'حسابات المستخدمين والصلاحيات',
          path: '/users',
          icon: Shield,
          show: isAdmin,
        },
      ],
    },
    {
      title: 'المتابعة والافتقاد',
      items: [
        {
          name: 'متابعة المخدومين',
          path: '/visits',
          icon: CalendarCheck,
          show: isSecretaryOrAdmin || isServant,
        },
        {
          name: 'متابعتي الأسبوعية',
          path: '/self-followup',
          icon: ClipboardCheck,
          show: isServant || isClassSecretary || isAdmin,
        },
        {
          name: 'تسجيل الحضور',
          path: '/attendance',
          icon: UserCheck,
          show: true,
        },
        {
          name: 'سجل الاعترافات',
          path: '/confessions',
          icon: HeartHandshake,
          show: true,
        },
        {
          name: 'الأرشيف والاستعادة',
          path: '/archive',
          icon: Archive,
          show: isSecretaryOrAdmin || isServant,
        },
      ],
    },
    {
      title: 'التحليلات والمتابعة',
      items: [
        {
          name: isServant && !isSecretaryOrAdmin ? 'إحصائيات افتقادي' : 'الإحصائيات والتقارير',
          path: '/statistics',
          icon: BarChart3,
          show: isSecretaryOrAdmin || isServant,
        },
      ],
    },
    {
      title: 'النظام',
      items: [
        {
          name: 'التنبيهات والإشعارات',
          path: '/notifications',
          icon: Bell,
          show: true,
        },
        {
          name: 'إعدادات النظام',
          path: '/settings',
          icon: Settings,
          show: isAdmin,
        },
        {
          name: 'سجل العمليات (Audit Log)',
          path: '/audit-logs',
          icon: FileClock,
          show: isAdmin,
        },
        {
          name: 'عن برنامج وزنة',
          path: '/about',
          icon: Info,
          show: true,
        },
      ],
    },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar Shell */}
      <aside
        className={clsx(
          'fixed inset-y-0 right-0 z-40 w-72 bg-white border-l border-gray-100 flex flex-col transition-transform duration-200 ease-in-out lg:translate-x-0 lg:static lg:z-auto shadow-sm',
          isOpen ? 'translate-x-0' : 'translate-x-full'
        )}
      >
        {/* Brand Header */}
        <div className="h-16 px-6 border-b border-gray-100 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <img
              src={waznaLogo}
              alt="شعار وزنة"
              className="w-10 h-10 rounded-2xl object-cover shadow-md shadow-primary-500/15 border border-gray-100 shrink-0"
            />
            <div>
              <h2 className="text-base font-black text-gray-900 leading-tight">وزنة</h2>
              <p className="text-[11px] text-gray-400 font-medium">نظام متابعة ورعاية الكنيسة</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 lg:hidden min-h-[40px] min-w-[40px] flex items-center justify-center"
            aria-label="إغلاق القائمة"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User Scope Info Banner */}
        <div className="p-4 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-800 truncate">{user?.fullName}</span>
            <div className="flex gap-1 shrink-0">
              {roles.slice(0, 1).map((r, i) => (
                <span
                  key={i}
                  className={`px-2 py-0.5 rounded-lg text-[10px] font-bold border ${getRoleBadgeClass(
                    r.role
                  )}`}
                >
                  {ROLE_LABELS[r.role]}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Navigation Groups List */}
        <nav className="flex-1 overflow-y-auto px-4 py-4 space-y-6">
          {navGroups.map((group, gIdx) => {
            const visibleItems = group.items.filter((item) => item.show);
            if (visibleItems.length === 0) return null;

            return (
              <div key={gIdx} className="space-y-1">
                {group.title && (
                  <h3 className="px-3 text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-2">
                    {group.title}
                  </h3>
                )}

                {visibleItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      end={item.path === '/'}
                      onClick={() => onClose()}
                      className={({ isActive }) =>
                        clsx(
                          'flex items-center justify-between px-3.5 py-2.5 rounded-2xl text-sm font-semibold transition duration-150 min-h-[44px]',
                          isActive
                            ? 'bg-primary-50 text-primary-700 font-bold shadow-sm shadow-primary-500/5'
                            : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                        )
                      }
                    >
                      <div className="flex items-center gap-3">
                        <Icon className="w-5 h-5 shrink-0" />
                        <span>{item.name}</span>
                      </div>
                      {item.badge && (
                        <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-primary-100 text-primary-700">
                          {item.badge}
                        </span>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            );
          })}
        </nav>

        {/* PWA Install Button (Mobile & Desktop) */}
        <div className="px-3 pb-1">
          <PWAInstallSidebarButton />
        </div>

        {/* Footer info */}
        <Link
          to="/about"
          onClick={() => onClose()}
          className="p-3 border-t border-gray-100 bg-gray-50/40 text-[11px] text-gray-500 hover:text-primary-700 hover:bg-primary-50/50 transition flex items-center justify-center gap-1.5 font-medium"
        >
          <Info className="w-3.5 h-3.5 text-primary-500" />
          <span>برنامج وزنة • الإصدار 1.0 (2026/10)</span>
        </Link>
      </aside>
    </>
  );
};
