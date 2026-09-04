import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-brackets',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, LoadingSpinnerComponent, MatIconModule],
  templateUrl: './brackets.component.html',
  styleUrls: ['./brackets.component.scss']
})
export class BracketsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  chartData: any[] = [];
  isLoading: boolean = false;
  error: string | null = null;

  // Pie chart options
  gradient: boolean = true;
  showLegend: boolean = true;
  showLabels: boolean = true;
  isDoughnut: boolean = false;
  legendPosition: any = 'below';

  // Warm gradient color scheme
  colorScheme: any = {
    domain: ['#ffb703', '#fb8500', '#e85d04', '#dc2f02', '#d00000', '#9d0208', '#6a040f', '#370617']
  };

  ngOnInit(): void {
    this.isLoading = true;
    this.analyticsService.getBrackets().subscribe({
      next: (data) => {
        if (data && data.brackets) {
          this.chartData = data.brackets.map(b => ({
            name: b.range,
            value: b.count
          }));
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load brackets:', err);
        this.error = 'Failed to load brackets data.';
        this.isLoading = false;
      }
    });
  }
}
