import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { StudentsComponent } from './students.component';

describe('StudentsComponent', () => {
  let component: StudentsComponent;
  let fixture: ComponentFixture<StudentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudentsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with initial students and active status filter', () => {
    expect(component).toBeTruthy();
    expect(component.students().length).toBeGreaterThan(0);
    expect(component.statusFilter()).toBe('STUDYING');
  });

  it('should filter students via instant search by nickname and parent phone', () => {
    component.setStatusFilter('ALL');
    component.search.set('Bé Tít');
    expect(component.visibleStudents().length).toBe(1);
    expect(component.visibleStudents()[0].fullName).toBe('Trần Minh Quân');

    component.search.set('0912111222');
    expect(component.visibleStudents().length).toBe(1);
    expect(component.visibleStudents()[0].fullName).toBe('Nguyễn Minh Anh');
  });

  it('should validate Vietnamese phone numbers and detect duplicate phone in the same class', () => {
    component.openCreateModal();
    component.studentForm.patchValue({
      classId: 'd0000000-0000-0000-0000-000000000001',
      fullName: 'Học Sinh Mới',
      parentPhone: '0123456789' // Đầu số không hợp lệ
    });
    component.saveStudent();
    expect(component.isPhoneFormatInvalid()).toBeTrue();

    // Trùng SĐT phụ huynh trong cùng lớp 8A (Nguyễn Minh Anh đang dùng 0912111222)
    component.studentForm.patchValue({
      parentPhone: '0912111222'
    });
    expect(component.isDuplicatePhoneInClass()).toBeTrue();
  });

  it('should create a student with custom tuition fee when valid', () => {
    const initialTotal = component.students().length;
    component.openCreateModal();
    component.toggleCustomFeeMode(true);
    component.studentForm.patchValue({
      classId: 'd0000000-0000-0000-0000-000000000001',
      fullName: 'Đinh Gia Huy',
      nickname: 'Bé Gấu',
      parentName: 'Chị Mai',
      parentPhone: '0934567890',
      customFee: 130000
    });

    component.saveStudent();

    expect(component.students().length).toBe(initialTotal + 1);
    expect(component.students()[0].fullName).toBe('Đinh Gia Huy');
    expect(component.students()[0].customFee).toBe(130000);
    expect(component.students()[0].effectiveFee).toBe(130000);
  });
});
