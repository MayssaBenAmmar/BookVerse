import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-rating',
  templateUrl: './rating.component.html',
  styleUrls: ['./rating.component.scss']
})
export class RatingComponent {

  @Input() rating: number = 0;
  @Input() readonly: boolean = false; // New input to control if rating is clickable
  @Input() showRatingValue: boolean = true; // New input to show/hide rating value
  @Output() ratingClicked: EventEmitter<number> = new EventEmitter<number>();

  maxRating: number = 5;

  get fullStars(): number {
    return Math.floor(this.rating);
  }

  get hasHalfStar(): boolean {
    return this.rating % 1 !== 0;
  }

  get emptyStars(): number {
    return this.maxRating - Math.ceil(this.rating);
  }

  get formattedRating(): string {
    return this.rating.toFixed(1);
  }

  onStarClick(starValue: number): void {
    if (!this.readonly) {
      this.ratingClicked.emit(starValue);
    }
  }

  onStarHover(starValue: number): void {
    // Optional: Add hover effects for interactive ratings
  }
}
