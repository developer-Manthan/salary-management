import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { FormsModule } from '@angular/forms';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { TopEarner } from '../../../core/models/analytics.model';

@Component({
  selector: 'app-top-earners',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatIconModule,
    MatChipsModule,
    FormsModule,
    LoadingSpinnerComponent
  ],
  templateUrl: './top-earners.component.html',
  styleUrls: ['./top-earners.component.scss']
})
export class TopEarnersComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  earners: TopEarner[] = [];
  displayedColumns: string[] = ['rank', 'name', 'employeeCode', 'department', 'country', 'currentSalary'];
  
  order: string = 'DESC'; // 'DESC' for top, 'ASC' for bottom
  limit: number = 5;
  limits: number[] = [5, 10, 20];
  
  isLoading: boolean = false;
  error: string | null = null;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.error = null;
    
    this.analyticsService.getTopEarners(this.limit, this.order).subscribe({
      next: (data) => {
        this.earners = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load top earners:', err);
        this.error = 'Failed to load data. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onControlsChange(): void {
    this.loadData();
  }
}
