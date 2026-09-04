import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { HttpErrorResponse } from '@angular/common/http';

import { PayrollService } from '../../../core/services/payroll.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PayrollRun } from '../../../core/models/payroll.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-payroll-run',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatDividerModule,
    MatExpansionModule,
    MatProgressBarModule,
    FormsModule,
    PageHeaderComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './payroll-run.component.html',
  styleUrl: './payroll-run.component.scss'
})
export class PayrollRunComponent implements OnInit {
  private payrollService = inject(PayrollService);
  private notificationService = inject(NotificationService);
  private dialog = inject(MatDialog);

  payrollRuns: PayrollRun[] = [];
  selectedMonth: string = this.getCurrentMonth();
  loading = true;
  runningPayroll = false;
  currentStep = 0;
  private stepInterval: any;

  displayedColumns: string[] = ['employeeCode', 'employeeName', 'baseSalary', 'finalAmount'];

  ngOnInit(): void {
    this.loadPayrollRuns();
  }

  getCurrentMonth(): string {
    const now = new Date();
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    return `${now.getFullYear()}-${month}`;
  }

  loadPayrollRuns(): void {
    this.loading = true;
    this.payrollService.getPayrollRuns().subscribe({
      next: (data) => {
        this.payrollRuns = data;
        this.loading = false;
      },
      error: (err) => {
        this.notificationService.error('Failed to load payroll runs');
        this.loading = false;
        console.error(err);
      }
    });
  }

  runPayroll(): void {
    if (!this.selectedMonth) {
      this.notificationService.error('Please select a month');
      return;
    }

    const formattedMonth = this.formatMonth(this.selectedMonth);

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Run Payroll',
        message: `Are you sure you want to run payroll for ${formattedMonth}?`,
        confirmText: 'Run Payroll',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.runningPayroll = true;
        this.currentStep = 1;
        this.startProgressSimulation();

        this.payrollService.runPayroll(this.selectedMonth).subscribe({
          next: () => {
            this.completeProgress();
            setTimeout(() => {
              this.notificationService.success(`Payroll processed successfully for ${formattedMonth}`);
              this.loadPayrollRuns();
              this.resetProgress();
            }, 800);
          },
          error: (err: HttpErrorResponse) => {
            this.resetProgress();
            if (err.status === 409) {
              this.notificationService.error(`Payroll for ${formattedMonth} has already been processed`);
            } else {
              this.notificationService.error('Failed to process payroll');
            }
            console.error(err);
          }
        });
      }
    });
  }

  private startProgressSimulation(): void {
    let stepIndex = 0;
    this.stepInterval = setInterval(() => {
      if (stepIndex < 4) {
        this.currentStep = stepIndex + 2;
        stepIndex++;
      }
    }, 1000);
  }

  private completeProgress(): void {
    if (this.stepInterval) clearInterval(this.stepInterval);
    this.currentStep = 6;
  }

  private resetProgress(): void {
    if (this.stepInterval) clearInterval(this.stepInterval);
    this.runningPayroll = false;
    this.currentStep = 0;
  }

  loadPayrollRunDetails(month: string, run: PayrollRun): void {
    if (!run.paySlips || run.paySlips.length === 0) {
      this.payrollService.getPayrollRun(month).subscribe({
        next: (data) => {
          run.paySlips = data.paySlips;
        },
        error: (err) => {
          this.notificationService.error('Failed to load payroll details');
          console.error(err);
        }  
      });
    }
  }

  getTotalPayout(run: PayrollRun): number {
    if (!run.paySlips) return 0;
    return run.paySlips.reduce((sum, line) => sum + line.finalAmount, 0);
  }

  formatMonth(monthString: string): string {
    if (!monthString) return '';
    const [year, month] = monthString.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1);
    return date.toLocaleString('default', { month: 'long', year: 'numeric' });
  }
}
