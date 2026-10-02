import { Role } from '../types/auth.types';

export const ROLE_LABELS: Record<Role, string> = {
  GENERAL_ADMIN: 'الأمين العام',
  SERVICE_SECRETARY: 'أمين الخدمة',
  CLASS_SECRETARY: 'أمين الفصل',
  SERVANT: 'خادم',
};

export const ACTIVITY_LABELS: Record<string, string> = {
  MASS: 'قداس',
  MEETING: 'اجتماع',
  TASBEHA: 'تسبحة',
};

export const VISIT_METHOD_LABELS: Record<string, string> = {
  VISIT: 'افت افتقاد منزلي',
  CALL: 'مكالمة هاتفية',
};

export const GENDER_LABELS: Record<string, string> = {
  MALE: 'ذكر',
  FEMALE: 'أنثى',
};

export const STUDENT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'نشط',
  GRADUATED: 'خريج',
};

export function getRoleBadgeClass(role: Role): string {
  switch (role) {
    case 'GENERAL_ADMIN':
      return 'bg-purple-100 text-purple-800 border-purple-200';
    case 'SERVICE_SECRETARY':
      return 'bg-blue-100 text-blue-800 border-blue-200';
    case 'CLASS_SECRETARY':
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    case 'SERVANT':
      return 'bg-amber-100 text-amber-800 border-amber-200';
    default:
      return 'bg-gray-100 text-gray-800 border-gray-200';
  }
}
