export type VisitMethod = 'VISIT' | 'CALL';

export interface WeekResponse {
  id: number;
  startDate: string;
  endDate: string;
  locked: boolean;
  active: boolean;
}

export interface VisitRecordResponse {
  id: number;
  studentId: number;
  studentName: string;
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  academicYearId: number;
  academicYearName: string;
  ministryId: number;
  ministryName: string;
  classId: number;
  className: string;
  servantId?: number | null;
  servantName?: string | null;
  method: VisitMethod;
  prayerScore?: number | null;
  readingScore?: number | null;
  noteScore?: number | null;
  notes?: string | null;
  recordedById: number;
  recordedByName: string;
  recordedAt: string;
}

export interface CreateVisitRequest {
  studentId: number;
  weekId: number;
  method: VisitMethod;
  prayerScore?: number | null;
  readingScore?: number | null;
  noteScore?: number | null;
  notes?: string | null;
}

export interface UpdateVisitRequest {
  method: VisitMethod;
  prayerScore?: number | null;
  readingScore?: number | null;
  noteScore?: number | null;
  notes?: string | null;
}

export interface StudentVisitStatusResponse {
  studentId: number;
  studentName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  address?: string;
  guardianPhone?: string;
  classId?: number;
  className?: string;
  servantId?: number;
  servantName?: string;
  visited: boolean;
  visitRecord?: VisitRecordResponse | null;
}

export interface ServantCurrentWeekResponse {
  week: WeekResponse;
  students: StudentVisitStatusResponse[];
}
