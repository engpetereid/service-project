import { apiClient } from './client';
import { CreateServantRequest, ServantResponse, UpdateServantRequest } from '../types/staff.types';

export const servantsApi = {
  findAll: async (params?: {
    ministryId?: number;
    classId?: number;
    search?: string;
  }): Promise<ServantResponse[]> => {
    const res = await apiClient.get<ServantResponse[]>('/servants', { params });
    return res.data;
  },

  findById: async (id: number): Promise<ServantResponse> => {
    const res = await apiClient.get<ServantResponse>(`/servants/${id}`);
    return res.data;
  },

  create: async (data: CreateServantRequest): Promise<ServantResponse> => {
    const res = await apiClient.post<ServantResponse>('/servants', data);
    return res.data;
  },

  update: async (id: number, data: UpdateServantRequest): Promise<ServantResponse> => {
    const res = await apiClient.put<ServantResponse>(`/servants/${id}`, data);
    return res.data;
  },

  softDelete: async (id: number): Promise<void> => {
    await apiClient.delete(`/servants/${id}`);
  },

  restore: async (id: number): Promise<void> => {
    await apiClient.post(`/servants/${id}/restore`);
  },

  createAccount: async (personId: number, password?: string): Promise<ServantResponse> => {
    const res = await apiClient.post<ServantResponse>(`/servants/${personId}/account`, { password });
    return res.data;
  },

  resetPassword: async (personId: number, password: string): Promise<ServantResponse> => {
    const res = await apiClient.put<ServantResponse>(`/servants/${personId}/reset-password`, { newPassword: password, password });
    return res.data;
  },
};
