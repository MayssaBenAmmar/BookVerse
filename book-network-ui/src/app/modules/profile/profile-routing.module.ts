import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProfileComponent } from './pages/profile/profile.component';
import { ProfileEditComponent } from './pages/profile-edit/profile-edit.component';
import { authGuard } from '../../services/guard/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: ProfileComponent,
    canActivate: [authGuard],
    data: { title: 'My Profile' }
  },
  {
    path: 'edit',
    component: ProfileEditComponent,
    canActivate: [authGuard],
    data: { title: 'Edit Profile' }
  },
  {
    // ✅ CORRECTION : Simplifier le routing
    path: ':username',
    component: ProfileComponent,
    canActivate: [authGuard],
    data: {
      title: 'User Profile',
      publicRoute: true
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProfileRoutingModule { }
