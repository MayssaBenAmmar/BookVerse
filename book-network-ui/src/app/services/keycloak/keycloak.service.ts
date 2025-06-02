import { Injectable } from '@angular/core';
import Keycloak, { KeycloakInstance } from 'keycloak-js';
import { UserProfile } from './user-profile';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private _keycloak: KeycloakInstance;
  private _profile?: UserProfile;
  private _initialized: boolean = false;

  constructor() {
    this._keycloak = new Keycloak({
      url: 'http://localhost:9090',           // 🔁 Keycloak server URL
      realm: 'book-social-network',           // 🔁 Your realm name
      clientId: 'bsn'                          // 🔁 Your client ID
    });
  }

  // 🧠 Getters
  get keycloakInstance(): KeycloakInstance {
    return this._keycloak;
  }

  get profile(): UserProfile | undefined {
    return this._profile;
  }

  get token(): string | undefined {
    return this._keycloak.token;
  }

  get username(): string | undefined {
    // First try to get preferred_username from token
    const preferredUsername = this._keycloak.tokenParsed?.['preferred_username'];
    if (preferredUsername) {
      return preferredUsername;
    }

    // Fall back to profile username if available
    if (this._profile && this._profile.username) {
      return this._profile.username;
    }

    // Last resort, use email from token or profile
    return this._keycloak.tokenParsed?.['email'] || this._profile?.email;
  }

  get isInitialized(): boolean {
    return this._initialized;
  }

  get isAuthenticated(): boolean {
    return !!this._keycloak.authenticated;
  }

  get userRoles(): string[] {
    return this._keycloak.realmAccess?.roles || [];
  }

  get fullName(): string | undefined {
    if (this._profile) {
      const firstName = this._profile.firstName || '';
      const lastName = this._profile.lastName || '';

      if (firstName || lastName) {
        return `${firstName} ${lastName}`.trim();
      }
    }

    // Fallback to name in token if available
    return this._keycloak.tokenParsed?.['name'];
  }

  // ✅ Called at app startup to init Keycloak
  async init(): Promise<boolean> {
    try {
      const authenticated = await this._keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,  // Optional for faster local dev
      });

      if (!authenticated) {
        console.warn('Not authenticated!');
        this.login();
        return false;
      }

      // Load user profile
      try {
        this._profile = await this._keycloak.loadUserProfile() as UserProfile;

        // Ensure username is set in profile
        if (this._profile && !this._profile.username) {
          this._profile.username = this._keycloak.tokenParsed?.['preferred_username'];
        }

        console.log('User profile loaded:', this._profile);
      } catch (error) {
        console.error('Failed to load user profile:', error);
      }

      // 🔁 Start token refresh loop
      this._setupAutoRefresh();

      this._initialized = true;
      return true;
    } catch (error) {
      console.error('Failed to initialize Keycloak:', error);
      return false;
    }
  }

  login(): void {
    this._keycloak.login();
  }

  logout(): void {
    this._keycloak.logout({ redirectUri: window.location.origin });
  }

  // Check if user has specific role
  hasRole(role: string): boolean {
    return this.userRoles.includes(role);
  }

  // Get authorization headers for API requests
  getAuthHeader(): { [key: string]: string } {
    if (this._keycloak && this._keycloak.token) {
      return {
        Authorization: `Bearer ${this._keycloak.token}`
      };
    }
    return {};
  }

  // Manually refresh the token
  async refreshToken(minValidity: number = 10): Promise<boolean> {
    try {
      return await this._keycloak.updateToken(minValidity);
    } catch (error) {
      console.error('Error refreshing token:', error);
      this.logout();
      return false;
    }
  }

  // ♻️ Refresh token every 60 seconds
  private _setupAutoRefresh(): void {
    setInterval(() => {
      this._keycloak.updateToken(60).catch(() => {
        console.warn('Failed to refresh token, logging out...');
        this.logout();
      });
    }, 60000); // 60s
  }
}
