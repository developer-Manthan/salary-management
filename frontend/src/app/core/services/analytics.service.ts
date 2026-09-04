import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyticsSummary, TopEarner, BracketData, AvgVsMedian } from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/analytics';

  getSummary(dimension: string, metric: string): Observable<AnalyticsSummary> {
    let params = new HttpParams()
      .set('dimension', dimension)
      .set('metric', metric);
    return this.http.get<AnalyticsSummary>(`${this.baseUrl}/summary`, { params });
  }

  getTopEarners(n?: number, order?: string): Observable<TopEarner[]> {
    let params = new HttpParams();
    if (n !== undefined) {
      params = params.set('n', n.toString());
    }
    if (order !== undefined) {
      params = params.set('order', order);
    }
    return this.http.get<TopEarner[]>(`${this.baseUrl}/top-earners`, { params });
  }

  getBrackets(): Observable<BracketData> {
    return this.http.get<BracketData>(`${this.baseUrl}/brackets`);
  }

  getAvgVsMedian(): Observable<AvgVsMedian> {
    return this.http.get<AvgVsMedian>(`${this.baseUrl}/avg-vs-median`);
  }
}
