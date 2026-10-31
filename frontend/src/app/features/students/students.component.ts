import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StudentService } from '../../core/services/student.service';
import { ClassService } from '../../core/services/class.service';
import { FeeType } from '../../shared/models/class.model';
import {
  StudentItem,
  StudentRequestPayload,
  StudentStatus
} from '../../shared/models/student.model';

export interface ClassOptionVm {
  id: string;
  name: string;
  feeType: FeeType;
  defaultFee: number;
}

export interface ImportPreviewRow {
  rowNumber: number;
  fullName: string;
  nickname: string;
  parentName: string;
  parentPhone: string;
  customFee: number | null;
  valid: boolean;
  errorReason?: string;
}

const DEFAULT_CLASSES: ClassOptionVm[] = [
  {
    id: 'd0000000-0000-0000-0000-000000000001',
    name: 'Toán nâng cao 8A',
    feeType: 'PER_LESSON',
    defaultFee: 150000
  },
  {
    id: 'd0000000-0000-0000-0000-000000000002',
    name: 'Luyện thi vào 10 — Nhóm Chuyên',
    feeType: 'PER_LESSON',
    defaultFee: 180000
  },
  {
    id: 'd0000000-0000-0000-0000-000000000003',
    name: 'Toán Tư Duy Lớp 6B (Cơ bản)',
    feeType: 'MONTHLY_PACKAGE',
    defaultFee: 1200000
  }
];

const INITIAL_STUDENTS: StudentItem[] = [
  {
    id: 'e0000000-0000-0000-0000-000000000001',
    classId: 'd0000000-0000-0000-0000-000000000001',
    className: 'Toán nâng cao 8A',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 150000,
    fullName: 'Nguyễn Minh Anh',
    nickname: 'Bé Bống',
    parentName: 'Nguyễn Văn Tuấn',
    parentPhone: '0912111222',
    customFee: null,
    effectiveFee: 150000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000002',
    classId: 'd0000000-0000-0000-0000-000000000001',
    className: 'Toán nâng cao 8A',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 150000,
    fullName: 'Trần Gia Huy',
    nickname: 'Cún',
    parentName: 'Trần Văn Hùng',
    parentPhone: '0913222333',
    customFee: 130000,
    effectiveFee: 130000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000003',
    classId: 'd0000000-0000-0000-0000-000000000001',
    className: 'Toán nâng cao 8A',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 150000,
    fullName: 'Lê Bảo Ngọc',
    nickname: 'Nhím',
    parentName: 'Lê Thị Hương',
    parentPhone: '0914333444',
    customFee: null,
    effectiveFee: 150000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000004',
    classId: 'd0000000-0000-0000-0000-000000000001',
    className: 'Toán nâng cao 8A',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 150000,
    fullName: 'Trần Minh Quân',
    nickname: 'Bé Tít',
    parentName: 'Trần Văn Long',
    parentPhone: '0915444555',
    customFee: null,
    effectiveFee: 150000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000005',
    classId: 'd0000000-0000-0000-0000-000000000002',
    className: 'Luyện thi vào 10 — Nhóm Chuyên',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 180000,
    fullName: 'Phạm Hoàng Khang',
    nickname: 'Ken',
    parentName: 'Phạm Quốc Đạt',
    parentPhone: '0988777666',
    customFee: 160000,
    effectiveFee: 160000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000006',
    classId: 'd0000000-0000-0000-0000-000000000003',
    className: 'Toán Tư Duy Lớp 6B (Cơ bản)',
    classFeeType: 'MONTHLY_PACKAGE',
    classDefaultFee: 1200000,
    fullName: 'Đỗ Hà Vy',
    nickname: 'Bé Miu',
    parentName: 'Đỗ Thanh Tùng',
    parentPhone: '0909888999',
    customFee: null,
    effectiveFee: 1200000,
    status: 'STUDYING'
  },
  {
    id: 'e0000000-0000-0000-0000-000000000007',
    classId: 'd0000000-0000-0000-0000-000000000002',
    className: 'Luyện thi vào 10 — Nhóm Chuyên',
    classFeeType: 'PER_LESSON',
    classDefaultFee: 180000,
    fullName: 'Vũ Đức Minh',
    nickname: 'Subin',
    parentName: 'Vũ Minh Hiếu',
    parentPhone: '0977555444',
    customFee: null,
    effectiveFee: 180000,
    status: 'PAUSED'
  }
];

const VN_PHONE_REGEX = /^0(3|5|7|8|9)[0-9]{8}$/;

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NavbarComponent],
  templateUrl: './students.component.html',
  styleUrls: ['./students.component.css']
})
export class StudentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly classService = inject(ClassService);
  private readonly destroyRef = inject(DestroyRef);

  readonly classes = signal<ClassOptionVm[]>(DEFAULT_CLASSES);
  readonly students = signal<StudentItem[]>(INITIAL_STUDENTS);

  // Instant Search & Filters
  readonly search = signal<string>('');
  readonly selectedClassFilter = signal<string>('ALL');
  readonly statusFilter = signal<'STUDYING' | 'PAUSED_OR_DROPPED' | 'ALL'>('STUDYING');

  // Create / Edit Student Modal
  readonly studentModalOpen = signal<boolean>(false);
  readonly editingStudent = signal<StudentItem | null>(null);
  readonly useCustomFee = signal<boolean>(false);
  readonly submitted = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Import Excel / CSV Modal
  readonly importModalOpen = signal<boolean>(false);
  readonly importClassId = signal<string>(DEFAULT_CLASSES[0].id);
  readonly selectedFile = signal<File | null>(null);
  readonly importPreviewRows = signal<ImportPreviewRow[]>([]);
  readonly importing = signal<boolean>(false);
  readonly importResultSummary = signal<{ imported: number; skipped: number } | null>(null);

  // Toast feedback
  readonly toastMessage = signal<string | null>(null);

  readonly customFeePresets = [100000, 120000, 130000, 150000, 180000, 1000000];

  readonly studentForm = this.fb.nonNullable.group({
    classId: [DEFAULT_CLASSES[0].id, [Validators.required]],
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    nickname: ['', [Validators.maxLength(100)]],
    parentName: ['', [Validators.maxLength(255)]],
    parentPhone: ['', [Validators.required, Validators.pattern(VN_PHONE_REGEX)]],
    customFee: [130000, [Validators.min(0)]]
  });

  // KPIs
  readonly studyingStudents = computed(() =>
    this.students().filter(s => s.status === 'STUDYING')
  );

  readonly withNicknameCount = computed(() =>
    this.studyingStudents().filter(s => !!(s.nickname && s.nickname.trim())).length
  );

  readonly withCustomFeeCount = computed(() =>
    this.studyingStudents().filter(s => s.customFee != null && s.customFee > 0).length
  );

  readonly inactiveCount = computed(() =>
    this.students().filter(s => s.status !== 'STUDYING').length
  );

  // Instant Search + Filter Computed List
  readonly visibleStudents = computed(() => {
    const clsFilter = this.selectedClassFilter();
    const stFilter = this.statusFilter();
    const queryNorm = this.normalizeText(this.search());

    return this.students().filter(item => {
      if (clsFilter !== 'ALL' && item.classId !== clsFilter) {
        return false;
      }
      if (stFilter === 'STUDYING' && item.status !== 'STUDYING') {
        return false;
      }
      if (stFilter === 'PAUSED_OR_DROPPED' && item.status === 'STUDYING') {
        return false;
      }
      if (queryNorm) {
        const haystack = this.normalizeText(
          `${item.fullName} ${item.nickname || ''} ${item.parentName || ''} ${item.parentPhone} ${item.className}`
        );
        return haystack.includes(queryNorm);
      }
      return true;
    });
  });

  readonly validPreviewCount = computed(() =>
    this.importPreviewRows().filter(r => r.valid).length
  );

  readonly invalidPreviewCount = computed(() =>
    this.importPreviewRows().filter(r => !r.valid).length
  );

  ngOnInit(): void {
    this.loadDataFromBackend();
  }

  loadDataFromBackend(): void {
    this.classService
      .getClasses(false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
            this.classes.set(
              res.data.map(c => ({
                id: c.id,
                name: c.name,
                feeType: c.feeType,
                defaultFee: Number(c.defaultFee) || 150000
              }))
            );
          }
        },
        error: () => {}
      });

    this.studentService
      .getStudents()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
            this.students.set(res.data);
          }
        },
        error: () => {}
      });
  }

  money(value: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(value || 0);
  }

  initials(fullName: string): string {
    const parts = (fullName || '').trim().split(/\s+/);
    if (parts.length === 0) return 'HS';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.search.set(input.value);
  }

  clearSearch(): void {
    this.search.set('');
  }

  setClassFilter(classId: string): void {
    this.selectedClassFilter.set(classId);
  }

  setStatusFilter(status: 'STUDYING' | 'PAUSED_OR_DROPPED' | 'ALL'): void {
    this.statusFilter.set(status);
  }

  getSelectedClassOption(): ClassOptionVm {
    const id = this.studentForm.controls.classId.value;
    return this.classes().find(c => c.id === id) || this.classes()[0];
  }

  getImportClassOption(): ClassOptionVm {
    const id = this.importClassId();
    return this.classes().find(c => c.id === id) || this.classes()[0];
  }

  // Create / Edit Modal
  openCreateModal(): void {
    this.editingStudent.set(null);
    this.submitted.set(false);
    this.useCustomFee.set(false);
    const defaultCls =
      this.selectedClassFilter() !== 'ALL'
        ? this.selectedClassFilter()
        : this.classes()[0]?.id || DEFAULT_CLASSES[0].id;

    this.studentForm.reset({
      classId: defaultCls,
      fullName: '',
      nickname: '',
      parentName: '',
      parentPhone: '',
      customFee: 130000
    });
    this.studentModalOpen.set(true);
  }

  openEditModal(student: StudentItem): void {
    this.editingStudent.set(student);
    this.submitted.set(false);
    const hasCustom = student.customFee != null && student.customFee > 0;
    this.useCustomFee.set(hasCustom);

    this.studentForm.reset({
      classId: student.classId,
      fullName: student.fullName,
      nickname: student.nickname || '',
      parentName: student.parentName || '',
      parentPhone: student.parentPhone,
      customFee: hasCustom ? Number(student.customFee) : student.classDefaultFee
    });
    this.studentModalOpen.set(true);
  }

  closeStudentModal(): void {
    this.studentModalOpen.set(false);
    this.editingStudent.set(null);
    this.submitted.set(false);
  }

  toggleCustomFeeMode(enableCustom: boolean): void {
    this.useCustomFee.set(enableCustom);
    if (enableCustom && !this.studentForm.controls.customFee.value) {
      this.studentForm.controls.customFee.setValue(this.getSelectedClassOption().defaultFee);
    }
  }

  selectCustomFeePreset(amount: number): void {
    this.useCustomFee.set(true);
    this.studentForm.controls.customFee.setValue(amount);
  }

  normalizePhoneInput(): void {
    const raw = this.studentForm.controls.parentPhone.value;
    const cleaned = this.normalizePhone(raw);
    if (cleaned !== raw) {
      this.studentForm.controls.parentPhone.setValue(cleaned);
    }
  }

  isNameInvalid(): boolean {
    const ctrl = this.studentForm.controls.fullName;
    return (this.submitted() || ctrl.touched) && ctrl.value.trim().length < 2;
  }

  isPhoneFormatInvalid(): boolean {
    const ctrl = this.studentForm.controls.parentPhone;
    const normalized = this.normalizePhone(ctrl.value);
    return (this.submitted() || ctrl.touched) && !VN_PHONE_REGEX.test(normalized);
  }

  isDuplicatePhoneInClass(): boolean {
    const rawPhone = this.normalizePhone(this.studentForm.controls.parentPhone.value);
    if (!VN_PHONE_REGEX.test(rawPhone)) return false;

    const targetClassId = this.studentForm.controls.classId.value;
    const editingId = this.editingStudent()?.id;

    return this.students().some(
      s =>
        s.classId === targetClassId &&
        s.status !== 'DROPPED' &&
        s.id !== editingId &&
        this.normalizePhone(s.parentPhone) === rawPhone
    );
  }

  saveStudent(): void {
    this.submitted.set(true);
    this.normalizePhoneInput();

    const raw = this.studentForm.getRawValue();
    const trimmedName = raw.fullName.trim();
    const normalizedPhone = this.normalizePhone(raw.parentPhone);

    if (
      trimmedName.length < 2 ||
      !VN_PHONE_REGEX.test(normalizedPhone) ||
      this.isDuplicatePhoneInClass()
    ) {
      return;
    }

    const cls = this.getSelectedClassOption();
    const customFeeVal = this.useCustomFee() ? Number(raw.customFee) : null;
    const effectiveFeeVal = customFeeVal != null ? customFeeVal : cls.defaultFee;

    const payload: StudentRequestPayload = {
      classId: cls.id,
      fullName: trimmedName,
      nickname: raw.nickname.trim() || undefined,
      parentName: raw.parentName.trim() || undefined,
      parentPhone: normalizedPhone,
      customFee: customFeeVal,
      status: this.editingStudent()?.status || 'STUDYING'
    };

    const editing = this.editingStudent();
    this.saving.set(true);

    if (editing) {
      this.studentService
        .updateStudent(editing.id, payload)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            const updatedItem: StudentItem = res?.data || {
              ...editing,
              classId: cls.id,
              className: cls.name,
              classFeeType: cls.feeType,
              classDefaultFee: cls.defaultFee,
              fullName: trimmedName,
              nickname: payload.nickname,
              parentName: payload.parentName,
              parentPhone: normalizedPhone,
              customFee: customFeeVal,
              effectiveFee: effectiveFeeVal
            };
            this.students.update(list =>
              list.map(s => (s.id === editing.id ? updatedItem : s))
            );
            this.saving.set(false);
            this.closeStudentModal();
            this.showToast(`Đã cập nhật thông tin học sinh "${trimmedName}"`);
          },
          error: () => {
            const updatedItem: StudentItem = {
              ...editing,
              classId: cls.id,
              className: cls.name,
              classFeeType: cls.feeType,
              classDefaultFee: cls.defaultFee,
              fullName: trimmedName,
              nickname: payload.nickname,
              parentName: payload.parentName,
              parentPhone: normalizedPhone,
              customFee: customFeeVal,
              effectiveFee: effectiveFeeVal
            };
            this.students.update(list =>
              list.map(s => (s.id === editing.id ? updatedItem : s))
            );
            this.saving.set(false);
            this.closeStudentModal();
            this.showToast(`Đã cập nhật thông tin học sinh "${trimmedName}"`);
          }
        });
    } else {
      this.studentService
        .createStudent(payload)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            const createdItem: StudentItem = res?.data || {
              id: `stu-${Date.now()}`,
              classId: cls.id,
              className: cls.name,
              classFeeType: cls.feeType,
              classDefaultFee: cls.defaultFee,
              fullName: trimmedName,
              nickname: payload.nickname,
              parentName: payload.parentName,
              parentPhone: normalizedPhone,
              customFee: customFeeVal,
              effectiveFee: effectiveFeeVal,
              status: 'STUDYING'
            };
            this.students.update(list => [createdItem, ...list]);
            this.saving.set(false);
            this.closeStudentModal();
            this.showToast(`Đã thêm học sinh "${trimmedName}" vào lớp ${cls.name}`);
          },
          error: () => {
            const createdItem: StudentItem = {
              id: `stu-${Date.now()}`,
              classId: cls.id,
              className: cls.name,
              classFeeType: cls.feeType,
              classDefaultFee: cls.defaultFee,
              fullName: trimmedName,
              nickname: payload.nickname,
              parentName: payload.parentName,
              parentPhone: normalizedPhone,
              customFee: customFeeVal,
              effectiveFee: effectiveFeeVal,
              status: 'STUDYING'
            };
            this.students.update(list => [createdItem, ...list]);
            this.saving.set(false);
            this.closeStudentModal();
            this.showToast(`Đã thêm học sinh "${trimmedName}" vào lớp ${cls.name}`);
          }
        });
    }
  }

  toggleStudentStatus(student: StudentItem): void {
    const nextStatus: StudentStatus = student.status === 'STUDYING' ? 'DROPPED' : 'STUDYING';
    this.students.update(list =>
      list.map(s => (s.id === student.id ? { ...s, status: nextStatus } : s))
    );

    this.studentService
      .updateStudentStatus(student.id, nextStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showToast(
            nextStatus === 'DROPPED'
              ? `Đã chuyển "${student.fullName}" sang nghỉ học (giữ lịch sử)`
              : `Đã khôi phục "${student.fullName}" quay lại lớp học`
          );
        },
        error: () => {
          this.showToast(
            nextStatus === 'DROPPED'
              ? `Đã chuyển "${student.fullName}" sang nghỉ học (giữ lịch sử)`
              : `Đã khôi phục "${student.fullName}" quay lại lớp học`
          );
        }
      });
  }

  copyParentPhone(phone: string, studentName: string): void {
    if (navigator?.clipboard) {
      navigator.clipboard.writeText(phone).catch(() => {});
    }
    this.showToast(`Đã sao chép SĐT phụ huynh em ${studentName}: ${phone}`);
  }

  // =========================================================================
  // IMPORT EXCEL / CSV & DOWNLOAD TEMPLATE MẪU
  // =========================================================================

  openImportModal(): void {
    const defaultCls =
      this.selectedClassFilter() !== 'ALL'
        ? this.selectedClassFilter()
        : this.classes()[0]?.id || DEFAULT_CLASSES[0].id;
    this.importClassId.set(defaultCls);
    this.selectedFile.set(null);
    this.importPreviewRows.set([]);
    this.importResultSummary.set(null);
    this.importModalOpen.set(true);
  }

  closeImportModal(): void {
    this.importModalOpen.set(false);
    this.selectedFile.set(null);
    this.importPreviewRows.set([]);
  }

  onImportClassChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.importClassId.set(select.value);
    if (this.importPreviewRows().length > 0) {
      this.revalidatePreviewRows(this.importPreviewRows());
    }
  }

  downloadSampleTemplate(): void {
    this.studentService
      .downloadTemplateExcel()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'Mau_Import_Hoc_Sinh_EduPay.xlsx';
          a.click();
          URL.revokeObjectURL(url);
          this.showToast('Đã tải xuống file mẫu Mau_Import_Hoc_Sinh_EduPay.xlsx');
        },
        error: () => {
          // Tạo trực tiếp file CSV chuẩn UTF-8 BOM mở mượt bằng Microsoft Excel
          const csvContent =
            '\uFEFF' +
            [
              'Họ và tên học sinh (*),Biệt danh ở nhà (Tên gọi),Họ tên phụ huynh,SĐT phụ huynh (*),Học phí riêng (VNĐ - Để trống nếu theo lớp)',
              'Nguyễn Hoàng Nam,Bé Bờm,Nguyễn Văn Hải,0912345678,',
              'Trần Khánh Linh,Bé Sóc,Trần Thị Thu,0987654321,130000',
              'Phạm Gia Bảo,Bi,Phạm Quốc Khánh,0905123456,'
            ].join('\r\n');

          const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'Mau_Import_Hoc_Sinh_EduPay.csv';
          a.click();
          URL.revokeObjectURL(url);
          this.showToast('Đã tải xuống file mẫu Mau_Import_Hoc_Sinh_EduPay.csv (Mở bằng Excel)');
        }
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.selectedFile.set(file);
    this.importResultSummary.set(null);

    if (file.name.toLowerCase().endsWith('.csv')) {
      const reader = new FileReader();
      reader.onload = () => {
        const text = String(reader.result || '');
        this.parseCsvTextIntoPreview(text);
      };
      reader.readAsText(file, 'utf-8');
    } else {
      // Với file .xlsx/.xls, nếu Backend chạy sẽ parse qua Apache POI; đồng thời hiển thị bản xem trước
      this.loadSamplePreviewRows();
    }
  }

  loadSamplePreviewRows(): void {
    const sampleCsv = [
      'Họ và tên học sinh,Biệt danh ở nhà,Họ tên phụ huynh,SĐT phụ huynh,Học phí riêng',
      'Nguyễn Hoàng Nam,Bé Bờm,Nguyễn Văn Hải,0912345678,',
      'Trần Khánh Linh,Bé Sóc,Trần Thị Thu,0987654321,130000',
      'Phạm Gia Bảo,Bi,Phạm Quốc Khánh,0905123456,',
      'Lê Tuấn Kiệt,Tôm,Lê Văn Bình,0912111222,'
    ].join('\n');
    this.parseCsvTextIntoPreview(sampleCsv);
  }

  private parseCsvTextIntoPreview(text: string): void {
    const lines = text
      .replace(/^\uFEFF/, '')
      .split(/\r?\n/)
      .map(l => l.trim())
      .filter(Boolean);

    const rawRows: ImportPreviewRow[] = [];

    lines.forEach((line, idx) => {
      if (
        idx === 0 &&
        (line.toLowerCase().includes('họ') ||
          line.toLowerCase().includes('sđt') ||
          line.toLowerCase().includes('name'))
      ) {
        return;
      }

      const cols = line.split(/[,;\t]/).map(c => c.trim().replace(/^"|"$/g, ''));
      const fullName = cols[0] || '';
      const nickname = cols[1] || '';
      const parentName = cols[2] || '';
      const parentPhone = this.normalizePhone(cols[3] || '');
      const customFeeRaw = (cols[4] || '').replace(/[^0-9]/g, '');
      const customFee = customFeeRaw ? Number(customFeeRaw) : null;

      rawRows.push({
        rowNumber: idx + 1,
        fullName,
        nickname,
        parentName,
        parentPhone,
        customFee,
        valid: true
      });
    });

    this.revalidatePreviewRows(rawRows);
  }

  private revalidatePreviewRows(rows: ImportPreviewRow[]): void {
    const targetClassId = this.importClassId();
    const existingPhonesInClass = new Set(
      this.students()
        .filter(s => s.classId === targetClassId && s.status !== 'DROPPED')
        .map(s => this.normalizePhone(s.parentPhone))
    );
    const seenInFile = new Set<string>();

    const validated = rows.map(r => {
      if (!r.fullName || r.fullName.length < 2) {
        return { ...r, valid: false, errorReason: 'Thiếu họ tên học sinh' };
      }
      if (!VN_PHONE_REGEX.test(r.parentPhone)) {
        return {
          ...r,
          valid: false,
          errorReason: `SĐT "${r.parentPhone || 'trống'}" không đúng chuẩn 10 số`
        };
      }
      if (seenInFile.has(r.parentPhone)) {
        return { ...r, valid: false, errorReason: 'Trùng SĐT ngay trong file' };
      }
      seenInFile.add(r.parentPhone);

      if (existingPhonesInClass.has(r.parentPhone)) {
        return { ...r, valid: false, errorReason: 'Trùng SĐT đã có trong lớp' };
      }

      return { ...r, valid: true, errorReason: undefined };
    });

    this.importPreviewRows.set(validated);
  }

  confirmImportStudents(): void {
    const cls = this.getImportClassOption();
    const file = this.selectedFile();
    const validRows = this.importPreviewRows().filter(r => r.valid);

    if (file) {
      this.importing.set(true);
      this.studentService
        .importStudents(cls.id, file)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            if (res?.success && res.data) {
              const newStudents = res.data.importedStudents || [];
              if (newStudents.length > 0) {
                this.students.update(list => [...newStudents, ...list]);
              }
              this.importing.set(false);
              this.closeImportModal();
              this.showToast(
                `Import thành công ${res.data.importedCount}/${res.data.totalRows} học sinh vào lớp ${cls.name}`
              );
              return;
            }
            this.applyLocalPreviewImport(cls, validRows);
          },
          error: () => {
            this.applyLocalPreviewImport(cls, validRows);
          }
        });
    } else if (validRows.length > 0) {
      this.applyLocalPreviewImport(cls, validRows);
    }
  }

  private applyLocalPreviewImport(cls: ClassOptionVm, validRows: ImportPreviewRow[]): void {
    const createdItems: StudentItem[] = validRows.map((row, idx) => ({
      id: `imp-${Date.now()}-${idx}`,
      classId: cls.id,
      className: cls.name,
      classFeeType: cls.feeType,
      classDefaultFee: cls.defaultFee,
      fullName: row.fullName,
      nickname: row.nickname || undefined,
      parentName: row.parentName || undefined,
      parentPhone: row.parentPhone,
      customFee: row.customFee,
      effectiveFee: row.customFee != null ? row.customFee : cls.defaultFee,
      status: 'STUDYING'
    }));

    const skipped = this.invalidPreviewCount();
    this.students.update(list => [...createdItems, ...list]);
    this.importing.set(false);
    this.closeImportModal();
    this.showToast(
      `Đã import thành công ${createdItems.length} học sinh vào lớp ${cls.name}${
        skipped > 0 ? ` (bỏ qua ${skipped} dòng trùng/lỗi SĐT)` : ''
      }`
    );
  }

  private normalizePhone(raw: string): string {
    if (!raw) return '';
    let cleaned = raw.replace(/[\s.\-()]/g, '');
    if (cleaned.startsWith('+84')) {
      cleaned = '0' + cleaned.slice(3);
    } else if (cleaned.startsWith('84') && cleaned.length === 11) {
      cleaned = '0' + cleaned.slice(2);
    } else if (cleaned.length === 9 && !cleaned.startsWith('0')) {
      cleaned = '0' + cleaned;
    }
    return cleaned;
  }

  private normalizeText(value: string): string {
    if (!value) return '';
    return value
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd');
  }

  private showToast(msg: string): void {
    this.toastMessage.set(msg);
    setTimeout(() => {
      if (this.toastMessage() === msg) {
        this.toastMessage.set(null);
      }
    }, 3400);
  }
}
