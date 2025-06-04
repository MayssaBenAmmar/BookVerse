import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';
import { UserService } from 'src/app/services/user.service';
import { UserProfileDto } from '../../../../models/user-profile-dto';
import { UserProfile } from '../../../../services/keycloak/user-profile';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy, AfterViewInit {
  userProfile: UserProfile | undefined;
  realUserProfile: any = null;
  loading = false;
  isCurrentUser = true;
  viewUsername: string | null = null;
  errorMessage: string | null = null;
  displayName: string = 'User';
  profileImageSrc: string = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDE1MCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxNTAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxjaXJjbGUgY3g9Ijc1IiBjeT0iNjAiIHI9IjI1IiBmaWxsPSIjRDBENEREIi8+CjxwYXRoIGQ9Ik0zMCAxMjBDMzAgMTA0LjUzNiA0Mi41MzYgOTIgNTggOTJIOTJDMTA3LjQ2NCA5MiAxMjAgMTA0LjUzNiAxMjAgMTIwVjE1MEgzMFYxMjBaIiBmaWxsPSIjRDBENEREIi8+Cjwvc3ZnPgo=';

  formattedUsername: string = '';
  formattedFullName: string = '';
  formattedEmail: string = '';

  private routeSubscription: Subscription | undefined;

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      const username = params.get('username');
      this.viewUsername = username;

      if (this.viewUsername) {
        console.log('Viewing profile for user:', this.viewUsername);

        const currentUsername = this.keycloakService.username || '';
        this.isCurrentUser = this.viewUsername === currentUsername;

        if (this.isCurrentUser) {
          this.loadCurrentUserProfile();
        } else {
          this.loadOtherUserProfile(this.viewUsername);
        }
      } else {
        this.loadCurrentUserProfile();
      }
    });
  }

  ngAfterViewInit() {
    // ✅ DOM fixing désactivé pour éviter l'interférence avec l'email
    console.log('🔥 DOM fixing désactivé pour corriger l\'affichage des emails');
  }

  ngOnDestroy() {
    if (this.routeSubscription) {
      this.routeSubscription.unsubscribe();
    }
  }

  private loadCurrentUserProfile() {
    this.loading = true;
    this.errorMessage = null;
    this.isCurrentUser = true;

    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.realUserProfile = profile;

        console.log('=== PROFIL UTILISATEUR ACTUEL ===');
        console.log('Username:', profile.username);
        console.log('FirstName:', profile.firstName);
        console.log('LastName:', profile.lastName);
        console.log('Email:', profile.email);
        console.log('ProfilePictureUrl:', profile.profilePictureUrl);
        console.log('ID:', profile.id);
        console.log('==================================');

        this.updateDisplayFromRealProfile(profile);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading current user profile:', error);
        this.handleProfileLoadError(error, 'current user');
        this.fallbackToKeycloakProfile();
        this.loading = false;
      }
    });
  }

  private loadOtherUserProfile(username: string) {
    console.log('Loading profile for user:', username);
    this.loading = true;
    this.errorMessage = null;

    this.userService.getUserProfile(username).subscribe({
      next: (profile) => {
        this.realUserProfile = profile;

        console.log('=== PROFIL AUTRE UTILISATEUR ===');
        console.log('Username:', profile.username);
        console.log('FirstName:', profile.firstName);
        console.log('LastName:', profile.lastName);
        console.log('Email:', profile.email);
        console.log('ProfilePictureUrl:', profile.profilePictureUrl);
        console.log('ID:', profile.id);
        console.log('================================');

        this.updateDisplayFromRealProfile(profile);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user profile:', error);
        this.handleProfileLoadError(error, username);
        this.loading = false;
      }
    });
  }

  // ✅ MÉTHODE CORRIGÉE : Username sans @ + titre de page
  private updateDisplayFromRealProfile(profile: any) {
    console.log('🔥 DEBUT updateDisplayFromRealProfile avec:', profile);

    // ✅ USERNAME : Afficher seulement la partie avant @
    const rawUsername = profile.username || 'Unknown';
    this.formattedUsername = rawUsername.includes('@')
      ? rawUsername.split('@')[0]  // Prendre seulement la partie avant @
      : rawUsername;

    // ✅ CONSTRUIRE LE NOM COMPLET PROPREMENT
    const firstName = profile.firstName || '';
    const lastName = profile.lastName || '';

    if (firstName && lastName) {
      this.formattedFullName = `${firstName} ${lastName}`.trim();
    } else if (firstName || lastName) {
      this.formattedFullName = (firstName + lastName).trim();
    } else {
      // Si pas de nom/prénom, utiliser la partie avant @ du username
      this.formattedFullName = this.formattedUsername;
    }

    // ✅ UTILISER LE VRAI EMAIL DIRECTEMENT (PAS DE FORMATAGE)
    this.formattedEmail = profile.email || 'No email provided';

    // ✅ DISPLAY NAME = NOM COMPLET OU USERNAME (partie avant @)
    this.displayName = (firstName || lastName)
      ? this.formattedFullName
      : this.formattedUsername;

    // ✅ METTRE À JOUR LE TITRE DE LA PAGE
    const pageTitle = this.isCurrentUser
      ? `My Profile - Book Network`
      : `${this.formattedUsername}'s Profile - Book Network`;

    document.title = pageTitle;

    // ✅ GESTION DE L'IMAGE
    this.setProfileImage(profile.profilePictureUrl);

    console.log('🔥 RÉSULTAT FINAL:');
    console.log('- Raw Username:', rawUsername);
    console.log('- Formatted Username:', this.formattedUsername);
    console.log('- Full Name:', this.formattedFullName);
    console.log('- Email:', this.formattedEmail);
    console.log('- Display Name:', this.displayName);
    console.log('- Page Title:', pageTitle);
    console.log('- Image:', this.profileImageSrc);
  }

  private setProfileImage(profilePictureUrl?: string): void {
    console.log('🔥 setProfileImage - URL from DB:', profilePictureUrl);

    // Priority 1: Image URL from database (pour tous les utilisateurs)
    if (profilePictureUrl && profilePictureUrl.trim() !== '') {
      this.profileImageSrc = profilePictureUrl;
      console.log('🔥 Using database image URL');
      return;
    }

    // Priority 2: LocalStorage image (SEULEMENT pour l'utilisateur actuel)
    if (this.isCurrentUser) {
      const username = this.keycloakService.username || 'default-user';
      const savedImage = localStorage.getItem(`profileImage_${username}`);

      if (savedImage) {
        this.profileImageSrc = savedImage;
        console.log('🔥 Using localStorage image for current user');
        return;
      }
    }

    // Priority 3: Avatars amusants et variés
    const avatarOptions = [
      // 🎭 Avatars de personnes réelles aléatoires
      `https://i.pravatar.cc/150?u=${encodeURIComponent(this.formattedEmail || this.formattedUsername)}`,

      // 🤖 Avatars robotiques amusants
      `https://robohash.org/${encodeURIComponent(this.formattedEmail || this.formattedUsername)}?size=150x150&set=set1`,

      // 🐱 Avatars d'animaux mignons
      `https://robohash.org/${encodeURIComponent(this.formattedEmail || this.formattedUsername)}?size=150x150&set=set4`,

      // 👾 Avatars de monstres colorés
      `https://robohash.org/${encodeURIComponent(this.formattedEmail || this.formattedUsername)}?size=150x150&set=set2`,

      // 🎨 Avatars géométriques colorés
      `https://api.dicebear.com/7.x/shapes/svg?seed=${encodeURIComponent(this.formattedEmail || this.formattedUsername)}&size=150`,

      // 🐻 Avatars d'ours en peluche
      `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(this.formattedEmail || this.formattedUsername)}&size=150`,

      // 🎭 Avatars cartoon style Pixar
      `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(this.formattedEmail || this.formattedUsername)}&size=150`,

      // 🦸 Avatars de super-héros
      `https://api.dicebear.com/7.x/personas/svg?seed=${encodeURIComponent(this.formattedEmail || this.formattedUsername)}&size=150`
    ];

    // Choisir un avatar basé sur un hash du nom/email pour la consistance
    const identifier = this.formattedEmail || this.formattedUsername || 'default';
    const hash = this.generateSimpleHash(identifier);
    const selectedAvatar = avatarOptions[hash % avatarOptions.length];

    this.profileImageSrc = selectedAvatar;
    console.log('🔥 Using fun avatar:', selectedAvatar);
  }

  // ✅ NOUVELLE MÉTHODE : Hash simple pour sélection cohérente
  private generateSimpleHash(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convertir en entier 32-bit
    }
    return Math.abs(hash);
  }

  private handleProfileLoadError(error: any, context: string): void {
    if (error.status === 404) {
      this.errorMessage = context === 'current user'
        ? 'Your profile could not be found. Please contact support.'
        : 'User not found';
    } else if (error.status === 403) {
      this.errorMessage = 'You do not have permission to view this profile';
    } else if (error.status === 0) {
      this.errorMessage = 'Network error. Please check your connection and try again.';
    } else {
      this.errorMessage = `Error loading profile (${error.status || 'Unknown'})`;
    }
  }

  private fallbackToKeycloakProfile() {
    console.log('🔥 FALLBACK vers Keycloak profile');

    if (!this.isCurrentUser) {
      this.errorMessage = 'Unable to load user profile';
      return;
    }

    this.userProfile = this.keycloakService.profile;
    const username = this.keycloakService.username || '';

    // ✅ APPLIQUER LE MÊME FORMATAGE POUR LE FALLBACK
    this.formattedUsername = username.includes('@')
      ? username.split('@')[0]
      : username;

    this.displayName = this.formattedUsername;
    this.formattedFullName = this.formattedUsername;
    this.formattedEmail = this.keycloakService.profile?.email || `${username}@example.com`;

    this.loadProfileImageFromLocalStorage();
  }

  private loadProfileImageFromLocalStorage(): void {
    if (!this.isCurrentUser) return;

    const username = this.keycloakService.username || 'default-user';
    const savedImage = localStorage.getItem(`profileImage_${username}`);

    if (savedImage) {
      this.profileImageSrc = savedImage;
      console.log('🔥 Image loaded from localStorage');
    } else {
      this.profileImageSrc = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDE1MCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxNTAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxjaXJjbGUgY3g9Ijc1IiBjeT0iNjAiIHI9IjI1IiBmaWxsPSIjRDBENEREIi8+CjxwYXRoIGQ9Ik0zMCAxMjBDMzAgMTA0LjUzNiA0Mi41MzYgOTIgNTggOTJIOTJDMTA3LjQ2NCA5MiAxMjAgMTA0LjUzNiAxMjAgMTIwVjE1MEgzMFYxMjBaIiBmaWxsPSIjRDBENEREIi8+Cjwvc3ZnPgo=';
      console.log('🔥 Using default placeholder');
    }
  }

  editProfile() {
    if (!this.isCurrentUser) return;

    const authServerUrl = this.keycloakService.keycloakInstance.authServerUrl;
    const realm = this.keycloakService.keycloakInstance.realm;

    if (authServerUrl && realm) {
      window.location.href = `${authServerUrl}/realms/${realm}/account`;
    } else {
      console.error('Unable to determine Keycloak URL');
      this.errorMessage = 'Unable to access account settings';
    }
  }

  goBack() {
    this.router.navigate(['/books']);
  }

  triggerFileInput(): void {
    if (!this.isCurrentUser) return;

    const fileInput = document.getElementById('profile-image-upload');
    if (fileInput) {
      fileInput.click();
    }
  }

  onProfileImageSelected(event: any): void {
    if (!this.isCurrentUser) return;

    const file = event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please select a valid image file';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Image file size must be less than 5MB';
      return;
    }

    const reader = new FileReader();

    reader.onload = (e: any) => {
      this.profileImageSrc = e.target.result;

      const username = this.keycloakService.username || 'default-user';
      localStorage.setItem(`profileImage_${username}`, this.profileImageSrc);
      console.log('🔥 Image saved to localStorage:', `profileImage_${username}`);
    };

    reader.onerror = () => {
      this.errorMessage = 'Error reading the image file';
    };

    reader.readAsDataURL(file);
  }

  // ✅ NOUVELLE MÉTHODE : Gestion des erreurs d'image
  onImageError(event: any): void {
    console.log('🔥 Image failed to load, using fallback');
    // Fallback vers placeholder en cas d'erreur
    event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDE1MCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxNTAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxjaXJjbGUgY3g9Ijc1IiBjeT0iNjAiIHI9IjI1IiBmaWxsPSIjRDBENEREIi8+CjxwYXRoIGQ9Ik0zMCAxMjBDMzAgMTA0LjUzNiA0Mi41MzYgOTIgNTggOTJIOTJDMTA3LjQ2NCA5MiAxMjAgMTA0LjUzNiAxMjAgMTIwVjE1MEgzMFYxMjBaIiBmaWxsPSIjRDBENEREIi8+Cjwvc3ZnPgo=';
  }

  // ✅ NOUVELLE MÉTHODE : Date d'inscription
  getCurrentDate(): string {
    return new Date().toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}
