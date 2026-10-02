import { apiClient } from './client';
import {
  BatchAssignServantRequest,
  BatchMoveClassRequest,
  ChangeAssignmentRequest,
  CreateStudentRequest,
  StudentResponse,
  UpdateStudentRequest,
} from '../types/student.types';

export const studentsApi = {
  findAll: async (params?: {
    ministryId?: number;
    classId?: number;
    servantId?: number;
    scope?: string;
    search?: string;
  }): Promise<StudentResponse[]> => {
    const res = await apiClient.get<StudentResponse[]>('/students', { params });
    return res.data;
  },

  findById: async (id: number): Promise<StudentResponse> => {
    const res = await apiClient.get<StudentResponse>(`/students/${id}`);
    return res.data;
  },

  create: async (data: CreateStudentRequest): Promise<StudentResponse> => {
    const res = await apiClient.post<StudentResponse>('/students', data);
    return res.data;
  },

  update: async (id: number, data: UpdateStudentRequest): Promise<StudentResponse> => {
    const res = await apiClient.put<StudentResponse>(`/students/${id}`, data);
    return res.data;
  },

  changeAssignment: async (id: number, data: ChangeAssignmentRequest): Promise<StudentResponse> => {
    const res = await apiClient.put<StudentResponse>(`/students/${id}/assignment`, data);
    return res.data;
  },

  batchAssignServant: async (data: BatchAssignServantRequest): Promise<StudentResponse[]> => {
    const res = await apiClient.put<StudentResponse[]>('/students/batch/servant', data);
    return res.data;
  },

  batchMoveClass: async (data: BatchMoveClassRequest): Promise<StudentResponse[]> => {
    const res = await apiClient.put<StudentResponse[]>('/students/batch/class', data);
    return res.data;
  },

  softDelete: async (id: number): Promise<void> => {
    await apiClient.delete(`/students/${id}`);
  },

  restore: async (id: number): Promise<void> => {
    await apiClient.post(`/students/${id}/restore`);
  },
};
