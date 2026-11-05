import { Link } from 'react-router-dom';

function StatusPage({ code, title, message }: { code: string; title: string; message: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <p className="text-5xl font-semibold text-brand-600">{code}</p>
      <h1 className="mt-4 text-2xl font-semibold text-slate-900">{title}</h1>
      <p className="mt-2 text-slate-500">{message}</p>
      <Link to="/" className="mt-6 text-sm font-medium text-brand-600 hover:underline">
        ← Về trang tổng quan
      </Link>
    </div>
  );
}

export const NotFoundPage = () => (
  <StatusPage code="404" title="Không tìm thấy trang" message="Trang bạn tìm không tồn tại." />
);

export const ForbiddenPage = () => (
  <StatusPage code="403" title="Không có quyền truy cập" message="Tài khoản của bạn không được phép xem trang này." />
);
