export interface ServantResponse {
  id: number;
  personId?: number;
  userId?: number | null;
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId?: number;
  ministryName?: string;
  classId?: number;
  className?: string;
  academicYearId?: number;
  academicYearName?: string;
  roles?: string[];
  isClassSecretary?: boolean;
  active: boolean;
}

export interface CreateServantRequest {
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId: number;
  classId: number;
  password?: string;
}

export interface UpdateServantRequest {
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  ministryId: number;
  classId: number;
}
