import { useMemo } from 'react';
import { useAuth } from './useAuth';
import { Role } from '../types/auth.types';

export const usePermissions = () => {
  const { user } = useAuth();

  const permissions = useMemo(() => {
    if (!user) {
      return {
        isAdmin: false,
        isServiceSecretary: false,
        isClassSecretary: false,
        isServant: false,
        hasRole: () => false,
        hasAnyRole: () => false,
        managedMinistryId: null,
        managedClassId: null,
      };
    }

    const roles = user.roles || [];
    const isAdmin = roles.some((r) => r.role === 'GENERAL_ADMIN');
    
    const serviceSecretaryRole = roles.find((r) => r.role === 'SERVICE_SECRETARY');
    const isServiceSecretary = !!serviceSecretaryRole;

    const classSecretaryRole = roles.find((r) => r.role === 'CLASS_SECRETARY');
    const isClassSecretary = !!classSecretaryRole;

    const servantRole = roles.find((r) => r.role === 'SERVANT');
    const isServant = !!servantRole;

    const managedMinistryIds = Array.from(
      new Set(
        roles
          .map((r) => r.ministryId)
          .filter((id): id is number => typeof id === 'number')
      )
    );
    const managedClassIds = Array.from(
      new Set(
        roles
          .map((r) => r.classId)
          .filter((id): id is number => typeof id === 'number')
      )
    );

    let managedMinistryId: number | null = null;
    let managedClassId: number | null = null;

    if (!isAdmin) {
      managedMinistryId = serviceSecretaryRole?.ministryId ?? classSecretaryRole?.ministryId ?? servantRole?.ministryId ?? (managedMinistryIds[0] ?? null);
      managedClassId = classSecretaryRole?.classId ?? servantRole?.classId ?? (managedClassIds[0] ?? null);
    }

    const hasRole = (role: Role) => roles.some((r) => r.role === role);
    const hasAnyRole = (checkRoles: Role[]) => checkRoles.some((cr) => hasRole(cr));

    return {
      isAdmin,
      isServiceSecretary,
      isClassSecretary,
      isServant,
      hasRole,
      hasAnyRole,
      managedMinistryId,
      managedClassId,
      managedMinistryIds,
      managedClassIds,
    };
  }, [user]);

  return permissions;
};
