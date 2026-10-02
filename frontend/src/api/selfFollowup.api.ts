import { apiClient } from './client';
import {
  SelfFollowUpCurrentWeekResponse,
  SelfFollowUpRequest,
  SelfFollowUpResponse,
  SelfFollowUpStatsResponse,
} from '../types/selfFollowup.types';

export const selfFollowupApi = {
  getCurrentWeek: async (): Promise<SelfFollowUpCurrentWeekResponse> => {
    const res = await apiClient.get<SelfFollowUpCurrentWeekResponse>('/self-followup/current-week');
    return res.data;
  },

  upsert: async (data: SelfFollowUpRequest): Promise<SelfFollowUpResponse> => {
    const res = await apiClient.put<SelfFollowUpResponse>('/self-followup', data);
    return res.data;
  },

  getByWeek: async (weekId: number): Promise<SelfFollowUpResponse> => {
    const res = await apiClient.get<SelfFollowUpResponse>(`/self-followup/weeks/${weekId}`);
    return res.data;
  },

  getHistory: async (): Promise<SelfFollowUpResponse[]> => {
    const res = await apiClient.get<SelfFollowUpResponse[]>('/self-followup/history');
    return res.data;
  },

  getStatistics: async (): Promise<SelfFollowUpStatsResponse> => {
    const res = await apiClient.get<SelfFollowUpStatsResponse>('/self-followup/statistics');
    return res.data;
  },
};
