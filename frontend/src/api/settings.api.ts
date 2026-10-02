import { apiClient } from './client';
import {
  AcademicYearResponse,
  PublicSettingsResponse,
  SettingResponse,
  UpdateSettingRequest,
} from '../types/settings.types';

export const settingsApi = {
  findAll: async (): Promise<SettingResponse[]> => {
    const res = await apiClient.get<SettingResponse[]>('/settings');
    return res.data;
  },

  update: async (data: UpdateSettingRequest): Promise<SettingResponse> => {
    const res = await apiClient.put<SettingResponse>('/settings', data);
    return res.data;
  },

  getPublic: async (): Promise<PublicSettingsResponse> => {
    const res = await apiClient.get<PublicSettingsResponse>('/settings/public');
    return res.data;
  },

  getAcademicYears: async (): Promise<AcademicYearResponse[]> => {
    const res = await apiClient.get<AcademicYearResponse[]>('/academic-years');
    return res.data;
  },

  getCurrentAcademicYear: async (): Promise<AcademicYearResponse> => {
    const res = await apiClient.get<AcademicYearResponse>('/academic-years/current');
    return res.data;
  },
};
