import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    children: [
      { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
      { path: 'register', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) }
    ]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'classes',
    loadComponent: () => import('./features/classes/classes.component').then(m => m.ClassesComponent)
  },
  {
    path: 'students',
    loadComponent: () => import('./features/students/students.component').then(m => m.StudentsComponent)
  },
  {
    path: 'attendance',
    loadComponent: () => import('./features/attendance/attendance.component').then(m => m.AttendanceComponent)
  },
  {
    path: 'billing',
    loadComponent: () => import('./features/billing/billing.component').then(m => m.BillingComponent)
  },
  {
    path: 'p/:hash', // Public invoice link cho Phụ huynh (không cần login)
    loadComponent: () => import('./features/public-invoice/public-invoice.component').then(m => m.PublicInvoiceComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
