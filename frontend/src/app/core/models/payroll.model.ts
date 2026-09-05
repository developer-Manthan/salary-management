export interface PayrollRun {
  id: number;
  month: string;
  triggeredBy: 'MANUAL' | 'SCHEDULED';
  runAt: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  paySlips?: PayrollRunLine[];
}

export interface PayrollRunLine {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  baseSalary: number;
  totalAdjustments: number;
  finalAmount: number;
}

export interface SalaryAdjustment {
  id: number;
  type: 'BONUS' | 'DEDUCTION' | 'REIMBURSEMENT' | 'COMPENSATION';
  amount: number;
  effectiveMonth: string;
  note: string;
  createdAt: string;
}

export interface CreateAdjustmentRequest {
  type: string;
  amount: number;
  effectiveMonth: string;
  note?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}