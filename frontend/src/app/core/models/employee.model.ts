export interface Employee {
  id: number;
  employeeCode: string;
  name: string;
  department: string;
  jobTitle: string;
  country: string;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE';
  dateJoined: string;
  currentSalary: number;
}

export interface EmployeeDetail extends Employee {
  salaryHistory: SalaryHistory[];
}

export interface SalaryHistory {
  id: number;
  amount: number;
  effectiveDate: string;
  reason: string;
  createdAt: string;
}

export interface CreateEmployeeRequest {
  employeeCode?: string;
  name: string;
  department: string;
  jobTitle: string;
  country: string;
  currency: string;
  initialSalary: number;
  dateJoined: string;
}

export interface UpdateEmployeeRequest {
  name?: string;
  department?: string;
  jobTitle?: string;
  country?: string;
  currency?: string;
  newSalary?: number;
  salaryChangeReason?: string;
}

export interface EmployeePayslip {
  payrollCycleId: number;
  month: string;
  status: string;
  runAt: string;
  baseSalary: number;
  totalAdjustments: number;
  finalAmount: number;
}