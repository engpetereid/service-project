import { apiClient } from './client';
import {
  MinistryRequest,
  MinistryResponse,
  GradeClassResponse,
  AssignSecretaryPayload,
} from '../types/ministry.types';

export const ministriesApi = {
  findAll: async (includeInactive = false): Promise<MinistryResponse[]> => {
    const res = await apiClient.get<MinistryResponse[]>('/ministries', {
      params: { includeInactive },
    });
    return res.data;
  },

  findById: async (id: number): Promise<MinistryResponse> => {
    const res = await apiClient.get<MinistryResponse>(`/ministries/${id}`);
    return res.data;
  },

  getClasses: async (id: number): Promise<GradeClassResponse[]> => {
    const res = await apiClient.get<GradeClassResponse[]>(`/classes`, {
      params: { ministryId: id },
    });
    return res.data;
  },

  create: async (data: MinistryRequest): Promise<MinistryResponse> => {
    const res = await apiClient.post<MinistryResponse>('/ministries', data);
    return res.data;
  },

  update: async (id: number, data: MinistryRequest): Promise<MinistryResponse> => {
    const res = await apiClient.put<MinistryResponse>(`/ministries/${id}`, data);
    return res.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/ministries/${id}`);
  },

  assignSecretary: async (id: number, payload: AssignSecretaryPayload): Promise<void> => {
    await apiClient.put(`/ministries/${id}/secretary`, payload);
  },

  removeSecretary: async (id: number): Promise<void> => {
    await apiClient.delete(`/ministries/${id}/secretary`);
  },
};
