export interface ConfessionResponse {
  id: number;
  studentId: number;
  studentName: string;
  academicYearId: number;
  academicYearName: string;
  confessionDate: string;
  confessionFather?: string | null;
  notes?: string | null;
  recordedById: number;
  recordedByName: string;
}

export interface CreateConfessionRequest {
  studentId: number;
  confessionDate: string;
  confessionFather?: string | null;
  notes?: string | null;
}

export interface UpdateConfessionRequest {
  confessionDate: string;
  confessionFather?: string | null;
  notes?: string | null;
}

export interface StudentConfessionSummary {
  studentId: number;
  studentName: string;
  phone?: string;
  gender: 'MALE' | 'FEMALE';
  classId?: number | null;
  className?: string | null;
  servantId?: number | null;
  servantName?: string | null;
  confessionFather?: string | null;
  lastConfessionDate?: string | null;
  daysSinceLastConfession?: number | null;
  totalConfessionsThisYear: number;
  status: 'UP_TO_DATE' | 'OVERDUE' | 'CRITICAL' | 'NEVER';
}

export interface CreateConfessionSessionRequest {
  sessionDate: string;
  confessionFather: string;
  studentIds: number[];
  notes?: string;
}

export interface SessionStudentDto {
  studentId: number;
  studentName: string;
  phone?: string;
  className?: string | null;
  recordId?: number | null;
}

export interface ConfessionSessionDto {
  sessionId: string;
  sessionDate: string;
  confessionFather: string;
  studentCount: number;
  notes?: string | null;
  recordedByName?: string | null;
  students: SessionStudentDto[];
}

