import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { appointmentApi, dashboardApi } from '@/api/endpoints';
import { useAuth } from '@/auth/AuthContext';
import { Badge, Card, ErrorState, PageHeader, Select, Spinner } from '@/components/ui';
import { formatCompactVnd, formatCurrency, formatDateTime, formatMonth } from '@/lib/format';
import { appointmentStatusLabels, appointmentStatusTones, propertyStatusColors, propertyStatusLabels } from '@/lib/labels';
import { getErrorMessage } from '@/lib/errors';

/** One cell of the KPI strip: label, figure, context. No decoration competing with the number. */
function Metric({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="bg-white px-5 py-4">
      <dt className="text-sm text-slate-600">{label}</dt>
      <dd className="tabular mt-1 text-2xl font-semibold text-slate-900">{value}</dd>
      {hint && <dd className="mt-0.5 text-sm text-slate-500">{hint}</dd>}
    </div>
  );
}

export default function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const [months, setMonths] = useState(12);

  const summary = useQuery({ queryKey: ['dashboard', 'summary'], queryFn: dashboardApi.summary });
  const byStatus = useQuery({ queryKey: ['dashboard', 'by-status'], queryFn: dashboardApi.propertiesByStatus });
  const revenue = useQuery({
    queryKey: ['dashboard', 'revenue', months],
    queryFn: () => dashboardApi.revenueByMonth(months),
  });
  const upcoming = useQuery({
    queryKey: ['appointments', 'upcoming'],
    queryFn: () =>
      appointmentApi.search({ status: 'SCHEDULED', from: new Date().toISOString(), size: 5, sort: 'scheduledAt,asc' }),
  });

  if (summary.isError) {
    return <ErrorState message={getErrorMessage(summary.error)} onRetry={() => summary.refetch()} />;
  }

  const s = summary.data;
  const pieData = (byStatus.data ?? []).filter((d) => d.count > 0);
  const chartData = (revenue.data ?? []).map((r) => ({ ...r, label: formatMonth(r.month) }));

  return (
    <>
      <PageHeader
        title={`Xin chào, ${user?.fullName}`}
        description={isAdmin ? 'Tổng quan hoạt động toàn công ty' : 'Tổng quan hoạt động của bạn'}
      />

      {!s ? (
        <Spinner />
      ) : (
        <Card className="overflow-hidden">
          <dl className="grid gap-px bg-slate-200 sm:grid-cols-2 xl:grid-cols-4">
            <Metric
              label="Doanh thu tháng này"
              value={formatCompactVnd(s.revenueThisMonth)}
              hint={`${s.closedDealsThisMonth} giao dịch đã chốt · hoa hồng ${formatCompactVnd(s.commissionThisMonth)}`}
            />
            <Metric label="Giao dịch đang xử lý" value={String(s.activeDeals)} hint="Đang đàm phán hoặc đã đặt cọc" />
            <Metric label="Lịch hẹn 7 ngày tới" value={String(s.upcomingAppointments)} hint="Buổi xem nhà đã lên lịch" />
            <Metric
              label="Bất động sản đang mở bán"
              value={`${s.availableProperties} / ${s.totalProperties}`}
              hint={`${s.totalCustomers} khách hàng ${isAdmin ? 'trong hệ thống' : 'bạn phụ trách'}`}
            />
          </dl>
        </Card>
      )}

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <Card className="p-5 xl:col-span-2">
          <div className="mb-4 flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="font-semibold text-slate-900">Doanh thu theo tháng</h2>
              <p className="text-xs text-slate-500">Giá trị giao dịch đã chốt và hoa hồng</p>
            </div>
            <Select
              aria-label="Khoảng thời gian"
              className="w-32"
              value={months}
              onChange={(e) => setMonths(Number(e.target.value))}
              options={[
                { value: 6, label: '6 tháng' },
                { value: 12, label: '12 tháng' },
                { value: 24, label: '24 tháng' },
              ]}
            />
          </div>
          {revenue.isLoading ? (
            <Spinner />
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <CartesianGrid stroke="oklch(0.925 0.007 185)" vertical={false} />
                  <XAxis dataKey="label" tick={{ fontSize: 12, fill: 'oklch(0.44 0.014 185)' }} axisLine={false} tickLine={false} />
                  <YAxis
                    yAxisId="left"
                    tickFormatter={(v: number) => formatCompactVnd(v)}
                    tick={{ fontSize: 12, fill: 'oklch(0.44 0.014 185)' }}
                    axisLine={false}
                    tickLine={false}
                    width={80}
                  />
                  <YAxis
                    yAxisId="right"
                    orientation="right"
                    tickFormatter={(v: number) => formatCompactVnd(v)}
                    tick={{ fontSize: 12, fill: 'oklch(0.44 0.014 185)' }}
                    axisLine={false}
                    tickLine={false}
                    width={80}
                  />
                  <Tooltip
                    formatter={(value: number, name: string) => [formatCurrency(value), name]}
                    labelClassName="font-medium"
                  />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar yAxisId="left" dataKey="revenue" name="Doanh thu" fill="oklch(0.58 0.11 185)" radius={[4, 4, 0, 0]} maxBarSize={36} />
                  <Line
                    yAxisId="right"
                    type="linear"
                    dataKey="commission"
                    name="Hoa hồng"
                    stroke="oklch(0.4 0.09 255)"
                    strokeWidth={2}
                    dot={{ r: 3, strokeWidth: 0, fill: 'oklch(0.4 0.09 255)' }}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card className="p-5">
          <h2 className="font-semibold text-slate-900">Bất động sản theo trạng thái</h2>
          <p className="text-xs text-slate-500">Toàn bộ nguồn hàng</p>
          {byStatus.isLoading ? (
            <Spinner />
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    dataKey="count"
                    nameKey="status"
                    innerRadius={60}
                    outerRadius={95}
                    paddingAngle={2}
                  >
                    {pieData.map((d) => (
                      <Cell key={d.status} fill={propertyStatusColors[d.status]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number, _n, item) => [v, propertyStatusLabels[item.payload.status as keyof typeof propertyStatusLabels]]} />
                  <Legend
                    formatter={(value: keyof typeof propertyStatusLabels) => propertyStatusLabels[value]}
                    wrapperStyle={{ fontSize: 12 }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>
      </div>

      <Card className="mt-6">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="font-semibold text-slate-900">Lịch hẹn sắp tới</h2>
          <Link to="/appointments" className="text-sm font-medium text-brand-600 hover:underline">
            Xem tất cả
          </Link>
        </div>
        {upcoming.isLoading ? (
          <Spinner />
        ) : upcoming.data?.content.length ? (
          <ul className="divide-y divide-slate-100">
            {upcoming.data.content.map((a) => (
              <li key={a.id} className="flex flex-col gap-1 px-5 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="truncate font-medium text-slate-900">{a.property.title}</p>
                  <p className="text-sm text-slate-500">
                    {a.customer.fullName} · {a.customer.phone} · {a.agent.fullName}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm text-slate-600">{formatDateTime(a.scheduledAt)}</span>
                  <Badge tone={appointmentStatusTones[a.status]}>{appointmentStatusLabels[a.status]}</Badge>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="px-5 py-8 text-center text-sm text-slate-500">Không có lịch hẹn sắp tới</p>
        )}
      </Card>
    </>
  );
}
