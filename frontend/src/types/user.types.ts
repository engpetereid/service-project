import { Role, RoleWithScope } from './auth.types';

export interface UserResponse {
  userId: number;
  personId: number;
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string | null;
  address?: string | null;
  confessionFather?: string | null;
  enabled: boolean;
  roles: RoleWithScope[];
}

export interface RoleAssignment {
  role: Role;
  ministryId?: number | null;
  classId?: number | null;
}

export interface CreateUserRequest {
  fullName: string;
  phone: string;
  password?: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
  roles?: RoleAssignment[];
}

export interface CreateUserForPersonRequest {
  password?: string;
  roles?: RoleAssignment[];
}

export interface UpdateUserRequest {
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE';
  dateOfBirth?: string;
  address?: string;
  confessionFather?: string;
}

export interface AssignRolesRequest {
  roles: RoleAssignment[];
}

export interface ResetPasswordRequest {
  newPassword?: string;
}
