import {Component, OnInit} from '@angular/core';
import {PageResponseBookResponse} from '../../../../services/models/page-response-book-response';
import {BookService} from '../../../../services/services/book.service';
import {BookResponse} from '../../../../services/models/book-response';
import {Router} from '@angular/router';
import {BookSearchService} from "../../../../services/services/book-search.service";

@Component({
  selector: 'app-my-books',
  templateUrl: './my-books.component.html',
  styleUrls: ['./my-books.component.scss']
})
export class MyBooksComponent implements OnInit {

  bookResponse: PageResponseBookResponse = {};
  page = 0;
  size = 5;
  pages: any = [];

  mainBook?: BookResponse;
  suggestedBooks: BookResponse[] = [];

  // Added message and level variables for alerts
  message: string = '';
  level: 'success' | 'error' | '' = '';

  constructor(
    private bookService: BookService,
    private readonly bookSearchService: BookSearchService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.bookSearchService.bookResults$.subscribe((books) => {
      if (books.content && books.content.length > 0) {
        this.mainBook = books.content[0];
        this.suggestedBooks = books.content.slice(1, 5);

        console.log("SUGGESTED BOOKS", this.suggestedBooks);

        this.bookResponse = {
          ...books,
        };

        this.pages = Array(books.totalPages).fill(0).map((_, i) => i);
      }
    });

    this.findAllBooks();
  }

  private findAllBooks() {
    this.bookService.findAllBooksByOwner({
      page: this.page,
      size: this.size
    })
      .subscribe({
        next: (books) => {
          this.bookResponse = books;
          this.pages = Array(this.bookResponse.totalPages)
            .fill(0)
            .map((x, i) => i);
        },
        error: (err) => {
          this.showMessage('Failed to load your books. Please try again.', 'error');
        }
      });
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllBooks();
  }

  goToLastPage() {
    this.page = this.bookResponse.totalPages as number - 1;
    this.findAllBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBooks();
  }

  get isLastPage() {
    return this.page === this.bookResponse.totalPages as number - 1;
  }

  archiveBook(book: BookResponse) {
    this.bookService.updateArchivedStatus({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        book.archived = !book.archived;
        this.findAllBooks();
        this.showMessage(`Book "${book.title}" has been ${book.archived ? 'archived' : 'unarchived'}.`, 'success');
      },
      error: (err) => {
        this.showMessage('Failed to update archive status. Please try again.', 'error');
      }
    });
  }

  shareBook(book: BookResponse) {
    this.bookService.updateShareableStatus({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        book.shareable = !book.shareable;
        this.findAllBooks();
        this.showMessage(`Book "${book.title}" is now ${book.shareable ? 'shared' : 'private'}.`, 'success');
      },
      error: (err) => {
        this.showMessage('Failed to update sharing status. Please try again.', 'error');
      }
    });
  }

  editBook(book: BookResponse) {
    this.router.navigate(['books', 'manage', book.id]);
  }

  onDeleteBook(bookId: number) {
    this.bookService.deleteBook({ 'book-id': bookId }).subscribe({
      next: () => {
        // Get book title before removing it
        let bookTitle = '';

        if (this.mainBook && this.mainBook.id === bookId) {
          bookTitle = this.mainBook.title || 'Book';
          this.mainBook = undefined;
        } else if (this.bookResponse.content) {
          const book = this.bookResponse.content.find(b => b.id === bookId);
          bookTitle = book?.title || 'Book';
        }

        // Remove from suggestedBooks
        this.suggestedBooks = this.suggestedBooks.filter(book => book.id !== bookId);

        // Remove from regular bookResponse.content
        if (this.bookResponse.content) {
          this.bookResponse.content = this.bookResponse.content.filter(book => book.id !== bookId);
        }

        // Update pagination counts
        if (this.bookResponse.totalElements && this.bookResponse.totalElements > 0) {
          this.bookResponse.totalElements--;
        }

        // Refresh the book list
        this.findAllBooks();

        // Show success message
        this.showMessage(`"${bookTitle}" has been successfully deleted.`, 'success');
      },
      error: (error) => {
        console.error('Error deleting book:', error);
        this.showMessage('Failed to delete book. Please try again.', 'error');
      }
    });
  }

  // Helper method to show messages
  private showMessage(msg: string, lvl: 'success' | 'error') {
    this.message = msg;
    this.level = lvl;

    // Auto-clear message after 5 seconds
    setTimeout(() => {
      this.message = '';
      this.level = '';
    }, 5000);
  }
}
