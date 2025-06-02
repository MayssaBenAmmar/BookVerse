import {Component, OnInit} from '@angular/core';
import {BookResponse} from '../../../../services/models/book-response';
import {BookService} from '../../../../services/services/book.service';
import {ActivatedRoute, Router} from '@angular/router';
import {FeedbackService} from '../../../../services/services/feedback.service';
import {PageResponseFeedbackResponse} from '../../../../services/models/page-response-feedback-response';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-book-details',
  templateUrl: './book-details.component.html', // Fixed: correct template file
  styleUrls: ['./book-details.component.scss']
})
export class BookDetailsComponent implements OnInit {
  book: BookResponse = {};
  feedbacks: PageResponseFeedbackResponse = { content: [] };
  page = 0;
  size = 5;
  pages: any = [];
  private bookId = 0;
  loading = true;
  error = false;

  constructor(
    private bookService: BookService,
    private feedbackService: FeedbackService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.bookId = this.activatedRoute.snapshot.params['bookId'];
    if (this.bookId) {
      this.loading = true;
      this.bookService.findBookById({
        'book-id': this.bookId
      })
        .pipe(
          catchError(error => {
            console.error('Error loading book details:', error);
            this.error = true;
            this.loading = false;
            // Return a dummy book object
            return of({
              id: this.bookId,
              title: 'Book Not Found',
              authorName: 'Unknown Author',
              isbn: 'N/A',
              synopsis: 'Sorry, this book could not be loaded.',
              rate: 0,
              owner: 'Unknown'
            });
          })
        )
        .subscribe({
          next: (book) => {
            this.book = book;
            this.loading = false;
            this.findAllFeedbacks();
          }
        });
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

  private findAllFeedbacks() {
    if (this.error) {
      // Skip loading feedbacks if there was an error loading the book
      return;
    }

    this.feedbackService.findAllFeedbacksByBook({
      'book-id': this.bookId,
      page: this.page,
      size: this.size
    })
      .pipe(
        catchError(error => {
          console.error('Error loading feedbacks:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        })
      )
      .subscribe({
        next: (data) => {
          this.feedbacks = data;
          // Generate page numbers array
          this.pages = Array.from(Array(this.feedbacks.totalPages || 0).keys());
        }
      });
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllFeedbacks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllFeedbacks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllFeedbacks();
  }

  goToLastPage() {
    this.page = this.feedbacks.totalPages as number - 1;
    this.findAllFeedbacks();
  }

  goToNextPage() {
    this.page++;
    this.findAllFeedbacks();
  }

  get isLastPage() {
    return this.page === this.feedbacks.totalPages as number - 1;
  }
}
