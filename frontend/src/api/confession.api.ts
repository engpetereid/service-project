import { apiClient } from './client';
import {
  ConfessionResponse,
  ConfessionSessionDto,
  CreateConfessionRequest,
  CreateConfessionSessionRequest,
  StudentConfessionSummary,
  UpdateConfessionRequest,
} from '../types/confession.types';

export const confessionApi = {
  findByStudent: async (studentId: number): Promise<ConfessionResponse[]> => {
    const res = await apiClient.get<ConfessionResponse[]>('/confessions', {
      params: { studentId },
    });
    return res.data;
  },

  getOverview: async (): Promise<StudentConfessionSummary[]> => {
    const res = await apiClient.get<StudentConfessionSummary[]>('/confessions/overview');
    return res.data;
  },

  getSessions: async (): Promise<ConfessionSessionDto[]> => {
    const res = await apiClient.get<ConfessionSessionDto[]>('/confessions/sessions');
    return res.data;
  },

  createSession: async (data: CreateConfessionSessionRequest): Promise<ConfessionSessionDto> => {
    const res = await apiClient.post<ConfessionSessionDto>('/confessions/session', data);
    return res.data;
  },

  create: async (data: CreateConfessionRequest): Promise<ConfessionResponse> => {
    const res = await apiClient.post<ConfessionResponse>('/confessions', data);
    return res.data;
  },

  update: async (id: number, data: UpdateConfessionRequest): Promise<ConfessionResponse> => {
    const res = await apiClient.put<ConfessionResponse>(`/confessions/${id}`, data);
    return res.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/confessions/${id}`);
  },
};
