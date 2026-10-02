import React from 'react';
import { NavLink } from 'react-router-dom';
import { usePermissions } from '../auth/usePermissions';
import {
  CalendarCheck,
  UserCheck,
  Users,
  LayoutDashboard,
  Menu,
} from 'lucide-react';
import { clsx } from 'clsx';

interface MobileNavProps {
  onOpenSidebar: () => void;
}

export const MobileNav: React.FC<MobileNavProps> = ({ onOpenSidebar }) => {
  const { isServant, isAdmin, isServiceSecretary, isClassSecretary } = usePermissions();
  const isSecretaryOrAdmin = isAdmin || isServiceSecretary || isClassSecretary;

  const items = [
    {
      name: 'الرئيسية',
      path: '/',
      icon: LayoutDashboard,
    },
    {
      name: 'الافتقاد',
      path: '/visits',
      icon: CalendarCheck,
    },
    {
      name: 'الحضور',
      path: '/attendance',
      icon: UserCheck,
    },
    {
      name: isServant && !isSecretaryOrAdmin ? 'مخدومي' : 'المخدومين',
      path: '/students',
      icon: Users,
    },
  ];

  return (
    <nav className="lg:hidden fixed bottom-0 inset-x-0 bg-white border-t border-gray-200/80 z-30 px-2 py-1 shadow-xl">
      <div className="flex items-center justify-around">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                clsx(
                  'flex flex-col items-center justify-center py-1 px-1.5 rounded-xl transition min-h-[48px] min-w-[52px]',
                  isActive
                    ? 'text-primary-600 font-bold bg-primary-50/50'
                    : 'text-gray-500 hover:text-gray-800'
                )
              }
            >
              <Icon className="w-5 h-5 mb-0.5" />
              <span className="text-[10.5px] leading-tight">{item.name}</span>
            </NavLink>
          );
        })}

        <button
          onClick={onOpenSidebar}
          className="flex flex-col items-center justify-center py-1 px-1.5 rounded-xl text-gray-500 hover:text-gray-800 transition min-h-[48px] min-w-[52px]"
          aria-label="المزيد من الخيارات"
        >
          <Menu className="w-5 h-5 mb-0.5" />
          <span className="text-[10.5px] leading-tight font-medium">المزيد</span>
        </button>
      </div>
    </nav>
  );
};
