import { Component, OnInit } from '@angular/core';
import { BookService } from '../../../../services/services/book.service';
import { PageResponseBorrowedBookResponse } from '../../../../services/models/page-response-borrowed-book-response';
import { BorrowedBookResponse } from '../../../../services/models/borrowed-book-response';
import { BookResponse } from '../../../../services/models/book-response';
import { FeedbackRequest } from '../../../../services/models/feedback-request';
import { FeedbackService } from '../../../../services/services/feedback.service';

@Component({
  selector: 'app-borrowed-book-list',
  templateUrl: './borrowed-book-list.component.html',
  styleUrls: ['./borrowed-book-list.component.scss']
})
export class BorrowedBookListComponent implements OnInit {
  page = 0;
  size = 5;
  pages: any = [];
  borrowedBooks: PageResponseBorrowedBookResponse = {};
  selectedBook: BookResponse | undefined = undefined;
  feedbackRequest: FeedbackRequest = { bookId: 0, comment: '', note: 1 }; // Changed: Initialize with 1 instead of 0

  // For handling rating updates
  isSubmittingFeedback = false;
  feedbackError: string | null = null;

  constructor(
    private bookService: BookService,
    private feedbackService: FeedbackService
  ) {}

  ngOnInit(): void {
    this.findAllBorrowedBooks();
  }

  // NEW: Utility method to format dates
  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';

    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    } catch (error) {
      return 'Invalid Date';
    }
  }

  // NEW: Utility method to calculate days borrowed
  getDaysBorrowed(borrowedDate: string | undefined, returnedDate: string | undefined): number {
    if (!borrowedDate) return 0;

    const borrowed = new Date(borrowedDate);
    const returned = returnedDate ? new Date(returnedDate) : new Date();
    const timeDiff = returned.getTime() - borrowed.getTime();
    return Math.ceil(timeDiff / (1000 * 3600 * 24));
  }

  private findAllBorrowedBooks() {
    this.bookService.findAllBorrowedBooks({
      page: this.page,
      size: this.size
    }).subscribe({
      next: (resp) => {
        this.borrowedBooks = resp;
        this.pages = Array(this.borrowedBooks.totalPages)
          .fill(0)
          .map((x, i) => i);
      },
      error: (error) => {
        console.error('Error fetching borrowed books:', error);
      }
    });
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllBorrowedBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllBorrowedBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllBorrowedBooks();
  }

  goToLastPage() {
    this.page = this.borrowedBooks.totalPages as number - 1;
    this.findAllBorrowedBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBorrowedBooks();
  }

  get isLastPage() {
    return this.page === this.borrowedBooks.totalPages as number - 1;
  }

  returnBorrowedBook(book: BorrowedBookResponse) {
    this.selectedBook = book;
    this.feedbackRequest.bookId = book.id as number;
    this.feedbackRequest.note = 1; // Changed: Set default rating to 1 instead of 0
    this.feedbackRequest.comment = ''; // Reset comment
    this.feedbackError = null;
  }

  // Handle rating changes from the rating component
  onRatingChange(rating: number) {
    this.feedbackRequest.note = rating;
  }

  returnBook(withFeedback: boolean) {
    // Fixed: Changed validation condition to check for null/undefined instead of just 0
    if (withFeedback && (this.feedbackRequest.note === null || this.feedbackRequest.note === undefined || this.feedbackRequest.note <= 0)) {
      this.feedbackError = 'Please provide a rating before submitting feedback.';
      return;
    }

    this.isSubmittingFeedback = true;
    this.feedbackError = null;

    this.bookService.returnBorrowBook({
      'book-id': this.selectedBook?.id as number
    }).subscribe({
      next: () => {
        if (withFeedback) {
          this.giveFeedback();
        } else {
          this.resetFeedbackForm();
          this.findAllBorrowedBooks();
        }
      },
      error: (error) => {
        console.error('Error returning book:', error);
        this.feedbackError = 'Failed to return book. Please try again.';
        this.isSubmittingFeedback = false;
      }
    });
  }

  private giveFeedback() {
    this.feedbackService.saveFeedback({
      body: this.feedbackRequest
    }).subscribe({
      next: () => {
        console.log('Feedback submitted successfully');
        this.resetFeedbackForm();
        this.findAllBorrowedBooks();
      },
      error: (error) => {
        console.error('Error submitting feedback:', error);
        this.feedbackError = 'Book returned but failed to submit feedback.';
        this.isSubmittingFeedback = false;
        // Still refresh the list since book was returned
        this.findAllBorrowedBooks();
      }
    });
  }

  private resetFeedbackForm() {
    this.selectedBook = undefined;
    this.feedbackRequest = { bookId: 0, comment: '', note: 1 }; // Changed: Reset to 1 instead of 0
    this.isSubmittingFeedback = false;
    this.feedbackError = null;
  }

  // Cancel feedback modal
  cancelFeedback() {
    this.resetFeedbackForm();
  }
}
