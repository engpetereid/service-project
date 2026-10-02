import { apiClient } from './client';
import { WeekResponse } from '../types/visit.types';

export const weeksApi = {
  getCurrentWeek: async (): Promise<WeekResponse> => {
    const res = await apiClient.get<WeekResponse>('/weeks/current');
    return res.data;
  },

  getAll: async (includeDeleted = false): Promise<WeekResponse[]> => {
    const res = await apiClient.get<WeekResponse[]>('/weeks', {
      params: { includeDeleted },
    });
    return res.data;
  },

  getById: async (id: number): Promise<WeekResponse> => {
    const res = await apiClient.get<WeekResponse>(`/weeks/${id}`);
    return res.data;
  },
};
