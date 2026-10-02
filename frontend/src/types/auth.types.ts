export type Role = 'GENERAL_ADMIN' | 'SERVICE_SECRETARY' | 'CLASS_SECRETARY' | 'SERVANT';

export interface RoleWithScope {
  role: Role;
  ministryId: number | null;
  classId: number | null;
}

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user?: UserInfoResponse;
  userId?: number;
  personId?: number;
  fullName?: string;
  phone?: string;
  roles?: RoleWithScope[];
}

export interface UserInfoResponse {
  userId: number;
  personId: number;
  fullName: string;
  phone: string;
  roles: RoleWithScope[];
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
