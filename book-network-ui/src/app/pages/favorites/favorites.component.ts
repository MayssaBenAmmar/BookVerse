import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';

// Define interfaces directly in this file since we don't have access to the original ones
interface BookResponse {
  id?: number;
  title?: string;
  author?: string;
  description?: string;
  genre?: string;
  coverUrl?: string;
  readUrl?: string;
  rating?: number;
}

// Mock service since we don't have the original
class BookService {
  getFavoriteBooks() {
    // Return an Observable of BookResponse[]
    return {
      subscribe: (callbacks: any) => {
        // Simulate API response
        setTimeout(() => {
          callbacks.next([]);
        }, 100);
      }
    };
  }

  unfavoriteBook(params: {[key: string]: number}) {
    // Return an Observable
    return {
      subscribe: (callbacks: any) => {
        // Simulate API response
        setTimeout(() => {
          callbacks.next();
        }, 100);
      }
    };
  }
}

@Component({
  selector: 'app-favorites',
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss']
})
export class FavoritesComponent implements OnInit {
  favoriteBooks: BookResponse[] = [];
  displayedBooks: BookResponse[] = [];
  searchQuery: string = '';
  showSortMenu: boolean = false;
  sortBy: 'title' | 'rating' | 'date' = 'date';
  message: string = '';
  level: 'success' | 'error' = 'success';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 1;

  constructor(
    private bookService: BookService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadFavoriteBooks();
  }

  loadFavoriteBooks() {
    this.bookService.getFavoriteBooks().subscribe({
      next: (books: BookResponse[]) => {
        this.favoriteBooks = books;
        this.displayedBooks = [...books];
        this.sortBooks(this.sortBy);
        this.calculateTotalPages();
      },
      error: (err: any) => {
        console.error('Error loading favorite books:', err);
        this.message = 'Failed to load your favorite books.';
        this.level = 'error';
      }
    });
  }

  filterBooks() {
    if (!this.searchQuery.trim()) {
      this.displayedBooks = [...this.favoriteBooks];
    } else {
      const query = this.searchQuery.toLowerCase();
      this.displayedBooks = this.favoriteBooks.filter(book =>
        book.title?.toLowerCase().includes(query) ||
        book.author?.toLowerCase().includes(query) ||
        book.genre?.toLowerCase().includes(query) ||
        book.description?.toLowerCase().includes(query)
      );
    }
    this.sortBooks(this.sortBy);
    this.calculateTotalPages();
    this.currentPage = 1; // Reset to first page after filtering
  }

  sortBooks(sortType: 'title' | 'rating' | 'date') {
    this.sortBy = sortType;
    this.showSortMenu = false;

    const sortedBooks = [...this.displayedBooks];

    switch (sortType) {
      case 'title':
        sortedBooks.sort((a, b) =>
          (a.title || '').localeCompare(b.title || '')
        );
        break;
      case 'rating':
        sortedBooks.sort((a, b) =>
          (b.rating || 0) - (a.rating || 0)
        );
        break;
      case 'date':
        // Assuming books have a dateAdded property or using id as a proxy
        sortedBooks.sort((a, b) =>
          (b.id || 0) - (a.id || 0)
        );
        break;
    }

    this.displayedBooks = sortedBooks;
    this.updateDisplayedBooks();
  }

  toggleSortMenu() {
    this.showSortMenu = !this.showSortMenu;
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (this.showSortMenu && !target.closest('.sort-btn') && !target.closest('.sort-menu')) {
      this.showSortMenu = false;
    }
  }

  removeFromFavorites(book: BookResponse) {
    this.bookService.unfavoriteBook({ 'book-id': book.id as number }).subscribe({
      next: () => {
        this.favoriteBooks = this.favoriteBooks.filter(b => b.id !== book.id);
        this.displayedBooks = this.displayedBooks.filter(b => b.id !== book.id);
        this.calculateTotalPages();
        this.message = `"${book.title}" removed from favorites`;
        this.level = 'success';
      },
      error: (err: any) => {
        console.error('Error removing from favorites:', err);
        this.message = 'Failed to remove book from favorites';
        this.level = 'error';
      }
    });
  }

  viewBookDetails(book: BookResponse) {
    this.router.navigate(['/books/details', book.id]);
  }

  readBook(book: BookResponse) {
    // Implement read book functionality or redirect to reading page
    if (book.readUrl) {
      window.open(book.readUrl, '_blank');
    }
  }

  shareBook(book: BookResponse) {
    // Implement share functionality
    if (navigator.share) {
      navigator.share({
        title: book.title,
        text: `Check out this book: ${book.title} by ${book.author}`,
        url: `${window.location.origin}/books/details/${book.id}`
      }).catch((err: any) => {
        console.error('Error sharing:', err);
      });
    } else {
      // Fallback for browsers that don't support navigator.share
      this.message = `Shareable link copied to clipboard!`;
      this.level = 'success';
      // Copy to clipboard functionality here
    }
  }

  getAverageRating(): number {
    if (!this.favoriteBooks.length) return 0;

    const totalRating = this.favoriteBooks.reduce((sum, book) => sum + (book.rating || 0), 0);
    return totalRating / this.favoriteBooks.length;
  }

  getTopGenre(): string {
    if (!this.favoriteBooks.length) return '';

    // Count occurrences of each genre
    const genreCounts: {[key: string]: number} = {};
    this.favoriteBooks.forEach(book => {
      if (book.genre) {
        genreCounts[book.genre] = (genreCounts[book.genre] || 0) + 1;
      }
    });

    // Find the most common genre
    let topGenre = '';
    let maxCount = 0;

    Object.entries(genreCounts).forEach(([genre, count]) => {
      if (count > maxCount) {
        maxCount = count;
        topGenre = genre;
      }
    });

    return topGenre;
  }

  // Pagination methods
  calculateTotalPages() {
    this.totalPages = Math.ceil(this.displayedBooks.length / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages || 1;
    }
  }

  updateDisplayedBooks() {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    const allBooks = [...this.displayedBooks];
    this.displayedBooks = allBooks.slice(startIndex, endIndex);
  }

  changePage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updateDisplayedBooks();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }
}
