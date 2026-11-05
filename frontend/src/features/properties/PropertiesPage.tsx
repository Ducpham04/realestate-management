import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Bath, BedDouble, MapPin, Pencil, Plus, Ruler, Search, Trash2 } from 'lucide-react';
import { propertyApi } from '@/api/endpoints';
import type { ListingType, Property, PropertyFilters, PropertyStatus, PropertyType } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { RoleGate } from '@/auth/guards';
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Input,
  PageHeader,
  Pagination,
  Select,
  Spinner,
} from '@/components/ui';
import { useDebounce } from '@/lib/hooks';
import { getErrorMessage } from '@/lib/errors';
import { formatArea, formatCompactVnd } from '@/lib/format';
import {
  listingTypeLabels,
  options,
  propertyStatusLabels,
  propertyStatusTones,
  propertyTypeLabels,
} from '@/lib/labels';
import { PropertyFormModal } from './PropertyFormModal';

const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260"><rect width="400" height="260" fill="%23e2e8f0"/><path d="M150 150l50-45 50 45v45h-100z" fill="%2394a3b8"/></svg>';

const SORTS = [
  { value: 'createdAt,desc', label: 'Mới nhất' },
  { value: 'price,asc', label: 'Giá tăng dần' },
  { value: 'price,desc', label: 'Giá giảm dần' },
  { value: 'area,desc', label: 'Diện tích lớn nhất' },
  { value: 'title,asc', label: 'Tên A–Z' },
];

function PropertyCard({
  property,
  canEdit,
  onEdit,
  onDelete,
}: {
  property: Property;
  canEdit: boolean;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { address } = property;
  return (
    <Card className="group flex flex-col overflow-hidden transition-colors duration-150 hover:border-slate-300">
      <div className="relative aspect-[16/10] bg-slate-100">
        <img
          src={property.images[0] ?? FALLBACK_IMAGE}
          alt={property.title}
          loading="lazy"
          className="h-full w-full object-cover"
          onError={(e) => {
            e.currentTarget.src = FALLBACK_IMAGE;
          }}
        />
        <div className="absolute left-3 top-3 flex gap-1.5">
          <Badge tone={propertyStatusTones[property.status]}>{propertyStatusLabels[property.status]}</Badge>
        </div>
        <span className="absolute bottom-3 left-3 rounded-lg bg-slate-900/75 px-2 py-1 text-xs font-medium text-white">
          {listingTypeLabels[property.listingType]} · {propertyTypeLabels[property.type]}
        </span>
      </div>
      <div className="flex flex-1 flex-col p-4">
        <h3 className="line-clamp-2 font-semibold text-slate-900">{property.title}</h3>
        <p className="mt-1 flex items-start gap-1 text-sm text-slate-500">
          <MapPin className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="line-clamp-1">
            {address.street}, {address.district}, {address.city}
          </span>
        </p>
        <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-600">
          <span className="inline-flex items-center gap-1">
            <Ruler className="h-4 w-4" /> {formatArea(property.area)}
          </span>
          {property.bedrooms != null && (
            <span className="inline-flex items-center gap-1">
              <BedDouble className="h-4 w-4" /> {property.bedrooms}
            </span>
          )}
          {property.bathrooms != null && (
            <span className="inline-flex items-center gap-1">
              <Bath className="h-4 w-4" /> {property.bathrooms}
            </span>
          )}
        </div>
        <div className="mt-auto flex items-end justify-between pt-4">
          <div>
            <p className="tabular text-lg font-semibold text-slate-900">
              {formatCompactVnd(property.price)}
              {property.listingType === 'RENT' && <span className="text-sm font-normal text-slate-500">/tháng</span>}
            </p>
            <p className="text-xs text-slate-500">{property.agent.fullName}</p>
          </div>
          <div className="flex gap-1">
            {canEdit && (
              <Button size="sm" variant="ghost" onClick={onEdit} aria-label="Sửa">
                <Pencil className="h-4 w-4" />
              </Button>
            )}
            <RoleGate roles={['ADMIN']}>
              <Button size="sm" variant="ghost" onClick={onDelete} aria-label="Xóa" className="text-red-600">
                <Trash2 className="h-4 w-4" />
              </Button>
            </RoleGate>
          </div>
        </div>
      </div>
    </Card>
  );
}

export default function PropertiesPage() {
  const { user, isAdmin } = useAuth();
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState<PropertyFilters>({ page: 0, size: 12, sort: 'createdAt,desc' });
  const [editing, setEditing] = useState<Property | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<Property | null>(null);
  const debouncedKeyword = useDebounce(keyword);

  const params = { ...filters, keyword: debouncedKeyword || undefined };
  const query = useQuery({
    queryKey: ['properties', params],
    queryFn: () => propertyApi.search(params),
    placeholderData: keepPreviousData,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => propertyApi.remove(id),
    onSuccess: () => {
      toast.success('Đã xóa bất động sản');
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setDeleting(null);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const update = (patch: Partial<PropertyFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));
  const PAGING_KEYS = ['page', 'size', 'sort'];
  const hasFilters =
    !!keyword ||
    Object.entries(filters).some(([k, v]) => !PAGING_KEYS.includes(k) && v !== undefined && v !== '');
  const resetFilters = () => {
    setKeyword('');
    setFilters({ page: 0, size: filters.size, sort: 'createdAt,desc' });
  };
  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const toNumber = (v: string) => (v === '' ? undefined : Number(v));

  return (
    <>
      <PageHeader
        title="Bất động sản"
        description="Quản lý nguồn hàng bán và cho thuê"
        actions={
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" /> Thêm bất động sản
          </Button>
        }
      />

      <Card className="mb-6 p-4">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
          <div className="relative lg:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              aria-label="Tìm kiếm"
              placeholder="Tìm theo tiêu đề, địa chỉ..."
              className="pl-9"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setFilters((f) => ({ ...f, page: 0 }));
              }}
            />
          </div>
          <Select
            aria-label="Loại hình"
            placeholder="Tất cả loại hình"
            options={options(propertyTypeLabels)}
            value={filters.type ?? ''}
            onChange={(e) => update({ type: (e.target.value || undefined) as PropertyType | undefined })}
          />
          <Select
            aria-label="Hình thức"
            placeholder="Bán & cho thuê"
            options={options(listingTypeLabels)}
            value={filters.listingType ?? ''}
            onChange={(e) => update({ listingType: (e.target.value || undefined) as ListingType | undefined })}
          />
          <Select
            aria-label="Trạng thái"
            placeholder="Mọi trạng thái"
            options={options(propertyStatusLabels)}
            value={filters.status ?? ''}
            onChange={(e) => update({ status: (e.target.value || undefined) as PropertyStatus | undefined })}
          />
          <Select
            aria-label="Sắp xếp"
            options={SORTS}
            value={filters.sort}
            onChange={(e) => update({ sort: e.target.value })}
          />
          <Input
            aria-label="Thành phố"
            placeholder="Tỉnh/Thành phố"
            value={filters.city ?? ''}
            onChange={(e) => update({ city: e.target.value || undefined })}
          />
          <Input
            aria-label="Giá từ"
            type="number"
            placeholder="Giá từ (VND)"
            value={filters.minPrice ?? ''}
            onChange={(e) => update({ minPrice: toNumber(e.target.value) })}
          />
          <Input
            aria-label="Giá đến"
            type="number"
            placeholder="Giá đến (VND)"
            value={filters.maxPrice ?? ''}
            onChange={(e) => update({ maxPrice: toNumber(e.target.value) })}
          />
          <Input
            aria-label="Diện tích từ"
            type="number"
            placeholder="Diện tích từ (m²)"
            value={filters.minArea ?? ''}
            onChange={(e) => update({ minArea: toNumber(e.target.value) })}
          />
          <Input
            aria-label="Số phòng ngủ tối thiểu"
            type="number"
            placeholder="Phòng ngủ ≥"
            value={filters.minBedrooms ?? ''}
            onChange={(e) => update({ minBedrooms: toNumber(e.target.value) })}
          />
          <Button variant="ghost" onClick={resetFilters} disabled={!hasFilters}>
            Xóa bộ lọc
          </Button>
        </div>
      </Card>

      {query.isLoading ? (
        <Spinner />
      ) : query.isError ? (
        <ErrorState message={getErrorMessage(query.error)} onRetry={() => query.refetch()} />
      ) : query.data && query.data.content.length > 0 ? (
        <>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {query.data.content.map((p) => (
              <PropertyCard
                key={p.id}
                property={p}
                canEdit={isAdmin || p.agent.id === user?.id}
                onEdit={() => {
                  setEditing(p);
                  setFormOpen(true);
                }}
                onDelete={() => setDeleting(p)}
              />
            ))}
          </div>
          <Card className="mt-6">
            <Pagination
              page={query.data.page}
              size={query.data.size}
              totalPages={query.data.totalPages}
              totalElements={query.data.totalElements}
              onPageChange={(page) => setFilters((f) => ({ ...f, page }))}
            />
          </Card>
        </>
      ) : (
        <Card>
          {hasFilters ? (
            <EmptyState
              title="Không có bất động sản phù hợp"
              description="Thử nới khoảng giá, diện tích hoặc bỏ bớt điều kiện lọc."
              action={
                <Button variant="secondary" onClick={resetFilters}>
                  Xóa bộ lọc
                </Button>
              }
            />
          ) : (
            <EmptyState
              title="Chưa có bất động sản nào"
              description="Thêm căn đầu tiên để bắt đầu quản lý nguồn hàng."
              action={<Button onClick={openCreate}>Thêm bất động sản</Button>}
            />
          )}
        </Card>
      )}

      <PropertyFormModal open={formOpen} property={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={!!deleting}
        title="Xóa bất động sản"
        confirmLabel="Xóa bất động sản"
        message={`Bạn chắc chắn muốn xóa "${deleting?.title}"? Thao tác này không thể hoàn tác.`}
        loading={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
