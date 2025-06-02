import {Component, OnInit} from '@angular/core';
import {PageResponseBorrowedBookResponse} from '../../../../services/models/page-response-borrowed-book-response';
import {BookService} from '../../../../services/services/book.service';
import {BorrowedBookResponse} from '../../../../services/models/borrowed-book-response';

@Component({
  selector: 'app-returned-books',
  templateUrl: './returned-books.component.html',
  styleUrls: ['./returned-books.component.scss']
})
export class ReturnedBooksComponent implements OnInit {

  page = 0;
  size = 5;
  pages: any = [];
  returnedBooks: PageResponseBorrowedBookResponse = {};
  message = '';
  level: 'success' |'error' = 'success';

  constructor(
    private bookService: BookService
  ) {
  }

  ngOnInit(): void {
    this.findAllReturnedBooks();
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

  // NEW: Utility method to calculate days borrowed (from borrowed to returned)
  getDaysBorrowed(borrowedDate: string | undefined, returnedDate: string | undefined): number {
    if (!borrowedDate || !returnedDate) return 0;

    const borrowed = new Date(borrowedDate);
    const returned = new Date(returnedDate);
    const timeDiff = returned.getTime() - borrowed.getTime();
    return Math.ceil(timeDiff / (1000 * 3600 * 24));
  }

  // NEW: Utility method to calculate days since returned
  getDaysSinceReturned(returnedDate: string | undefined): number {
    if (!returnedDate) return 0;

    const returned = new Date(returnedDate);
    const now = new Date();
    const timeDiff = now.getTime() - returned.getTime();
    return Math.ceil(timeDiff / (1000 * 3600 * 24));
  }

  private findAllReturnedBooks() {
    this.bookService.findAllReturnedBooks({
      page: this.page,
      size: this.size
    }).subscribe({
      next: (resp) => {
        this.returnedBooks = resp;
        this.pages = Array(this.returnedBooks.totalPages)
          .fill(0)
          .map((x, i) => i);
      }
    });
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllReturnedBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllReturnedBooks();
  }

  goToPreviousPage() {
    this.page --;
    this.findAllReturnedBooks();
  }

  goToLastPage() {
    this.page = this.returnedBooks.totalPages as number - 1;
    this.findAllReturnedBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllReturnedBooks();
  }

  get isLastPage() {
    return this.page === this.returnedBooks.totalPages as number - 1;
  }

  approveBookReturn(book: BorrowedBookResponse) {
    if (!book.returned) {
      return;
    }
    this.bookService.approveReturnBorrowBook({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        this.level = 'success';
        this.message = 'Book return approved';
        this.findAllReturnedBooks();
      }
    });
  }
}
