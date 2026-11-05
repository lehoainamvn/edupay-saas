import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  template: `
    <router-outlet></router-outlet>
    <!-- Component Thông Báo Toàn Cục (Single Source of Truth) -->
    <app-toast></app-toast>
  `
})
export class AppComponent {
  title = 'edupay-frontend';
}
