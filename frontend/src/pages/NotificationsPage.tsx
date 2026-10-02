import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '../api/notifications.api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { formatDateTime } from '../utils/date';
import { Bell, CheckCheck, Clock, AlertTriangle, CalendarCheck, Info, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

export const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications', 'all'],
    queryFn: notificationsApi.findAll,
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

  const filteredNotifications = notifications.filter((n) => {
    if (filter === 'UNREAD') return !n.read;
    return true;
  });

  const unreadCount = notifications.filter((n) => !n.read).length;

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'WEEKLY_REMINDER':
        return <CalendarCheck className="w-5 h-5 text-blue-600" />;
      case 'ABSENCE_ALERT':
        return <AlertTriangle className="w-5 h-5 text-amber-600" />;
      default:
        return <Info className="w-5 h-5 text-purple-600" />;
    }
  };

  const getActionLink = (type: string) => {
    if (type === 'WEEKLY_REMINDER') return { text: 'فتح الأسبوع الحالي', path: '/visits' };
    if (type === 'ABSENCE_ALERT') return { text: 'متابعة الغياب والافتقاد', path: '/visits' };
    return null;
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">التنبيهات والإشعارات</h1>
          <p className="text-xs text-gray-500 mt-1">
            سجل التنبيهات الصادرة وتذكيرات الافتقاد الأسبوعي والغياب المتتالي
          </p>
        </div>

        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllMutation.mutate()}
            isLoading={markAllMutation.isPending}
            className="font-bold"
          >
            <CheckCheck className="w-4 h-4 ml-1.5 text-primary-600" />
            تحديد الكل كمقروء
          </Button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2">
        <button
          onClick={() => setFilter('ALL')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition min-h-[40px] flex items-center gap-1.5 ${
            filter === 'ALL'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-100'
          }`}
        >
          <span>الكل</span>
          <span className={`px-1.5 py-0.2 rounded-md text-[10px] ${
            filter === 'ALL' ? 'bg-primary-700 text-white' : 'bg-gray-100 text-gray-700'
          }`}>
            {notifications.length}
          </span>
        </button>

        <button
          onClick={() => setFilter('UNREAD')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition min-h-[40px] flex items-center gap-1.5 ${
            filter === 'UNREAD'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-100'
          }`}
        >
          <span>غير مقروء</span>
          <span className={`px-1.5 py-0.2 rounded-md text-[10px] ${
            filter === 'UNREAD' ? 'bg-primary-700 text-white' : 'bg-red-50 text-red-700'
          }`}>
            {unreadCount}
          </span>
        </button>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل الإشعارات...</p>
        </div>
      ) : filteredNotifications.length === 0 ? (
        <Card className="text-center py-16">
          <div className="w-14 h-14 rounded-2xl bg-gray-50 text-gray-400 flex items-center justify-center mx-auto mb-3">
            <Bell className="w-7 h-7" />
          </div>
          <h3 className="text-base font-bold text-gray-900 mb-1">لا توجد إشعارات</h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto">
            {filter === 'UNREAD'
              ? 'رائع! لا توجد لديك أي إشعارات غير مقروءة حالياً.'
              : 'سجل الإشعارات فارغ حالياً.'}
          </p>
        </Card>
      ) : (
        <div className="space-y-3">
          {filteredNotifications.map((n) => {
            const action = getActionLink(n.type);
            return (
              <Card
                key={n.id}
                className={`p-5 transition hover:shadow-sm ${
                  !n.read ? 'border-primary-200 bg-primary-50/10' : 'bg-white'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-2xl bg-gray-100 flex items-center justify-center shrink-0">
                    {getNotificationIcon(n.type)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <h3 className="font-bold text-gray-900 text-sm">{n.title}</h3>
                        {!n.read && (
                          <Badge variant="primary" className="text-[10px]">
                            جديد
                          </Badge>
                        )}
                      </div>
                      <span className="text-[11px] text-gray-400 flex items-center gap-1 font-mono shrink-0">
                        <Clock className="w-3.5 h-3.5" />
                        {formatDateTime(n.createdAt)}
                      </span>
                    </div>

                    <p className="text-xs text-gray-600 mt-1.5 leading-relaxed">
                      {n.message}
                    </p>

                    <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-50">
                      <div>
                        {action && (
                          <Link to={action.path}>
                            <Button size="sm" variant="primary" className="text-xs font-bold">
                              {action.text}
                              <ExternalLink className="w-3.5 h-3.5 mr-1" />
                            </Button>
                          </Link>
                        )}
                      </div>

                      {!n.read && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => markSingleMutation.mutate(n.id)}
                          className="text-xs text-gray-400 hover:text-gray-600"
                        >
                          تحديد كمقروء
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};
