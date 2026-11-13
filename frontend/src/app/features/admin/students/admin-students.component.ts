import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { StudentService } from '../../../core/services/student.service';
import { ClassService } from '../../../core/services/class.service';
import { StudentItem, StudentRequestPayload, StudentStatus } from '../../../shared/models/student.model';
import { ClassItem } from '../../../shared/models/class.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-students',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './admin-students.component.html',
  styleUrls: ['./admin-students.component.css'],
})
export class AdminStudentsComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly classService = inject(ClassService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  loading = signal<boolean>(true);
  students = signal<StudentItem[]>([]);
  classes = signal<ClassItem[]>([]);

  searchQuery = signal<string>('');
  classFilter = signal<string>('ALL');
  statusFilter = signal<string>('ALL');

  // Modal State
  showModal = signal<boolean>(false);
  editingStudent = signal<StudentItem | null>(null);
  submitting = signal<boolean>(false);

  // Form
  studentForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    parentName: [''],
    parentPhone: ['', [Validators.required, Validators.pattern(/^(0[3|5|7|8|9])+([0-9]{8})$/)]],
    classId: ['', Validators.required],
    customFee: [null as number | null],
    status: ['STUDYING' as StudentStatus, Validators.required],
  });

  // KPI Computations
  readonly totalStudents = computed(() => this.students().length);
  readonly studyingStudents = computed(() => this.students().filter((s) => s.status === 'STUDYING').length);
  readonly pausedStudents = computed(() => this.students().filter((s) => s.status === 'PAUSED').length);
  readonly classesCovered = computed(() => {
    const set = new Set(this.students().map((s) => s.classId).filter(Boolean));
    return set.size;
  });

  // Filtered List
  readonly filteredStudents = computed(() => {
    let list = this.students();
    const query = this.searchQuery().trim().toLowerCase();
    const cls = this.classFilter();
    const st = this.statusFilter();

    if (query) {
      list = list.filter(
        (s) =>
          s.fullName.toLowerCase().includes(query) ||
          (s.parentPhone && s.parentPhone.includes(query)) ||
          (s.parentName && s.parentName.toLowerCase().includes(query)) ||
          (s.className && s.className.toLowerCase().includes(query))
      );
    }

    if (cls !== 'ALL') {
      list = list.filter((s) => s.classId === cls);
    }

    if (st !== 'ALL') {
      list = list.filter((s) => s.status === st);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.studentService.getStudents().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.students.set(res.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Không thể tải danh sách học sinh của cơ sở');
      },
    });

    this.classService.getClasses(true).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.classes.set(res.data);
        }
      },
      error: () => {},
    });
  }

  openCreateModal(): void {
    this.editingStudent.set(null);
    this.studentForm.reset({
      fullName: '',
      parentName: '',
      parentPhone: '',
      classId: this.classes().length > 0 ? this.classes()[0].id : '',
      customFee: null,
      status: 'STUDYING',
    });
    this.showModal.set(true);
  }

  openEditModal(s: StudentItem): void {
    this.editingStudent.set(s);
    this.studentForm.patchValue({
      fullName: s.fullName,
      parentName: s.parentName || '',
      parentPhone: s.parentPhone,
      classId: s.classId,
      customFee: s.customFee ?? null,
      status: s.status,
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingStudent.set(null);
  }

  onSubmit(): void {
    if (this.studentForm.invalid) {
      this.studentForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.studentForm.value;
    const payload: StudentRequestPayload = {
      fullName: val.fullName!,
      parentName: val.parentName || undefined,
      parentPhone: val.parentPhone!,
      classId: val.classId!,
      customFee: val.customFee ? Number(val.customFee) : null,
      status: val.status as StudentStatus,
    };

    const current = this.editingStudent();
    if (current) {
      this.studentService.updateStudent(current.id, payload).subscribe({
        next: (res) => {
          this.submitting.set(false);
          if (res.success) {
            this.toastService.success('Cập nhật hồ sơ học sinh thành công');
            this.closeModal();
            this.loadData();
          }
        },
        error: (err) => {
          this.submitting.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi cập nhật học sinh');
        },
      });
    } else {
      this.studentService.createStudent(payload).subscribe({
        next: (res) => {
          this.submitting.set(false);
          if (res.success) {
            this.toastService.success('Thêm học sinh mới vào lớp thành công');
            this.closeModal();
            this.loadData();
          }
        },
        error: (err) => {
          this.submitting.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi thêm học sinh');
        },
      });
    }
  }

  onDelete(s: StudentItem): void {
    if (confirm(`Bạn có chắc chắn muốn xóa học sinh "${s.fullName}" khỏi cơ sở?`)) {
      this.studentService.softDeleteStudent(s.id).subscribe({
        next: () => {
          this.toastService.success('Đã xóa học sinh');
          this.loadData();
        },
        error: () => this.toastService.error('Có lỗi khi xóa học sinh'),
      });
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }
}

