import { apiClient } from './client';

export interface AuditLogItem {
  id: number;
  actorUserId: number;
  actorName: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE' | 'RESTORE' | 'LOGIN' | 'LOGOUT' | 'PASSWORD_RESET' | 'WEEK_LOCK_OVERRIDE' | 'PROMOTION_RUN';
  entityType: string;
  entityId: number;
  oldValues?: string | null;
  newValues?: string | null;
  clientIp?: string | null;
  timestamp: string;
}

export interface AuditLogFilterParams {
  entityType?: string;
  entityId?: number;
  actorUserId?: number;
  action?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const auditApi = {
  getLogs: async (params?: AuditLogFilterParams): Promise<PageResponse<AuditLogItem>> => {
    const res = await apiClient.get<PageResponse<AuditLogItem>>('/audit-logs', { params });
    return res.data;
  },
};
