export interface MinistryResponse {
  id: number;
  name: string;
  active: boolean;
  secretaryUserId?: number | null;
  secretaryPersonId?: number | null;
  secretaryName?: string | null;
  secretaryPhone?: string | null;
  classesCount?: number;
  servantsCount?: number;
  studentsCount?: number;
}

export interface MinistryRequest {
  name: string;
}

export interface GradeClassResponse {
  id: number;
  name: string;
  ministryId: number;
  ministryName: string;
  active: boolean;
  sortOrder: number;
  secretaryUserId?: number | null;
  secretaryPersonId?: number | null;
  secretaryName?: string | null;
  secretaryPhone?: string | null;
  servantsCount?: number;
  studentsCount?: number;
}

export interface GradeClassRequest {
  name: string;
  ministryId: number;
  sortOrder: number;
}

export interface AssignSecretaryPayload {
  personId?: number | null;
  userId?: number | null;
}
