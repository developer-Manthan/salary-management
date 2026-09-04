import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'employees',
        pathMatch: 'full'
      },
      {
        path: 'employees',
        loadComponent: () =>
          import('./features/employees/employee-list/employee-list.component').then((m) => m.EmployeeListComponent)
      },
      {
        path: 'employees/new',
        loadComponent: () =>
          import('./features/employees/employee-form/employee-form.component').then((m) => m.EmployeeFormComponent)
      },
      {
        path: 'employees/:id',
        loadComponent: () =>
          import('./features/employees/employee-detail/employee-detail.component').then((m) => m.EmployeeDetailComponent)
      },
      {
        path: 'employees/:id/edit',
        loadComponent: () =>
          import('./features/employees/employee-form/employee-form.component').then((m) => m.EmployeeFormComponent)
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./features/analytics/analytics-dashboard/analytics-dashboard.component').then((m) => m.AnalyticsDashboardComponent)
      },
      {
        path: 'payroll',
        loadComponent: () =>
          import('./features/payroll/payroll-run/payroll-run.component').then((m) => m.PayrollRunComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'employees'
  }    
];
