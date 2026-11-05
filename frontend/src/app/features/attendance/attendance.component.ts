import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AttendanceService } from '../../core/services/attendance.service';
import { ClassService } from '../../core/services/class.service';
import { ToastService } from '../../core/services/toast.service';
import { ClassItem } from '../../shared/models/class.model';
import {
  AttendanceStatus,
  DailyAttendance,
  DailyAttendanceItem,
  MonthlyAttendanceMatrix,
  StudentMonthlyAttendance,
  AttendanceRecord
} from '../../shared/models/attendance.model';

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.css']
})
export class AttendanceComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
  private readonly classService = inject(ClassService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  // Core State
  readonly classes = signal<ClassItem[]>([]);
  readonly selectedClassId = signal<string>('');
  readonly activeTab = signal<'daily' | 'monthly'>('daily');

  // Daily Attendance State (UC-13)
  readonly selectedDate = signal<string>(this.formatDate(new Date()));
  readonly dailyAttendance = signal<DailyAttendance | null>(null);
  readonly dailyItems = signal<DailyAttendanceItem[]>([]);
  readonly isDailyLoading = signal<boolean>(false);
  readonly isSavingDaily = signal<boolean>(false);
  readonly hasUnsavedChanges = signal<boolean>(false);

  // Monthly Attendance State (UC-14, UC-15)
  readonly selectedMonth = signal<string>(this.formatMonth(new Date()));
  readonly monthlyMatrix = signal<MonthlyAttendanceMatrix | null>(null);
  readonly isMonthlyLoading = signal<boolean>(false);

  // Modals & Popups
  readonly isNoteModalOpen = signal<boolean>(false);
  readonly noteStudent = signal<DailyAttendanceItem | null>(null);
  readonly noteText = signal<string>('');

  readonly isEditCellModalOpen = signal<boolean>(false);
  readonly editCellStudent = signal<StudentMonthlyAttendance | null>(null);
  readonly editCellDate = signal<string>('');
  readonly editCellStatus = signal<AttendanceStatus>('PRESENT');
  readonly editCellNote = signal<string>('');
  readonly editCellAttendanceId = signal<string | null>(null);
  readonly isSavingCell = signal<boolean>(false);

  // Feedback Notification
  readonly toastMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // Computed Properties
  readonly selectedClass = computed(() => {
    const cid = this.selectedClassId();
    return this.classes().find(c => c.id === cid) ?? null;
  });

  readonly dailyTotal = computed(() => this.dailyItems().length);

  readonly dailyPresentCount = computed(() => {
    return this.dailyItems().filter(item => item.status === 'PRESENT').length;
  });

  readonly dailyExcusedCount = computed(() => {
    return this.dailyItems().filter(item => item.status === 'EXCUSED').length;
  });

  readonly dailyAbsentCount = computed(() => {
    return this.dailyItems().filter(item => item.status === 'ABSENT').length;
  });

  readonly dailyAttendanceRate = computed(() => {
    const total = this.dailyTotal();
    if (total === 0) return 0;
    return Math.round((this.dailyPresentCount() / total) * 100);
  });

  readonly isTodaySelected = computed(() => {
    return this.selectedDate() === this.formatDate(new Date());
  });

  ngOnInit(): void {
    this.loadClasses();
  }

  loadClasses(): void {
    this.classService.getClasses(false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res.data && res.data.length > 0) {
            this.classes.set(res.data);
            
            // Check query params if classId is provided
            this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
              const paramClassId = params['classId'];
              if (paramClassId && res.data.some(c => c.id === paramClassId)) {
                this.selectedClassId.set(paramClassId);
              } else if (!this.selectedClassId()) {
                this.selectedClassId.set(res.data[0].id);
              }
              this.refreshActiveTab();
            });
          } else {
            this.classes.set([]);
          }
        },
        error: (err) => {
          this.showError('Không thể tải danh sách lớp: ' + (err.error?.message || err.message));
        }
      });
  }

  onClassChange(classId: string): void {
    if (this.hasUnsavedChanges()) {
      const confirmDiscard = confirm('Bạn có thay đổi điểm danh chưa lưu. Bạn có chắc muốn chuyển lớp không?');
      if (!confirmDiscard) {
        return;
      }
    }
    this.selectedClassId.set(classId);
    this.hasUnsavedChanges.set(false);
    this.refreshActiveTab();
  }

  switchTab(tab: 'daily' | 'monthly'): void {
    if (this.hasUnsavedChanges() && tab === 'monthly') {
      const confirmDiscard = confirm('Bạn có thay đổi điểm danh chưa lưu. Bạn có muốn rời đi không?');
      if (!confirmDiscard) {
        return;
      }
      this.hasUnsavedChanges.set(false);
    }
    this.activeTab.set(tab);
    this.refreshActiveTab();
  }

  refreshActiveTab(): void {
    if (!this.selectedClassId()) return;

    if (this.activeTab() === 'daily') {
      this.loadDailyAttendance();
    } else {
      this.loadMonthlyAttendance();
    }
  }

  // ==========================================
  // DAILY ATTENDANCE (UC-13)
  // ==========================================

  loadDailyAttendance(): void {
    const classId = this.selectedClassId();
    const date = this.selectedDate();
    if (!classId || !date) return;

    this.isDailyLoading.set(true);
    this.attendanceService.getDailyAttendance(classId, date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isDailyLoading.set(false);
          if (res.data) {
            this.dailyAttendance.set(res.data);
            // Deep copy items to support optimistic mutation
            this.dailyItems.set(res.data.students.map(s => ({ ...s })));
            this.hasUnsavedChanges.set(false);
          }
        },
        error: (err) => {
          this.isDailyLoading.set(false);
          this.showError('Lỗi khi tải buổi điểm danh: ' + (err.error?.message || err.message));
        }
      });
  }

  onDateChange(newDate: string): void {
    if (this.hasUnsavedChanges()) {
      const confirmDiscard = confirm('Bạn có thay đổi điểm danh chưa lưu. Bạn có chắc muốn đổi ngày không?');
      if (!confirmDiscard) {
        return;
      }
    }
    this.selectedDate.set(newDate);
    this.hasUnsavedChanges.set(false);
    this.loadDailyAttendance();
  }

  selectDatePreset(preset: 'today' | 'yesterday'): void {
    const target = new Date();
    if (preset === 'yesterday') {
      target.setDate(target.getDate() - 1);
    }
    this.onDateChange(this.formatDate(target));
  }

  setStatus(item: DailyAttendanceItem, newStatus: AttendanceStatus): void {
    if (item.isLocked) {
      this.showToast('Học sinh này đã chốt phiếu thu tháng, không thể thay đổi.');
      return;
    }

    if (item.status === newStatus) {
      return;
    }

    // Optimistic UI update
    this.dailyItems.update(list =>
      list.map(s => s.studentId === item.studentId ? { ...s, status: newStatus } : s)
    );
    this.hasUnsavedChanges.set(true);
  }

  cycleStatus(item: DailyAttendanceItem): void {
    if (item.isLocked) {
      this.showToast('Học sinh này đã chốt phiếu thu tháng, không thể thay đổi.');
      return;
    }

    const nextStatus: AttendanceStatus =
      item.status === 'PRESENT' ? 'EXCUSED' :
      item.status === 'EXCUSED' ? 'ABSENT' : 'PRESENT';

    this.setStatus(item, nextStatus);
  }

  markAllPresent(): void {
    if (this.dailyAttendance()?.isClassLocked) {
      this.showToast('Lớp học trong kỳ này đã có học sinh chốt phiếu thu.');
    }

    let modified = false;
    this.dailyItems.update(list =>
      list.map(s => {
        if (!s.isLocked && s.status !== 'PRESENT') {
          modified = true;
          return { ...s, status: 'PRESENT' };
        }
        return s;
      })
    );

    if (modified) {
      this.hasUnsavedChanges.set(true);
      this.showToast('Đã đánh dấu tất cả học sinh hợp lệ: Có mặt');
    }
  }

  saveDailyAttendance(): void {
    const classId = this.selectedClassId();
    const date = this.selectedDate();
    const items = this.dailyItems();

    if (!classId || !date) return;

    if (items.length === 0) {
      this.showError('Lớp học chưa có học sinh để điểm danh');
      return;
    }

    // Build batch payload
    const payloadItems = items.map(s => ({
      studentId: s.studentId,
      status: s.status || 'PRESENT', // default PRESENT if untouched
      note: s.note || null
    }));

    this.isSavingDaily.set(true);
    this.attendanceService.batchUpsertAttendance({
      classId,
      lessonDate: date,
      items: payloadItems
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        this.isSavingDaily.set(false);
        this.hasUnsavedChanges.set(false);
        this.showToast('Lưu điểm danh ngày ' + date + ' thành công');
        this.loadDailyAttendance();
      },
      error: (err) => {
        this.isSavingDaily.set(false);
        this.showError('Không thể lưu điểm danh: ' + (err.error?.message || err.message));
      }
    });
  }

  clearDailyAttendance(): void {
    const classId = this.selectedClassId();
    const date = this.selectedDate();
    if (!classId || !date) return;

    const confirmed = confirm(`Bạn có chắc chắn muốn hủy toàn bộ điểm danh ngày ${date} của lớp không?`);
    if (!confirmed) return;

    this.attendanceService.deleteDailyAttendance(classId, date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showToast('Đã hủy dữ liệu điểm danh ngày ' + date);
          this.hasUnsavedChanges.set(false);
          this.loadDailyAttendance();
        },
        error: (err) => {
          this.showError('Không thể hủy buổi điểm danh: ' + (err.error?.message || err.message));
        }
      });
  }

  openNoteModal(item: DailyAttendanceItem): void {
    this.noteStudent.set(item);
    this.noteText.set(item.note || '');
    this.isNoteModalOpen.set(true);
  }

  saveNote(): void {
    const student = this.noteStudent();
    if (!student) return;

    const text = this.noteText().trim();
    this.dailyItems.update(list =>
      list.map(s => s.studentId === student.studentId ? { ...s, note: text || null } : s)
    );
    this.hasUnsavedChanges.set(true);
    this.isNoteModalOpen.set(false);
    this.showToast(`Đã lưu ghi chú cho ${student.fullName}`);
  }

  closeNoteModal(): void {
    this.isNoteModalOpen.set(false);
    this.noteStudent.set(null);
  }

  // ==========================================
  // MONTHLY ATTENDANCE MATRIX (UC-14, UC-15)
  // ==========================================

  loadMonthlyAttendance(): void {
    const classId = this.selectedClassId();
    const month = this.selectedMonth();
    if (!classId || !month) return;

    this.isMonthlyLoading.set(true);
    this.attendanceService.getMonthlyAttendanceMatrix(classId, month)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isMonthlyLoading.set(false);
          if (res.data) {
            this.monthlyMatrix.set(res.data);
          }
        },
        error: (err) => {
          this.isMonthlyLoading.set(false);
          this.showError('Không thể tải lịch sử tháng: ' + (err.error?.message || err.message));
        }
      });
  }

  onMonthChange(newMonth: string): void {
    this.selectedMonth.set(newMonth);
    this.loadMonthlyAttendance();
  }

  selectMonthPreset(preset: 'thisMonth' | 'lastMonth'): void {
    const target = new Date();
    if (preset === 'lastMonth') {
      target.setMonth(target.getMonth() - 1);
    }
    this.onMonthChange(this.formatMonth(target));
  }

  openEditCellModal(student: StudentMonthlyAttendance, date: string): void {
    if (student.isLocked) {
      this.showToast('Học sinh này đã chốt phiếu thu trong tháng, không được sửa điểm danh.');
      return;
    }

    const record: AttendanceRecord | undefined = student.dailyRecords[date];
    this.editCellStudent.set(student);
    this.editCellDate.set(date);
    this.editCellStatus.set(record?.status || 'PRESENT');
    this.editCellNote.set(record?.note || '');
    this.editCellAttendanceId.set(record?.attendanceId || null);
    this.isEditCellModalOpen.set(true);
  }

  saveEditCell(): void {
    const student = this.editCellStudent();
    const date = this.editCellDate();
    const status = this.editCellStatus();
    const note = this.editCellNote().trim();
    const attendanceId = this.editCellAttendanceId();
    const classId = this.selectedClassId();

    if (!student || !date || !classId) return;

    this.isSavingCell.set(true);

    if (attendanceId) {
      // Update individual record
      this.attendanceService.updateAttendance(attendanceId, { status, note: note || null })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.isSavingCell.set(false);
            this.isEditCellModalOpen.set(false);
            this.showToast('Cập nhật điểm danh ngày ' + date + ' thành công');
            this.loadMonthlyAttendance();
          },
          error: (err) => {
            this.isSavingCell.set(false);
            this.showError('Lỗi cập nhật: ' + (err.error?.message || err.message));
          }
        });
    } else {
      // Create new record via batch endpoint for this single student
      this.attendanceService.batchUpsertAttendance({
        classId,
        lessonDate: date,
        items: [{ studentId: student.studentId, status, note: note || null }]
      }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.isSavingCell.set(false);
          this.isEditCellModalOpen.set(false);
          this.showToast('Thêm điểm danh ngày ' + date + ' thành công');
          this.loadMonthlyAttendance();
        },
        error: (err) => {
          this.isSavingCell.set(false);
          this.showError('Lỗi tạo điểm danh: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  closeEditCellModal(): void {
    this.isEditCellModalOpen.set(false);
    this.editCellStudent.set(null);
  }

  // ==========================================
  // HELPERS & UTILITIES
  // ==========================================

  getRecord(student: StudentMonthlyAttendance, date: string): AttendanceRecord | undefined {
    return student.dailyRecords ? student.dailyRecords[date] : undefined;
  }

  getStatusSymbol(status?: AttendanceStatus): string {
    if (status === 'PRESENT') return '✓';
    if (status === 'EXCUSED') return 'P';
    if (status === 'ABSENT') return 'V';
    return '-';
  }

  formatDay(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    return parts.length >= 3 ? parts[2] : dateStr;
  }

  formatDateVN(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private formatMonth(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  }

  showToast(message: string, type: 'success' | 'warning' | 'info' = 'success'): void {
    this.toastMessage.set(message);
    this.errorMessage.set(null);
    if (type === 'warning') {
      this.toastService.warning(message);
    } else if (type === 'info') {
      this.toastService.info(message);
    } else {
      this.toastService.success(message);
    }
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => {
      this.toastMessage.set(null);
    }, 3500);
  }

  showError(message: string): void {
    this.errorMessage.set(message);
    this.toastService.error(message);
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => {
      this.errorMessage.set(null);
    }, 4500);
  }
}
