export type NotificationType = 'WEEKLY_REMINDER' | 'ABSENCE_ALERT' | 'SYSTEM';

export interface NotificationResponse {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  readAt?: string | null;
  referenceId?: string | null;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
