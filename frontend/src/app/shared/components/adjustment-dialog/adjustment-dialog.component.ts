import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface AdjustmentDialogData {
  employeeName: string;
}

@Component({
  selector: 'app-adjustment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="dialog-icon">account_balance_wallet</mat-icon>
      Add Adjustment for {{ data.employeeName }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="adjustment-form">
        <mat-form-field appearance="outline">
          <mat-label>Adjustment Type</mat-label>
          <mat-select formControlName="type" required>
            <mat-option value="BONUS">🎁 Bonus</mat-option>
            <mat-option value="DEDUCTION">📉 Deduction</mat-option>
            <mat-option value="REIMBURSEMENT">💰 Reimbursement</mat-option>
            <mat-option value="COMPENSATION">📈 Compensation</mat-option>
          </mat-select>
          <mat-error>Type is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Amount (USD)</mat-label>
          <input matInput type="number" formControlName="amount" min="1" placeholder="e.g. 5000">
          <mat-error *ngIf="form.get('amount')?.hasError('required')">Amount is required</mat-error>
          <mat-error *ngIf="form.get('amount')?.hasError('min')">Must be greater than 0</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Effective Month</mat-label>
          <input matInput type="month" formControlName="effectiveMonth" required>
          <mat-error>Month is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Note (optional)</mat-label>
          <input matInput formControlName="note" placeholder="e.g. Q3 performance bonus">
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">
        <mat-icon>add</mat-icon> Add Adjustment
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-icon {
      vertical-align: middle;
      margin-right: 8px;
      color: #1976d2;
    }

    .adjustment-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 400px;
      padding-top: 8px;
    }

    mat-dialog-actions {
      padding: 16px 0 0;
      gap: 8px;
    }
  `]
})
export class AdjustmentDialogComponent {
  data: AdjustmentDialogData = inject(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<AdjustmentDialogComponent>);
  private fb = inject(FormBuilder);

  form: FormGroup = this.fb.group({
    type: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(1)]],
    effectiveMonth: [this.getCurrentMonth(), Validators.required],
    note: ['']
  });

  private getCurrentMonth(): string {
    const now = new Date();
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    return `${now.getFullYear()}-${month}`;
  }

  submit(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }
}
