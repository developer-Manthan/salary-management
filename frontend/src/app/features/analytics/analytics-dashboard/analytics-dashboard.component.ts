import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatDividerModule } from '@angular/material/divider';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { SummaryChartComponent } from '../summary-chart/summary-chart.component';
import { TopEarnersComponent } from '../top-earners/top-earners.component';
import { BracketsComponent } from '../brackets/brackets.component';
import { AvgVsMedianComponent } from '../avg-vs-median/avg-vs-median.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { DimensionMetricEntry } from '../../../core/models/analytics.model';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    FormsModule,
    MatGridListModule,
    MatDividerModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    SummaryChartComponent,
    TopEarnersComponent,
    BracketsComponent,
    AvgVsMedianComponent
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.scss']
})
export class AnalyticsDashboardComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  // Summary Explorer State
  selectedDimension: string = 'department';
  selectedMetric: string = 'avg';
  
  dimensions = [
    { value: 'department', viewValue: 'Department' },
    { value: 'country', viewValue: 'Country' },
    { value: 'jobTitle', viewValue: 'Job Title' },
    { value: 'status', viewValue: 'Status' }
  ];

  metrics = [
    { value: 'sum', viewValue: 'Total Sum' },
    { value: 'avg', viewValue: 'Average' },
    { value: 'median', viewValue: 'Median' },
    { value: 'min', viewValue: 'Minimum' },
    { value: 'max', viewValue: 'Maximum' },
    { value: 'count', viewValue: 'Headcount' },
    { value: 'shareOfTotal', viewValue: 'Share of Total %' }
  ];

  summaryData: DimensionMetricEntry[] = [];
  isSummaryLoading: boolean = false;
  summaryError: string | null = null;

  ngOnInit(): void {
    this.runSummaryQuery();
  }

  runSummaryQuery(): void {
    this.isSummaryLoading = true;
    this.summaryError = null;

    this.analyticsService.getSummary(this.selectedDimension, this.selectedMetric).subscribe({
      next: (result) => {
        this.summaryData = result.data;
        this.isSummaryLoading = false;
      },
      error: (err) => {
        console.error('Failed to load summary data:', err);
        this.summaryError = 'Failed to load summary data.';
        this.isSummaryLoading = false;
      }
    });
  }
}
