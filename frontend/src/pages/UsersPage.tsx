import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../api/users.api';
import { ministriesApi } from '../api/ministries.api';
import { classesApi } from '../api/classes.api';
import { UserResponse, RoleAssignment } from '../types/user.types';
import { Role } from '../types/auth.types';
import { CreateUserModal } from '../components/users/CreateUserModal';
import { ResetPasswordModal } from '../components/users/ResetPasswordModal';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Drawer } from '../components/ui/Drawer';
import { Alert } from '../components/ui/Alert';
import { Spinner } from '../components/ui/Spinner';
import { EmptyState } from '../components/ui/EmptyState';
import { ROLE_LABELS, getRoleBadgeClass } from '../utils/arabic';
import { useDebounce } from '../hooks/useDebounce';
import {
  UserCheck,
  Shield,
  Search,
  CheckCircle2,
  XCircle,
  Plus,
  Trash2,
  KeyRound,
  Sparkles,
} from 'lucide-react';

export const UsersPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [roleFilter, setRoleFilter] = useState<Role | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [resetPasswordUser, setResetPasswordUser] = useState<UserResponse | null>(null);

  // Role Drawer state
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null);
  const [isRoleDrawerOpen, setIsRoleDrawerOpen] = useState(false);
  const [assignedRoles, setAssignedRoles] = useState<RoleAssignment[]>([]);
  const [roleError, setRoleError] = useState<string | null>(null);

  // Queries
  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.findAll(),
  });

  const { data: ministries = [] } = useQuery({
    queryKey: ['ministries'],
    queryFn: () => ministriesApi.findAll(),
    enabled: isRoleDrawerOpen,
  });

  const { data: allClasses = [] } = useQuery({
    queryKey: ['classes', 'all'],
    queryFn: () => classesApi.findAll(),
    enabled: isRoleDrawerOpen,
  });

  // KPI Summary
  const stats = useMemo(() => {
    const total = users.length;
    const active = users.filter((u) => u.enabled).length;
    const inactive = total - active;
    const admins = users.filter((u) => u.roles.some((r) => r.role === 'GENERAL_ADMIN')).length;
    const serviceSecs = users.filter((u) => u.roles.some((r) => r.role === 'SERVICE_SECRETARY')).length;
    const classSecs = users.filter((u) => u.roles.some((r) => r.role === 'CLASS_SECRETARY')).length;

    return { total, active, inactive, admins, serviceSecs, classSecs };
  }, [users]);

  // Filter users by search, role, status
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      // Search
      const q = debouncedSearch.toLowerCase().trim();
      if (q && !u.fullName.toLowerCase().includes(q) && !u.phone.includes(q)) {
        return false;
      }
      // Status
      if (statusFilter === 'ACTIVE' && !u.enabled) return false;
      if (statusFilter === 'INACTIVE' && u.enabled) return false;
      // Role
      if (roleFilter !== 'ALL' && !u.roles.some((r) => r.role === roleFilter)) {
        return false;
      }
      return true;
    });
  }, [users, debouncedSearch, statusFilter, roleFilter]);

  // Mutations
  const toggleActiveMutation = useMutation({
    mutationFn: (userId: number) => usersApi.toggleActive(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
    },
  });

  const rolesMutation = useMutation({
    mutationFn: async () => {
      if (!selectedUser) return;
      return usersApi.assignRoles(selectedUser.userId, { roles: assignedRoles });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['ministries'] });
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      queryClient.invalidateQueries({ queryKey: ['admin-setup'] });
      setIsRoleDrawerOpen(false);
    },
    onError: (err: Error) => {
      setRoleError(err.message || 'فشل تحديث الأدوار');
    },
  });

  const handleOpenRoleDrawer = (user: UserResponse) => {
    setSelectedUser(user);
    setAssignedRoles(
      user.roles.map((r) => ({
        role: r.role,
        ministryId: r.ministryId,
        classId: r.classId,
      }))
    );
    setRoleError(null);
    setIsRoleDrawerOpen(true);
  };

  const handleAddRole = (role: Role) => {
    if (assignedRoles.some((r) => r.role === role)) return;
    setAssignedRoles([...assignedRoles, { role, ministryId: null, classId: null }]);
  };

  const handleRemoveRole = (index: number) => {
    setAssignedRoles(assignedRoles.filter((_, i) => i !== index));
  };

  const handleUpdateRoleScope = (
    index: number,
    field: 'ministryId' | 'classId',
    value: number | null
  ) => {
    const updated = [...assignedRoles];
    updated[index] = { ...updated[index], [field]: value };
    setAssignedRoles(updated);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">
            إدارة حسابات المستخدمين والصلاحيات
          </h1>
          <p className="text-xs text-gray-500 mt-1">
            إدارة حسابات تسجيل الدخول، تفعيل الحسابات للخدام، وتعيين الأدوار والنطاقات المصرح بها
          </p>
        </div>

        <Button
          variant="primary"
          onClick={() => setIsCreateModalOpen(true)}
          className="shadow-sm font-bold self-start sm:self-auto"
        >
          <Plus className="w-4 h-4 ml-2" />
          إضافة مستخدم جديد
        </Button>
      </div>

      {/* Educational Guidance Box */}
      <div className="p-3.5 bg-blue-50/80 border border-blue-100 rounded-2xl flex items-start gap-3">
        <div className="w-8 h-8 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center flex-shrink-0 mt-0.5">
          <Sparkles className="w-4 h-4" />
        </div>
        <div className="space-y-0.5 text-xs text-blue-900">
          <p className="font-bold">مفهوم الحسابات والأشخاص في النظام:</p>
          <p className="text-blue-700 leading-relaxed">
            لكل خادم أو مستخدم شخص (Person) وحساب تسجيل دخول (UserAccount). اسم المستخدم دائماً هو{' '}
            <strong>رقم الهاتف</strong>. يمكنك إنشاء حساب فوراً لأي خادم مسجل، أو إضافة مستخدم جديد مباشرة، كما يمكنك إعادة تعيين كلمة المرور لأي خادم بنقرة زر واحدة.
          </p>
        </div>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm">
          <span className="text-[11px] text-gray-400 font-bold block">إجمالي الحسابات</span>
          <span className="text-xl font-black text-gray-900 mt-0.5 block">{stats.total}</span>
        </div>

        <div
          onClick={() => setStatusFilter('ACTIVE')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-emerald-200 transition"
        >
          <span className="text-[11px] text-emerald-600 font-bold block">حسابات نشطة</span>
          <span className="text-xl font-black text-emerald-700 mt-0.5 block">{stats.active}</span>
        </div>

        <div
          onClick={() => setStatusFilter('INACTIVE')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-danger-200 transition"
        >
          <span className="text-[11px] text-danger-600 font-bold block">حسابات معطلة</span>
          <span className="text-xl font-black text-danger-700 mt-0.5 block">{stats.inactive}</span>
        </div>

        <div
          onClick={() => setRoleFilter('GENERAL_ADMIN')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-purple-200 transition"
        >
          <span className="text-[11px] text-purple-600 font-bold block">مديرو النظام</span>
          <span className="text-xl font-black text-purple-700 mt-0.5 block">{stats.admins}</span>
        </div>

        <div
          onClick={() => setRoleFilter('SERVICE_SECRETARY')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-blue-200 transition"
        >
          <span className="text-[11px] text-blue-600 font-bold block">أمناء الخدمات</span>
          <span className="text-xl font-black text-blue-700 mt-0.5 block">{stats.serviceSecs}</span>
        </div>

        <div
          onClick={() => setRoleFilter('CLASS_SECRETARY')}
          className="bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm cursor-pointer hover:border-primary-200 transition"
        >
          <span className="text-[11px] text-primary-600 font-bold block">أمناء الفصول</span>
          <span className="text-xl font-black text-primary-700 mt-0.5 block">{stats.classSecs}</span>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border border-gray-100 shadow-sm">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-gray-400 absolute right-3 top-2.5" />
          <Input
            placeholder="بحث بالاسم أو رقم الهاتف..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pr-9 text-xs"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Role Filter Pills */}
          <div className="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-100 text-xs">
            <button
              onClick={() => setRoleFilter('ALL')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                roleFilter === 'ALL' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              الكل
            </button>
            <button
              onClick={() => setRoleFilter('GENERAL_ADMIN')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                roleFilter === 'GENERAL_ADMIN'
                  ? 'bg-white text-purple-700 shadow-sm'
                  : 'text-gray-500 hover:text-purple-700'
              }`}
            >
              أدمن
            </button>
            <button
              onClick={() => setRoleFilter('SERVICE_SECRETARY')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                roleFilter === 'SERVICE_SECRETARY'
                  ? 'bg-white text-blue-700 shadow-sm'
                  : 'text-gray-500 hover:text-blue-700'
              }`}
            >
              أمين خدمة
            </button>
            <button
              onClick={() => setRoleFilter('CLASS_SECRETARY')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                roleFilter === 'CLASS_SECRETARY'
                  ? 'bg-white text-primary-700 shadow-sm'
                  : 'text-gray-500 hover:text-primary-700'
              }`}
            >
              أمين فصل
            </button>
            <button
              onClick={() => setRoleFilter('SERVANT')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                roleFilter === 'SERVANT' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              خادم
            </button>
          </div>

          {/* Status Filter */}
          <div className="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-100 text-xs">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                statusFilter === 'ALL' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
              }`}
            >
              جميع الحالات
            </button>
            <button
              onClick={() => setStatusFilter('ACTIVE')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                statusFilter === 'ACTIVE' ? 'bg-white text-emerald-700 shadow-sm' : 'text-gray-500'
              }`}
            >
              نشط
            </button>
            <button
              onClick={() => setStatusFilter('INACTIVE')}
              className={`px-2.5 py-1 rounded-lg font-bold transition ${
                statusFilter === 'INACTIVE' ? 'bg-white text-danger-700 shadow-sm' : 'text-gray-500'
              }`}
            >
              معطل
            </button>
          </div>
        </div>
      </div>

      {/* Content Area */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <Spinner size="lg" />
          <p className="text-xs text-gray-500 mt-3 font-medium">جاري تحميل حسابات المستخدمين...</p>
        </div>
      ) : filteredUsers.length === 0 ? (
        <EmptyState
          icon={UserCheck}
          title="لا توجد حسابات مطابقة"
          description="لم يتم العثور على مستخدمين يطابقون خيارات البحث أو التصفية الحالية."
          actionLabel="إضافة مستخدم جديد"
          onAction={() => setIsCreateModalOpen(true)}
          actionIcon={Plus}
        />
      ) : (
        <Card className="p-0 overflow-hidden border-gray-200 shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100 uppercase">
                <tr>
                  <th className="py-3.5 px-4">المستخدم / الخادم</th>
                  <th className="py-3.5 px-4">اسم المستخدم (الهاتف)</th>
                  <th className="py-3.5 px-4">الأدوار والصلاحيات</th>
                  <th className="py-3.5 px-4 text-center">حالة الحساب</th>
                  <th className="py-3.5 px-4 text-center">إجراءات الحساب</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredUsers.map((u) => (
                  <tr key={u.userId} className="hover:bg-gray-50/70 transition">
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-xs">
                          {u.fullName.charAt(0)}
                        </div>
                        <div>
                          <span className="font-bold text-gray-900 text-sm block">{u.fullName}</span>
                          <span className="text-[10px] text-gray-400">ID: {u.userId}</span>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 font-mono text-gray-700 font-bold" dir="ltr">
                      {u.phone}
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="flex flex-wrap items-center gap-1.5">
                        {u.roles.length === 0 ? (
                          <span className="text-gray-400 text-[11px]">بدون أدوار</span>
                        ) : (
                          u.roles.map((r, i) => (
                            <span
                              key={i}
                              className={`inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold border ${getRoleBadgeClass(
                                r.role
                              )}`}
                            >
                              {ROLE_LABELS[r.role]}
                              {r.ministryId && ` (${r.ministryId})`}
                              {r.classId && ` (${r.classId})`}
                            </span>
                          ))
                        )}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <button
                        onClick={() => toggleActiveMutation.mutate(u.userId)}
                        disabled={toggleActiveMutation.isPending}
                        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold transition ${
                          u.enabled
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
                            : 'bg-red-50 text-red-700 border border-red-200 hover:bg-red-100'
                        }`}
                        title="انقر لتغيير حالة الحساب"
                      >
                        {u.enabled ? (
                          <>
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            مفعل
                          </>
                        ) : (
                          <>
                            <XCircle className="w-3.5 h-3.5" />
                            معطل
                          </>
                        )}
                      </button>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleOpenRoleDrawer(u)}
                          className="text-[11px] font-bold py-1 px-2.5 h-auto"
                        >
                          <Shield className="w-3.5 h-3.5 ml-1" />
                          الصلاحيات
                        </Button>

                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setResetPasswordUser(u)}
                          className="text-[11px] font-bold text-gray-500 hover:text-gray-800 py-1 px-2 h-auto"
                          title="إعادة تعيين كلمة المرور"
                        >
                          <KeyRound className="w-3.5 h-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Role Management Drawer */}
      <Drawer
        isOpen={isRoleDrawerOpen}
        onClose={() => setIsRoleDrawerOpen(false)}
        title={`تعديل صلاحيات: ${selectedUser?.fullName || ''}`}
        footer={
          <div className="flex items-center justify-between w-full">
            <Button
              variant="outline"
              onClick={() => setIsRoleDrawerOpen(false)}
              disabled={rolesMutation.isPending}
            >
              إلغاء
            </Button>
            <Button
              variant="primary"
              onClick={() => rolesMutation.mutate()}
              isLoading={rolesMutation.isPending}
              className="font-bold"
            >
              حفظ الصلاحيات
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          {roleError && <Alert variant="error">{roleError}</Alert>}

          <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-between text-xs">
            <span className="text-gray-500 font-bold">المستخدم:</span>
            <span className="font-bold text-gray-900">{selectedUser?.fullName}</span>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-gray-700 block">إضافة دور جديد:</label>
            <div className="flex flex-wrap gap-1.5">
              {(['SERVANT', 'SERVICE_SECRETARY', 'CLASS_SECRETARY', 'GENERAL_ADMIN'] as Role[]).map(
                (r) => (
                  <Button
                    key={r}
                    size="sm"
                    variant="outline"
                    onClick={() => handleAddRole(r)}
                    disabled={assignedRoles.some((ro) => ro.role === r)}
                    className="text-xs font-bold py-1 px-2 h-auto"
                  >
                    + {ROLE_LABELS[r]}
                  </Button>
                )
              )}
            </div>
          </div>

          <div className="space-y-3 pt-2">
            <label className="text-xs font-bold text-gray-700 block">الأدوار الممنوحة حالياً:</label>
            {assignedRoles.length === 0 ? (
              <p className="text-xs text-gray-400 p-3 bg-gray-50 rounded-xl text-center">
                لا توجد أدوار محددة لهذا المستخدم.
              </p>
            ) : (
              assignedRoles.map((r, index) => (
                <div
                  key={index}
                  className="p-3 bg-gray-50 rounded-xl border border-gray-100 space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-900">
                      {ROLE_LABELS[r.role]}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleRemoveRole(index)}
                      className="text-gray-400 hover:text-danger-600 p-1"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {r.role === 'SERVICE_SECRETARY' && (
                    <Select
                      label="الخدمة التابعة *"
                      value={r.ministryId || ''}
                      onChange={(e) =>
                        handleUpdateRoleScope(
                          index,
                          'ministryId',
                          e.target.value ? Number(e.target.value) : null
                        )
                      }
                      options={ministries.map((m) => ({ value: m.id, label: m.name }))}
                      placeholder="اختر الخدمة..."
                    />
                  )}

                  {r.role === 'CLASS_SECRETARY' && (
                    <Select
                      label="الفصل التابع *"
                      value={r.classId || ''}
                      onChange={(e) =>
                        handleUpdateRoleScope(
                          index,
                          'classId',
                          e.target.value ? Number(e.target.value) : null
                        )
                      }
                      options={allClasses.map((c) => ({
                        value: c.id,
                        label: `${c.name} (${c.ministryName})`,
                      }))}
                      placeholder="اختر الفصل..."
                    />
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </Drawer>

      {/* Create User Modal */}
      <CreateUserModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['users'] });
        }}
      />

      {/* Reset Password Modal */}
      <ResetPasswordModal
        isOpen={!!resetPasswordUser}
        onClose={() => setResetPasswordUser(null)}
        user={resetPasswordUser}
      />
    </div>
  );
};
