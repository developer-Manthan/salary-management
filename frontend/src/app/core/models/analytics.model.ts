export interface AnalyticsSummary {
  dimension: string;
  metric: string;
  data: DimensionMetricEntry[];
}

export interface DimensionMetricEntry {
  label: string;
  value: number;
}

export interface TopEarner {
  employeeId: number;
  employeeCode: string;
  name: string;
  department: string;
  jobTitle: string;
  country: string;
  currentSalary: number;
}

export interface BracketData {
  brackets: BracketEntry[];
}

export interface BracketEntry {
  range: string;
  count: number;
  percentage: number;
}

export interface AvgVsMedian {
  average: number;
  median: number;
  difference: number;
  skewDirection: 'RIGHT' | 'LEFT' | 'SYMMETRIC';
}
