import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
    pathMatch: 'full',
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register.component').then((m) => m.RegisterComponent),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'report:read' },
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'home',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'classes',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'class:read', feature: 'CLASS_MANAGEMENT' },
    loadComponent: () =>
      import('./features/classes/classes.component').then((m) => m.ClassesComponent),
  },
  {
    path: 'students',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'student:read', feature: 'STUDENT_MANAGEMENT' },
    loadComponent: () =>
      import('./features/students/students.component').then((m) => m.StudentsComponent),
  },
  {
    path: 'attendance',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'attendance:read', feature: 'ATTENDANCE' },
    loadComponent: () =>
      import('./features/attendance/attendance.component').then((m) => m.AttendanceComponent),
  },
  {
    path: 'billing',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'invoice:read', feature: 'BILLING_MANUAL' },
    loadComponent: () =>
      import('./features/billing/billing.component').then((m) => m.BillingComponent),
  },
  {
    path: 'settings',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'settings:read' },
    loadComponent: () =>
      import('./features/settings/settings.component').then((m) => m.SettingsComponent),
  },
  {
    path: 'system-config',
    canActivate: [authGuard, permissionGuard],
    data: { permission: 'system:config:read' },
    loadComponent: () =>
      import('./features/system-config/system-config.component').then(
        (m) => m.SystemConfigComponent
      ),
  },
  {
    path: 'p/:hash', // Public invoice link cho Phụ huynh (không cần login)
    loadComponent: () =>
      import('./features/public-invoice/public-invoice.component').then(
        (m) => m.PublicInvoiceComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
