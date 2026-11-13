import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingService } from '../../../core/services/billing.service';
import { ClassService } from '../../../core/services/class.service';
import { Invoice, InvoiceStatus } from '../../../core/models/billing.models';
import { ClassItem } from '../../../shared/models/class.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-billing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-billing.component.html',
  styleUrls: ['./admin-billing.component.css'],
})
export class AdminBillingComponent implements OnInit {
  private readonly billingService = inject(BillingService);
  private readonly classService = inject(ClassService);
  private readonly toastService = inject(ToastService);

  loading = signal<boolean>(true);
  invoices = signal<Invoice[]>([]);
  classes = signal<ClassItem[]>([]);

  searchQuery = signal<string>('');
  classFilter = signal<string>('ALL');
  statusFilter = signal<string>('ALL');
  monthFilter = signal<string>(this.getDefaultMonth());

  // Modal State
  selectedInvoice = signal<Invoice | null>(null);
  qrModalOpen = signal<boolean>(false);
  actionLoading = signal<boolean>(false);

  // KPI Computations
  readonly totalInvoices = computed(() => this.invoices().length);
  readonly paidInvoices = computed(() => this.invoices().filter((i) => i.status === 'PAID').length);
  readonly pendingInvoices = computed(() => this.invoices().filter((i) => i.status === 'PENDING').length);

  readonly totalRevenue = computed(() =>
    this.invoices().reduce((acc, i) => acc + (i.totalAmount || 0), 0)
  );
  readonly collectedRevenue = computed(() =>
    this.invoices()
      .filter((i) => i.status === 'PAID')
      .reduce((acc, i) => acc + (i.totalAmount || 0), 0)
  );
  readonly pendingRevenue = computed(() =>
    this.invoices()
      .filter((i) => i.status === 'PENDING')
      .reduce((acc, i) => acc + (i.totalAmount || 0), 0)
  );

  // Filtered List
  readonly filteredInvoices = computed(() => {
    let list = this.invoices();
    const query = this.searchQuery().trim().toLowerCase();
    const cls = this.classFilter();
    const st = this.statusFilter();

    if (query) {
      list = list.filter(
        (i) =>
          (i.studentName && i.studentName.toLowerCase().includes(query)) ||
          (i.parentPhone && i.parentPhone.includes(query)) ||
          (i.transferCode && i.transferCode.toLowerCase().includes(query)) ||
          (i.className && i.className.toLowerCase().includes(query))
      );
    }

    if (cls !== 'ALL') {
      list = list.filter((i) => i.classId === cls);
    }

    if (st !== 'ALL') {
      list = list.filter((i) => i.status === st);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadClasses();
    this.loadInvoices();
  }

  getDefaultMonth(): string {
    const d = new Date();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    return `${d.getFullYear()}-${m}`;
  }

  loadClasses(): void {
    this.classService.getClasses(true).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.classes.set(res.data);
        }
      },
      error: () => {},
    });
  }

  loadInvoices(): void {
    this.loading.set(true);
    const filters: { classId?: string; month?: string; status?: InvoiceStatus } = {};

    if (this.classFilter() !== 'ALL') filters.classId = this.classFilter();
    if (this.monthFilter()) filters.month = this.monthFilter();
    if (this.statusFilter() !== 'ALL') filters.status = this.statusFilter() as InvoiceStatus;

    this.billingService.getInvoices(filters).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.invoices.set(res.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Không thể tải danh sách phiếu thu học phí của cơ sở');
      },
    });
  }

  onFilterChange(): void {
    this.loadInvoices();
  }

  viewQr(inv: Invoice): void {
    this.selectedInvoice.set(inv);
    this.qrModalOpen.set(true);
  }

  closeQrModal(): void {
    this.qrModalOpen.set(false);
    this.selectedInvoice.set(null);
  }

  markPaid(inv: Invoice): void {
    if (confirm(`Xác nhận học sinh "${inv.studentName}" đã nộp đủ ${this.formatCurrency(inv.totalAmount)}?`)) {
      this.actionLoading.set(true);
      this.billingService.markAsPaid(inv.id, { paidMethod: 'TIỀN MẶT / QUẢN LÝ XÁC NHẬN' }).subscribe({
        next: () => {
          this.actionLoading.set(false);
          this.toastService.success('Đã xác nhận gạch nợ thành công');
          this.loadInvoices();
        },
        error: (err: any) => {
          this.actionLoading.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi gạch nợ phiếu thu');
        },
      });
    }
  }

  cancelInvoice(inv: Invoice): void {
    if (confirm(`Bạn có chắc chắn muốn hủy phiếu thu "${inv.transferCode}"?`)) {
      this.actionLoading.set(true);
      this.billingService.cancelInvoice(inv.id).subscribe({
        next: () => {
          this.actionLoading.set(false);
          this.toastService.success('Đã hủy phiếu thu');
          this.loadInvoices();
        },
        error: (err: any) => {
          this.actionLoading.set(false);
          this.toastService.error(err?.error?.message || 'Có lỗi khi hủy phiếu thu');
        },
      });
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }
}
