import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { EmployeeService } from '../../../core/services/employee.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Employee } from '../../../core/models/employee.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatMenuModule,
    RouterLink,
    FormsModule,
    PageHeaderComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss'
})
export class EmployeeListComponent implements OnInit, OnDestroy {
  private employeeService = inject(EmployeeService);
  private notificationService = inject(NotificationService);
  private dialog = inject(MatDialog);

  employees: Employee[] = [];
  totalElements = 0;
  loading = false;
  
  displayedColumns: string[] = ['employeeCode', 'name', 'department', 'jobTitle', 'country', 'currentSalary', 'status', 'actions'];

  searchTerm = '';
  departmentFilter = '';
  countryFilter = '';
  statusFilter = '';
  jobTitleFilter = '';
  jobTitleSearchTerm = '';

  departments = ['Engineering', 'Product', 'Design', 'Marketing', 'Sales', 'HR', 'Finance', 'Operations', 'Legal', 'Support'];
  countries = ['US', 'UK', 'India', 'Germany', 'Canada', 'Australia', 'Japan', 'Singapore'];
  statuses = ['ACTIVE', 'INACTIVE'];
  jobTitles: string[] = [];
  filteredJobTitles: string[] = [];

  pageSize = 10;
  pageIndex = 0;
  sortField = 'name';
  sortDirection = 'asc';

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.pageIndex = 0;
      this.loadEmployees();
    });

    this.employeeService.getJobTitles().subscribe(titles => {
      this.jobTitles = titles;
      this.filteredJobTitles = titles;
    });

    this.loadEmployees();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  loadEmployees(): void {
    this.loading = true;
    
    const params: any = {
      page: this.pageIndex,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDirection}`
    };

    if (this.searchTerm) params.search = this.searchTerm;
    if (this.departmentFilter) params.department = this.departmentFilter;
    if (this.countryFilter) params.country = this.countryFilter;
    if (this.statusFilter) params.status = this.statusFilter;
    if (this.jobTitleFilter) params.jobTitle = this.jobTitleFilter;

    this.employeeService.getEmployees(params).subscribe({
      next: (page) => {
        this.employees = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.notificationService.error('Failed to load employees');
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadEmployees();
  }

  onSortChange(event: Sort): void {
    this.sortField = event.active;
    this.sortDirection = event.direction || 'asc';
    this.pageIndex = 0;
    this.loadEmployees();
  }

  onFilterChange(): void {
    this.pageIndex = 0;
    this.loadEmployees();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.departmentFilter = '';
    this.countryFilter = '';
    this.statusFilter = '';
    this.jobTitleFilter = '';
    this.jobTitleSearchTerm = '';
    this.filteredJobTitles = this.jobTitles;
    this.pageIndex = 0;
    this.loadEmployees();
  }

  filterJobTitles(searchTerm: string): void {
    this.jobTitleSearchTerm = searchTerm;
    if (!searchTerm) {
      this.filteredJobTitles = this.jobTitles;
    } else {
      const lower = searchTerm.toLowerCase();
      this.filteredJobTitles = this.jobTitles.filter(t => t.toLowerCase().includes(lower));
    }
  }

  onDeactivate(employee: Employee): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate Employee',
        message: `Are you sure you want to deactivate ${employee.name}?`,
        confirmText: 'Deactivate',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loading = true;
        this.employeeService.deactivateEmployee(employee.id).subscribe({
          next: () => {
            this.notificationService.success('Employee deactivated successfully');
            this.loadEmployees();
          },
          error: (err) => {
            this.notificationService.error('Failed to deactivate employee');
            this.loading = false;
            console.error(err);
          }
        });
      }
    });
  }

  formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency }).format(amount);
  }
}
