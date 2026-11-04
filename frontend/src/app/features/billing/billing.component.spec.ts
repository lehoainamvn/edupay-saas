import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BillingComponent } from './billing.component';
import { ClassService } from '../../core/services/class.service';
import { BillingService } from '../../core/services/billing.service';

describe('BillingComponent', () => {
  let component: BillingComponent;
  let fixture: ComponentFixture<BillingComponent>;
  let classServiceSpy: jasmine.SpyObj<ClassService>;
  let billingServiceSpy: jasmine.SpyObj<BillingService>;

  beforeEach(async () => {
    classServiceSpy = jasmine.createSpyObj('ClassService', ['getClasses']);
    billingServiceSpy = jasmine.createSpyObj('BillingService', [
      'getBillingPreview',
      'generateInvoices',
      'getInvoices',
      'cancelInvoice',
      'markAsPaid'
    ]);

    classServiceSpy.getClasses.and.returnValue(
      of({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: [
          {
            id: 'class-1',
            name: 'Toán 9A',
            feeType: 'PER_LESSON',
            defaultFee: 150000,
            activeStudentsCount: 2,
            isActive: true,
            createdAt: new Date().toISOString()
          }
        ]
      })
    );

    billingServiceSpy.getBillingPreview.and.returnValue(
      of({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          classId: 'class-1',
          className: 'Toán 9A',
          feeType: 'PER_LESSON',
          billingMonth: '2026-10',
          totalStudents: 2,
          totalAttendedCount: 14,
          totalCalculatedAmount: 1920000,
          hasBankConfigured: true,
          items: [
            {
              studentId: 'st-1',
              fullName: 'Trần Minh Quân',
              nickname: 'Tít',
              attendedCount: 8,
              unitPrice: 150000,
              baseAmount: 1200000,
              extraFee: 0,
              discount: 0,
              totalAmount: 1200000,
              isLocked: false
            },
            {
              studentId: 'st-2',
              fullName: 'Lê Ngọc Bích',
              nickname: 'Bông',
              attendedCount: 6,
              unitPrice: 120000,
              baseAmount: 720000,
              extraFee: 0,
              discount: 0,
              totalAmount: 720000,
              isLocked: true,
              existingInvoiceStatus: 'PENDING'
            }
          ]
        }
      })
    );

    await TestBed.configureTestingModule({
      imports: [BillingComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ClassService, useValue: classServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nên khởi tạo component và tải dữ liệu xem trước bảng tính thành công', () => {
    expect(component).toBeTruthy();
    expect(component.classes().length).toBe(1);
    expect(component.editableItems().length).toBe(2);
    // Em thứ nhất chưa bị khóa nên được chọn mặc định
    expect(component.selectedCount()).toBe(1);
    expect(component.selectedTotalAmount()).toBe(1200000);
  });

  it('nên tính lại tổng tiền chính xác khi thêm phụ phí và giảm trừ', () => {
    const item = component.editableItems()[0];
    item.tempExtraFee = 50000;
    item.tempDiscount = 20000;

    const total = component.calculateItemTotal(item);
    // 1.200.000 + 50.000 - 20.000 = 1.230.000
    expect(total).toBe(1230000);
  });

  it('nên chuyển đổi tab mượt mà giữa Bảng tính và Danh sách phiếu thu', () => {
    billingServiceSpy.getInvoices.and.returnValue(
      of({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: []
      })
    );

    component.switchTab('invoices');
    expect(component.activeTab()).toBe('invoices');
    expect(billingServiceSpy.getInvoices).toHaveBeenCalled();
  });
});
