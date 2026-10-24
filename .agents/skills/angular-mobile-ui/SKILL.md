---
name: angular-mobile-ui
description: Best practices, guidelines, and code snippets for building modern Mobile-First UI components in Angular 17+ with Standalone Components, Signals, and Touch UX for EduPay SaaS.
---

# 📱 Angular 17+ Mobile-First UI Development Skill

Use this skill whenever you are generating, refactoring, or reviewing Angular components, styles, forms, or views for EduPay SaaS.

---

## 1. Triết Lý Thiết Kế: Mobile-First Touch UX
EduPay phục vụ giáo viên dạy kèm và quản lý trung tâm. 90% thao tác diễn ra trên điện thoại di động (iPhone / Android).

### Tiêu Chuẩn Bắt Buộc:
- **Kích thước vùng bấm (Touch Target):** Tối thiểu **44px × 44px**, lý tưởng **48px** (`min-h-[48px]`, `min-w-[48px]`).
- **Khoảng cách phím bấm:** Cách nhau ít nhất **8px** để tránh bấm nhầm khi đi lại trong lớp.
- **Phản hồi tức thì (Optimistic UI):** Khi điểm danh học sinh (Có mặt / Nghỉ phép / Vắng), đổi màu ngay trên UI lập tức mà không chờ request server hoàn tất. Nếu request lỗi thì hoàn tác (rollback) và hiển thị Toast thông báo.
- **Không dùng Table ngang trên Mobile:** Trên màn hình `< 640px` (SM), chuyển danh sách học sinh / hóa đơn thành dạng **Card** hoặc **List Item** xếp dọc.

---

## 2. Tiêu Chuẩn Standalone Component (Angular 17+)
Mọi component mới phải là **Standalone Component**:

```typescript
import { Component, ChangeDetectionStrategy, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-attendance-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './attendance-card.component.html',
  styleUrls: ['./attendance-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttendanceCardComponent {
  // Dùng Signals input/output mới của Angular 17.1+
  student = input.required<StudentDto>();
  statusChange = output<AttendanceStatus>();

  currentStatus = signal<AttendanceStatus>('PRESENT');

  setStatus(status: AttendanceStatus): void {
    this.currentStatus.set(status);
    this.statusChange.emit(status);
  }
}
```

---

## 3. Hệ Thống Màu Trạng Thái Điểm Danh

| Trạng thái | Mã Hex | Tailwind Class | Mô tả |
| :--- | :--- | :--- | :--- |
| **Có mặt (Present)** | `#22c55e` | `bg-emerald-500 text-white` | Học sinh đi học đủ |
| **Nghỉ có phép (Excused)** | `#eab308` | `bg-amber-500 text-white` | Phụ huynh đã xin phép |
| **Nghỉ không phép (Absent)** | `#ef4444` | `bg-rose-500 text-white` | Vắng không báo trước |

---

## 4. Quản Lý Form & Validation (Reactive Forms)
Mọi form nhập liệu đều phải thông báo lỗi trực quan ngay dưới trường dữ liệu:

```typescript
export class CreateClassComponent {
  private fb = inject(FormBuilder);
  private classService = inject(ClassService);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    pricePerSession: [0, [Validators.required, Validators.min(1000)]],
    scheduleNote: ['']
  });

  isFieldInvalid(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
```

Template HTML:
```html
<div class="mb-4">
  <label class="block text-sm font-semibold text-slate-700 mb-1">Tên lớp học *</label>
  <input 
    type="text" 
    formControlName="name"
    class="w-full h-12 px-3 rounded-lg border text-base"
    [class.border-rose-500]="isFieldInvalid('name')"
    placeholder="VD: Toán 9 Nâng Cao" />
  
  @if (isFieldInvalid('name')) {
    <p class="text-xs text-rose-500 mt-1">Tên lớp học bắt buộc và tối thiểu 3 ký tự</p>
  }
</div>
```

---

## 5. Tránh Rò Rỉ Bộ Nhớ (Memory Leak Prevention)
- Tuyệt đối không để `subscribe()` mở vô hạn mà không hủy.
- Sử dụng `takeUntilDestroyed()` hoặc `async` pipe:

```typescript
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({ ... })
export class StudentListComponent {
  private destroyRef = inject(DestroyRef);
  private studentService = inject(StudentService);

  loadStudents(): void {
    this.studentService.getStudents()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => this.students.set(res.data),
        error: (err) => this.toast.error('Không thể tải danh sách học sinh')
      });
  }
}
```
