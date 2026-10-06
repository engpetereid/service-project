export type StudentStatus = 'ACTIVE' | 'GRADUATED';

export interface StudentResponse {
  id: number;
  personId?: number;
  fullName: string;
  phone?: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId?: number;
  ministryName?: string;
  classId?: number;
  className?: string;
  servantId?: number;
  servantName?: string;
  responsibleServantId?: number;
  responsibleServantName?: string;
  academicYearId: number;
  academicYearName: string;
  status: StudentStatus;
  guardianPhone?: string;
  talents?: string;
  additionalDetails?: string;
  assignedAt?: string;
  active: boolean;
}

export interface CreateStudentRequest {
  fullName: string;
  phone?: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId: number;
  classId: number;
  servantId?: number | null;
  guardianPhone?: string;
  talents?: string;
  additionalDetails?: string;
}

export interface UpdateStudentRequest {
  fullName: string;
  phone?: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId?: number;
  classId?: number;
  guardianPhone?: string;
  talents?: string;
  additionalDetails?: string;
}

export interface ChangeAssignmentRequest {
  servantId?: number | null;
}

export interface BatchAssignServantRequest {
  studentIds: number[];
  servantId?: number | null;
}

export interface BatchMoveClassRequest {
  studentIds: number[];
  ministryId: number;
  classId: number;
}
