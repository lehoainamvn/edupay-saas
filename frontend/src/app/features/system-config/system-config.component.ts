import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemParameterService } from '../../core/services/system-parameter.service';
import { ToastService } from '../../core/services/toast.service';
import {
  PARAMETER_GROUPS,
  ParameterGroup,
  ParameterGroupMeta,
  SystemParameterItem
} from '../../core/models/system-parameter.models';

export interface GroupSection {
  code: 'TUITION' | 'NOTIFICATION' | 'SYSTEM';
  title: string;
  badge: string;
  description: string;
  items: SystemParameterItem[];
}

@Component({
  selector: 'app-system-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './system-config.component.html',
  styleUrls: ['./system-config.component.css']
})
export class SystemConfigComponent implements OnInit {
  private readonly paramService = inject(SystemParameterService);
  private readonly toastService = inject(ToastService);

  readonly groups: ParameterGroupMeta[] = PARAMETER_GROUPS;

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly parameters = signal<SystemParameterItem[]>([]);
  readonly loading = signal<boolean>(false);
  readonly reloadingCache = signal<boolean>(false);
  readonly savingKey = signal<string | null>(null);
  readonly resetting = signal<boolean>(false);

  readonly selectedGroup = signal<ParameterGroup>('ALL');
  readonly searchQuery = signal<string>('');

  // Inline edit state
  readonly expandedKey = signal<string | null>(null);
  readonly draftValues = signal<Record<string, string>>({});
  readonly confirmResetOpen = signal<boolean>(false);

  // ─── COMPUTED SIGNALS ───────────────────────────────────────────────────────
  readonly filteredParameters = computed(() => {
    const list = this.parameters();
    const group = this.selectedGroup();
    const query = this.searchQuery().trim().toLowerCase();

    return list.filter((item) => {
      const matchGroup = group === 'ALL' || item.paramGroup === group;
      if (!matchGroup) return false;

      if (!query) return true;
      return (
        item.paramKey.toLowerCase().includes(query) ||
        item.displayName.toLowerCase().includes(query) ||
        (item.description && item.description.toLowerCase().includes(query)) ||
        item.paramValue.toLowerCase().includes(query)
      );
    });
  });

  readonly counts = computed(() => {
    const list = this.parameters();
    return {
      ALL: list.length,
      SYSTEM: list.filter((p) => p.paramGroup === 'SYSTEM').length,
      TUITION: list.filter((p) => p.paramGroup === 'TUITION').length,
      NOTIFICATION: list.filter((p) => p.paramGroup === 'NOTIFICATION').length
    };
  });

  readonly groupSections = computed<GroupSection[]>(() => {
    const items = this.filteredParameters();
    const sections: GroupSection[] = [];

    const tuitionItems = items.filter((p) => p.paramGroup === 'TUITION');
    if (tuitionItems.length > 0) {
      sections.push({
        code: 'TUITION',
        title: 'Học phí & Đối soát',
        badge: 'HỌC PHÍ',
        description: 'Quy định hạn nộp, chu kỳ lập phiếu thu và xử lý quá hạn',
        items: tuitionItems
      });
    }

    const notifItems = items.filter((p) => p.paramGroup === 'NOTIFICATION');
    if (notifItems.length > 0) {
      sections.push({
        code: 'NOTIFICATION',
        title: 'Thông báo & Nhắc nợ',
        badge: 'THÔNG BÁO',
        description: 'Tự động gửi thông báo qua Zalo/SMS và nội dung tin nhắn mẫu',
        items: notifItems
      });
    }

    const systemItems = items.filter((p) => p.paramGroup === 'SYSTEM');
    if (systemItems.length > 0) {
      sections.push({
        code: 'SYSTEM',
        title: 'Hệ thống & Vận hành',
        badge: 'HỆ THỐNG',
        description: 'Tên trung tâm hiển thị, phiên làm việc và quyền giáo viên',
        items: systemItems
      });
    }

    return sections;
  });

  // ─── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadParameters();
  }

  // ─── DATA LOADING ───────────────────────────────────────────────────────────
  loadParameters(): void {
    this.loading.set(true);
    this.paramService.getParameters().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.parameters.set(res.data);
          // Khởi tạo draft map
          const drafts: Record<string, string> = {};
          res.data.forEach((p) => {
            drafts[p.paramKey] = p.paramValue;
          });
          this.draftValues.set(drafts);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(
          err.error?.message || 'Không thể tải danh sách tham số cấu hình'
        );
      }
    });
  }

  reloadCache(): void {
    if (this.reloadingCache()) return;
    this.reloadingCache.set(true);
    this.paramService.reloadCache().subscribe({
      next: () => {
        this.reloadingCache.set(false);
        this.toastService.success(
          'In-memory Cache đã được làm mới đồng bộ từ Database thành công!'
        );
        this.loadParameters();
      },
      error: (err) => {
        this.reloadingCache.set(false);
        this.toastService.error(
          err.error?.message || 'Làm mới In-memory Cache thất bại'
        );
      }
    });
  }

  selectGroup(group: ParameterGroup): void {
    this.selectedGroup.set(group);
  }

  // ─── 1-CLICK DIRECT ACTIONS ─────────────────────────────────────────────────
  toggleBoolean(param: SystemParameterItem): void {
    if (this.savingKey()) return;
    const nextVal = param.paramValue === 'true' ? 'false' : 'true';
    this.saveParam(param.paramKey, nextVal, `Đã chuyển "${param.displayName}" sang: ${nextVal === 'true' ? 'BẬT' : 'TẮT'}`);
  }

  stepNumber(param: SystemParameterItem, delta: number, min: number, max: number): void {
    if (this.savingKey()) return;
    const current = Number(param.paramValue) || min;
    const next = current + delta;
    if (next < min || next > max) {
      this.toastService.warning(`Giá trị phải nằm trong khoảng từ ${min} đến ${max}`);
      return;
    }
    this.saveParam(param.paramKey, String(next), `Đã cập nhật "${param.displayName}" thành ${next}`);
  }

  setChoice(param: SystemParameterItem, choiceVal: string): void {
    if (this.savingKey() || param.paramValue === choiceVal) return;
    this.saveParam(param.paramKey, choiceVal, `Đã cập nhật chính sách "${param.displayName}"`);
  }

  // ─── INLINE EXPAND EDIT (For Text / Template) ──────────────────────────────
  toggleExpand(paramKey: string, currentValue: string): void {
    if (this.expandedKey() === paramKey) {
      this.expandedKey.set(null);
    } else {
      this.expandedKey.set(paramKey);
      this.draftValues.update((d) => ({ ...d, [paramKey]: currentValue }));
    }
  }

  updateDraft(paramKey: string, val: string): void {
    this.draftValues.update((d) => ({ ...d, [paramKey]: val }));
  }

  getDraft(paramKey: string, fallback: string = ''): string {
    return this.draftValues()[paramKey] ?? fallback;
  }

  saveDraft(param: SystemParameterItem): void {
    const val = (this.draftValues()[param.paramKey] ?? '').trim();
    if (!val) {
      this.toastService.warning('Giá trị không được để trống');
      return;
    }

    if (param.dataType === 'NUMBER') {
      const num = Number(val);
      if (isNaN(num)) {
        this.toastService.warning('Giá trị phải là số nguyên hợp lệ');
        return;
      }
      const bounds = this.getNumberBounds(param.paramKey);
      if (num < bounds.min || num > bounds.max) {
        this.toastService.warning(`Giá trị phải từ ${bounds.min} đến ${bounds.max}`);
        return;
      }
    }

    this.saveParam(param.paramKey, val, `Đã cập nhật "${param.displayName}"`, () => {
      this.expandedKey.set(null);
    });
  }

  cancelDraft(param: SystemParameterItem): void {
    this.expandedKey.set(null);
    this.draftValues.update((d) => ({ ...d, [param.paramKey]: param.paramValue }));
  }

  insertPlaceholder(paramKey: string, placeholder: string): void {
    const current = this.getDraft(paramKey, '');
    this.updateDraft(paramKey, current ? current + ' ' + placeholder : placeholder);
  }

  // ─── CORE SAVE FUNCTION (Hot-Reload) ────────────────────────────────────────
  private saveParam(paramKey: string, newVal: string, successMsg?: string, onSuccess?: () => void): void {
    this.savingKey.set(paramKey);

    this.paramService.updateParameter(paramKey, newVal).subscribe({
      next: (res) => {
        this.savingKey.set(null);
        if (res.success && res.data) {
          // Optimistic local state update
          const updatedList = this.parameters().map((item) =>
            item.paramKey === paramKey ? res.data : item
          );
          this.parameters.set(updatedList);
          this.draftValues.update((d) => ({ ...d, [paramKey]: newVal }));

          if (successMsg) {
            this.toastService.success(successMsg);
          }
          if (onSuccess) onSuccess();
        }
      },
      error: (err) => {
        this.savingKey.set(null);
        this.toastService.error(
          err.error?.message || 'Không thể lưu tham số cấu hình'
        );
      }
    });
  }

  // ─── RESET DEFAULTS ─────────────────────────────────────────────────────────
  openConfirmReset(): void {
    this.confirmResetOpen.set(true);
  }

  closeConfirmReset(): void {
    this.confirmResetOpen.set(false);
  }

  confirmReset(): void {
    if (this.resetting()) return;
    this.resetting.set(true);

    this.paramService.resetDefaults().subscribe({
      next: () => {
        this.resetting.set(false);
        this.confirmResetOpen.set(false);
        this.toastService.success(
          'Đã khôi phục toàn bộ tham số về giá trị khuyến nghị ban đầu!'
        );
        this.loadParameters();
      },
      error: (err) => {
        this.resetting.set(false);
        this.toastService.error(
          err.error?.message || 'Khôi phục tham số mặc định thất bại'
        );
      }
    });
  }

  // ─── HELPERS ────────────────────────────────────────────────────────────────
  getNumberBounds(key: string): { min: number; max: number; unit: string } {
    switch (key) {
      case 'TUITION_DUE_DAYS':
        return { min: 1, max: 90, unit: 'ngày' };
      case 'TUITION_AUTO_CREATE_DAY':
        return { min: 1, max: 31, unit: 'ngày trong tháng' };
      case 'NOTIFICATION_REMINDER_BEFORE_DAYS':
        return { min: 1, max: 10, unit: 'ngày' };
      case 'SYSTEM_AUTO_LOGOUT_HOURS':
        return { min: 1, max: 72, unit: 'giờ' };
      default:
        return { min: 1, max: 100, unit: '' };
    }
  }

  getPolicyLabel(val: string): string {
    return val === 'SUSPEND_ATTENDANCE' ? 'Tạm dừng điểm danh' : 'Chỉ nhắc nhở';
  }

  getCleanDescription(param: SystemParameterItem): string {
    switch (param.paramKey) {
      case 'TUITION_OVERDUE_POLICY':
        return 'Quy định hình thức xử lý khi học sinh chưa đóng học phí đúng hạn';
      case 'NOTIFICATION_TEMPLATE_INVOICE':
        return 'Nội dung tin nhắn tự động khi chia sẻ phiếu thu qua Zalo hoặc SMS cho phụ huynh';
      case 'TUITION_AUTO_CREATE_DAY':
        return 'Ngày trong tháng hệ thống tự động tổng hợp số buổi học và lập phiếu thu';
      case 'TUITION_DUE_DAYS':
        return 'Số ngày phụ huynh được phép thanh toán kể từ ngày phát hành phiếu thu';
      case 'NOTIFICATION_AUTO_REMINDER':
        return 'Tự động kích hoạt tin nhắn nhắc nhở phụ huynh khi sắp đến hạn thanh toán';
      case 'NOTIFICATION_REMINDER_BEFORE_DAYS':
        return 'Số ngày hệ thống gửi thông báo nhắc nhở trước khi phiếu thu hết hạn';
      case 'SYSTEM_CENTER_NAME':
        return 'Tên trung tâm hiển thị trên phiếu thu và thông báo gửi phụ huynh';
      case 'SYSTEM_ALLOW_TEACHER_EDIT_FEE':
        return 'Cho phép tài khoản giáo viên điều chỉnh mức học phí buổi khi điểm danh';
      case 'SYSTEM_AUTO_LOGOUT_HOURS':
        return 'Thời lượng phiên làm việc trước khi tài khoản tự động đăng xuất';
      default:
        return param.description || '';
    }
  }
}
