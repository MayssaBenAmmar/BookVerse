import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Recommendation {
  book_id: number;
  score: number;
  title: string;
  authorName: string;
  cover: string;
  rate: number;
  genre: string;
  description?: string;
  recommendationReason?: string;
  relevanceScore?: number;
  similarBooks?: string[];
}

export interface RecommendationFeedback {
  userId: number;
  bookId: number;
  action: 'liked' | 'disliked' | 'not_interested' | 'saved';
  rating?: number;
  comment?: string;
  recommendationId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private baseUrl = `${environment.apiUrl}/recommendations`;

  constructor(private http: HttpClient) {}

  getUserRecommendations(userId: number, limit: number = 10): Observable<Recommendation[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Recommendation[]>(`${this.baseUrl}/${userId}`, { params });
  }

  getFeaturedBooks(count: number = 6): Observable<Recommendation[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.get<Recommendation[]>(`${this.baseUrl}/featured`, { params });
  }

  getRecommendationsByGenre(genre: string, count: number = 6): Observable<Recommendation[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.get<Recommendation[]>(`${this.baseUrl}/genre/${genre}`, { params });
  }

  getSimilarBooks(bookId: number, count: number = 5): Observable<Recommendation[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.get<Recommendation[]>(`${this.baseUrl}/similar/${bookId}`, { params });
  }

  getTrendingBooks(count: number = 8): Observable<Recommendation[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.get<Recommendation[]>(`${this.baseUrl}/trending`, { params });
  }

  submitFeedback(feedback: RecommendationFeedback): Observable<any> {
    return this.http.post(`${this.baseUrl}/feedback`, feedback);
  }

  getServiceStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/status`);
  }
}
