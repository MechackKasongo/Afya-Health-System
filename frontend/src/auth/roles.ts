import type { MeResponse } from '../api/types';

export type AppRole = 'ROLE_ADMIN' | 'ROLE_MEDECIN' | 'ROLE_INFIRMIER' | 'ROLE_RECEPTION';

export function hasRole(user: MeResponse | null, role: AppRole): boolean {
  return Boolean(user?.roles?.includes(role));
}

export function hasAnyRole(user: MeResponse | null, roles: AppRole[]): boolean {
  return roles.some((role) => hasRole(user, role));
}
