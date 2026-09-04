import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <h1>{{ title() }}</h1>
      <p *ngIf="subtitle()">{{ subtitle() }}</p>
    </div>
  `,
  styles: [`
    .page-header {
      margin-bottom: 24px;
      font-family: 'Inter', sans-serif;
      color: #1a1a2e;

      h1 {
        font-size: 28px;
        font-weight: 600;
        margin: 0 0 8px 0;
        letter-spacing: -0.5px;
      }

      p {
        font-size: 14px;
        color: #666;
        margin: 0;
      }
    }
  `]
})
export class PageHeaderComponent {
  title = input<string>('');
  subtitle = input<string>('');
}
