import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ClassService } from '../../../core/services/class.service';
import { CenterTeacherService, CenterTeacherItem } from '../../../core/services/center-teacher.service';
import { ClassItem, ClassRequestPayload, FeeType } from '../../../shared/models/class.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-classes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './admin-classes.component.html',
  styleUrls: ['./admin-classes.component.css'],
})
export class AdminClassesComponent implements OnInit {
  private readonly classService = inject(ClassService);
  private readonly teacherService = inject(CenterTeacherService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  loading = signal<boolean>(true);
  classes = signal<ClassItem[]>([]);
  teachers = signal<CenterTeacherItem[]>([]);

  searchQuery = signal<string>('');
  statusFilter = signal<string>('ALL');

  // Modal State
  showModal = signal<boolean>(false);
  editingClass = signal<ClassItem | null>(null);
  submitting = signal<boolean>(false);

  // Form
  classForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    teacherId: [''],
    feeType: ['PER_LESSON' as FeeType, Validators.required],
    defaultFee: [150000, [Validators.required, Validators.min(0)]],
    scheduleDays: ['Thứ 2, 4, 6'],
    startTime: ['18:00'],
    endTime: ['19:30'],
  });

  // KPI Computations
  readonly totalClasses = computed(() => this.classes().length);
  readonly activeClasses = computed(() => this.classes().filter((c) => c.isActive).length);
  readonly totalStudents = computed(() =>
    this.classes().reduce((acc, c) => acc + (c.studentCount || 0), 0)
  );
  readonly avgFee = computed(() => {
    const list = this.classes();
    if (list.length === 0) return 0;
    const sum = list.reduce((acc, c) => acc + (c.defaultFee || 0), 0);
    return Math.round(sum / list.length);
  });

  // Filtered List
  readonly filteredClasses = computed(() => {
    let list = this.classes();
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    if (query) {
      list = list.filter(
        (c) =>
          c.name.toLowerCase().includes(query) ||
          (c.teacherName && c.teacherName.toLowerCase().includes(query)) ||
          (c.scheduleDays && c.scheduleDays.toLowerCase().includes(query))
      );
    }

    if (status === 'ACTIVE') {
      list = list.filter((c) => c.isActive);
    } else if (status === 'INACTIVE') {
      list = list.filter((c) => !c.isActive);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.classService.getClasses(true).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.classes.set(res.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Không thể tải danh sách lớp học của cơ sở');
      },
    });

    this.teacherService.getTeachers().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.teachers.set(res.data);
        }
      },
      error: () => {},
    });
  }

  openCreateModal(): void {
    this.editingClass.set(null);
    this.classForm.reset({
      name: '',
      description: '',
      teacherId: this.teachers().length > 0 ? this.teachers()[0].id : '',
      feeType: 'PER_LESSON',
      defaultFee: 150000,
      scheduleDays: 'Thứ 2, 4, 6',
      startTime: '18:00',
      endTime: '19:30',
    });
    this.showModal.set(true);
  }

  openEditModal(c: ClassItem): void {
    this.editingClass.set(c);
    this.classForm.patchValue({
      name: c.name,
      description: c.description || '',
      teacherId: c.teacherId || '',
      feeType: c.feeType || 'PER_LESSON',
      defaultFee: c.defaultFee || 0,
      scheduleDays: c.scheduleDays || '',
      startTime: c.startTime || '',
      endTime: c.endTime || '',
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingClass.set(null);
  }

  onSubmit(): void {
    if (this.classForm.invalid) {
      this.classForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.classForm.value;
    const payload: ClassRequestPayload = {
      name: val.name!,
      description: val.description || undefined,
      teacherId: val.teacherId || undefined,
      feeType: val.feeType as FeeType,
      defaultFee: Number(val.defaultFee) || 0,
      scheduleDays: val.scheduleDays || undefined,
      startTime: val.startTime || undefined,
      endTime: val.endTime || undefined,
    };

    const current = this.editingClass();
    if (current) {
      this.classService.updateClass(current.id, payload).subscribe({
        next: (res) => {
          this.submitting.set(false);
          if (res.success) {
            this.toastService.success('Cập nhật lớp học thành công');
            this.closeModal();
            this.loadData();
          }
        },
        error: (err) => {
          this.submitting.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi cập nhật lớp học');
        },
      });
    } else {
      this.classService.createClass(payload).subscribe({
        next: (res) => {
          this.submitting.set(false);
          if (res.success) {
            this.toastService.success('Tạo lớp học mới thành công');
            this.closeModal();
            this.loadData();
          }
        },
        error: (err) => {
          this.submitting.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi tạo lớp học');
        },
      });
    }
  }

  toggleStatus(c: ClassItem): void {
    if (c.isActive) {
      if (confirm(`Bạn có chắc chắn muốn tạm dừng hoạt động của lớp "${c.name}"?`)) {
        this.classService.softDeleteClass(c.id).subscribe({
          next: () => {
            this.toastService.success('Đã tạm dừng hoạt động lớp học');
            this.loadData();
          },
          error: () => this.toastService.error('Có lỗi khi tạm dừng lớp học'),
        });
      }
    } else {
      this.classService.restoreClass(c.id).subscribe({
        next: () => {
          this.toastService.success('Đã kích hoạt lại lớp học');
          this.loadData();
        },
        error: () => this.toastService.error('Có lỗi khi kích hoạt lại lớp học'),
      });
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }
}

