import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee, EmployeeDetail, CreateEmployeeRequest, UpdateEmployeeRequest, SalaryHistory } from '../models/employee.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/employees';

  getEmployees(params?: { page?: number; size?: number; sort?: string; search?: string; department?: string; country?: string; status?: string; jobTitle?: string }): Observable<Page<Employee>> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          httpParams = httpParams.set(key, value.toString());
        }
      });
    }
    return this.http.get<Page<Employee>>(this.baseUrl, { params: httpParams });
  }

  getJobTitles(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/job-titles`);
  }

  getEmployee(id: number): Observable<EmployeeDetail> {
    return this.http.get<EmployeeDetail>(`${this.baseUrl}/${id}`);
  }

  createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, request);
  }

  updateEmployee(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}`, request);
  }

  deactivateEmployee(id: number): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
