import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PayrollRun } from '../models/payroll.model';

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
}
