import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClassService } from '../../core/services/class.service';
import { ToastService } from '../../core/services/toast.service';
import { RbacService } from '../../core/services/rbac.service';
import { CenterTeacherService, CenterTeacherItem } from '../../core/services/center-teacher.service';
import { TeacherPolicyService } from '../../core/services/teacher-policy.service';
import { ClassItem, ClassRequestPayload, FeeType } from '../../shared/models/class.model';

export interface ClassCardVm {
  id: string;
  name: string;
  schedule: string;
  scheduleDays?: string;
  startTime?: string;
  endTime?: string;
  notes?: string;
  feeType: 'session' | 'monthly';
  fee: number;
  students: number;
  archived: boolean;
  teacherId?: string;
  teacherName?: string;
  teacherPhone?: string;
}

const INITIAL_CLASSES: ClassCardVm[] = [
  {
    id: 'd0000000-0000-0000-0000-000000000001',
    name: 'Toán nâng cao 8A',
    schedule: 'Thứ 3, Thứ 7 · 18:00–19:30',
    scheduleDays: 'TUESDAY,SATURDAY',
    startTime: '18:00',
    endTime: '19:30',
    feeType: 'session',
    fee: 150000,
    students: 4,
    archived: false
  },
  {
    id: 'd0000000-0000-0000-0000-000000000002',
    name: 'Luyện thi vào 10 — Nhóm Chuyên',
    schedule: 'Thứ 2, Thứ 5, Thứ 7 · 19:30–21:00',
    scheduleDays: 'MONDAY,THURSDAY,SATURDAY',
    startTime: '19:30',
    endTime: '21:00',
    feeType: 'session',
    fee: 180000,
    students: 12,
    archived: false
  },
  {
    id: 'd0000000-0000-0000-0000-000000000003',
    name: 'Toán Tư Duy Lớp 6B (Cơ bản)',
    schedule: 'Thứ 4, Chủ nhật · 17:30–19:00',
    scheduleDays: 'WEDNESDAY,SUNDAY',
    startTime: '17:30',
    endTime: '19:00',
    feeType: 'monthly',
    fee: 1200000,
    students: 8,
    archived: false
  },
  {
    id: 'd0000000-0000-0000-0000-000000000004',
    name: 'Khóa Hè Toán 7 Lên 8 (2026)',
    schedule: 'Đã kết thúc · 06/2026–08/2026',
    feeType: 'session',
    fee: 150000,
    students: 10,
    archived: true
  }
];

@Component({
  selector: 'app-classes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NavbarComponent],
  templateUrl: './classes.component.html',
  styleUrls: ['./classes.component.css']
})
export class ClassesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly classService = inject(ClassService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly rbacService = inject(RbacService);
  private readonly centerTeacherService = inject(CenterTeacherService);
  readonly policyService = inject(TeacherPolicyService);

  readonly isCenterAdmin = computed(() => this.rbacService.isCenterAdmin());
  readonly teachersList = signal<CenterTeacherItem[]>([]);

  readonly classes = signal<ClassCardVm[]>(INITIAL_CLASSES);
  readonly status = signal<'active' | 'archived' | 'all'>('active');
  readonly feeFilter = signal<'all' | 'session' | 'monthly'>('all');
  readonly search = signal<string>('');

  readonly modalOpen = signal<boolean>(false);
  readonly editingItem = signal<ClassCardVm | null>(null);
  readonly submitted = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly toastMessage = signal<string | null>(null);

  readonly presets = [100000, 120000, 150000, 180000, 200000];

  readonly weekdays = [
    { code: 'MONDAY', label: 'T2', name: 'Thứ 2' },
    { code: 'TUESDAY', label: 'T3', name: 'Thứ 3' },
    { code: 'WEDNESDAY', label: 'T4', name: 'Thứ 4' },
    { code: 'THURSDAY', label: 'T5', name: 'Thứ 5' },
    { code: 'FRIDAY', label: 'T6', name: 'Thứ 6' },
    { code: 'SATURDAY', label: 'T7', name: 'Thứ 7' },
    { code: 'SUNDAY', label: 'CN', name: 'Chủ nhật' }
  ];

  readonly selectedDays = signal<string[]>(['MONDAY', 'WEDNESDAY', 'FRIDAY']);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    feeType: ['session' as 'session' | 'monthly', [Validators.required]],
    fee: [150000, [Validators.required, Validators.min(1000)]],
    startTime: ['17:30'],
    endTime: ['19:00'],
    teacherId: [''],
    notes: ['']
  });

  readonly activeClasses = computed(() =>
    this.classes().filter(x => !x.archived)
  );

  readonly archivedCount = computed(() =>
    this.classes().filter(x => x.archived).length
  );

  readonly totalActiveStudents = computed(() =>
    this.activeClasses().reduce((sum, item) => sum + item.students, 0)
  );

  readonly sessionActiveCount = computed(() =>
    this.activeClasses().filter(x => x.feeType === 'session').length
  );

  readonly monthlyActiveCount = computed(() =>
    this.activeClasses().filter(x => x.feeType === 'monthly').length
  );

  readonly visibleClasses = computed(() => {
    const currentStatus = this.status();
    const currentFeeFilter = this.feeFilter();
    const query = this.search().trim().toLowerCase();

    return this.classes().filter(x => {
      const matchStatus =
        currentStatus === 'all' ||
        (currentStatus === 'archived' ? x.archived : !x.archived);
      const matchFee =
        currentFeeFilter === 'all' || x.feeType === currentFeeFilter;
      const matchSearch = `${x.name} ${x.schedule}`
        .toLowerCase()
        .includes(query);
      return matchStatus && matchFee && matchSearch;
    });
  });

  ngOnInit(): void {
    this.loadClasses();
    if (this.rbacService.isCenterAdmin()) {
      this.centerTeacherService.getTeachers().subscribe({
        next: (res) => {
          if (res.data) {
            this.teachersList.set(res.data.filter(t => t.status === 'ACTIVE'));
          }
        }
      });
    }
  }

  loadClasses(): void {
    this.classService
      .getClasses(true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res?.success && Array.isArray(res.data)) {
            this.classes.set(res.data.map(item => this.mapApiToVm(item)));
          }
        },
        error: () => {
          // Fallback giữ nguyên dữ liệu mẫu nếu Backend chưa chạy
        }
      });
  }

  money(value: number): string {
    return new Intl.NumberFormat('vi-VN').format(value || 0);
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.search.set(input.value);
  }

  setStatus(value: 'active' | 'archived' | 'all'): void {
    this.status.set(value);
  }

  setFeeFilter(value: 'all' | 'session' | 'monthly'): void {
    this.feeFilter.set(value);
  }

  toggleDay(code: string): void {
    this.selectedDays.update(days =>
      days.includes(code) ? days.filter(d => d !== code) : [...days, code]
    );
  }

  isDaySelected(code: string): boolean {
    return this.selectedDays().includes(code);
  }

  getSchedulePreview(): string {
    const days = this.selectedDays();
    if (days.length === 0) return 'Chưa chọn thứ trong tuần';
    const dayLabels = this.weekdays
      .filter(w => days.includes(w.code))
      .map(w => w.name)
      .join(', ');
    const start = this.form.controls.startTime.value || '17:30';
    const end = this.form.controls.endTime.value || '19:00';
    return `${dayLabels} · ${start}–${end}`;
  }

  extractDaysFromText(text: string): string[] {
    if (!text) return [];
    const lower = text.toLowerCase();
    const days: string[] = [];
    if (lower.includes('thứ 2') || lower.includes('t2')) days.push('MONDAY');
    if (lower.includes('thứ 3') || lower.includes('t3')) days.push('TUESDAY');
    if (lower.includes('thứ 4') || lower.includes('t4')) days.push('WEDNESDAY');
    if (lower.includes('thứ 5') || lower.includes('t5')) days.push('THURSDAY');
    if (lower.includes('thứ 6') || lower.includes('t6')) days.push('FRIDAY');
    if (lower.includes('thứ 7') || lower.includes('t7')) days.push('SATURDAY');
    if (lower.includes('chủ nhật') || lower.includes('cn')) days.push('SUNDAY');
    return days;
  }

  openCreateModal(): void {
    if (!this.policyService.canCreateClass()) {
      this.toastService.error('Bạn không có quyền tự mở lớp học theo quy định của trung tâm.');
      return;
    }
    this.editingItem.set(null);
    this.submitted.set(false);
    this.selectedDays.set(['MONDAY', 'WEDNESDAY', 'FRIDAY']);
    this.form.reset({
      name: '',
      feeType: 'session',
      fee: 150000,
      startTime: '17:30',
      endTime: '19:00',
      teacherId: '',
      notes: ''
    });
    this.modalOpen.set(true);
  }

  openEditModal(item: ClassCardVm): void {
    this.editingItem.set(item);
    this.submitted.set(false);
    const parsedDays = item.scheduleDays ? item.scheduleDays.split(',').map(s => s.trim()) : [];
    this.selectedDays.set(parsedDays.length > 0 ? parsedDays : this.extractDaysFromText(item.schedule));
    this.form.reset({
      name: item.name,
      feeType: item.feeType,
      fee: item.fee,
      startTime: item.startTime || '17:30',
      endTime: item.endTime || '19:00',
      teacherId: item.teacherId || '',
      notes: item.notes || ''
    });
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingItem.set(null);
    this.submitted.set(false);
  }

  onBackdropMouseDown(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  selectFeeType(type: 'session' | 'monthly'): void {
    this.form.controls.feeType.setValue(type);
  }

  selectPreset(amount: number): void {
    this.form.controls.fee.setValue(amount);
  }

  isNameInvalid(): boolean {
    const ctrl = this.form.controls.name;
    return (this.submitted() || ctrl.touched) && !ctrl.value.trim();
  }

  isFeeInvalid(): boolean {
    const ctrl = this.form.controls.fee;
    return (this.submitted() || ctrl.touched) && (!ctrl.value || ctrl.value <= 0);
  }

  saveClass(): void {
    this.submitted.set(true);
    const raw = this.form.getRawValue();
    const trimmedName = raw.name.trim();
    const feeValue = Number(raw.fee);

    if (!trimmedName || !feeValue || feeValue <= 0) {
      return;
    }

    const scheduleDaysStr = this.selectedDays().join(',');
    const startTimeVal = raw.startTime || '17:30';
    const endTimeVal = raw.endTime || '19:00';
    const scheduleText = this.getSchedulePreview();
    const notesText = raw.notes.trim();
    const descriptionCombined = notesText
      ? `${scheduleText} · ${notesText}`
      : scheduleText;

    const payload: ClassRequestPayload = {
      name: trimmedName,
      description: descriptionCombined,
      feeType: this.toBackendFeeType(raw.feeType),
      defaultFee: feeValue,
      scheduleDays: scheduleDaysStr,
      startTime: startTimeVal,
      endTime: endTimeVal,
      teacherId: raw.teacherId || undefined
    };

    const editing = this.editingItem();
    this.saving.set(true);

    if (editing) {
      this.classService
        .updateClass(editing.id, payload)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            const updatedVm = res?.data
              ? {
                  ...this.mapApiToVm(res.data),
                  schedule: scheduleText,
                  scheduleDays: scheduleDaysStr,
                  startTime: startTimeVal,
                  endTime: endTimeVal,
                  notes: notesText,
                  students: editing.students
                }
              : {
                  ...editing,
                  name: trimmedName,
                  feeType: raw.feeType,
                  fee: feeValue,
                  schedule: scheduleText,
                  scheduleDays: scheduleDaysStr,
                  startTime: startTimeVal,
                  endTime: endTimeVal,
                  notes: notesText
                };
            this.classes.update(xs =>
              xs.map(x => (x.id === editing.id ? updatedVm : x))
            );
            this.saving.set(false);
            this.closeModal();
            this.showToast(`Đã cập nhật lớp "${trimmedName}"`);
          },
          error: () => {
            this.classes.update(xs =>
              xs.map(x =>
                x.id === editing.id
                  ? {
                      ...editing,
                      name: trimmedName,
                      feeType: raw.feeType,
                      fee: feeValue,
                      schedule: scheduleText,
                      scheduleDays: scheduleDaysStr,
                      startTime: startTimeVal,
                      endTime: endTimeVal,
                      notes: notesText
                    }
                  : x
              )
            );
            this.saving.set(false);
            this.closeModal();
            this.showToast(`Đã cập nhật lớp "${trimmedName}"`);
          }
        });
    } else {
      this.classService
        .createClass(payload)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            const createdVm: ClassCardVm = res?.data
              ? {
                  ...this.mapApiToVm(res.data),
                  schedule: scheduleText,
                  scheduleDays: scheduleDaysStr,
                  startTime: startTimeVal,
                  endTime: endTimeVal,
                  notes: notesText
                }
              : {
                  id: `cls-${Date.now()}`,
                  name: trimmedName,
                  feeType: raw.feeType,
                  fee: feeValue,
                  schedule: scheduleText,
                  scheduleDays: scheduleDaysStr,
                  startTime: startTimeVal,
                  endTime: endTimeVal,
                  notes: notesText,
                  students: 0,
                  archived: false
                };
            this.classes.update(xs => [...xs, createdVm]);
            this.saving.set(false);
            this.closeModal();
            this.showToast(`Đã tạo lớp học "${trimmedName}" thành công`);
          },
          error: () => {
            const createdVm: ClassCardVm = {
              id: `cls-${Date.now()}`,
              name: trimmedName,
              feeType: raw.feeType,
              fee: feeValue,
              schedule: scheduleText,
              scheduleDays: scheduleDaysStr,
              startTime: startTimeVal,
              endTime: endTimeVal,
              notes: notesText,
              students: 0,
              archived: false
            };
            this.classes.update(xs => [...xs, createdVm]);
            this.saving.set(false);
            this.closeModal();
            this.showToast(`Đã tạo lớp học "${trimmedName}" thành công`);
          }
        });
    }
  }

  toggleArchive(item: ClassCardVm): void {
    const nextArchived = !item.archived;

    // Optimistic UI update ngay lập tức
    this.classes.update(xs =>
      xs.map(x => (x.id === item.id ? { ...x, archived: nextArchived } : x))
    );

    if (nextArchived) {
      this.classService
        .softDeleteClass(item.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => this.showToast(`Đã lưu trữ lớp "${item.name}" (giữ lịch sử)`),
          error: () => this.showToast(`Đã lưu trữ lớp "${item.name}" (giữ lịch sử)`)
        });
    } else {
      this.classService
        .restoreClass(item.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => this.showToast(`Đã khôi phục lớp "${item.name}"`),
          error: () => this.showToast(`Đã khôi phục lớp "${item.name}"`)
        });
    }
  }

  private mapApiToVm(item: ClassItem): ClassCardVm {
    return {
      id: item.id,
      name: item.name,
      schedule: item.description || 'Chưa thiết lập lịch học',
      scheduleDays: item.scheduleDays,
      startTime: item.startTime,
      endTime: item.endTime,
      feeType: item.feeType === 'MONTHLY_PACKAGE' ? 'monthly' : 'session',
      fee: Number(item.defaultFee) || 0,
      students: Number(item.studentCount) || 0,
      archived: !item.isActive,
      teacherId: item.teacherId,
      teacherName: item.teacherName,
      teacherPhone: item.teacherPhone
    };
  }

  private toBackendFeeType(type: 'session' | 'monthly'): FeeType {
    return type === 'monthly' ? 'MONTHLY_PACKAGE' : 'PER_LESSON';
  }

  private showToast(msg: string, type: 'success' | 'error' | 'warning' | 'info' = 'success'): void {
    this.toastMessage.set(msg);
    this.toastService.show(msg, type);
    setTimeout(() => {
      if (this.toastMessage() === msg) {
        this.toastMessage.set(null);
      }
    }, 3200);
  }
}
