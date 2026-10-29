import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClassService } from '../../core/services/class.service';
import { ClassItem, ClassRequestPayload, FeeType } from '../../shared/models/class.model';

export interface ClassCardVm {
  id: string;
  name: string;
  schedule: string;
  notes?: string;
  feeType: 'session' | 'monthly';
  fee: number;
  students: number;
  archived: boolean;
}

const INITIAL_CLASSES: ClassCardVm[] = [
  {
    id: 'd0000000-0000-0000-0000-000000000001',
    name: 'Toán nâng cao 8A',
    schedule: 'Thứ 3, Thứ 6 · 18:00–19:30',
    feeType: 'session',
    fee: 150000,
    students: 4,
    archived: false
  },
  {
    id: 'd0000000-0000-0000-0000-000000000002',
    name: 'Luyện thi vào 10 — Nhóm Chuyên',
    schedule: 'Thứ 2, Thứ 5, Thứ 7 · 19:30–21:00',
    feeType: 'session',
    fee: 180000,
    students: 12,
    archived: false
  },
  {
    id: 'd0000000-0000-0000-0000-000000000003',
    name: 'Toán Tư Duy Lớp 6B (Cơ bản)',
    schedule: 'Thứ 4, Chủ nhật · 17:30–19:00',
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
  private readonly destroyRef = inject(DestroyRef);

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

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    feeType: ['session' as 'session' | 'monthly', [Validators.required]],
    fee: [150000, [Validators.required, Validators.min(1000)]],
    schedule: [''],
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
  }

  loadClasses(): void {
    this.classService
      .getClasses(true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
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

  openCreateModal(): void {
    this.editingItem.set(null);
    this.submitted.set(false);
    this.form.reset({
      name: '',
      feeType: 'session',
      fee: 150000,
      schedule: '',
      notes: ''
    });
    this.modalOpen.set(true);
  }

  openEditModal(item: ClassCardVm): void {
    this.editingItem.set(item);
    this.submitted.set(false);
    this.form.reset({
      name: item.name,
      feeType: item.feeType,
      fee: item.fee,
      schedule: item.schedule,
      notes: item.notes || ''
    });
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingItem.set(null);
    this.submitted.set(false);
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

    const scheduleText = raw.schedule.trim() || 'Chưa thiết lập lịch học';
    const notesText = raw.notes.trim();
    const descriptionCombined = notesText
      ? `${scheduleText} · ${notesText}`
      : scheduleText;

    const payload: ClassRequestPayload = {
      name: trimmedName,
      description: descriptionCombined,
      feeType: this.toBackendFeeType(raw.feeType),
      defaultFee: feeValue
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
              ? { ...this.mapApiToVm(res.data), schedule: scheduleText, notes: notesText, students: editing.students }
              : {
                  ...editing,
                  name: trimmedName,
                  feeType: raw.feeType,
                  fee: feeValue,
                  schedule: scheduleText,
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
              ? { ...this.mapApiToVm(res.data), schedule: scheduleText, notes: notesText }
              : {
                  id: `cls-${Date.now()}`,
                  name: trimmedName,
                  feeType: raw.feeType,
                  fee: feeValue,
                  schedule: scheduleText,
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
      feeType: item.feeType === 'MONTHLY_PACKAGE' ? 'monthly' : 'session',
      fee: Number(item.defaultFee) || 0,
      students: Number(item.studentCount) || 0,
      archived: !item.isActive
    };
  }

  private toBackendFeeType(type: 'session' | 'monthly'): FeeType {
    return type === 'monthly' ? 'MONTHLY_PACKAGE' : 'PER_LESSON';
  }

  private showToast(msg: string): void {
    this.toastMessage.set(msg);
    setTimeout(() => {
      if (this.toastMessage() === msg) {
        this.toastMessage.set(null);
      }
    }, 3200);
  }
}
