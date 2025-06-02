import { Component, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  template: `
    <div class="star-rating">
      <ng-container *ngFor="let star of stars">
        <i [ngClass]="star.class" class="material-icons">{{ star.icon }}</i>
      </ng-container>
      <span *ngIf="showRatingValue" class="rating-value">{{ rating }}</span>
    </div>
  `,
  styles: [`
    .star-rating {
      display: flex;
      align-items: center;
    }

    .material-icons {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .star-full {
      color: #FFD700;
    }

    .star-empty {
      color: #C5C5C5;
    }

    .star-half {
      color: #FFD700;
    }

    .rating-value {
      margin-left: 5px;
      font-size: 14px;
      color: #555;
    }
  `]
})
export class StarRatingComponent implements OnChanges {
  @Input() rating: number = 0;
  @Input() showRatingValue: boolean = true;

  stars: { icon: string, class: string }[] = [];

  ngOnChanges(): void {
    this.stars = [];
    const fullStars = Math.floor(this.rating);
    const hasHalfStar = this.rating % 1 >= 0.5;

    // Add full stars
    for (let i = 0; i < fullStars; i++) {
      this.stars.push({ icon: 'star', class: 'star-full' });
    }

    // Add half star if needed
    if (hasHalfStar) {
      this.stars.push({ icon: 'star_half', class: 'star-half' });
    }

    // Add empty stars
    const remainingStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
    for (let i = 0; i < remainingStars; i++) {
      this.stars.push({ icon: 'star_border', class: 'star-empty' });
    }
  }
}
