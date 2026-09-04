import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateAdjustmentRequest, PayrollRun, SalaryAdjustment } from '../models/payroll.model';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  private http = inject(HttpClient);
  
  runPayroll(month: string): Observable<PayrollRun> {
    return this.http.post<PayrollRun>(`/api/v1/payroll-cycle/${month}`, {});
  }

  getPayrollRun(month: string): Observable<PayrollRun> {
    return this.http.get<PayrollRun>(`/api/v1/payroll-cycle/${month}`);
  }
  
  getPayrollRuns(): Observable<PayrollRun[]> {
    return this.http.get<PayrollRun[]>('/api/v1/payroll-cycles');
  }

  createAdjustment(employeeId: number, request: CreateAdjustmentRequest): Observable<SalaryAdjustment> {
    return this.http.post<SalaryAdjustment>(`/api/v1/employees/${employeeId}/adjustments`, request);
  }

  getAdjustments(employeeId: number, month?: string): Observable<SalaryAdjustment[]> {
    const params: any = {};
    if (month) {
      params.month = month;
    }
    return this.http.get<SalaryAdjustment[]>(`/api/v1/employees/${employeeId}/adjustments`, { params });
  }
}
