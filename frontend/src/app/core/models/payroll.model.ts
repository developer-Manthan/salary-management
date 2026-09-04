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
    finalAmount: number;
}
