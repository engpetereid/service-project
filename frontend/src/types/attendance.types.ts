export type ActivityType = 'MASS' | 'MEETING' | 'TASBEHA';

export interface AttendanceSessionResponse {
  id: number;
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  activityType: ActivityType;
  sessionDate: string;
  presentCount: number;
}

export interface AttendanceRecordResponse {
  id: number;
  sessionId: number;
  studentId: number;
  studentName: string;
  present: boolean;
  recordedById: number;
  recordedByName: string;
  recordedAt: string;
}

export interface CreateSessionRequest {
  weekId: number;
  activityType: ActivityType;
  sessionDate: string;
}

export interface ToggleAttendanceRequest {
  sessionId: number;
  studentId: number;
  present: boolean;
}

export interface BatchToggleAttendanceRequest {
  sessionId: number;
  studentIds: number[];
  present: boolean;
}

export interface SessionDetailResponse {
  session: AttendanceSessionResponse;
  records: AttendanceRecordResponse[];
}
