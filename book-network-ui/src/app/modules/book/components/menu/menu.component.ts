import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';
import { BookSearchService } from "../../../../services/services/book-search.service";
import { Router } from '@angular/router';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent implements OnInit {

  query = '';
  isDropdownOpen = false;
  userEmail: string = '';
  profileImageSrc: string = '';

  constructor(
    private keycloakService: KeycloakService,
    private readonly bookSearchService: BookSearchService,
    private router: Router,
    private elementRef: ElementRef
  ) {
  }

  ngOnInit(): void {
    const linkColor = document.querySelectorAll('.nav-link');
    linkColor.forEach(link => {
      if (window.location.href.endsWith(link.getAttribute('href') || '')) {
        link.classList.add('active');
      }
      link.addEventListener('click', () => {
        linkColor.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
      });
    });

    // Get user email from keycloak if available
    this.userEmail = this.keycloakService.profile?.email || '';

    // Load profile image from localStorage (matching the profile component approach)
    this.loadProfileImage();
  }

  get userName() {
    return this.keycloakService.profile?.firstName;
  }

  getUserInitial(): string {
    return this.userName ? this.userName.charAt(0).toUpperCase() : 'U';
  }

  onSearch() {
    if (this.query.trim()) {
      this.bookSearchService.searchBooks({query: this.query});
    }
  }

  toggleDropdown(event: Event) {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown() {
    this.isDropdownOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.closeDropdown();
    }
  }

  async logout() {
    this.closeDropdown();
    await this.keycloakService.logout();
  }

  loadProfileImage(): void {
    const username = this.keycloakService.username || 'default-user';
    const savedImage = localStorage.getItem(`profileImage_${username}`);

    if (savedImage) {
      this.profileImageSrc = savedImage;
    } else {
      // Default image if none is found in localStorage
      this.profileImageSrc = '';
    }
  }
}
