import { apiClient } from './client';
import {
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  BatchToggleAttendanceRequest,
  CreateSessionRequest,
  SessionDetailResponse,
  ToggleAttendanceRequest,
} from '../types/attendance.types';

export const attendanceApi = {
  getSessionsByWeek: async (weekId: number): Promise<AttendanceSessionResponse[]> => {
    const res = await apiClient.get<AttendanceSessionResponse[]>('/attendance/sessions', {
      params: { weekId },
    });
    return res.data;
  },

  createSession: async (data: CreateSessionRequest): Promise<AttendanceSessionResponse> => {
    const res = await apiClient.post<AttendanceSessionResponse>('/attendance/sessions', data);
    return res.data;
  },

  getSessionDetails: async (id: number): Promise<SessionDetailResponse> => {
    const res = await apiClient.get<SessionDetailResponse>(`/attendance/sessions/${id}`);
    return res.data;
  },

  toggleAttendance: async (data: ToggleAttendanceRequest): Promise<AttendanceRecordResponse> => {
    const res = await apiClient.post<AttendanceRecordResponse>('/attendance/records', data);
    return res.data;
  },

  batchToggleAttendance: async (
    data: BatchToggleAttendanceRequest
  ): Promise<AttendanceRecordResponse[]> => {
    const res = await apiClient.post<AttendanceRecordResponse[]>('/attendance/records/batch', data);
    return res.data;
  },
};
