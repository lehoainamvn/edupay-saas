import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
    pathMatch: 'full'
  },
  {
    path: 'auth',
    children: [
      { path: 'login',    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
      { path: 'register', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'classes',
    canActivate: [authGuard],
    loadComponent: () => import('./features/classes/classes.component').then(m => m.ClassesComponent)
  },
  {
    path: 'students',
    canActivate: [authGuard],
    loadComponent: () => import('./features/students/students.component').then(m => m.StudentsComponent)
  },
  {
    path: 'attendance',
    canActivate: [authGuard],
    loadComponent: () => import('./features/attendance/attendance.component').then(m => m.AttendanceComponent)
  },
  {
    path: 'billing',
    canActivate: [authGuard],
    loadComponent: () => import('./features/billing/billing.component').then(m => m.BillingComponent)
  },
  {
    path: 'p/:hash', // Public invoice link cho Phụ huynh (không cần login)
    loadComponent: () => import('./features/public-invoice/public-invoice.component').then(m => m.PublicInvoiceComponent)
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
