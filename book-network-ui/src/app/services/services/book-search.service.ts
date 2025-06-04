import {Injectable} from "@angular/core";
import {BookService} from "./book.service";
import {SearchBooks$Params} from "../fn/book/search-books";
import {PageResponseBookResponse} from "../models/page-response-book-response";
import {BehaviorSubject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class BookSearchService {
  private bookResultsSubject = new BehaviorSubject<PageResponseBookResponse>({});
  bookResults$ = this.bookResultsSubject.asObservable();

  constructor(private readonly bookService: BookService) {
  }

  searchBooks(searchParams: SearchBooks$Params) {
    this.bookService.searchBooks(searchParams).subscribe(bookResponse => {
      this.bookResultsSubject.next(bookResponse);
    });
  }
}
