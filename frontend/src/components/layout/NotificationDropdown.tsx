import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '../../api/notifications.api';
import { NotificationResponse } from '../../types/notification.types';
import { Bell, Clock, AlertTriangle, Info, CalendarCheck, ExternalLink, X } from 'lucide-react';
import { formatDateTime } from '../../utils/date';
import { Link } from 'react-router-dom';

export const NotificationDropdown: React.FC = () => {
  const queryClient = useQueryClient();
  const [isOpen, setIsOpen] = useState(false);

  const { data: unreadData } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.getUnreadCount,
    refetchInterval: 30000,
  });

  const { data: notifications = [], isLoading } = useQuery<NotificationResponse[]>({
    queryKey: ['notifications', 'all'],
    queryFn: notificationsApi.findAll,
    enabled: isOpen,
  });

  const markAllMutation = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markSingleMutation = useMutation({
    mutationFn: (id: number) => notificationsApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const unreadCount = unreadData?.unreadCount ?? 0;

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'WEEKLY_REMINDER':
        return <CalendarCheck className="w-4 h-4 text-blue-600" />;
      case 'ABSENCE_ALERT':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      default:
        return <Info className="w-4 h-4 text-purple-600" />;
    }
  };

  const getActionLink = (n: NotificationResponse) => {
    if (n.type === 'WEEKLY_REMINDER') return '/visits';
    if (n.type === 'ABSENCE_ALERT') return '/visits';
    return null;
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="p-2.5 rounded-xl text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition min-h-[44px] min-w-[44px] flex items-center justify-center relative"
        aria-label="التنبيهات والإشعارات"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-2 left-2 w-4 h-4 bg-red-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-40"
            onClick={() => setIsOpen(false)}
            aria-hidden="true"
          />

          {/* Popover Panel */}
          <div className="absolute left-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-2xl border border-gray-100 py-3 z-50 animate-in fade-in zoom-in-95">
            {/* Header */}
            <div className="px-4 pb-3 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="font-bold text-gray-900 text-sm">التنبيهات والإشعارات</span>
                {unreadCount > 0 && (
                  <span className="bg-red-50 text-red-700 text-xs font-bold px-2 py-0.5 rounded-full border border-red-100">
                    {unreadCount} جديد
                  </span>
                )}
              </div>
              <div className="flex items-center gap-1">
                {unreadCount > 0 && (
                  <button
                    onClick={() => markAllMutation.mutate()}
                    disabled={markAllMutation.isPending}
                    className="text-xs text-primary-600 hover:text-primary-800 font-bold px-2 py-1 rounded-lg hover:bg-primary-50 transition"
                  >
                    تحديد الكل كمقروء
                  </button>
                )}
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Content List */}
            <div className="max-h-[380px] overflow-y-auto divide-y divide-gray-50">
              {isLoading ? (
                <div className="p-8 text-center text-xs text-gray-400">
                  جاري تحميل التنبيهات...
                </div>
              ) : notifications.length === 0 ? (
                <div className="p-8 text-center">
                  <div className="w-10 h-10 rounded-xl bg-gray-50 text-gray-400 flex items-center justify-center mx-auto mb-2">
                    <Bell className="w-5 h-5" />
                  </div>
                  <p className="text-xs font-bold text-gray-700">لا توجد تنبيهات حالياً</p>
                  <p className="text-[11px] text-gray-400 mt-0.5">ستصلك التنبيهات هنا فور صدورها</p>
                </div>
              ) : (
                notifications.slice(0, 10).map((n) => {
                  const actionPath = getActionLink(n);
                  return (
                    <div
                      key={n.id}
                      className={`p-3.5 transition flex items-start gap-3 hover:bg-gray-50/80 ${
                        !n.read ? 'bg-primary-50/20' : ''
                      }`}
                      onClick={() => {
                        if (!n.read) markSingleMutation.mutate(n.id);
                      }}
                    >
                      <div className="mt-0.5 w-8 h-8 rounded-xl bg-gray-100 flex items-center justify-center shrink-0">
                        {getNotificationIcon(n.type)}
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-1">
                          <h4 className={`text-xs ${!n.read ? 'font-bold text-gray-900' : 'font-semibold text-gray-700'} truncate`}>
                            {n.title}
                          </h4>
                          {!n.read && (
                            <span className="w-2 h-2 rounded-full bg-primary-600 shrink-0" />
                          )}
                        </div>

                        <p className="text-[11px] text-gray-500 mt-0.5 line-clamp-2 leading-relaxed">
                          {n.message}
                        </p>

                        <div className="flex items-center justify-between mt-2 pt-1 border-t border-gray-50/50">
                          <span className="text-[10px] text-gray-400 flex items-center gap-1 font-mono">
                            <Clock className="w-3.5 h-3.5" />
                            {formatDateTime(n.createdAt)}
                          </span>

                          {actionPath && (
                            <Link
                              to={actionPath}
                              onClick={() => setIsOpen(false)}
                              className="text-[11px] font-bold text-primary-600 hover:text-primary-800 flex items-center gap-1"
                            >
                              <span>معالجة الآن</span>
                              <ExternalLink className="w-3 h-3" />
                            </Link>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Footer */}
            {notifications.length > 0 && (
              <div className="px-4 pt-2.5 border-t border-gray-100 text-center">
                <Link
                  to="/notifications"
                  onClick={() => setIsOpen(false)}
                  className="text-xs font-bold text-primary-600 hover:text-primary-800 block py-1"
                >
                  عرض جميع التنبيهات
                </Link>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
