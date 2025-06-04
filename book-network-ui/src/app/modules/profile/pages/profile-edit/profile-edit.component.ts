import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';


@Component({
  selector: 'app-profile-edit',
  templateUrl: './profile-edit.component.html',
  styleUrls: ['./profile-edit.component.scss']
})
export class ProfileEditComponent implements OnInit {
  profileForm!: FormGroup;
  loading = false;
  saving = false;
  error: string | null = null;
  success = false;

  constructor(
    private formBuilder: FormBuilder,
    private keycloakService: KeycloakService,
    private router: Router
  ) { }

  ngOnInit() {
    this.initForm();
    this.loadProfile();
  }

  initForm() {
    this.profileForm = this.formBuilder.group({
      username: [{ value: '', disabled: true }],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]]
    });
  }

  loadProfile() {
    const profile = this.keycloakService.profile;
    if (profile) {
      this.profileForm.patchValue({
        username: profile.username,
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        email: profile.email || ''
      });
    }
  }

  async onSubmit() {
    if (this.profileForm.invalid) {
      return;
    }

    this.saving = true;
    this.error = null;
    this.success = false;

    // Since we can't update profile directly in our app, show message and redirect to Keycloak
    this.error = "Profile updates must be done through Keycloak account management. Redirecting...";

    setTimeout(() => {
      window.location.href = `${this.keycloakService.keycloakInstance.authServerUrl}/realms/${this.keycloakService.keycloakInstance.realm}/account`;
    }, 2000);
  }

  cancel() {
    this.router.navigate(['/profile']);
  }
}
