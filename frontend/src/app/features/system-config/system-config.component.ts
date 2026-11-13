import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemParameterService } from '../../core/services/system-parameter.service';
import { ToastService } from '../../core/services/toast.service';
import {
  PARAMETER_GROUPS,
  ParameterGroup,
  ParameterGroupMeta,
  SystemParameterItem,
  CreateSystemParameterRequest,
} from '../../core/models/system-parameter.models';

@Component({
  selector: 'app-system-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './system-config.component.html',
  styleUrls: ['./system-config.component.css'],
})
export class SystemConfigComponent implements OnInit {
  private readonly paramService = inject(SystemParameterService);
  private readonly toastService = inject(ToastService);

  readonly groups: ParameterGroupMeta[] = PARAMETER_GROUPS;

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly parameters = signal<SystemParameterItem[]>([]);
  readonly loading = signal<boolean>(false);
  readonly reloadingCache = signal<boolean>(false);
  readonly resetting = signal<boolean>(false);

  readonly selectedGroup = signal<ParameterGroup>('ALL');
  readonly selectedType = signal<'ALL' | 'NUMBER' | 'BOOLEAN' | 'STRING'>('ALL');
  readonly searchQuery = signal<string>('');

  // ─── MODAL STATES ───────────────────────────────────────────────────────────
  // 1. Edit Modal
  readonly editingItem = signal<SystemParameterItem | null>(null);
  readonly editForm = signal<{
    paramValue: string;
    displayName: string;
    description: string;
  }>({ paramValue: '', displayName: '', description: '' });
  readonly editSaving = signal<boolean>(false);

  // 2. Create Modal
  readonly createModalOpen = signal<boolean>(false);
  readonly createForm = signal<CreateSystemParameterRequest>({
    paramKey: '',
    paramValue: '',
    paramGroup: 'SYSTEM',
    dataType: 'STRING',
    displayName: '',
    description: '',
  });
  readonly createSaving = signal<boolean>(false);

  // 3. Delete Confirmation Modal
  readonly deletingItem = signal<SystemParameterItem | null>(null);
  readonly deleteSaving = signal<boolean>(false);

  // 4. Reset Confirmation Modal
  readonly confirmResetOpen = signal<boolean>(false);

  // ─── COMPUTED SIGNALS ───────────────────────────────────────────────────────
  readonly filteredParameters = computed(() => {
    let list = this.parameters();
    const group = this.selectedGroup();
    const type = this.selectedType();
    const query = this.searchQuery().trim().toLowerCase();

    if (group !== 'ALL') {
      list = list.filter((p) => (p.paramGroup || '').toUpperCase() === group);
    }

    if (type !== 'ALL') {
      list = list.filter((p) => p.dataType === type);
    }

    if (query) {
      list = list.filter(
        (p) =>
          p.paramKey.toLowerCase().includes(query) ||
          p.displayName.toLowerCase().includes(query) ||
          (p.description && p.description.toLowerCase().includes(query)) ||
          p.paramValue.toLowerCase().includes(query)
      );
    }

    return list;
  });

  readonly counts = computed(() => {
    const list = this.parameters();
    return {
      ALL: list.length,
      SYSTEM: list.filter((p) => (p.paramGroup || '').toUpperCase() === 'SYSTEM').length,
      TUITION: list.filter((p) => (p.paramGroup || '').toUpperCase() === 'TUITION').length,
      NOTIFICATION: list.filter((p) => (p.paramGroup || '').toUpperCase() === 'NOTIFICATION').length,
      SECURITY: list.filter((p) => (p.paramGroup || '').toUpperCase() === 'SECURITY').length,
    };
  });

  // ─── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadParameters();
  }

  loadParameters(): void {
    this.loading.set(true);
    this.paramService.getParameters().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.parameters.set(res.data);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(
          err?.error?.message || 'Không thể tải danh sách tham số cấu hình.'
        );
      },
    });
  }

  // ─── QUICK TOGGLE FOR BOOLEAN PARAMETERS ────────────────────────────────────
  toggleBoolean(item: SystemParameterItem): void {
    const currentBool = item.paramValue === 'true' || item.paramValue === '1';
    const nextVal = (!currentBool).toString();

    this.paramService.updateParameter(item.paramKey, nextVal).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.parameters.update((list) =>
            list.map((p) => (p.paramKey === item.paramKey ? res.data : p))
          );
          this.toastService.success(
            `Đã chuyển [${item.displayName}]: ${nextVal === 'true' ? 'BẬT' : 'TẮT'} (Hot-reload 0ms)`
          );
        }
      },
      error: (err) => {
        this.toastService.error(
          err?.error?.message || 'Không thể cập nhật tham số.'
        );
      },
    });
  }

  // ─── EDIT MODAL ─────────────────────────────────────────────────────────────
  openEditModal(item: SystemParameterItem): void {
    this.editingItem.set(item);
    this.editForm.set({
      paramValue: item.paramValue,
      displayName: item.displayName,
      description: item.description || '',
    });
  }

  closeEditModal(): void {
    this.editingItem.set(null);
    this.editSaving.set(false);
  }

  submitEdit(): void {
    const item = this.editingItem();
    if (!item) return;

    const form = this.editForm();
    if (!form.paramValue || !form.paramValue.trim()) {
      this.toastService.warning('Giá trị tham số không được để trống.');
      return;
    }

    this.editSaving.set(true);
    this.paramService
      .updateParameter(item.paramKey, {
        paramValue: form.paramValue.trim(),
        displayName: form.displayName.trim(),
        description: form.description.trim(),
      })
      .subscribe({
        next: (res) => {
          this.editSaving.set(false);
          this.closeEditModal();
          if (res.success && res.data) {
            this.parameters.update((list) =>
              list.map((p) => (p.paramKey === item.paramKey ? res.data : p))
            );
            this.toastService.success(
              `Đã cập nhật tham số [${item.paramKey}] thành công! Caching đã được Hot-reload.`
            );
          }
        },
        error: (err) => {
          this.editSaving.set(false);
          this.toastService.error(
            err?.error?.message || 'Có lỗi xảy ra khi lưu tham số.'
          );
        },
      });
  }

  // ─── CREATE MODAL ───────────────────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm.set({
      paramKey: '',
      paramValue: '',
      paramGroup: 'SYSTEM',
      dataType: 'STRING',
      displayName: '',
      description: '',
    });
    this.createModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.createModalOpen.set(false);
    this.createSaving.set(false);
  }

  submitCreate(): void {
    const form = this.createForm();

    if (!form.paramKey || !form.paramKey.trim()) {
      this.toastService.warning('Mã tham số không được để trống.');
      return;
    }

    const cleanKey = form.paramKey.trim().toUpperCase();
    if (!/^[A-Z0-9_]{3,100}$/.test(cleanKey)) {
      this.toastService.warning(
        'Mã tham số chỉ gồm chữ cái IN HOA, chữ số và gạch dưới (VD: TUITION_DUE_DAYS).'
      );
      return;
    }

    if (!form.displayName || !form.displayName.trim()) {
      this.toastService.warning('Tên hiển thị không được để trống.');
      return;
    }

    if (!form.paramValue || !form.paramValue.trim()) {
      this.toastService.warning('Giá trị tham số không được để trống.');
      return;
    }

    this.createSaving.set(true);
    this.paramService
      .createParameter({
        ...form,
        paramKey: cleanKey,
        paramValue: form.paramValue.trim(),
        displayName: form.displayName.trim(),
      })
      .subscribe({
        next: (res) => {
          this.createSaving.set(false);
          this.closeCreateModal();
          if (res.success && res.data) {
            this.parameters.update((list) => [...list, res.data]);
            this.toastService.success(
              `Đã thêm tham số mới [${cleanKey}] vào hệ thống!`
            );
          }
        },
        error: (err) => {
          this.createSaving.set(false);
          this.toastService.error(
            err?.error?.message || 'Không thể tạo tham số mới.'
          );
        },
      });
  }

  // ─── DELETE MODAL ───────────────────────────────────────────────────────────
  openDeleteModal(item: SystemParameterItem): void {
    if (item.isSystem) {
      this.toastService.warning(
        `Tham số [${item.paramKey}] là tham số cốt lõi của hệ thống, không được phép xóa!`
      );
      return;
    }
    this.deletingItem.set(item);
  }

  closeDeleteModal(): void {
    this.deletingItem.set(null);
    this.deleteSaving.set(false);
  }

  submitDelete(): void {
    const item = this.deletingItem();
    if (!item) return;

    this.deleteSaving.set(true);
    this.paramService.deleteParameter(item.paramKey).subscribe({
      next: (res) => {
        this.deleteSaving.set(false);
        this.closeDeleteModal();
        if (res.success) {
          this.parameters.update((list) =>
            list.filter((p) => p.paramKey !== item.paramKey)
          );
          this.toastService.success(
            `Đã xóa tham số [${item.paramKey}] khỏi hệ thống.`
          );
        }
      },
      error: (err) => {
        this.deleteSaving.set(false);
        this.toastService.error(
          err?.error?.message || 'Không thể xóa tham số cấu hình.'
        );
      },
    });
  }

  // ─── CACHE HOT-RELOAD & RESET DEFAULTS ───────────────────────────────────────
  handleReloadCache(): void {
    this.reloadingCache.set(true);
    this.paramService.reloadCache().subscribe({
      next: () => {
        this.reloadingCache.set(false);
        this.toastService.success(
          `Đã làm mới In-memory Cache thành công! Tất cả ${this.parameters().length} tham số đã đồng bộ vào RAM.`
        );
        this.loadParameters();
      },
      error: (err) => {
        this.reloadingCache.set(false);
        this.toastService.error(
          err?.error?.message || 'Không thể làm mới cache.'
        );
      },
    });
  }

  openResetModal(): void {
    this.confirmResetOpen.set(true);
  }

  closeResetModal(): void {
    this.confirmResetOpen.set(false);
    this.resetting.set(false);
  }

  confirmReset(): void {
    this.resetting.set(true);
    this.paramService.resetDefaults().subscribe({
      next: () => {
        this.resetting.set(false);
        this.closeResetModal();
        this.toastService.success(
          'Đã khôi phục toàn bộ tham số về giá trị mặc định của hệ thống!'
        );
        this.loadParameters();
      },
      error: (err) => {
        this.resetting.set(false);
        this.toastService.error(
          err?.error?.message || 'Không thể khôi phục tham số mặc định.'
        );
      },
    });
  }

  // ─── HELPERS ────────────────────────────────────────────────────────────────
  copyKey(key: string, event: MouseEvent): void {
    event.stopPropagation();
    navigator.clipboard.writeText(key).then(() => {
      this.toastService.info(`Đã sao chép mã tham số: ${key}`);
    });
  }

  formatValuePreview(item: SystemParameterItem): string {
    const val = item.paramValue;
    const key = item.paramKey;

    if (item.dataType === 'BOOLEAN') {
      return val === 'true' || val === '1' ? 'BẬT (True)' : 'TẮT (False)';
    }

    if (item.dataType === 'NUMBER') {
      if (key.includes('HOURS')) return `${val} giờ`;
      if (key.includes('DAYS')) return `${val} ngày`;
      if (key.includes('MINUTES')) return `${val} phút`;
      if (key.includes('ATTEMPTS')) return `${val} lần`;
      if (key.includes('STUDENTS')) return `${val} học sinh`;
      return val;
    }

    if (val.length > 45) {
      return val.substring(0, 42) + '...';
    }

    return val;
  }

  getGroupBadge(group: string): { label: string; class: string } {
    const u = (group || '').toUpperCase();
    switch (u) {
      case 'SYSTEM':
        return { label: 'HỆ THỐNG', class: 'badge-system' };
      case 'TUITION':
        return { label: 'HỌC PHÍ', class: 'badge-tuition' };
      case 'NOTIFICATION':
        return { label: 'THÔNG BÁO', class: 'badge-notification' };
      case 'SECURITY':
        return { label: 'BẢO MẬT', class: 'badge-security' };
      default:
        return { label: u, class: 'badge-custom' };
    }
  }

  getTypeBadge(type: string): { label: string; class: string } {
    const u = (type || '').toUpperCase();
    switch (u) {
      case 'NUMBER':
        return { label: 'NUMBER', class: 'type-number' };
      case 'BOOLEAN':
        return { label: 'BOOLEAN', class: 'type-boolean' };
      case 'STRING':
        return { label: 'STRING', class: 'type-string' };
      case 'JSON':
        return { label: 'JSON', class: 'type-json' };
      default:
        return { label: u, class: 'type-default' };
    }
  }
}
