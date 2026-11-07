import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  inject,
  effect
} from '@angular/core';
import { RbacService } from '../../core/services/rbac.service';

/**
 * Structural Directive: *appHasPermission
 * Ẩn hoặc hiển thị element dựa trên quyền hạn của người dùng.
 *
 * Cú pháp:
 *   <button *appHasPermission="'invoice:create'">Tạo phiếu thu</button>
 *   <div *appHasPermission="['class:update', 'class:delete']; mode: 'AND'">...</div>
 *   <div *appHasPermission="'report:read'; else: noPermissionTpl">...</div>
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly rbacService = inject(RbacService);

  private requiredPermission: string | string[] = [];
  private mode: 'AND' | 'OR' = 'OR';
  private elseTemplate?: TemplateRef<unknown>;
  private isShown = false;

  @Input('appHasPermission')
  set permission(val: string | string[]) {
    this.requiredPermission = val;
    this.updateView();
  }

  @Input('appHasPermissionMode')
  set permissionMode(val: 'AND' | 'OR') {
    this.mode = val || 'OR';
    this.updateView();
  }

  @Input('appHasPermissionElse')
  set permissionElse(val: TemplateRef<unknown> | undefined) {
    this.elseTemplate = val;
    this.updateView();
  }

  constructor() {
    // Tự động re-evaluate khi permissions hoặc roles trong RbacService thay đổi
    effect(() => {
      this.rbacService.permissions();
      this.rbacService.roles();
      this.updateView();
    });
  }

  private updateView(): void {
    const hasPerm = this.rbacService.hasPermission(this.requiredPermission, this.mode);

    if (hasPerm && !this.isShown) {
      this.viewContainer.clear();
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isShown = true;
    } else if (!hasPerm && this.isShown) {
      this.viewContainer.clear();
      if (this.elseTemplate) {
        this.viewContainer.createEmbeddedView(this.elseTemplate);
      }
      this.isShown = false;
    } else if (!hasPerm && !this.isShown && this.elseTemplate && this.viewContainer.length === 0) {
      this.viewContainer.createEmbeddedView(this.elseTemplate);
    }
  }
}

