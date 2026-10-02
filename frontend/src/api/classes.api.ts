import { apiClient } from './client';
import { GradeClassRequest, GradeClassResponse, AssignSecretaryPayload } from '../types/ministry.types';

export const classesApi = {
  findAll: async (ministryId?: number): Promise<GradeClassResponse[]> => {
    const res = await apiClient.get<GradeClassResponse[]>('/classes', {
      params: { ministryId },
    });
    return res.data;
  },

  findById: async (id: number): Promise<GradeClassResponse> => {
    const res = await apiClient.get<GradeClassResponse>(`/classes/${id}`);
    return res.data;
  },

  create: async (data: GradeClassRequest): Promise<GradeClassResponse> => {
    const res = await apiClient.post<GradeClassResponse>('/classes', data);
    return res.data;
  },

  update: async (id: number, data: GradeClassRequest): Promise<GradeClassResponse> => {
    const res = await apiClient.put<GradeClassResponse>(`/classes/${id}`, data);
    return res.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/classes/${id}`);
  },

  assignSecretary: async (id: number, payload: AssignSecretaryPayload): Promise<void> => {
    await apiClient.put(`/classes/${id}/secretary`, payload);
  },

  removeSecretary: async (id: number): Promise<void> => {
    await apiClient.delete(`/classes/${id}/secretary`);
  },
};
