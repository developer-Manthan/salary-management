import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog } from '@angular/material/dialog';

import { EmployeeService } from '../../../core/services/employee.service';
import { PayrollService } from '../../../core/services/payroll.service';
import { NotificationService } from '../../../core/services/notification.service';
import { EmployeeDetail } from '../../../core/models/employee.model';
import { SalaryAdjustment } from '../../../core/models/payroll.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AdjustmentDialogComponent } from '../../../shared/components/adjustment-dialog/adjustment-dialog.component';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule,
    MatTabsModule,
    RouterLink,
    PageHeaderComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss'
})
export class EmployeeDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private employeeService = inject(EmployeeService);
  private payrollService = inject(PayrollService);
  private notificationService = inject(NotificationService);
  private dialog = inject(MatDialog);
  private location = inject(Location);

  employee: EmployeeDetail | null = null;
  adjustments: SalaryAdjustment[] = [];
  loading = true;
  loadingAdjustments = false;
  salaryColumns: string[] = ['effectiveDate', 'amount', 'reason', 'createdAt'];
  adjustmentColumns: string[] = ['type', 'amount', 'effectiveMonth', 'note', 'createdAt'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEmployee(+id);
    } else {
      this.notificationService.error('Invalid employee ID');
      this.router.navigate(['/employees']);
    }
  }

  loadEmployee(id: number): void {
    this.loading = true;
    this.employeeService.getEmployee(id).subscribe({
      next: (data) => {
        this.employee = data;
        this.loading = false;
        this.loadAdjustments(id);
      },
      error: (err) => {
        this.notificationService.error('Failed to load employee details');
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadAdjustments(employeeId: number): void {
    this.loadingAdjustments = true;
    this.payrollService.getAdjustments(employeeId).subscribe({
      next: (data) => {
        this.adjustments = data;
        this.loadingAdjustments = false;
      },
      error: () => {
        this.loadingAdjustments = false;
      }
    });
  }

  openAdjustmentDialog(): void {
    if (!this.employee) return;

    const dialogRef = this.dialog.open(AdjustmentDialogComponent, {
      width: '500px',
      data: { employeeName: this.employee.name }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.employee) {
        this.payrollService.createAdjustment(this.employee.id, result).subscribe({
          next: () => {
            this.notificationService.success('Adjustment added successfully');
            this.loadAdjustments(this.employee!.id);
          },
          error: (err) => {
            this.notificationService.error('Failed to add adjustment');
            console.error(err);
          }
        });
      }
    });
  }

  deactivate(): void {
    if (!this.employee) return;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate Employee',
        message: `Are you sure you want to deactivate ${this.employee.name}?`,
        confirmText: 'Deactivate',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.employee) {
        this.loading = true;
        this.employeeService.deactivateEmployee(this.employee.id).subscribe({
          next: () => {
            this.notificationService.success('Employee deactivated successfully');
            this.loadEmployee(this.employee!.id);
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

  goBack(): void {
    this.location.back();
  }

  formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency }).format(amount);
  }

  getAdjustmentTypeClass(type: string): string {
    switch (type) {
      case 'BONUS': return 'adj-bonus';
      case 'DEDUCTION': return 'adj-deduction';
      case 'REIMBURSEMENT': return 'adj-reimbursement';
      case 'COMPENSATION': return 'adj-compensation';
      default: return '';
    }
  }
}
