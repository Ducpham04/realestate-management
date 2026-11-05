import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Building2 } from 'lucide-react';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input } from '@/components/ui';
import { getErrorMessage } from '@/lib/errors';

const schema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
});
type FormValues = z.infer<typeof schema>;

const DEMO_ACCOUNTS = [
  { label: 'Admin', email: 'admin@realestate.com', password: 'Admin@123' },
  { label: 'Môi giới', email: 'agent1@realestate.com', password: 'Agent@123' },
];

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/';

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } });

  if (isAuthenticated) return <Navigate to={from} replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      await login(values.email, values.password);
      navigate(from, { replace: true });
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-brand-900 lg:block">
        <img
          src="https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=1400&q=80"
          alt=""
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="relative flex h-full flex-col justify-end bg-gradient-to-t from-brand-900 via-brand-900/60 to-transparent p-12 text-white">
          <h2 className="max-w-md text-3xl font-semibold leading-tight">Quản lý bất động sản tập trung</h2>
          <p className="mt-3 max-w-md leading-relaxed text-brand-50">
            Theo dõi nguồn hàng, khách hàng, lịch xem nhà và giao dịch trên một nền tảng duy nhất.
          </p>
        </div>
      </div>

      <div className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-600 text-white">
              <Building2 className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-xl font-semibold text-slate-900">Đăng nhập</h1>
              <p className="text-sm text-slate-500">RealEstate Manager</p>
            </div>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <Field label="Email" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" autoComplete="email" invalid={!!errors.email} {...register('email')} />
            </Field>
            <Field label="Mật khẩu" htmlFor="password" error={errors.password?.message}>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                invalid={!!errors.password}
                {...register('password')}
              />
            </Field>
            {error && (
              <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                {error}
              </div>
            )}
            <Button type="submit" className="w-full" loading={isSubmitting}>
              Đăng nhập
            </Button>
          </form>

          <div className="mt-8 rounded-xl border border-dashed border-slate-300 p-4">
            <p className="text-sm font-medium text-slate-700">Dùng thử với tài khoản demo</p>
            <div className="mt-2 flex gap-2">
              {DEMO_ACCOUNTS.map((acc) => (
                <Button
                  key={acc.email}
                  type="button"
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    setValue('email', acc.email);
                    setValue('password', acc.password);
                  }}
                >
                  {acc.label}
                </Button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
