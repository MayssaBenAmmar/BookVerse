import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { FavoriteService } from '../../../../services/favorite.service';
import { BookResponse } from '../../../../services/models/book-response';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';

@Component({
  selector: 'app-favorites',
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss']
})
export class FavoritesComponent implements OnInit {
  favorites: BookResponse[] = [];
  displayedBooks: BookResponse[] = [];
  loading = true;
  error: string | null = null;

  // Pagination
  currentPage = 1;
  itemsPerPage = 8; // Adjust based on your design
  totalPages = 1;

  constructor(
    private favoriteService: FavoriteService,
    private router: Router,
    public keycloakService: KeycloakService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  loadFavorites(): void {
    this.loading = true;
    this.error = null;

    this.favoriteService.getFavorites()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (books) => {
          this.favorites = books;
          this.calculateTotalPages();
          this.updateDisplayedBooks();
        },
        error: (err) => {
          console.error('Failed to load favorites', err);
          this.error = 'Unable to load your favorite books. Please try again later.';
        }
      });
  }

  calculateTotalPages(): void {
    this.totalPages = Math.ceil(this.favorites.length / this.itemsPerPage);
    this.currentPage = this.currentPage > this.totalPages ? (this.totalPages || 1) : this.currentPage;
  }

  updateDisplayedBooks(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = Math.min(startIndex + this.itemsPerPage, this.favorites.length);
    this.displayedBooks = this.favorites.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updateDisplayedBooks();
    }
  }

  goToFirstPage(): void {
    this.goToPage(1);
  }

  goToPreviousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  goToNextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  goToLastPage(): void {
    this.goToPage(this.totalPages);
  }

  // Just replace the getPageNumbers method
  getPageNumbers(): number[] {
    // Show a maximum of 5 page numbers
    const totalPages = this.totalPages;
    const currentPage = this.currentPage;
    const maxPagesToShow = 5;

    if (totalPages <= maxPagesToShow) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    let startPage = Math.max(currentPage - Math.floor(maxPagesToShow / 2), 1);
    let endPage = startPage + maxPagesToShow - 1;

    if (endPage > totalPages) {
      endPage = totalPages;
      startPage = Math.max(endPage - maxPagesToShow + 1, 1);
    }

    return Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i);
  }

  browseBooks(): void {
    this.router.navigate(['/books']);
  }

  retryLoading(): void {
    this.loadFavorites();
  }

  handleFavoriteChanged(event: {book: BookResponse, isFavorited: boolean}): void {
    if (!event.isFavorited) {
      // Remove book from the favorites list
      this.favorites = this.favorites.filter(book => book.id !== event.book.id);
      this.calculateTotalPages();
      this.updateDisplayedBooks();
    }
  }
}
