import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Pencil, Plus } from 'lucide-react';
import { userApi } from '@/api/endpoints';
import type { Role, User } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import {
  Badge,
  Button,
  Card,
  ErrorState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Table,
  Td,
  Th,
} from '@/components/ui';
import { getErrorMessage } from '@/lib/errors';
import { formatDate } from '@/lib/format';
import { options, roleLabels } from '@/lib/labels';

const phone = z.union([z.literal(''), z.string().regex(/^[0-9+\-\s]{8,20}$/, 'Số điện thoại không hợp lệ')]);

const createSchema = z.object({
  fullName: z.string().trim().min(1, 'Vui lòng nhập họ tên'),
  email: z.string().email('Email không hợp lệ'),
  password: z
    .string()
    .min(8, 'Tối thiểu 8 ký tự')
    .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Cần có cả chữ và số'),
  phone,
  role: z.enum(['ADMIN', 'AGENT']),
});

const editSchema = z.object({
  fullName: z.string().trim().min(1, 'Vui lòng nhập họ tên'),
  phone,
  role: z.enum(['ADMIN', 'AGENT']),
  enabled: z.enum(['true', 'false']),
});

function CreateUserModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<z.infer<typeof createSchema>>({
    resolver: zodResolver(createSchema),
    defaultValues: { fullName: '', email: '', password: '', phone: '', role: 'AGENT' },
  });

  useEffect(() => {
    if (open) reset();
  }, [open, reset]);

  const mutation = useMutation({
    mutationFn: userApi.create,
    onSuccess: () => {
      toast.success('Đã tạo tài khoản');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Thêm nhân viên"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="create-user-form" loading={mutation.isPending}>
            Tạo tài khoản
          </Button>
        </>
      }
    >
      <form
        id="create-user-form"
        className="space-y-4"
        noValidate
        onSubmit={handleSubmit((v) => mutation.mutate({ ...v, phone: v.phone || null }))}
      >
        <Field label="Họ tên" htmlFor="u-name" error={errors.fullName?.message} required>
          <Input id="u-name" {...register('fullName')} />
        </Field>
        <Field label="Email" htmlFor="u-email" error={errors.email?.message} required>
          <Input id="u-email" type="email" {...register('email')} />
        </Field>
        <Field label="Mật khẩu" htmlFor="u-password" error={errors.password?.message} required>
          <Input id="u-password" type="password" autoComplete="new-password" {...register('password')} />
        </Field>
        <Field label="Số điện thoại" htmlFor="u-phone" error={errors.phone?.message}>
          <Input id="u-phone" {...register('phone')} />
        </Field>
        <Field label="Vai trò" htmlFor="u-role" required>
          <Select id="u-role" options={options(roleLabels)} {...register('role')} />
        </Field>
      </form>
    </Modal>
  );
}

function EditUserModal({ user, onClose }: { user: User | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { user: me } = useAuth();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<z.infer<typeof editSchema>>({ resolver: zodResolver(editSchema) });

  useEffect(() => {
    if (user) {
      reset({ fullName: user.fullName, phone: user.phone ?? '', role: user.role, enabled: String(user.enabled) as 'true' });
    }
  }, [user, reset]);

  const mutation = useMutation({
    mutationFn: (v: z.infer<typeof editSchema>) =>
      userApi.update(user!.id, {
        fullName: v.fullName,
        phone: v.phone || null,
        role: v.role as Role,
        enabled: v.enabled === 'true',
      }),
    onSuccess: () => {
      toast.success('Đã cập nhật tài khoản');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const isSelf = user?.id === me?.id;

  return (
    <Modal
      open={!!user}
      onClose={onClose}
      title={`Cập nhật: ${user?.email ?? ''}`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="edit-user-form" loading={mutation.isPending}>
            Lưu
          </Button>
        </>
      }
    >
      <form id="edit-user-form" className="space-y-4" noValidate onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <Field label="Họ tên" htmlFor="e-name" error={errors.fullName?.message} required>
          <Input id="e-name" {...register('fullName')} />
        </Field>
        <Field label="Số điện thoại" htmlFor="e-phone" error={errors.phone?.message}>
          <Input id="e-phone" {...register('phone')} />
        </Field>
        <Field label="Vai trò" htmlFor="e-role" hint={isSelf ? 'Không thể đổi vai trò của chính mình' : undefined}>
          <Select id="e-role" disabled={isSelf} options={options(roleLabels)} {...register('role')} />
        </Field>
        <Field label="Trạng thái" htmlFor="e-enabled">
          <Select
            id="e-enabled"
            disabled={isSelf}
            options={[
              { value: 'true', label: 'Đang hoạt động' },
              { value: 'false', label: 'Đã khóa' },
            ]}
            {...register('enabled')}
          />
        </Field>
      </form>
    </Modal>
  );
}

export default function UsersPage() {
  const [page, setPage] = useState(0);
  const [role, setRole] = useState<Role | ''>('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);

  const query = useQuery({
    queryKey: ['users', { page, role }],
    queryFn: () => userApi.search({ page, size: 10, role: role || undefined, sort: 'fullName,asc' }),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader
        title="Nhân viên"
        description="Quản lý tài khoản quản trị viên và môi giới"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" /> Thêm nhân viên
          </Button>
        }
      />
      <Card>
        <div className="border-b border-slate-200 p-4">
          <Select
            aria-label="Vai trò"
            className="max-w-xs"
            placeholder="Mọi vai trò"
            options={options(roleLabels)}
            value={role}
            onChange={(e) => {
              setRole(e.target.value as Role | '');
              setPage(0);
            }}
          />
        </div>
        {query.isLoading ? (
          <Spinner />
        ) : query.isError ? (
          <ErrorState message={getErrorMessage(query.error)} onRetry={() => query.refetch()} />
        ) : (
          query.data && (
            <>
              <Table>
                <thead className="bg-slate-50">
                  <tr>
                    <Th>Họ tên</Th>
                    <Th>Email</Th>
                    <Th>Điện thoại</Th>
                    <Th>Vai trò</Th>
                    <Th>Trạng thái</Th>
                    <Th>Ngày tạo</Th>
                    <Th className="text-right">Thao tác</Th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {query.data.content.map((u) => (
                    <tr key={u.id} className="hover:bg-slate-50">
                      <Td className="font-medium text-slate-900">{u.fullName}</Td>
                      <Td>{u.email}</Td>
                      <Td>{u.phone || '—'}</Td>
                      <Td>
                        <Badge tone={u.role === 'ADMIN' ? 'violet' : 'blue'}>{roleLabels[u.role]}</Badge>
                      </Td>
                      <Td>
                        <Badge tone={u.enabled ? 'green' : 'slate'}>{u.enabled ? 'Hoạt động' : 'Đã khóa'}</Badge>
                      </Td>
                      <Td>{formatDate(u.createdAt)}</Td>
                      <Td className="text-right">
                        <Button size="sm" variant="ghost" aria-label="Sửa" onClick={() => setEditing(u)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </Table>
              <Pagination
                page={query.data.page}
                size={query.data.size}
                totalPages={query.data.totalPages}
                totalElements={query.data.totalElements}
                onPageChange={setPage}
              />
            </>
          )
        )}
      </Card>
      <CreateUserModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <EditUserModal user={editing} onClose={() => setEditing(null)} />
    </>
  );
}
