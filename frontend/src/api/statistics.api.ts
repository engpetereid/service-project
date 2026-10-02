import { apiClient } from './client';
import {
  AbsenceAlertResponse,
  AdminSetupResponse,
  DashboardStatisticsResponse,
  WeeklyTrendDataPoint,
  MinistryStatisticsResponse,
  ClassStatisticsResponse,
  ServantStatisticsResponse,
  StudentStatisticsResponse,
} from '../types/statistics.types';

export const statisticsApi = {
  getAdminSetup: async (): Promise<AdminSetupResponse> => {
    const res = await apiClient.get<AdminSetupResponse>('/statistics/admin-setup');
    return res.data;
  },

  getDashboard: async (weekId?: number): Promise<DashboardStatisticsResponse> => {
    const params = weekId ? { weekId } : undefined;
    const res = await apiClient.get<DashboardStatisticsResponse>('/statistics/dashboard', { params });
    return res.data;
  },

  getTrends: async (count: number = 8): Promise<WeeklyTrendDataPoint[]> => {
    const res = await apiClient.get<WeeklyTrendDataPoint[]>('/statistics/trends', {
      params: { count },
    });
    return res.data;
  },

  getMinistryStatistics: async (id: number, weekId?: number): Promise<MinistryStatisticsResponse> => {
    const params = weekId ? { weekId } : undefined;
    const res = await apiClient.get<MinistryStatisticsResponse>(`/statistics/ministry/${id}`, { params });
    return res.data;
  },

  getClassStatistics: async (id: number, weekId?: number): Promise<ClassStatisticsResponse> => {
    const params = weekId ? { weekId } : undefined;
    const res = await apiClient.get<ClassStatisticsResponse>(`/statistics/class/${id}`, { params });
    return res.data;
  },

  getServantStatistics: async (id: number, weekId?: number): Promise<ServantStatisticsResponse> => {
    const params = weekId ? { weekId } : undefined;
    const res = await apiClient.get<ServantStatisticsResponse>(`/statistics/servant/${id}`, { params });
    return res.data;
  },

  getStudentStatistics: async (id: number): Promise<StudentStatisticsResponse> => {
    const res = await apiClient.get<StudentStatisticsResponse>(`/statistics/student/${id}`);
    return res.data;
  },

  getAbsenceAlerts: async (threshold: number = 2): Promise<AbsenceAlertResponse[]> => {
    const res = await apiClient.get<AbsenceAlertResponse[]>('/statistics/absence-alerts', {
      params: { threshold },
    });
    return res.data;
  },
};
