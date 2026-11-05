import { useQuery } from '@tanstack/react-query';
import { customerApi, propertyApi, userApi } from '@/api/endpoints';
import type { PropertyStatus } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';

/** Active users that can be assigned to records (only needed, and only fetched, for admins). */
export function useAgentOptions() {
  const { isAdmin } = useAuth();
  const query = useQuery({
    queryKey: ['users', 'agents'],
    queryFn: userApi.agents,
    enabled: isAdmin,
    staleTime: 5 * 60_000,
  });
  return (query.data?.content ?? []).map((u) => ({ value: u.id, label: u.fullName }));
}

export function useCustomerOptions() {
  const query = useQuery({
    queryKey: ['customers', 'options'],
    queryFn: () => customerApi.search({ size: 100, sort: 'fullName,asc' }),
  });
  return (query.data?.content ?? []).map((c) => ({ value: c.id, label: `${c.fullName} · ${c.phone}` }));
}

export function usePropertyOptions(status?: PropertyStatus) {
  const query = useQuery({
    queryKey: ['properties', 'options', status ?? 'all'],
    queryFn: () => propertyApi.search({ size: 100, sort: 'title,asc', status }),
  });
  return (query.data?.content ?? []).map((p) => ({ value: p.id, label: p.title, status: p.status }));
}
