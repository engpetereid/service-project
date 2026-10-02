import { apiClient } from './client';
import { ChangePasswordRequest, LoginRequest, LoginResponse, UserInfoResponse } from '../types/auth.types';

export const authApi = {
  login: async (request: LoginRequest): Promise<LoginResponse> => {
    const res = await apiClient.post<LoginResponse>('/auth/login', request);
    return res.data;
  },

  getCurrentUser: async (): Promise<UserInfoResponse> => {
    const res = await apiClient.get<UserInfoResponse>('/auth/me');
    return res.data;
  },

  changePassword: async (request: ChangePasswordRequest): Promise<void> => {
    await apiClient.put('/auth/change-password', request);
  },
};
