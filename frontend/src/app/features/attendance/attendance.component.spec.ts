import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AttendanceComponent } from './attendance.component';
import { AttendanceService } from '../../core/services/attendance.service';
import { ClassService } from '../../core/services/class.service';
import { ClassItem } from '../../shared/models/class.model';
import { DailyAttendance, MonthlyAttendanceMatrix } from '../../shared/models/attendance.model';

describe('AttendanceComponent', () => {
  let component: AttendanceComponent;
  let fixture: ComponentFixture<AttendanceComponent>;
  let mockAttendanceService: jasmine.SpyObj<AttendanceService>;
  let mockClassService: jasmine.SpyObj<ClassService>;

  const mockClasses: ClassItem[] = [
    {
      id: 'c1',
      name: 'Toán 9A',
      feeType: 'PER_SESSION',
      standardFee: 150000,
      studentCount: 2,
      isArchived: false,
      createdAt: '2026-10-01T00:00:00Z',
      updatedAt: '2026-10-01T00:00:00Z'
    }
  ];

  const mockDailyData: DailyAttendance = {
    classId: 'c1',
    className: 'Toán 9A',
    lessonDate: '2026-10-24',
    totalStudents: 2,
    markedCount: 2,
    presentCount: 2,
    excusedCount: 0,
    absentCount: 0,
    isClassLocked: false,
    students: [
      {
        studentId: 's1',
        fullName: 'Nguyễn Văn An',
        nickname: 'Bé Bo',
        parentPhone: '0912345678',
        attendanceId: 'a1',
        status: 'PRESENT',
        note: null,
        isLocked: false
      },
      {
        studentId: 's2',
        fullName: 'Trần Thị Bình',
        nickname: 'Bé Bông',
        parentPhone: '0987654321',
        attendanceId: 'a2',
        status: 'PRESENT',
        note: null,
        isLocked: false
      }
    ]
  };

  const mockMonthlyData: MonthlyAttendanceMatrix = {
    classId: 'c1',
    className: 'Toán 9A',
    month: '2026-10',
    lessonDates: ['2026-10-20', '2026-10-24'],
    totalClassLessons: 2,
    students: [
      {
        studentId: 's1',
        fullName: 'Nguyễn Văn An',
        nickname: 'Bé Bo',
        attendedCount: 2,
        excusedCount: 0,
        absentCount: 0,
        totalLessons: 2,
        isLocked: false,
        dailyRecords: {
          '2026-10-20': { attendanceId: 'rec1', status: 'PRESENT', note: null },
          '2026-10-24': { attendanceId: 'a1', status: 'PRESENT', note: null }
        }
      }
    ]
  };

  beforeEach(async () => {
    mockAttendanceService = jasmine.createSpyObj('AttendanceService', [
      'getDailyAttendance',
      'batchUpsertAttendance',
      'getMonthlyAttendanceMatrix',
      'updateAttendance',
      'deleteDailyAttendance'
    ]);

    mockClassService = jasmine.createSpyObj('ClassService', ['getClasses']);

    mockClassService.getClasses.and.returnValue(of({
      status: 'SUCCESS',
      message: 'OK',
      data: mockClasses,
      timestamp: '2026-10-24T00:00:00Z'
    }));

    mockAttendanceService.getDailyAttendance.and.returnValue(of({
      status: 'SUCCESS',
      message: 'OK',
      data: mockDailyData,
      timestamp: '2026-10-24T00:00:00Z'
    }));

    mockAttendanceService.getMonthlyAttendanceMatrix.and.returnValue(of({
      status: 'SUCCESS',
      message: 'OK',
      data: mockMonthlyData,
      timestamp: '2026-10-24T00:00:00Z'
    }));

    mockAttendanceService.batchUpsertAttendance.and.returnValue(of({
      status: 'SUCCESS',
      message: 'OK',
      data: [],
      timestamp: '2026-10-24T00:00:00Z'
    }));

    await TestBed.configureTestingModule({
      imports: [AttendanceComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AttendanceService, useValue: mockAttendanceService },
        { provide: ClassService, useValue: mockClassService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AttendanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nên khởi tạo component và tự động chọn lớp học đầu tiên', () => {
    expect(component).toBeTruthy();
    expect(component.classes().length).toBe(1);
    expect(component.selectedClassId()).toBe('c1');
    expect(component.dailyItems().length).toBe(2);
    expect(component.dailyPresentCount()).toBe(2);
    expect(component.dailyAttendanceRate()).toBe(100);
  });

  it('nên cập nhật trạng thái học sinh tức thì (Optimistic UI) khi chọn trạng thái mới', () => {
    const student = component.dailyItems()[0];
    component.setStatus(student, 'EXCUSED');

    expect(component.dailyItems()[0].status).toBe('EXCUSED');
    expect(component.hasUnsavedChanges()).toBeTrue();
    expect(component.dailyExcusedCount()).toBe(1);
    expect(component.dailyPresentCount()).toBe(1);
  });

  it('nên hỗ trợ nút Tất cả có mặt để đánh dấu toàn bộ học sinh sang PRESENT', () => {
    // Đổi 1 em sang ABSENT
    component.setStatus(component.dailyItems()[0], 'ABSENT');
    expect(component.dailyAbsentCount()).toBe(1);

    component.markAllPresent();

    expect(component.dailyItems()[0].status).toBe('PRESENT');
    expect(component.dailyItems()[1].status).toBe('PRESENT');
    expect(component.dailyPresentCount()).toBe(2);
    expect(component.dailyAbsentCount()).toBe(0);
    expect(component.hasUnsavedChanges()).toBeTrue();
  });

  it('không cho phép sửa điểm danh nếu học sinh đã bị khóa do chốt phiếu thu', () => {
    // Giả lập học sinh bị khóa
    component.dailyItems.set([
      {
        studentId: 's_locked',
        fullName: 'Học sinh Bị Khóa',
        status: 'PRESENT',
        isLocked: true
      }
    ]);

    const lockedStudent = component.dailyItems()[0];
    component.setStatus(lockedStudent, 'ABSENT');

    // Trạng thái vẫn là PRESENT, không thay đổi
    expect(component.dailyItems()[0].status).toBe('PRESENT');
    expect(component.toastMessage()).toContain('đã chốt phiếu thu');
  });

  it('nên lưu điểm danh qua batch endpoint khi bấm Lưu buổi học', () => {
    component.saveDailyAttendance();

    expect(mockAttendanceService.batchUpsertAttendance).toHaveBeenCalled();
    expect(component.hasUnsavedChanges()).toBeFalse();
    expect(component.toastMessage()).toContain('thành công');
  });

  it('nên tải ma trận tháng khi chuyển sang tab monthly', () => {
    component.switchTab('monthly');

    expect(component.activeTab()).toBe('monthly');
    expect(mockAttendanceService.getMonthlyAttendanceMatrix).toHaveBeenCalledWith('c1', component.selectedMonth());
    expect(component.monthlyMatrix()?.students.length).toBe(1);
    expect(component.monthlyMatrix()?.totalClassLessons).toBe(2);
  });
});
