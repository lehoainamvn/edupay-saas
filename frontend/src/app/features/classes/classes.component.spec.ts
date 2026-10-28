import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClassesComponent } from './classes.component';

describe('ClassesComponent', () => {
  let component: ClassesComponent;
  let fixture: ComponentFixture<ClassesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClassesComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClassesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nên khởi tạo component thành công với 3 lớp đang mở và 1 lớp đã lưu trữ', () => {
    expect(component).toBeTruthy();
    expect(component.activeClasses().length).toBe(3);
    expect(component.archivedCount()).toBe(1);
    expect(component.totalActiveStudents()).toBe(24);
  });

  it('nên lọc đúng theo hình thức học phí (session / monthly)', () => {
    component.setFeeFilter('monthly');
    expect(component.visibleClasses().length).toBe(1);
    expect(component.visibleClasses()[0].name).toContain('Toán Tư Duy Lớp 6B');

    component.setFeeFilter('session');
    expect(component.visibleClasses().length).toBe(2);
  });

  it('nên mở popup tạo mới và thêm lớp học mới khi dữ liệu hợp lệ', () => {
    component.openCreateModal();
    expect(component.modalOpen()).toBeTrue();

    component.form.patchValue({
      name: 'Toán Hình 9A',
      feeType: 'session',
      fee: 160000,
      schedule: 'Thứ 4, Thứ 7 · 18:00'
    });

    component.saveClass();

    expect(component.modalOpen()).toBeFalse();
    expect(component.activeClasses().length).toBe(4);
    expect(component.activeClasses().some(x => x.name === 'Toán Hình 9A')).toBeTrue();
  });

  it('nên chuyển trạng thái lưu trữ (soft delete) để giữ lịch sử khi bấm Lưu trữ', () => {
    const firstClass = component.activeClasses()[0];
    component.toggleArchive(firstClass);

    expect(component.activeClasses().length).toBe(2);
    expect(component.archivedCount()).toBe(2);
  });
});
