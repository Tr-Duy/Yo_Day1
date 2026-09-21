import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell
} from 'recharts';
import {
  Users,
  BookOpen,
  School,
  Coins,
  AlertCircle,
  TrendingUp,
  Award,
  ChevronRight
} from 'lucide-react';
import { reportsApi } from '../features/reports/reports.api';

export const DashboardView: React.FC = () => {
  const navigate = useNavigate();
  const currentYear = 2026;
  const currentMonth = 6;

  // Fetch Dashboard Cards Statistics
  const { data: statsData, isLoading: isStatsLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: async () => reportsApi.getDashboardStats()
  });

  // Fetch Monthly Revenue
  const { data: monthlyRevData, isLoading: isMonthlyLoading } = useQuery({
    queryKey: ['monthly-revenue-chart', currentYear],
    queryFn: async () => reportsApi.getMonthlyRevenue(currentYear)
  });

  // Fetch Course Revenue
  const { data: courseRevData, isLoading: isCourseLoading } = useQuery({
    queryKey: ['course-revenue-chart', currentYear, currentMonth],
    queryFn: async () => reportsApi.getCourseRevenue({ year: currentYear, month: currentMonth })
  });

  const formatVND = (value: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
  };

  const chartMonthlyData = [...(monthlyRevData || [])]
    .sort((a, b) => a.month - b.month)
    .map(item => ({
      name: `Tháng ${item.month}`,
      'Phải thu': item.totalFinalAmount,
      'Thực thu': item.totalAmountPaid,
      'Còn nợ': item.totalBalance
    }));

  const chartCourseData = (courseRevData || [])
    .map(item => ({
      name: item.className,
      'Thực thu': item.totalAmountPaid,
      'Phải thu': item.totalFinalAmount
    }))
    .slice(0, 6);

  const BAR_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#3b82f6'];
  const stats = statsData as any;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-semibold text-foreground">Tổng quan</h2>
          <p className="text-sm text-foreground-muted mt-1">Kết quả tài chính, sĩ số lớp học và tiến độ đào tạo.</p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <StatCard
          title="Học viên"
          value={isStatsLoading ? '...' : stats?.studentsCount}
          subtitle="Đang tham gia học"
          icon={<Users size={18} className="text-blue-500" />}
        />
        <StatCard
          title="Môn học"
          value={isStatsLoading ? '...' : stats?.coursesCount}
          subtitle="Chương trình chuẩn"
          icon={<BookOpen size={18} className="text-violet-500" />}
        />
        <StatCard
          title="Lớp học"
          value={isStatsLoading ? '...' : stats?.classesCount}
          subtitle="Đang mở"
          icon={<School size={18} className="text-amber-500" />}
        />
        <StatCard
          title="Doanh thu tháng"
          value={isStatsLoading ? '...' : formatVND(Number(stats?.currentMonthRevenue))}
          subtitle="Thực nhận"
          icon={<Coins size={18} className="text-emerald-500" />}
        />
        <StatCard
          title="Hóa đơn nợ"
          value={isStatsLoading ? '...' : stats?.unpaidInvoicesCount}
          subtitle="Cần đôn đốc"
          icon={<AlertCircle size={18} className="text-rose-500" />}
          danger
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-surface border border-border rounded-xl p-5 shadow-sm transition-colors">
          <div className="mb-4">
            <h3 className="text-sm font-medium text-foreground flex items-center gap-2">
              <TrendingUp size={16} className="text-brand-600 dark:text-brand-400" />
              Doanh thu theo tháng
            </h3>
          </div>
          <div className="h-72 w-full">
            {isMonthlyLoading ? (
              <div className="h-full flex items-center justify-center text-sm text-foreground-muted">Đang tải...</div>
            ) : chartMonthlyData.length === 0 ? (
              <div className="h-full flex items-center justify-center text-sm text-foreground-muted">Chưa có dữ liệu</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartMonthlyData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="colorDue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366f1" stopOpacity={0.1} />
                      <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="text-border" vertical={false} />
                  <XAxis dataKey="name" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(v) => `${v / 1000000}M`} />
                  <Tooltip
                    contentStyle={{ backgroundColor: 'var(--color-surface, #ffffff)', borderColor: '#cbd5e1', borderRadius: '8px', fontSize: '13px' }}
                    formatter={(value: any) => [formatVND(value), '']}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} iconType="circle" />
                  <Area type="monotone" dataKey="Thực thu" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#colorRev)" />
                  <Area type="monotone" dataKey="Phải thu" stroke="#6366f1" strokeWidth={2} fillOpacity={1} fill="url(#colorDue)" />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="bg-surface border border-border rounded-xl p-5 shadow-sm transition-colors">
          <div className="mb-4">
            <h3 className="text-sm font-medium text-foreground flex items-center gap-2">
              <Award size={16} className="text-violet-500 dark:text-violet-400" />
              Đóng góp theo lớp
            </h3>
          </div>
          <div className="h-72 w-full">
            {isCourseLoading ? (
              <div className="h-full flex items-center justify-center text-sm text-foreground-muted">Đang tải...</div>
            ) : chartCourseData.length === 0 ? (
              <div className="h-full flex items-center justify-center text-sm text-foreground-muted">Chưa có dữ liệu</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartCourseData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="text-border" vertical={false} />
                  <XAxis dataKey="name" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(v) => `${v / 1000000}M`} />
                  <Tooltip
                    contentStyle={{ backgroundColor: 'var(--color-surface, #ffffff)', borderColor: '#cbd5e1', borderRadius: '8px', fontSize: '13px' }}
                    formatter={(value: any) => [formatVND(value), '']}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} iconType="circle" />
                  <Bar dataKey="Thực thu" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={40}>
                    {chartCourseData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={BAR_COLORS[index % BAR_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div>
        <h3 className="text-sm font-medium text-foreground-muted mb-3 uppercase tracking-wider">Truy cập nhanh</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <ActionCard title="Quản lý Học viên" desc="Tra cứu & đăng ký" path="/students" navigate={navigate} />
          <ActionCard title="Tài chính học phí" desc="Lập hóa đơn & đôn nợ" path="/billing" navigate={navigate} />
          <ActionCard title="Thanh toán" desc="Ghi nhận biên lai" path="/payments" navigate={navigate} />
          <ActionCard title="Báo cáo" desc="Tổng hợp tài chính" path="/reports" navigate={navigate} />
        </div>
      </div>
    </div>
  );
};

function StatCard({ title, value, subtitle, icon, danger = false }: any) {
  return (
    <div className={`bg-surface border ${danger ? 'border-rose-500/30' : 'border-border'} rounded-xl p-4 flex flex-col justify-between h-28 shadow-sm transition-colors`}>
      <div className="flex items-center justify-between text-foreground-muted">
        <span className="text-xs font-medium uppercase tracking-wider">{title}</span>
        {icon}
      </div>
      <div>
        <div className={`text-2xl font-bold ${danger ? 'text-rose-600 dark:text-rose-400' : 'text-foreground'} leading-none`}>{value}</div>
        <div className="text-xs text-foreground-muted mt-1">{subtitle}</div>
      </div>
    </div>
  );
}

function ActionCard({ title, desc, path, navigate }: any) {
  return (
    <button
      onClick={() => navigate(path)}
      className="flex items-center justify-between p-4 bg-surface border border-border hover:border-brand-500/50 hover:bg-surface-hover rounded-xl transition-all text-left group shadow-sm"
    >
      <div>
        <span className="text-sm font-medium text-foreground block">{title}</span>
        <span className="text-xs text-foreground-muted mt-0.5 block">{desc}</span>
      </div>
      <ChevronRight size={16} className="text-foreground-muted group-hover:text-brand-500 transition-colors" />
    </button>
  );
}

export default DashboardView;
