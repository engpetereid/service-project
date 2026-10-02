import { apiClient } from './client';
import {
  CreateVisitRequest,
  ServantCurrentWeekResponse,
  UpdateVisitRequest,
  VisitRecordResponse,
  WeekResponse,
} from '../types/visit.types';

export const visitsApi = {
  getCurrentWeek: async (): Promise<ServantCurrentWeekResponse> => {
    const res = await apiClient.get<ServantCurrentWeekResponse>('/visits/current-week');
    return res.data;
  },

  getWeeks: async (): Promise<WeekResponse[]> => {
    const res = await apiClient.get<WeekResponse[]>('/weeks');
    return res.data;
  },

  findByWeek: async (weekId: number): Promise<VisitRecordResponse[]> => {
    const res = await apiClient.get<VisitRecordResponse[]>('/visits', {
      params: { weekId },
    });
    return res.data;
  },

  create: async (data: CreateVisitRequest): Promise<VisitRecordResponse> => {
    const res = await apiClient.post<VisitRecordResponse>('/visits', data);
    return res.data;
  },

  update: async (id: number, data: UpdateVisitRequest): Promise<VisitRecordResponse> => {
    const res = await apiClient.put<VisitRecordResponse>(`/visits/${id}`, data);
    return res.data;
  },
};
