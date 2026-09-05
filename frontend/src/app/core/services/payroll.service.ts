import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateAdjustmentRequest, PagedResponse, PayrollCycleSummary, PayrollRun, PayrollRunLine, SalaryAdjustment } from '../models/payroll.model';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  private http = inject(HttpClient);
  
  runPayroll(month: string): Observable<PayrollRun> {
    return this.http.post<PayrollRun>(`/api/v1/payroll-cycle/${month}`, {});
  }

  retryPayroll(month: string): Observable<PayrollRun> {
    return this.http.post<PayrollRun>(`/api/v1/payroll-cycle/${month}/retry`, {});
  }

  getPayrollRun(month: string): Observable<PayrollRun> {
    return this.http.get<PayrollRun>(`/api/v1/payroll-cycle/${month}`);
  }
  
  getPayrollRuns(): Observable<PayrollRun[]> {
    return this.http.get<PayrollRun[]>('/api/v1/payroll-cycles');
  }

  getPayrollRunLines(month: string, page: number = 0, size: number = 20, search?: string): Observable<PagedResponse<PayrollRunLine>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<PagedResponse<PayrollRunLine>>(`/api/v1/pay-slips/${month}`, { params });
  }

  getPayrollCycleSummary(month: string): Observable<PayrollCycleSummary> {
    return this.http.get<PayrollCycleSummary>(`/api/v1/payroll-cycle/${month}/summary`);
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
