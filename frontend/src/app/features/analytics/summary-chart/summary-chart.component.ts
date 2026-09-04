import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { DimensionMetricEntry } from '../../../core/models/analytics.model';

@Component({
  selector: 'app-summary-chart',
  standalone: true,
  imports: [CommonModule, NgxChartsModule],
  templateUrl: './summary-chart.component.html',
  styleUrls: ['./summary-chart.component.scss']
})
export class SummaryChartComponent implements OnChanges {
  @Input() data: DimensionMetricEntry[] = [];
  @Input() dimension: string = '';
  @Input() metric: string = '';

  chartData: any[] = [];
  
  // Chart options
  showXAxis: boolean = true;
  showYAxis: boolean = true;
  gradient: boolean = false;
  showLegend: boolean = false;
  showXAxisLabel: boolean = true;
  showYAxisLabel: boolean = true;
  xAxisLabel: string = '';
  yAxisLabel: string = '';

  // Custom colors for blue-to-teal gradient palette
  colorScheme: any = {
    domain: ['#003f5c', '#2f4b7c', '#665191', '#a05195', '#d45087', '#f95d6a', '#ff7c43', '#ffa600']
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && this.data) {
      this.chartData = this.data.map(item => ({
        name: item.label,
        value: item.value
      }));
    }
    
    if (changes['dimension'] || changes['metric']) {
      this.xAxisLabel = this.dimension.charAt(0).toUpperCase() + this.dimension.slice(1);
      this.yAxisLabel = this.metric.charAt(0).toUpperCase() + this.metric.slice(1);
    }
  }
}
