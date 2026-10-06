import { apiClient } from './client';
import { NotificationResponse, UnreadCountResponse } from '../types/notification.types';

export const notificationsApi = {
  findAll: async (): Promise<NotificationResponse[]> => {
    const res = await apiClient.get<any>('/notifications', {
      params: { size: 100 },
    });
    if (res.data && Array.isArray(res.data.content)) {
      return res.data.content;
    }
    return Array.isArray(res.data) ? res.data : [];
  },

  getUnreadCount: async (): Promise<UnreadCountResponse> => {
    const res = await apiClient.get<UnreadCountResponse>('/notifications/unread-count');
    return res.data;
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.put(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.put('/notifications/read-all');
  },
};
