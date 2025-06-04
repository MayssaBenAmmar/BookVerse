import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KeycloakService } from 'src/app/services/keycloak/keycloak.service';

export interface UserProfileDto {
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  id?: string;
  profilePictureUrl?: string;
  booksCount?: number;
  bio?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // ✅ URL CORRIGÉE : Context-path /api/v1 + endpoint /api/v1/users
  private readonly baseUrl = 'http://localhost:8088/api/v1/api/v1/users';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService
  ) { }

  private getAuthHeaders(): HttpHeaders {
    return new HttpHeaders(this.keycloakService.getAuthHeader());
  }

  getCurrentUserProfile(): Observable<UserProfileDto> {
    console.log('🔥 Calling:', `${this.baseUrl}/profile`);
    return this.http.get<UserProfileDto>(`${this.baseUrl}/profile`, {
      headers: this.getAuthHeaders()
    });
  }

  getUserProfile(username: string): Observable<UserProfileDto> {
    const encodedUsername = encodeURIComponent(username);
    const url = `${this.baseUrl}/profile/${encodedUsername}`;
    console.log('🔥 Calling:', url);

    return this.http.get<UserProfileDto>(url, {
      headers: this.getAuthHeaders()
    });
  }

  updateProfileImage(imageData: string): Observable<any> {
    const url = `${this.baseUrl}/profile/image`;
    return this.http.put(url, { profileImageUrl: imageData }, {
      headers: this.getAuthHeaders()
    });
  }
}
