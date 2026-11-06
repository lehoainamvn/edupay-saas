import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SettingsComponent } from './settings.component';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let fixture: ComponentFixture<SettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with 54+ default banks and MBBank preselected', () => {
    expect(component).toBeTruthy();
    expect(component.banks().length).toBeGreaterThanOrEqual(54);
    expect(component.selectedBankBin()).toBe('970422');
    expect(component.selectedBankName()).toBe('MBBank');
  });

  it('should not show liveQrUrl until all required fields are filled', () => {
    // Ban đầu chưa nhập STK và tên
    expect(component.isInfoComplete()).toBeFalse();
    expect(component.liveQrUrl()).toBeNull();

    // Chỉ nhập STK
    component.accountNumber.set('0988111222');
    expect(component.isInfoComplete()).toBeFalse();
    expect(component.liveQrUrl()).toBeNull();

    // Nhập đủ Tên chủ tài khoản
    component.accountName.set('LE HOAI NAM');
    expect(component.isInfoComplete()).toBeTrue();
    expect(component.liveQrUrl()).not.toBeNull();
    expect(component.liveQrUrl()).toContain('970422-0988111222-compact2.png');
    expect(component.liveQrUrl()).toContain('LE%20HOAI%20NAM');
    expect(component.liveQrUrl()).toContain('HP9A01210');
  });

  it('should filter banks correctly by name, code or bin query', () => {
    component.bankSearchQuery.set('Vietcombank');
    const filtered = component.filteredBanks();
    expect(filtered.length).toBeGreaterThan(0);
    expect(filtered.some((b) => b.shortName === 'Vietcombank')).toBeTrue();

    component.bankSearchQuery.set('970407');
    const tcbFiltered = component.filteredBanks();
    expect(tcbFiltered.length).toBe(1);
    expect(tcbFiltered[0].shortName).toBe('Techcombank');
  });

  it('should dynamically update sample transfer code and QR addInfo when prefix changes', () => {
    component.accountNumber.set('0988111222');
    component.accountName.set('NGUYEN THI MAI');
    component.transferPrefix.set('TOAN9A');

    expect(component.sampleTransferCode()).toBe('TOAN9A9A01210');
    expect(component.liveQrUrl()).toContain('addInfo=TOAN9A9A01210');
  });
});
