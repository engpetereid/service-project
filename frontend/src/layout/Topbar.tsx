import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLE_LABELS, getRoleBadgeClass } from '../utils/arabic';
import { LogOut, Menu, User, Shield, Info } from 'lucide-react';
import { Breadcrumbs } from '../components/layout/Breadcrumbs';
import { NotificationDropdown } from '../components/layout/NotificationDropdown';

interface TopbarProps {
  onToggleSidebar: () => void;
}

export const Topbar: React.FC<TopbarProps> = ({ onToggleSidebar }) => {
  const { user, logout } = useAuth();
  const [showUserMenu, setShowUserMenu] = useState(false);

  const roles = user?.roles || [];

  return (
    <header className="h-16 bg-white border-b border-gray-100 px-4 lg:px-8 flex items-center justify-between sticky top-0 z-30 shadow-sm/50">
      {/* Right side: hamburger for mobile + greeting / breadcrumbs */}
      <div className="flex items-center gap-3 min-w-0">
        <button
          onClick={onToggleSidebar}
          className="p-2.5 rounded-xl text-gray-500 hover:text-gray-700 hover:bg-gray-100 lg:hidden min-h-[44px] min-w-[44px] flex items-center justify-center shrink-0"
          aria-label="القائمة الرئيسية"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="min-w-0">
          <div className="hidden sm:block">
            <Breadcrumbs />
          </div>
          <div className="sm:hidden font-bold text-gray-900 text-sm truncate">
            {user?.fullName || 'وزنة'}
          </div>
        </div>
      </div>

      {/* Left side: notifications and user controls */}
      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        {/* Notifications Popover */}
        <NotificationDropdown />

        {/* User Badge & Menu */}
        <div className="relative">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="flex items-center gap-2 p-1.5 rounded-xl hover:bg-gray-50 transition min-h-[44px]"
            aria-label="قائمة الحساب"
          >
            <div className="w-9 h-9 rounded-xl bg-primary-50 text-primary-700 border border-primary-200 flex items-center justify-center font-bold text-sm">
              <User className="w-5 h-5" />
            </div>

            <div className="hidden md:flex flex-col items-start text-right">
              <span className="text-xs font-bold text-gray-900 leading-tight">
                {user?.fullName || 'المستخدم'}
              </span>
              <div className="flex items-center gap-1 mt-0.5">
                {roles.slice(0, 2).map((r, idx) => (
                  <span
                    key={idx}
                    className={`inline-flex px-1.5 py-0.2 rounded text-[10px] font-semibold border ${getRoleBadgeClass(
                      r.role
                    )}`}
                  >
                    {ROLE_LABELS[r.role]}
                  </span>
                ))}
                {roles.length > 2 && (
                  <span className="text-[10px] text-gray-400 font-bold">+{roles.length - 2}</span>
                )}
              </div>
            </div>
          </button>

          {/* User dropdown menu */}
          {showUserMenu && (
            <>
              <div
                className="fixed inset-0 z-40"
                onClick={() => setShowUserMenu(false)}
                aria-hidden="true"
              />
              <div className="absolute left-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 z-50 animate-in fade-in zoom-in-95">
                <div className="px-4 py-3 border-b border-gray-100">
                  <p className="text-sm font-bold text-gray-900 truncate">
                    {user?.fullName}
                  </p>
                  <p className="text-xs text-gray-400 font-mono mt-0.5" dir="ltr">
                    {user?.phone}
                  </p>

                  {/* All Roles */}
                  <div className="mt-2.5 pt-2 border-t border-gray-50 flex flex-wrap gap-1">
                    {roles.map((r, idx) => (
                      <span
                        key={idx}
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-xs font-semibold border ${getRoleBadgeClass(
                          r.role
                        )}`}
                      >
                        <Shield className="w-3 h-3" />
                        {ROLE_LABELS[r.role]}
                      </span>
                    ))}
                  </div>
                </div>

                <Link
                  to="/about"
                  onClick={() => setShowUserMenu(false)}
                  className="w-full px-4 py-2.5 text-right text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2 font-medium transition min-h-[44px]"
                >
                  <Info className="w-4 h-4 text-primary-600" />
                  <span>عن برنامج وزنة (About Us)</span>
                </Link>

                <button
                  onClick={() => {
                    setShowUserMenu(false);
                    logout();
                  }}
                  className="w-full px-4 py-2.5 text-right text-sm text-red-600 hover:bg-red-50 flex items-center gap-2 font-semibold transition min-h-[44px]"
                >
                  <LogOut className="w-4 h-4" />
                  <span>تسجيل الخروج</span>
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  );
};
