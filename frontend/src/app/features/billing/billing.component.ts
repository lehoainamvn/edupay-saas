import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { ClassService } from '../../core/services/class.service';
import { BillingService } from '../../core/services/billing.service';
import { ClassItem } from '../../shared/models/class.model';
import {
  BillingPreview,
  BillingPreviewItem,
  Invoice,
  InvoiceBatchGenerateRequest,
  InvoiceItemGenerateRequest,
  InvoiceStatus
} from '../../core/models/billing.models';

interface EditableBillingItem extends BillingPreviewItem {
  selected: boolean;
  tempExtraFee: number;
  tempDiscount: number;
  tempNote: string;
}

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './billing.component.html',
  styleUrls: ['./billing.component.css']
})
export class BillingComponent implements OnInit {
  private readonly classService = inject(ClassService);
  private readonly billingService = inject(BillingService);

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly classes = signal<ClassItem[]>([]);
  readonly selectedClassId = signal<string>('');
  readonly selectedMonth = signal<string>(this.getDefaultMonth());
  readonly activeTab = signal<'calculator' | 'invoices'>('calculator');

  readonly loadingClasses = signal<boolean>(false);
  readonly loadingPreview = signal<boolean>(false);
  readonly loadingInvoices = signal<boolean>(false);
  readonly submittingBatch = signal<boolean>(false);
  readonly previewError = signal<string | null>(null);

  readonly toastMessage = signal<string | null>(null);
  readonly toastType = signal<'success' | 'error'>('success');

  readonly previewData = signal<BillingPreview | null>(null);
  readonly editableItems = signal<EditableBillingItem[]>([]);

  readonly invoices = signal<Invoice[]>([]);
  readonly invoiceStatusFilter = signal<string>('ALL');

  // Modals state
  readonly showConfirmModal = signal<boolean>(false);
  readonly showQrModal = signal<boolean>(false);
  readonly activeQrInvoice = signal<Invoice | null>(null);
  readonly showMarkPaidModal = signal<boolean>(false);
  readonly activePayInvoice = signal<Invoice | null>(null);
  readonly payMethod = signal<string>('CASH');
  readonly payNote = signal<string>('');

  // ─── COMPUTED SIGNALS ───────────────────────────────────────────────────────
  readonly currentClass = computed(() => {
    const id = this.selectedClassId();
    return this.classes().find((c) => c.id === id) || null;
  });

  readonly selectableItems = computed(() => {
    return this.editableItems().filter((item) => !item.isLocked);
  });

  readonly selectedItems = computed(() => {
    return this.editableItems().filter((item) => !item.isLocked && item.selected);
  });

  readonly selectedCount = computed(() => this.selectedItems().length);

  readonly isAllSelected = computed(() => {
    const selectable = this.selectableItems();
    return selectable.length > 0 && selectable.every((item) => item.selected);
  });

  readonly selectedTotalAmount = computed(() => {
    return this.selectedItems().reduce((sum, item) => sum + this.calculateItemTotal(item), 0);
  });

  readonly filteredInvoices = computed(() => {
    const filter = this.invoiceStatusFilter();
    const list = this.invoices();
    if (filter === 'ALL') return list;
    return list.filter((inv) => inv.status === filter);
  });

  // ─── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadClasses();
  }

  // ─── LOAD DATA ──────────────────────────────────────────────────────────────
  loadClasses(): void {
    this.loadingClasses.set(true);
    this.classService.getClasses(false).subscribe({
      next: (res) => {
        this.loadingClasses.set(false);
        if (res.success && res.data && res.data.length > 0) {
          this.classes.set(res.data);
          this.selectedClassId.set(res.data[0].id);
          this.loadPreview();
        } else {
          this.classes.set([]);
        }
      },
      error: () => {
        this.loadingClasses.set(false);
        this.showToast('Không thể tải danh sách lớp học', 'error');
      }
    });
  }

  loadPreview(): void {
    const classId = this.selectedClassId();
    const month = this.selectedMonth();
    if (!classId || !month) return;

    this.loadingPreview.set(true);
    this.previewError.set(null);
    this.billingService.getBillingPreview(classId, month).subscribe({
      next: (res) => {
        this.loadingPreview.set(false);
        if (res.success && res.data) {
          this.previewData.set(res.data);
          const mapped: EditableBillingItem[] = res.data.items.map((it) => ({
            ...it,
            selected: !it.isLocked,
            tempExtraFee: it.extraFee || 0,
            tempDiscount: it.discount || 0,
            tempNote: ''
          }));
          this.editableItems.set(mapped);
        }
      },
      error: (err) => {
        this.loadingPreview.set(false);
        const msg =
          err.status === 404
            ? 'Backend hiện tại chưa có API /api/billing (Lỗi 404). Vui lòng RESTART lại ứng dụng Backend Java (Spring Boot) để nạp các file Controller mới!'
            : err.error?.message || 'Không thể tải bảng tính học phí';
        this.previewError.set(msg);
        this.showToast(msg, 'error');
      }
    });
  }

  loadInvoices(): void {
    const classId = this.selectedClassId();
    const month = this.selectedMonth();
    this.loadingInvoices.set(true);

    this.billingService
      .getInvoices({
        classId: classId || undefined,
        month: month || undefined
      })
      .subscribe({
        next: (res) => {
          this.loadingInvoices.set(false);
          if (res.success && res.data) {
            this.invoices.set(res.data);
          }
        },
        error: () => {
          this.loadingInvoices.set(false);
          this.showToast('Không thể tải danh sách phiếu thu', 'error');
        }
      });
  }

  // ─── USER ACTIONS ───────────────────────────────────────────────────────────
  onClassChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedClassId.set(select.value);
    if (this.activeTab() === 'calculator') {
      this.loadPreview();
    } else {
      this.loadInvoices();
    }
  }

  onMonthChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedMonth.set(input.value);
    if (this.activeTab() === 'calculator') {
      this.loadPreview();
    } else {
      this.loadInvoices();
    }
  }

  switchTab(tab: 'calculator' | 'invoices'): void {
    this.activeTab.set(tab);
    if (tab === 'calculator') {
      this.loadPreview();
    } else {
      this.loadInvoices();
    }
  }

  toggleSelectAll(): void {
    const newState = !this.isAllSelected();
    this.editableItems.update((items) =>
      items.map((it) => (it.isLocked ? it : { ...it, selected: newState }))
    );
  }

  toggleItemSelection(studentId: string): void {
    this.editableItems.update((items) =>
      items.map((it) =>
        it.studentId === studentId && !it.isLocked ? { ...it, selected: !it.selected } : it
      )
    );
  }

  calculateItemTotal(item: EditableBillingItem): number {
    const base = item.baseAmount || 0;
    const extra = Number(item.tempExtraFee) || 0;
    const discount = Number(item.tempDiscount) || 0;
    const total = base + extra - discount;
    return total > 0 ? total : 0;
  }

  openConfirmModal(): void {
    if (this.selectedCount() === 0) {
      this.showToast('Vui lòng chọn ít nhất 1 học sinh để xuất phiếu thu', 'error');
      return;
    }
    this.showConfirmModal.set(true);
  }

  closeConfirmModal(): void {
    this.showConfirmModal.set(false);
  }

  confirmGenerateInvoices(): void {
    const classId = this.selectedClassId();
    const month = this.selectedMonth();
    const selected = this.selectedItems();

    if (!classId || !month || selected.length === 0) return;

    this.submittingBatch.set(true);
    const items: InvoiceItemGenerateRequest[] = selected.map((it) => ({
      studentId: it.studentId,
      attendedCount: it.attendedCount,
      unitPrice: it.unitPrice,
      extraFee: Number(it.tempExtraFee) || 0,
      discount: Number(it.tempDiscount) || 0,
      note: it.tempNote || undefined
    }));

    const payload: InvoiceBatchGenerateRequest = {
      classId,
      billingMonth: month,
      items
    };

    this.billingService.generateInvoices(payload).subscribe({
      next: (res) => {
        this.submittingBatch.set(false);
        this.closeConfirmModal();
        this.showToast(`Đã xuất thành công ${res.data?.length || 0} phiếu thu!`, 'success');
        // Chuyển sang tab phiếu thu để xem danh sách vừa xuất
        this.switchTab('invoices');
      },
      error: (err) => {
        this.submittingBatch.set(false);
        const msg = err.error?.message || 'Có lỗi xảy ra khi xuất phiếu thu';
        this.showToast(msg, 'error');
      }
    });
  }

  copyPublicLink(invoice: Invoice, event?: Event): void {
    if (event) event.stopPropagation();
    const fullUrl = `${window.location.origin}/p/${invoice.publicHash}`;
    navigator.clipboard.writeText(fullUrl).then(() => {
      this.showToast(`Đã sao chép link phụ huynh: ${invoice.studentName}`, 'success');
    });
  }

  copyZaloMessage(invoice: Invoice, event?: Event): void {
    if (event) event.stopPropagation();
    const fullUrl = `${window.location.origin}/p/${invoice.publicHash}`;
    const formattedAmount = this.formatCurrency(invoice.totalAmount);
    const text = `Dạ em gửi phiếu báo học phí tháng ${invoice.billingMonth} của bé ${invoice.studentName} (${invoice.nickname || ''}) ạ:\n- Số buổi học: ${invoice.attendedCount} buổi\n- Tổng học phí: ${formattedAmount}\n- Link xem chi tiết và quét mã QR chuyển khoản:\n${fullUrl}\nEm cảm ơn ba mẹ!`;

    navigator.clipboard.writeText(text).then(() => {
      this.showToast('Đã sao chép mẫu tin nhắn Zalo kèm link phiếu thu!', 'success');
    });
  }

  viewQr(invoice: Invoice, event?: Event): void {
    if (event) event.stopPropagation();
    this.activeQrInvoice.set(invoice);
    this.showQrModal.set(true);
  }

  closeQrModal(): void {
    this.showQrModal.set(false);
    this.activeQrInvoice.set(null);
  }

  copyText(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.showToast(`Đã sao chép ${label}!`, 'success');
    });
  }

  openMarkPaidModal(invoice: Invoice, event?: Event): void {
    if (event) event.stopPropagation();
    this.activePayInvoice.set(invoice);
    this.payMethod.set('CASH');
    this.payNote.set('');
    this.showMarkPaidModal.set(true);
  }

  closeMarkPaidModal(): void {
    this.showMarkPaidModal.set(false);
    this.activePayInvoice.set(null);
  }

  confirmMarkPaid(): void {
    const inv = this.activePayInvoice();
    if (!inv) return;

    this.billingService
      .markAsPaid(inv.id, {
        paidMethod: this.payMethod(),
        note: this.payNote()
      })
      .subscribe({
        next: () => {
          this.closeMarkPaidModal();
          this.showToast(`Đã xác nhận thanh toán cho bé ${inv.studentName}!`, 'success');
          this.loadInvoices();
        },
        error: (err) => {
          const msg = err.error?.message || 'Không thể cập nhật trạng thái phiếu thu';
          this.showToast(msg, 'error');
        }
      });
  }

  cancelInvoice(invoice: Invoice, event?: Event): void {
    if (event) event.stopPropagation();
    const confirmed = confirm(
      `Bạn có chắc chắn muốn hủy phiếu thu của học sinh "${invoice.studentName}" không?\n\nSau khi hủy, bạn có thể chỉnh sửa lại điểm danh của học sinh trong tháng này.`
    );
    if (!confirmed) return;

    this.billingService.cancelInvoice(invoice.id).subscribe({
      next: () => {
        this.showToast(`Đã hủy phiếu thu của ${invoice.studentName}`, 'success');
        this.loadInvoices();
      },
      error: (err) => {
        const msg = err.error?.message || 'Không thể hủy phiếu thu';
        this.showToast(msg, 'error');
      }
    });
  }

  // ─── HELPERS ────────────────────────────────────────────────────────────────
  formatCurrency(amount: number | null | undefined): string {
    if (amount == null) return '0 đ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }

  formatDate(isoString: string | undefined): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage.set(message);
    this.toastType.set(type);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 3500);
  }

  private getDefaultMonth(): string {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  }
}
