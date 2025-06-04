import { Component, OnInit } from '@angular/core';
import { RecommendationService, Recommendation } from 'src/app/services/recommendation.service';
import { BookResponse } from 'src/app/services/models/book-response';
import { KeycloakService } from 'src/app/services/keycloak/keycloak.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-featured-books',
  templateUrl: './featured-books.component.html',
  styleUrls: ['./featured-books.component.scss']
})
export class FeaturedBooksComponent implements OnInit {
  personalRecommendations: BookResponse[] = [];
  featuredBooks: BookResponse[] = [];
  trendingBooks: BookResponse[] = [];
  genreRecommendations: {genre: string, books: BookResponse[]}[] = [];

  isAuthenticated = false;
  aiServiceOnline = false;
  currentUserId?: number;
  isLoading = true;
  errorMessage: string | null = null;

  private genres = ['ROMANCE', 'FANTASY', 'COMEDY', 'SCIFI', 'HORROR']; // Removed MYSTERY and FICTION

  constructor(
    private recommendationService: RecommendationService,
    public keycloakService: KeycloakService, // Changed to public
    private router: Router
  ) {}

  ngOnInit() {
    this.checkAuthentication();
    this.checkAIServiceStatus();
    this.loadAllRecommendations();
  }

  checkAuthentication() {
    this.isAuthenticated = this.keycloakService.token !== undefined;
    if (this.isAuthenticated) {
      // Get user ID from Keycloak token or username
      this.currentUserId = this.getUserIdFromKeycloak();
    }
  }

  private getUserIdFromKeycloak(): number | undefined {
    // Try to get user ID from token, fallback to a hash of username
    const username = this.keycloakService.username;
    if (username) {
      // Simple hash function to convert username to number
      return this.hashCode(username);
    }
    return undefined;
  }

  private hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return Math.abs(hash);
  }

  checkAIServiceStatus() {
    this.recommendationService.getServiceStatus().subscribe({
      next: (status) => {
        this.aiServiceOnline = status.available;
      },
      error: () => {
        this.aiServiceOnline = false;
      }
    });
  }

  loadAllRecommendations() {
    this.isLoading = true;
    this.errorMessage = null;

    // Load featured books
    this.loadFeaturedBooks();

    // Load trending books
    this.loadTrendingBooks();

    // Load personal recommendations if authenticated
    if (this.isAuthenticated && this.currentUserId) {
      this.loadPersonalRecommendations();
    }

    // Load genre recommendations
    this.loadGenreRecommendations();
  }

  loadFeaturedBooks() {
    this.recommendationService.getFeaturedBooks(6).subscribe({
      next: (recommendations) => {
        this.featuredBooks = this.convertRecommendationsToBookResponse(recommendations);
      },
      error: (error) => {
        console.error('Error loading featured books:', error);
        this.handleError('Failed to load featured books');
      }
    });
  }

  loadTrendingBooks() {
    this.recommendationService.getTrendingBooks(8).subscribe({
      next: (recommendations) => {
        this.trendingBooks = this.convertRecommendationsToBookResponse(recommendations);
      },
      error: (error) => {
        console.error('Error loading trending books:', error);
        this.handleError('Failed to load trending books');
      }
    });
  }

  loadPersonalRecommendations() {
    if (!this.currentUserId) return;

    this.recommendationService.getUserRecommendations(this.currentUserId, 10).subscribe({
      next: (recommendations) => {
        this.personalRecommendations = this.convertRecommendationsToBookResponse(recommendations);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading personal recommendations:', error);
        this.handleError('Failed to load personalized recommendations');
        this.isLoading = false;
      }
    });
  }

  loadGenreRecommendations() {
    let completedGenres = 0;
    const totalGenres = this.genres.length;

    this.genres.forEach(genre => {
      this.recommendationService.getRecommendationsByGenre(genre, 6).subscribe({
        next: (recommendations) => {
          if (recommendations.length > 0) {
            this.genreRecommendations.push({
              genre: this.formatGenre(genre),
              books: this.convertRecommendationsToBookResponse(recommendations)
            });
          } else {
            console.warn(`No recommendations found for genre: ${genre}`);
          }
          completedGenres++;
          if (completedGenres === totalGenres && !this.isAuthenticated) {
            this.isLoading = false;
          }
        },
        error: (error) => {
          console.error(`Error loading ${genre} recommendations:`, error);
          // Don't show error to user, just skip this genre
          completedGenres++;
          if (completedGenres === totalGenres && !this.isAuthenticated) {
            this.isLoading = false;
          }
        }
      });
    });
  }

  private convertRecommendationsToBookResponse(recommendations: Recommendation[]): BookResponse[] {
    return recommendations.map(rec => ({
      id: rec.book_id,
      title: rec.title,
      authorName: rec.authorName,
      cover: rec.cover ? [rec.cover] : [], // Keep as base64 string array - book card will handle the prefix
      rate: rec.rate,
      genre: rec.genre,
      synopsis: rec.description || 'No description available',
      shareable: true,
      archived: false,
      owner: rec.authorName || 'System'
    }));
  }

  // Remove the processBookCover method - it was causing issues

  private formatGenre(genre: string): string {
    return genre.charAt(0) + genre.slice(1).toLowerCase().replace('_', ' ');
  }

  private handleError(message: string) {
    this.errorMessage = message;
    setTimeout(() => {
      this.errorMessage = null;
    }, 5000);
  }

  // Book card event handlers
  onShare(book: BookResponse) {
    console.log('Sharing book:', book.title);
    // Implement share functionality
  }

  onArchive(book: BookResponse) {
    console.log('Archiving book:', book.title);
    // Implement archive functionality
  }

  onAddToWaitingList(book: BookResponse) {
    console.log('Adding to waiting list:', book.title);
    // Implement waiting list functionality
  }

  onBorrow(book: BookResponse) {
    console.log('Borrowing book:', book.title);
    this.router.navigate(['/books/borrow', book.id]);
  }

  onEdit(book: BookResponse) {
    console.log('Editing book:', book.title);
    this.router.navigate(['/books/manage', book.id]);
  }

  onShowDetails(book: BookResponse) {
    console.log('Showing details for:', book.title);
    this.router.navigate(['/books/details', book.id]);
  }

  onFavoriteChanged(event: {book: BookResponse, isFavorited: boolean}) {
    console.log('Favorite status changed:', event);
    // Submit feedback to AI system
    if (this.currentUserId) {
      this.recommendationService.submitFeedback({
        userId: this.currentUserId,
        bookId: event.book.id!,
        action: event.isFavorited ? 'liked' : 'disliked'
      }).subscribe({
        next: () => {
          console.log('Feedback submitted to AI system');
          // Optionally refresh recommendations
          this.refreshPersonalRecommendations();
        },
        error: (error) => console.error('Error submitting feedback:', error)
      });
    }
  }

  onBookDeleted(bookId: number) {
    console.log('Book deleted:', bookId);
    // Remove book from all lists
    this.personalRecommendations = this.personalRecommendations.filter(book => book.id !== bookId);
    this.featuredBooks = this.featuredBooks.filter(book => book.id !== bookId);
    this.trendingBooks = this.trendingBooks.filter(book => book.id !== bookId);
    this.genreRecommendations.forEach(genreRec => {
      genreRec.books = genreRec.books.filter(book => book.id !== bookId);
    });
  }

  onViewProfile(userId: string) {
    console.log('Viewing profile for:', userId);
    this.router.navigate(['/profile/user', userId]);
  }

  refreshRecommendations() {
    this.loadAllRecommendations();
  }

  private refreshPersonalRecommendations() {
    if (this.isAuthenticated && this.currentUserId) {
      setTimeout(() => {
        this.loadPersonalRecommendations();
      }, 1000);
    }
  }
}
