import { apiClient } from './client';
import { WeekResponse } from '../types/visit.types';

export interface ArchiveSummaryResponse {
  deletedPeopleCount: number | null;
  lockedWeeksCount: number;
  totalVisitsCount: number;
  totalAttendanceSessionsCount: number;
  totalConfessionsCount: number;
  auditLogsCount: number | null;
}

export interface DeletedPersonItem {
  id: number;
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  personType: string;
  deletedAt: string;
  ministryName?: string | null;
  className?: string | null;
}

export const archiveApi = {
  getSummary: async (): Promise<ArchiveSummaryResponse> => {
    const res = await apiClient.get<ArchiveSummaryResponse>('/archive/summary');
    return res.data;
  },

  getDeletedPeople: async (): Promise<DeletedPersonItem[]> => {
    const res = await apiClient.get<DeletedPersonItem[]>('/archive/deleted-people');
    return res.data;
  },

  getLockedWeeks: async (): Promise<WeekResponse[]> => {
    const res = await apiClient.get<WeekResponse[]>('/archive/weeks');
    return res.data;
  },

  restoreStudent: async (id: number): Promise<void> => {
    await apiClient.post(`/students/${id}/restore`);
  },

  restoreServant: async (id: number): Promise<void> => {
    await apiClient.post(`/servants/${id}/restore`);
  },

  restoreWeek: async (id: number): Promise<void> => {
    await apiClient.post(`/weeks/${id}/restore`);
  },
};
