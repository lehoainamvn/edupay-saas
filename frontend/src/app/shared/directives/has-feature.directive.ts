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
 * Structural Directive: *appHasFeature
 * Ẩn hoặc hiển thị element dựa trên Feature Flag của Tenant/User.
 *
 * Cú pháp:
 *   <div *appHasFeature="'AUTO_RECONCILE'">...</div>
 */
@Directive({
  selector: '[appHasFeature]',
  standalone: true
})
export class HasFeatureDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly rbacService = inject(RbacService);

  private featureId = '';
  private elseTemplate?: TemplateRef<unknown>;
  private isShown = false;

  @Input('appHasFeature')
  set feature(val: string) {
    this.featureId = val;
    this.updateView();
  }

  @Input('appHasFeatureElse')
  set featureElse(val: TemplateRef<unknown> | undefined) {
    this.elseTemplate = val;
    this.updateView();
  }

  constructor() {
    effect(() => {
      this.rbacService.features();
      this.updateView();
    });
  }

  private updateView(): void {
    const hasFeat = this.rbacService.hasFeature(this.featureId);

    if (hasFeat && !this.isShown) {
      this.viewContainer.clear();
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isShown = true;
    } else if (!hasFeat && this.isShown) {
      this.viewContainer.clear();
      if (this.elseTemplate) {
        this.viewContainer.createEmbeddedView(this.elseTemplate);
      }
      this.isShown = false;
    } else if (!hasFeat && !this.isShown && this.elseTemplate && this.viewContainer.length === 0) {
      this.viewContainer.createEmbeddedView(this.elseTemplate);
    }
  }
}

