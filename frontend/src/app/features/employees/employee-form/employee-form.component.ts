import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { EmployeeService } from '../../../core/services/employee.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { CreateEmployeeRequest, UpdateEmployeeRequest } from '../../../core/models/employee.model';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    MatDividerModule
  ],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss'
})
export class EmployeeFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private employeeService = inject(EmployeeService);
  private notificationService = inject(NotificationService);
  private location = inject(Location);

  employeeForm!: FormGroup;
  isEditMode = false;
  employeeId?: number;
  loading = false;
  initialLoading = false;

  departments = ['Engineering', 'Product', 'Design', 'Marketing', 'Sales', 'HR', 'Finance', 'Operations', 'Legal', 'Support'];
  countries = ['US', 'UK', 'India', 'Germany', 'Canada', 'Australia', 'Japan', 'Singapore'];
  currencies = ['USD', 'GBP', 'INR', 'EUR', 'CAD', 'AUD', 'JPY', 'SGD'];

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.employeeId = +idParam;
    }

    this.buildForm();

    if (this.isEditMode) {
      this.loadEmployeeData();
    }
  }

  buildForm(): void {
    this.employeeForm = this.fb.group({
      employeeCode: [{ value: '', disabled: this.isEditMode }],
      name: ['', Validators.required],
      department: ['', Validators.required],
      jobTitle: ['', Validators.required],
      country: ['', Validators.required],
      currency: ['', Validators.required],
      dateJoined: [new Date(), this.isEditMode ? [] : [Validators.required]],
      initialSalary: [null, this.isEditMode ? [] : [Validators.required, Validators.min(0)]],
      newSalary: [null, this.isEditMode ? [Validators.min(0)] : []],
      salaryChangeReason: ['', this.isEditMode ? [] : []]
    });

    if (this.isEditMode) {
      this.employeeForm.get('newSalary')?.valueChanges.subscribe(val => {
        const reasonCtrl = this.employeeForm.get('salaryChangeReason');
        if (val !== null && val !== '') {
          reasonCtrl?.setValidators([Validators.required]);
        } else {
          reasonCtrl?.clearValidators();
        }
        reasonCtrl?.updateValueAndValidity();
      });
    }
  }

  loadEmployeeData(): void {
    if (!this.employeeId) return;
    
    this.initialLoading = true;
    this.employeeService.getEmployee(this.employeeId).subscribe({
      next: (data) => {
        this.employeeForm.patchValue({
          employeeCode: data.employeeCode,
          name: data.name,
          department: data.department,
          jobTitle: data.jobTitle,
          country: data.country,
          currency: data.currency
        });
        this.initialLoading = false;
      },
      error: (err) => {
        this.notificationService.error('Failed to load employee data');
        this.initialLoading = false;
        this.router.navigate(['/employees']);
      }
    });
  }

  onSubmit(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const formValue = this.employeeForm.getRawValue();

    if (this.isEditMode) {
      const request: UpdateEmployeeRequest = {
        name: formValue.name,
        department: formValue.department,
        jobTitle: formValue.jobTitle,
        country: formValue.country,
        currency: formValue.currency,
        newSalary: formValue.newSalary || undefined,
        salaryChangeReason: formValue.salaryChangeReason || undefined
      };

      this.employeeService.updateEmployee(this.employeeId!, request).subscribe({
        next: () => {
          this.notificationService.success('Employee updated successfully');
          this.router.navigate(['/employees', this.employeeId]);
        },
        error: (err) => {
          this.notificationService.error('Failed to update employee');
          this.loading = false;
          console.error(err);
        }
      });
    } else {
      const d = new Date(formValue.dateJoined);
      const dateJoinedStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

      const request: CreateEmployeeRequest = {
        employeeCode: formValue.employeeCode || undefined,
        name: formValue.name,
        department: formValue.department,
        jobTitle: formValue.jobTitle,
        country: formValue.country,
        currency: formValue.currency,
        initialSalary: formValue.initialSalary,
        dateJoined: dateJoinedStr
      };

      this.employeeService.createEmployee(request).subscribe({
        next: (res) => {
          this.notificationService.success('Employee created successfully');
          this.router.navigate(['/employees', res.id]);
        },
        error: (err) => {
          this.notificationService.error('Failed to create employee');
          this.loading = false;
          console.error(err);
        }
      });
    }
  }

  goBack(): void {
    this.location.back();
  }
}
