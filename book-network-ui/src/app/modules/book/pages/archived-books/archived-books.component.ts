import {Component, OnInit} from '@angular/core';
import {PageResponseBookResponse} from "../../../../services/models/page-response-book-response";
import {BookService} from "../../../../services/services/book.service";
import {Router} from "@angular/router";
import {BookResponse} from "../../../../services/models/book-response";
import {BookSearchService} from "../../../../services/services/book-search.service";

@Component({
  selector: 'app-archived-books',
  templateUrl: './archived-books.component.html',
  styleUrls: ['./archived-books.component.scss']
})
export class ArchivedBooksComponent implements OnInit {
  bookResponse: PageResponseBookResponse = {
    content: [],
    totalElements: 0,
    totalPages: 0
  };

  page = 0;
  size = 5;
  pages: number[] = [];
  message = '';
  level: 'success' | 'error' = 'success';

  mainBook?: BookResponse;
  suggestedBooks: BookResponse[] = [];

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
        this.suggestedBooks = books.content.slice(1);

        this.bookResponse = {
          ...books,
          content: books.content.slice(1) // optionally override content too
        };

        this.updatePagesArray();
      }
    });

    this.findAllBooks();
  }

  private updatePagesArray(): void {
    const totalPages = this.bookResponse.totalPages || 0;
    this.pages = Array(totalPages).fill(0).map((_, i) => i);
  }

  private findAllBooks() {
    this.bookService.findAllArchivedBooks({
      page: this.page,
      size: this.size
    })
      .subscribe({
        next: (books) => {
          this.bookResponse = books;
          this.updatePagesArray();
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
    const totalPages = this.bookResponse.totalPages || 0;
    this.page = totalPages > 0 ? totalPages - 1 : 0;
    this.findAllBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBooks();
  }

  get isLastPage() {
    const totalPages = this.bookResponse.totalPages || 0;
    return totalPages > 0 ? this.page === totalPages - 1 : true;
  }

  borrowBook(book: BookResponse) {
    this.message = '';
    this.level = 'success';
    this.bookService.borrowBook({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        this.level = 'success';
        this.message = 'Book successfully added to your list';
      },
      error: (err) => {
        console.log(err);
        this.level = 'error';
        this.message = err.error.error;
      }
    });
  }

  displayBookDetails(book: BookResponse) {
    this.router.navigate(['books', 'details', book.id]);
  }
}
