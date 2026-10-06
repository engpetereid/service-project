import { apiClient } from './client';
import {
  AssignRolesRequest,
  CreateUserRequest,
  CreateUserForPersonRequest,
  UpdateUserRequest,
  UserResponse,
  ResetPasswordRequest,
} from '../types/user.types';

export const usersApi = {
  findAll: async (): Promise<UserResponse[]> => {
    const res = await apiClient.get<UserResponse[]>('/users');
    return res.data;
  },

  create: async (data: CreateUserRequest): Promise<UserResponse> => {
    const res = await apiClient.post<UserResponse>('/users', data);
    return res.data;
  },

  createForPerson: async (
    personId: number,
    data?: CreateUserForPersonRequest
  ): Promise<UserResponse> => {
    const res = await apiClient.post<UserResponse>(`/users/from-person/${personId}`, data || {});
    return res.data;
  },

  update: async (id: number, data: UpdateUserRequest): Promise<UserResponse> => {
    const res = await apiClient.put<UserResponse>(`/users/${id}`, data);
    return res.data;
  },

  assignRoles: async (id: number, data: AssignRolesRequest): Promise<UserResponse> => {
    const res = await apiClient.put<UserResponse>(`/users/${id}/roles`, data);
    return res.data;
  },

  resetPassword: async (id: number, data?: ResetPasswordRequest): Promise<void> => {
    await apiClient.put(`/users/${id}/reset-password`, data || {});
  },

  toggleActive: async (id: number): Promise<void> => {
    await apiClient.put(`/users/${id}/toggle-active`);
  },

  restore: async (id: number): Promise<void> => {
    await apiClient.post(`/users/${id}/restore`);
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};
