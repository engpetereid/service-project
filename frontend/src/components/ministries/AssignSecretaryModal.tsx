import React, { useState, useMemo } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { ministriesApi } from '../../api/ministries.api';
import { classesApi } from '../../api/classes.api';
import { servantsApi } from '../../api/servants.api';
import { Drawer } from '../ui/Drawer';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { Spinner } from '../ui/Spinner';
import { Badge } from '../ui/Badge';
import {
  UserCheck,
  Search,
  Check,
  Phone,
  UserX,
  AlertCircle,
  Sparkles,
} from 'lucide-react';

interface CurrentSecretaryInfo {
  id?: number | null;
  name?: string | null;
  phone?: string | null;
}

interface AssignSecretaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetType: 'ministry' | 'class';
  targetId: number;
  targetName: string;
  currentSecretary?: CurrentSecretaryInfo | null;
  ministryIdForClass?: number;
  onSuccess: () => void;
}

export const AssignSecretaryModal: React.FC<AssignSecretaryModalProps> = ({
  isOpen,
  onClose,
  targetType,
  targetId,
  targetName,
  currentSecretary,
  ministryIdForClass,
  onSuccess,
}) => {
  const [search, setSearch] = useState('');
  const [selectedPersonId, setSelectedPersonId] = useState<number | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [showUnassignConfirm, setShowUnassignConfirm] = useState(false);

  // Load candidate servants
  const { data: servants = [], isLoading: isLoadingServants } = useQuery({
    queryKey: ['candidate-servants', ministryIdForClass],
    queryFn: () => servantsApi.findAll({ ministryId: ministryIdForClass }),
    enabled: isOpen,
  });

  // Filter candidates
  const filteredServants = useMemo(() => {
    if (!search.trim()) return servants;
    const q = search.trim().toLowerCase();
    return servants.filter(
      (s) =>
        s.fullName.toLowerCase().includes(q) ||
        (s.phone && s.phone.includes(q))
    );
  }, [servants, search]);

  // Mutation to assign secretary
  const assignMutation = useMutation({
    mutationFn: async () => {
      if (!selectedPersonId && !selectedUserId) {
        throw new Error('يرجى اختيار خادم لتعيينه كأمين');
      }
      const payload = {
        personId: selectedPersonId,
        userId: selectedUserId,
      };

      if (targetType === 'ministry') {
        await ministriesApi.assignSecretary(targetId, payload);
      } else {
        await classesApi.assignSecretary(targetId, payload);
      }
    },
    onSuccess: () => {
      onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'حدث خطأ أثناء تعيين الأمين');
    },
  });

  // Mutation to remove secretary
  const removeMutation = useMutation({
    mutationFn: async () => {
      if (targetType === 'ministry') {
        await ministriesApi.removeSecretary(targetId);
      } else {
        await classesApi.removeSecretary(targetId);
      }
    },
    onSuccess: () => {
      onSuccess();
      handleClose();
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'حدث خطأ أثناء إلغاء تعيين الأمين');
    },
  });

  const handleClose = () => {
    setSearch('');
    setSelectedPersonId(null);
    setSelectedUserId(null);
    setErrorMsg(null);
    setShowUnassignConfirm(false);
    onClose();
  };

  const handleSelectServant = (personId: number, userId?: number | null) => {
    setSelectedPersonId(personId);
    setSelectedUserId(userId ?? null);
    setErrorMsg(null);
  };

  const isCurrent = (personId: number) => {
    return currentSecretary?.id === personId;
  };

  const roleTitle = targetType === 'ministry' ? 'أمين الخدمة' : 'أمين الفصل';

  return (
    <Drawer
      isOpen={isOpen}
      onClose={handleClose}
      title={`تعيين ${roleTitle} — ${targetName}`}
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" onClick={handleClose} disabled={assignMutation.isPending || removeMutation.isPending}>
            إلغاء
          </Button>
          <Button
            variant="primary"
            onClick={() => assignMutation.mutate()}
            isLoading={assignMutation.isPending}
            disabled={!selectedPersonId || removeMutation.isPending}
            className="font-bold"
          >
            <Check className="w-4 h-4 ml-1.5" />
            حفظ التعيين
          </Button>
        </div>
      }
    >
      <div className="space-y-5">
        {errorMsg && <Alert variant="error">{errorMsg}</Alert>}

        {/* Current Secretary Box */}
        <div className="p-4 rounded-xl bg-gray-50 border border-gray-100">
          <span className="text-xs font-bold text-gray-500 block mb-2">الأمين الحالي:</span>
          {currentSecretary && currentSecretary.name ? (
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-sm">
                  {currentSecretary.name.charAt(0)}
                </div>
                <div>
                  <h4 className="font-bold text-gray-900 text-sm">{currentSecretary.name}</h4>
                  {currentSecretary.phone && (
                    <p className="text-xs text-gray-500 flex items-center gap-1 mt-0.5" dir="ltr">
                      <Phone className="w-3 h-3 text-gray-400" />
                      {currentSecretary.phone}
                    </p>
                  )}
                </div>
              </div>

              {showUnassignConfirm ? (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-danger-600 font-bold">تأكيد الإلغاء؟</span>
                  <Button
                    size="sm"
                    variant="danger"
                    onClick={() => removeMutation.mutate()}
                    isLoading={removeMutation.isPending}
                  >
                    نعم
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setShowUnassignConfirm(false)}
                  >
                    لا
                  </Button>
                </div>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setShowUnassignConfirm(true)}
                  className="text-xs text-danger-600 border-danger-200 hover:bg-danger-50"
                >
                  <UserX className="w-3.5 h-3.5 ml-1" />
                  إلغاء التعيين
                </Button>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2 text-amber-700 bg-amber-50 p-2.5 rounded-lg text-xs font-semibold">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              لا يوجد أمين معين حالياً. اختر خادماً من القائمة أدناه لتعيينه.
            </div>
          )}
        </div>

        {/* Search Input */}
        <div>
          <label className="text-xs font-bold text-gray-700 block mb-1.5">
            اختر الخادم المرشح:
          </label>
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute right-3 top-3" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="ابحث باسم الخادم أو رقم الهاتف..."
              className="pr-9 text-xs"
            />
          </div>
        </div>

        {/* Candidate List */}
        <div className="space-y-2 max-h-[350px] overflow-y-auto pr-1">
          {isLoadingServants ? (
            <div className="py-8 text-center">
              <Spinner size="md" />
              <p className="text-xs text-gray-400 mt-2 font-medium">جاري تحميل الخدام...</p>
            </div>
          ) : filteredServants.length === 0 ? (
            <div className="text-center py-8 bg-gray-50 rounded-xl border border-dashed border-gray-200">
              <UserCheck className="w-8 h-8 text-gray-300 mx-auto mb-2" />
              <p className="text-xs text-gray-500 font-semibold">لم يتم العثور على خدام مطابقين للبحث</p>
              <p className="text-[11px] text-gray-400 mt-1">
                تأكد من تسجيل الخادم أولاً في شاشة إدارة الخدام
              </p>
            </div>
          ) : (
            filteredServants.map((servant) => {
              const personId = servant.personId || servant.id!;
              const isSelected = selectedPersonId === personId;
              const isAlreadyCurrent = isCurrent(personId);

              return (
                <div
                  key={personId}
                  onClick={() => handleSelectServant(personId, servant.userId)}
                  className={`p-3.5 rounded-xl border transition cursor-pointer flex items-center justify-between ${
                    isSelected
                      ? 'border-primary-500 bg-primary-50/60 ring-2 ring-primary-500/20'
                      : isAlreadyCurrent
                      ? 'border-emerald-200 bg-emerald-50/40 opacity-80'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs ${
                        isSelected
                          ? 'bg-primary-600 text-white'
                          : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {servant.fullName.charAt(0)}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-gray-900 text-xs">
                          {servant.fullName}
                        </span>
                        {isAlreadyCurrent && (
                          <Badge variant="success" className="text-[10px] px-1.5 py-0.5">
                            الأمين الحالي
                          </Badge>
                        )}
                      </div>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[11px] text-gray-500" dir="ltr">
                          {servant.phone}
                        </span>
                        {servant.className && (
                          <span className="text-[10px] text-gray-400">
                            • {servant.className}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {servant.userId ? (
                      <span className="inline-flex items-center text-[10px] text-emerald-700 bg-emerald-100/70 font-semibold px-2 py-0.5 rounded-md">
                        حساب مفعل
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[10px] text-blue-700 bg-blue-100/70 font-semibold px-2 py-0.5 rounded-md">
                        <Sparkles className="w-2.5 h-2.5" />
                        تفعيل تلقائي
                      </span>
                    )}

                    <div
                      className={`w-5 h-5 rounded-full border flex items-center justify-center transition ${
                        isSelected
                          ? 'border-primary-600 bg-primary-600 text-white'
                          : 'border-gray-300'
                      }`}
                    >
                      {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="p-3 bg-blue-50/70 rounded-xl border border-blue-100 text-[11px] text-blue-800 space-y-1">
          <p className="font-bold flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5 text-blue-600" />
            ملاحظة مهمة بشأن الحسابات والصلاحيات:
          </p>
          <p className="text-blue-700">
            عند تعيين الخادم، سيحصل فوراً على صلاحية {roleTitle}. إذا لم يكن لديه حساب تسجيل دخول سابقاً، سيتم تفعيل حسابه تلقائياً برقم هاتفه ككلمة مرور مبدئية.
          </p>
        </div>
      </div>
    </Drawer>
  );
};
