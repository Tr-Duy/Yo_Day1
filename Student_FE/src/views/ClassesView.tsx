import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { toast } from 'react-hot-toast';
import {
  Search, Plus, Edit, Trash2, Eye, School, Calendar, Clock, DoorOpen, UserCheck, AlertCircle, RefreshCw, Award
} from 'lucide-react';
import { classesApi } from '../features/classes/classes.api';
import { referenceApi } from '../features/reference/reference.api';
import { coursesApi } from '../features/courses/courses.api';
import type { CourseClassResponse, ClassStatus } from '../types/yoedu';

import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/Table';
import { EmptyState } from '../components/ui/EmptyState';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { ClassGradesModal } from '../components/ClassGradesModal';

// ==========================================
// FORM VALIDATION SCHEMA WITH ZOD
// ==========================================
const classFormSchema = z.object({
  classCode: z.string().min(2, 'Mã lớp tối thiểu 2 ký tự'),
  name: z.string().min(2, 'Tên lớp tối thiểu 2 ký tự'),
  courseId: z.any(),
  roomId: z.any(),
  scheduleSlotId: z.any(),
  mainTeacherId: z.any(),
  assistantTeacherId: z.any(),
  startDate: z.string().min(1, 'Ngày bắt đầu là bắt buộc'),
  endDate: z.string().min(1, 'Ngày kết thúc là bắt buộc'),
  maxStudents: z.any(),
  tuitionFee: z.any(),
  status: z.enum(['OPEN', 'ONGOING', 'CLOSED', 'FULL']),
});

type ClassFormValues = z.infer<typeof classFormSchema>;

export const ClassesView: React.FC = () => {
  const queryClient = useQueryClient();

  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [isUpsertOpen, setIsUpsertOpen] = useState(false);
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const [isGradesOpen, setIsGradesOpen] = useState(false);
  const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);
  const [selectedClass, setSelectedClass] = useState<CourseClassResponse | null>(null);
  const [editingClassId, setEditingClassId] = useState<number | null>(null);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(0);
    }, 450);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const { data: classesData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['courseClasses', debouncedSearch, statusFilter, page],
    queryFn: async () => {
      const activeStatus = statusFilter !== 'ALL' ? (statusFilter as ClassStatus) : undefined;
      return classesApi.search({ search: debouncedSearch, status: activeStatus, page, size: pageSize });
    },
  });

  const { data: coursesList } = useQuery({ queryKey: ['reference-courses'], queryFn: () => coursesApi.getAll(), enabled: isUpsertOpen });
  const { data: teachersList } = useQuery({ queryKey: ['reference-teachers'], queryFn: () => referenceApi.getTeachers(), enabled: isUpsertOpen });
  const { data: roomsList } = useQuery({ queryKey: ['reference-rooms'], queryFn: () => referenceApi.getRooms(), enabled: isUpsertOpen });
  const { data: scheduleSlotsList } = useQuery({ queryKey: ['reference-scheduleSlots'], queryFn: () => referenceApi.getScheduleSlots(), enabled: isUpsertOpen });

  const upsertMutation = useMutation({
    mutationFn: async (values: ClassFormValues) => {
      const payload = {
        classCode: values.classCode,
        name: values.name,
        courseId: Number(values.courseId) || 0,
        roomId: Number(values.roomId) || 0,
        scheduleSlotId: Number(values.scheduleSlotId) || 0,
        mainTeacherId: Number(values.mainTeacherId) || 0,
        assistantTeacherId: values.assistantTeacherId && values.assistantTeacherId !== 'null' ? Number(values.assistantTeacherId) : null,
        startDate: values.startDate,
        endDate: values.endDate,
        maxStudents: Number(values.maxStudents) || 20,
        tuitionFee: Number(values.tuitionFee) || 0,
        status: values.status,
      };

      if (editingClassId) {
        return classesApi.update(editingClassId, payload);
      } else {
        return classesApi.create(payload);
      }
    },
    onSuccess: () => {
      toast.success(editingClassId ? 'Cập nhật lớp học thành công!' : 'Tạo lớp học mới thành công!');
      queryClient.invalidateQueries({ queryKey: ['courseClasses'] });
      queryClient.invalidateQueries({ queryKey: ['enrollment-classes'] });
      queryClient.invalidateQueries({ queryKey: ['classes-list'] });
      queryClient.invalidateQueries({ queryKey: ['classes-lookup'] });
      setIsUpsertOpen(false);
    },
    onError: (err: any) => {
      toast.error(err.message || 'Thao tác không thành công');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => classesApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa lớp học thành công!');
      queryClient.invalidateQueries({ queryKey: ['courseClasses'] });
      queryClient.invalidateQueries({ queryKey: ['enrollment-classes'] });
      queryClient.invalidateQueries({ queryKey: ['classes-list'] });
      queryClient.invalidateQueries({ queryKey: ['classes-lookup'] });
      setIsConfirmDeleteOpen(false);
    },
    onError: (err: any) => {
      toast.error(err.message || 'Không thể xóa lớp học');
    }
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<ClassFormValues>({
    resolver: zodResolver(classFormSchema),
    defaultValues: {
      classCode: '', name: '', courseId: '', roomId: '', scheduleSlotId: '',
      mainTeacherId: '', assistantTeacherId: 'null',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      maxStudents: 20, tuitionFee: 1500000, status: 'OPEN',
    }
  });

  const onSubmitForm = (values: ClassFormValues) => {
    upsertMutation.mutate(values);
  };

  const handleEditClick = (c: CourseClassResponse) => {
    setEditingClassId(c.id);
    reset({
      classCode: c.classCode,
      name: c.name,
      courseId: String(c.courseId || (c as any).course?.id || ''),
      roomId: String(c.roomId || (c as any).room?.id || ''),
      scheduleSlotId: String(c.scheduleSlotId || (c as any).scheduleSlot?.id || ''),
      mainTeacherId: String(c.mainTeacherId || (c as any).mainTeacher?.id || ''),
      assistantTeacherId: String(c.assistantTeacherId || (c as any).assistantTeacher?.id || 'null'),
      startDate: c.startDate,
      endDate: c.endDate,
      maxStudents: c.maxStudents,
      tuitionFee: c.tuitionFee,
      status: c.status,
    });
    setIsUpsertOpen(true);
  };

  const handleOpenAdd = () => {
    setEditingClassId(null);
    reset({
      classCode: '', name: '', courseId: '', roomId: '', scheduleSlotId: '',
      mainTeacherId: '', assistantTeacherId: 'null',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      maxStudents: 20, tuitionFee: 1500000, status: 'OPEN',
    });
    setIsUpsertOpen(true);
  };

  const formatVND = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

  const classesList = classesData?.content || [];
  const totalPages = classesData?.totalPages || 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground">Quản lý Lớp học</h2>
          <p className="text-foreground-muted text-sm mt-1">Lập kế hoạch tổ chức lớp, phân công giảng viên và quản lý sĩ số.</p>
        </div>
        <Button variant="primary" icon={<Plus size={16} />} onClick={handleOpenAdd}>Thêm Lớp Học</Button>
      </div>

      <div className="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-foreground-muted pointer-events-none" size={16} />
          <input
            type="text"
            placeholder="Tìm theo mã lớp, tên lớp..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-surface border border-border rounded-lg text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all placeholder:text-foreground-muted/60"
          />
        </div>

        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
          {(['ALL', 'OPEN', 'ONGOING', 'CLOSED', 'FULL'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => { setStatusFilter(filter); setPage(0); }}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors ${
                statusFilter === filter
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-surface border border-border text-foreground-secondary hover:bg-surface-hover hover:text-foreground'
              }`}
            >
              {filter === 'ALL' && 'Tất cả'}
              {filter === 'OPEN' && 'Mở đăng ký'}
              {filter === 'ONGOING' && 'Đang học'}
              {filter === 'CLOSED' && 'Đã đóng'}
              {filter === 'FULL' && 'Đã đầy'}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden shadow-sm transition-colors">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã Lớp</TableHead>
              <TableHead>Lớp Học</TableHead>
              <TableHead>Lịch & Phòng</TableHead>
              <TableHead>Giáo Viên</TableHead>
              <TableHead>Sĩ Số / Học Phí</TableHead>
              <TableHead>Trạng Thái</TableHead>
              <TableHead className="text-right">Thao Tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-12 text-foreground-muted">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <RefreshCw className="h-6 w-6 animate-spin text-brand-500" />
                    <span className="text-sm font-medium">Đang tải lớp học...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={7} className="py-12 text-center">
                  <div className="flex flex-col items-center justify-center gap-3 max-w-md mx-auto">
                    <div className="p-3 bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 rounded-full">
                      <AlertCircle className="h-6 w-6" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-rose-600 dark:text-rose-400 text-sm">Không thể tải danh sách lớp học</h4>
                      <p className="text-xs text-foreground-muted mt-1">{(error as Error)?.message || 'Lỗi kết nối máy chủ'}</p>
                    </div>
                    <Button variant="secondary" size="sm" onClick={() => refetch()} icon={<RefreshCw size={14} />}>
                      Thử lại
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ) : classesList.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="py-12">
                  <EmptyState
                    title={debouncedSearch || statusFilter !== 'ALL' ? 'Không tìm thấy lớp học' : 'Chưa có lớp học'}
                    description={debouncedSearch || statusFilter !== 'ALL' ? 'Không có lớp học nào khớp với tìm kiếm hoặc bộ lọc.' : 'Chưa có lớp học nào trong hệ thống.'}
                    isSearch={Boolean(debouncedSearch || statusFilter !== 'ALL')}
                  />
                </TableCell>
              </TableRow>
            ) : (
              classesList.map((c) => {
                const enrolled = c.enrolledCount || 0;
                const max = c.maxStudents || 0;
                const isOverCapacity = max > 0 && enrolled > max;
                const isFull = max > 0 && enrolled >= max;

                return (
                  <TableRow key={c.id}>
                    <TableCell className="font-mono text-brand-600 dark:text-brand-400 font-medium">{c.classCode}</TableCell>
                    <TableCell>
                      <div className="font-medium text-foreground">{c.name}</div>
                      <div className="text-xs text-foreground-muted mt-0.5">{c.courseName || (c as any).course?.name || 'N/A'}</div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm text-foreground-secondary flex items-center gap-1.5">
                        <Clock size={12}/>
                        {c.scheduleLabel || ((c as any).scheduleSlot ? `${(c as any).scheduleSlot.dayOfWeek} (${(c as any).scheduleSlot.startTime?.slice(0,5)} - ${(c as any).scheduleSlot.endTime?.slice(0,5)})` : 'Chưa xếp lịch')}
                      </div>
                      <div className="text-xs text-foreground-muted flex items-center gap-1.5 mt-0.5">
                        <DoorOpen size={12}/>Phòng: {c.roomName || (c as any).room?.name || 'N/A'}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm text-foreground-secondary flex items-center gap-1.5">
                        <UserCheck size={12}/>GV: {c.mainTeacherName || (c as any).mainTeacher?.fullName || 'Chưa gán'}
                      </div>
                      {(c.assistantTeacherName || (c as any).assistantTeacher) && (
                        <div className="text-xs text-foreground-muted flex items-center gap-1.5 mt-0.5">
                          <UserCheck size={12}/>TG: {c.assistantTeacherName || (c as any).assistantTeacher?.fullName}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="text-sm flex items-center gap-1.5">
                        <span className={isOverCapacity ? 'text-rose-600 dark:text-rose-400 font-bold' : 'text-foreground-secondary font-medium'}>
                          {enrolled} / {max}
                        </span>
                        {isOverCapacity && (
                          <span className="text-[10px] bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300 px-1.5 py-0.5 rounded font-semibold" title="Sĩ số vượt quá sức chứa tối đa!">
                            Lỗi sĩ số
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-emerald-600 dark:text-emerald-400 mt-0.5 font-medium">{formatVND(c.tuitionFee)}</div>
                    </TableCell>
                    <TableCell>
                      {(() => {
                        if (c.status === 'CLOSED') {
                          return <Badge variant="danger">Đã đóng</Badge>;
                        }
                        if (c.status === 'ONGOING') {
                          return (
                            <div className="flex flex-col gap-1 items-start">
                              <Badge variant="info">Đang học</Badge>
                              {isFull && (
                                <span className="text-[10px] font-semibold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-1.5 py-0.5 rounded">
                                  Đã đầy sĩ số
                                </span>
                              )}
                            </div>
                          );
                        }
                        // For OPEN or FULL: ONLY display 'Đã đầy' if actually full (enrolled >= max)
                        if (isFull) {
                          return <Badge variant="warning">Đã đầy</Badge>;
                        }
                        return <Badge variant="success">Mở đăng ký</Badge>;
                      })()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" size="sm" title="Bảng điểm" onClick={() => { setSelectedClass(c); setIsGradesOpen(true); }}><Award size={16}/></Button>
                        <Button variant="ghost" size="sm" onClick={() => { setSelectedClass(c); setIsDetailsOpen(true); }}><Eye size={16}/></Button>
                        <Button variant="ghost" size="sm" onClick={() => handleEditClick(c)}><Edit size={16}/></Button>
                        <Button variant="ghost" size="sm" className="text-rose-600 dark:text-rose-400 hover:text-rose-500" onClick={() => { setSelectedClass(c); setIsConfirmDeleteOpen(true); }}><Trash2 size={16}/></Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
        {!isLoading && totalPages > 1 && (
          <div className="p-4 border-t border-border flex justify-between items-center bg-surface-hover/30">
            <span className="text-sm text-foreground-muted">Trang {page + 1} / {totalPages}</span>
            <div className="flex gap-2">
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Trước</Button>
              <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Sau</Button>
            </div>
          </div>
        )}
      </div>

      <Modal isOpen={isDetailsOpen} onClose={() => setIsDetailsOpen(false)} title="Chi tiết Lớp học" maxWidth="2xl">
        {selectedClass && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 p-4 rounded-xl bg-surface-hover/50 border border-border">
              <div className="h-12 w-12 rounded-lg bg-brand-600/15 text-brand-600 dark:text-brand-400 border border-brand-500/25 flex items-center justify-center font-bold text-xl">
                <School size={24} />
              </div>
              <div>
                <h4 className="text-lg font-semibold text-foreground">{selectedClass.name}</h4>
                <div className="text-sm text-foreground-muted mt-1 flex gap-3">
                  <span>Mã: {selectedClass.classCode}</span>
                  <span>Khóa học: {selectedClass.courseName || (selectedClass as any).course?.name || 'N/A'}</span>
                </div>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-6 text-sm">
              <div className="space-y-3">
                <h5 className="font-semibold text-foreground border-b border-border pb-2">Thời gian & Địa điểm</h5>
                <div className="flex justify-between">
                  <span className="text-foreground-muted">Lịch học:</span>
                  <span className="text-foreground">{selectedClass.scheduleLabel || ((selectedClass as any).scheduleSlot ? `${(selectedClass as any).scheduleSlot.dayOfWeek} (${(selectedClass as any).scheduleSlot.startTime?.slice(0,5)} - ${(selectedClass as any).scheduleSlot.endTime?.slice(0,5)})` : 'Chưa xếp lịch')}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-foreground-muted">Phòng:</span>
                  <span className="text-foreground">{selectedClass.roomName || (selectedClass as any).room?.name || 'N/A'}</span>
                </div>
                <div className="flex justify-between"><span className="text-foreground-muted">Khởi giảng:</span><span className="text-foreground">{selectedClass.startDate}</span></div>
                <div className="flex justify-between"><span className="text-foreground-muted">Kết thúc:</span><span className="text-foreground">{selectedClass.endDate}</span></div>
              </div>
              <div className="space-y-3">
                <h5 className="font-semibold text-foreground border-b border-border pb-2">Học vụ & Phân công</h5>
                <div className="flex justify-between">
                  <span className="text-foreground-muted">GV Chính:</span>
                  <span className="text-foreground">{selectedClass.mainTeacherName || (selectedClass as any).mainTeacher?.fullName || 'Chưa gán'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-foreground-muted">Trợ giảng:</span>
                  <span className="text-foreground">{selectedClass.assistantTeacherName || (selectedClass as any).assistantTeacher?.fullName || 'Không có'}</span>
                </div>
                <div className="flex justify-between"><span className="text-foreground-muted">Sĩ số:</span><span className="text-foreground">{selectedClass.enrolledCount || 0} / {selectedClass.maxStudents}</span></div>
                <div className="flex justify-between"><span className="text-foreground-muted">Học phí:</span><span className="text-emerald-600 dark:text-emerald-400 font-semibold">{formatVND(selectedClass.tuitionFee)}</span></div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-border">
              <Button variant="secondary" onClick={() => setIsDetailsOpen(false)}>Đóng</Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal isOpen={isUpsertOpen} onClose={() => setIsUpsertOpen(false)} title={editingClassId ? 'Cập nhật Lớp học' : 'Thêm Lớp học mới'} maxWidth="3xl">
        <form onSubmit={handleSubmit(onSubmitForm)} className="space-y-6">
          <div className="grid grid-cols-2 gap-4">
            <Input label="Mã Lớp *" {...register('classCode')} error={errors.classCode?.message} />
            <Input label="Tên Lớp *" {...register('name')} error={errors.name?.message} />
            
            <Select label="Khóa Học *" {...register('courseId')} error={errors.courseId?.message as string}>
              <option value="">-- Chọn Khóa Học --</option>
              {coursesList?.map((c: any) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
            <Select label="Phòng Học *" {...register('roomId')} error={errors.roomId?.message as string}>
              <option value="">-- Chọn Phòng Học --</option>
              {roomsList?.map((r: any) => <option key={r.id} value={r.id}>{r.name} - Sức chứa: {r.capacity}</option>)}
            </Select>

            <Select label="Ca Học *" {...register('scheduleSlotId')} error={errors.scheduleSlotId?.message as string}>
              <option value="">-- Chọn Ca Học --</option>
              {scheduleSlotsList?.map((s: any) => <option key={s.id} value={s.id}>{s.slotName} - {s.dayOfWeek} ({s.startTime.slice(0,5)} - {s.endTime.slice(0,5)})</option>)}
            </Select>
            <Select label="Trạng thái *" {...register('status')} error={errors.status?.message}>
              <option value="OPEN">Mở đăng ký</option><option value="ONGOING">Đang học</option><option value="FULL">Đã đầy</option><option value="CLOSED">Đã đóng</option>
            </Select>

            <Select label="GV Chủ nhiệm *" {...register('mainTeacherId')} error={errors.mainTeacherId?.message as string}>
              <option value="">-- Chọn GV Chủ nhiệm --</option>
              {teachersList?.map((t: any) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
            </Select>
            <Select label="Trợ giảng" {...register('assistantTeacherId')}>
              <option value="null">-- Không có Trợ giảng --</option>
              {teachersList?.map((t: any) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
            </Select>

            <Input label="Ngày Bắt Đầu *" type="date" {...register('startDate')} error={errors.startDate?.message} />
            <Input label="Ngày Kết Thúc *" type="date" {...register('endDate')} error={errors.endDate?.message} />
            <Input label="Sĩ Số Tối Đa" type="number" {...register('maxStudents')} error={errors.maxStudents?.message as string} />
            <Input label="Học phí lớp (VND)" type="number" {...register('tuitionFee')} error={errors.tuitionFee?.message as string} />
          </div>
          
          <div className="flex justify-end gap-3 pt-4 border-t border-border">
            <Button variant="secondary" type="button" onClick={() => setIsUpsertOpen(false)}>Hủy</Button>
            <Button variant="primary" type="submit" isLoading={upsertMutation.isPending}>Lưu</Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={isConfirmDeleteOpen}
        onClose={() => setIsConfirmDeleteOpen(false)}
        onConfirm={() => selectedClass && deleteMutation.mutate(selectedClass.id)}
        title="Xóa lớp học"
        description="Bạn có chắc chắn muốn xóa lớp học này? Hành động này sẽ ảnh hưởng đến các học viên đã đăng ký vào lớp."
        isDanger
        isLoading={deleteMutation.isPending}
      />

      <ClassGradesModal 
        isOpen={isGradesOpen} 
        onClose={() => setIsGradesOpen(false)} 
        classId={selectedClass?.id || null} 
        className={selectedClass?.name || ''} 
      />
    </div>
  );
};
