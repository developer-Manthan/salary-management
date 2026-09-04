import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AvgVsMedian } from '../../../core/models/analytics.model';

@Component({
  selector: 'app-avg-vs-median',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, LoadingSpinnerComponent],
  templateUrl: './avg-vs-median.component.html',
  styleUrls: ['./avg-vs-median.component.scss']
})
export class AvgVsMedianComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  data: AvgVsMedian | null = null;
  isLoading: boolean = false;
  error: string | null = null;

  ngOnInit(): void {
    this.isLoading = true;
    this.analyticsService.getAvgVsMedian().subscribe({
      next: (data) => {
        this.data = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load avg vs median:', err);
        this.error = 'Failed to load comparison data.';
        this.isLoading = false;
      }
    });
  }

  getSkewMessage(): string {
    if (!this.data) return '';
    switch (this.data.skewDirection) {
      case 'RIGHT':
        return 'High earners skew the average up';
      case 'LEFT':
        return 'Lower earners pull the average down';
      case 'SYMMETRIC':
        return 'Distribution is roughly balanced';
      default:
        return '';
    }
  }

  getSkewIcon(): string {
    if (!this.data) return '';
    switch (this.data.skewDirection) {
      case 'RIGHT': return 'trending_up';
      case 'LEFT': return 'trending_down';
      case 'SYMMETRIC': return 'trending_flat';
      default: return 'remove';
    }
  }
}
