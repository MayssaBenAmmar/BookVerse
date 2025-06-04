import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';
import { BookResponse } from './models/book-response';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private apiUrl = `${environment.apiUrl}/favorites`;

  constructor(private http: HttpClient) {}

  /**
   * Get all favorite books for the current user
   * Auth is handled by the HttpTokenInterceptor that adds the Keycloak token
   */
  getFavorites(): Observable<BookResponse[]> {
    return this.http.get<BookResponse[]>(this.apiUrl).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle "User not found" error gracefully
        if (error.status === 500 && error.error?.error === 'User not found') {
          console.warn('User authentication issue - returning empty favorites list');
          return of([]); // Return empty array instead of erroring
        }

        console.error('Error fetching favorites:', error);
        return of([]); // Return empty array for any error to prevent UI breaking
      })
    );
  }

  /**
   * Add a book to favorites
   */
  addFavorite(bookId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${bookId}`, {}).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error adding favorite:', error);
        return throwError(() => new Error('Failed to add to favorites'));
      })
    );
  }

  /**
   * Remove a book from favorites
   */
  removeFavorite(bookId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${bookId}`).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error removing favorite:', error);
        return throwError(() => new Error('Failed to remove from favorites'));
      })
    );
  }
}
