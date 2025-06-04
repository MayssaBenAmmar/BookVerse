import { Component, EventEmitter, Input, Output, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { BookResponse } from '../../../../services/models/book-response';
import { FavoriteService } from '../../../../services/favorite.service';
import { finalize } from 'rxjs/operators';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-book-card',
  templateUrl: './book-card.component.html',
  styleUrls: ['./book-card.component.scss']
})
export class BookCardComponent implements OnInit, OnChanges {
  private _book: BookResponse = {};
  private _manage = false;

  isFavorited = false;
  isLoading = false;
  favoriteLoading = false;
  errorMessage: string | null = null;

  constructor(
    private favoriteService: FavoriteService,
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkFavoriteStatus();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Check favorite status when book changes
    if (changes['book'] && changes['book'].currentValue?.id !== changes['book'].previousValue?.id) {
      this.checkFavoriteStatus();
    }
  }

  /**
   * Format genre enum value for display
   */
  formatGenre(genre: string): string {
    if (!genre) return 'Unknown';

    // Convert ENUM value to Title Case
    // Example: ROMANCE -> Romance
    return genre.charAt(0) + genre.slice(1).toLowerCase();
  }

  /**
   * Format a username to be more user-friendly
   */
  formatUsername(username: string): string {
    if (!username) return 'Unknown';

    // If it's a UUID-style username (contains dashes and is long)
    if (username.includes('-') && username.length > 20) {
      return `User ${username.split('-')[0]}`;
    }

    // If it's an email address
    if (username.includes('@')) {
      return username.split('@')[0];
    }

    return username;
  }

  /**
   * Get CSS class for genre badge
   */
  getGenreClass(genre: string): string {
    switch (genre) {
      case 'ROMANCE':
        return 'badge-romance';
      case 'HORROR':
        return 'badge-horror';
      case 'COMEDY':
        return 'badge-comedy';
      case 'SCIFI':
        return 'badge-scifi';
      case 'FANTASY':
        return 'badge-fantasy';
      default:
        return 'badge-secondary';
    }
  }

  private checkFavoriteStatus(): void {
    if (!this._book?.id) return;

    this.isLoading = true;
    this.errorMessage = null;

    this.favoriteService.getFavorites()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: favorites => {
          this.isFavorited = favorites.some(fav => fav.id === this._book.id);
        },
        error: err => {
          console.error('Error fetching favorite status:', err);
          this.errorMessage = 'Could not load favorite status';
          this.isFavorited = false;
        }
      });
  }

  get bookCover(): string {
    try {
      if (this._book.cover) {
        // If cover is an array with items
        if (Array.isArray(this._book.cover) && this._book.cover.length > 0) {
          return 'data:image/jpg;base64,' + this._book.cover[0];
        }
        // If cover is a string (single item)
        else if (typeof this._book.cover === 'string') {
          return 'data:image/jpg;base64,' + this._book.cover;
        }
      }
    } catch (e) {
      console.error('Error parsing image data:', e, this._book.cover);
    }

    // Use a publicly available placeholder image
    return 'https://via.placeholder.com/200x300?text=Book+Cover';
  }

  get book(): BookResponse {
    return this._book;
  }

  @Input()
  set book(value: BookResponse) {
    this._book = value;
  }

  get manage(): boolean {
    return this._manage;
  }

  @Input()
  set manage(value: boolean) {
    this._manage = value;
  }

  // Check if the current user is the book owner
  get isOwner(): boolean {
    return this._book.owner === this.keycloakService.username;
  }

  @Output() private share: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private archive: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private addToWaitingList: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private borrow: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private edit: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private details: EventEmitter<BookResponse> = new EventEmitter<BookResponse>();
  @Output() private favoriteChanged: EventEmitter<{book: BookResponse, isFavorited: boolean}> =
    new EventEmitter<{book: BookResponse, isFavorited: boolean}>();
  @Output() private bookDeleted: EventEmitter<number> = new EventEmitter<number>();
  @Output() private viewProfile: EventEmitter<string> = new EventEmitter<string>();
  @Output() private ratingChanged: EventEmitter<{book: BookResponse, rating: number}> =
    new EventEmitter<{book: BookResponse, rating: number}>();

  onShare() {
    this.share.emit(this._book);
  }

  onArchive() {
    this.archive.emit(this._book);
  }

  onAddToWaitingList() {
    this.addToWaitingList.emit(this._book);
  }

  onBorrow() {
    this.borrow.emit(this._book);
  }

  onEdit() {
    this.edit.emit(this._book);
  }

  onShowDetails() {
    this.details.emit(this._book);
  }

  onDelete(event: Event) {
    event.stopPropagation();

    if (confirm('Are you sure you want to delete this book? This action cannot be undone.')) {
      this.bookDeleted.emit(this._book.id);
    }
  }

  onViewProfile(userId?: string) {
    // Try to get a valid user ID from various sources
    if (!userId && this._book.owner) {
      userId = this._book.owner;
    }

    if (userId) {
      // Format the username before navigation
      const formattedUsername = this.formatUsername(userId);
      console.log('Navigating to profile for user:', userId);
      console.log('Formatted username:', formattedUsername);

      this.router.navigate(['/profile', userId]);

      // Also emit event to parent component
      this.viewProfile.emit(userId);
    } else {
      console.error('Cannot view profile: No user ID available');
      this.errorMessage = 'Cannot view profile: User information not available';

      // Clear error after 3 seconds
      setTimeout(() => {
        this.errorMessage = null;
      }, 3000);
    }
  }

  onRatingClick(newRating: number) {
    // Emit the new rating to parent component
    this.ratingChanged.emit({book: this._book, rating: newRating});
  }

  toggleFavorite(): void {
    if (!this._book.id || this.favoriteLoading) return;

    this.favoriteLoading = true;
    this.errorMessage = null;

    if (this.isFavorited) {
      this.favoriteService.removeFavorite(this._book.id)
        .pipe(finalize(() => this.favoriteLoading = false))
        .subscribe({
          next: () => {
            this.isFavorited = false;
            this.favoriteChanged.emit({book: this._book, isFavorited: false});
          },
          error: err => {
            console.error('Error removing from favorites:', err);
            this.errorMessage = 'Failed to remove from favorites';
          }
        });
    } else {
      this.favoriteService.addFavorite(this._book.id)
        .pipe(finalize(() => this.favoriteLoading = false))
        .subscribe({
          next: () => {
            this.isFavorited = true;
            this.favoriteChanged.emit({book: this._book, isFavorited: true});
          },
          error: err => {
            console.error('Error adding to favorites:', err);
            this.errorMessage = 'Failed to add to favorites';
          }
        });
    }
  }

  getGenreIcon(genre: string): string {
    switch (genre) {
      case 'ROMANCE':
        return 'fa-heart';
      case 'HORROR':
        return 'fa-ghost';
      case 'COMEDY':
        return 'fa-laugh';
      case 'SCIFI':
        return 'fa-rocket';
      case 'FANTASY':
        return 'fa-hat-wizard';
      default:
        return 'fa-book';
    }
  }
}
