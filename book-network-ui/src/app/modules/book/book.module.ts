import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http'; // ADD THIS for DashboardService

import { BookRoutingModule } from './book-routing.module';
import { MainComponent } from './pages/main/main.component';
import { MenuComponent } from './components/menu/menu.component';
import { BookListComponent } from './pages/book-list/book-list.component';
import { BookCardComponent } from './components/book-card/book-card.component';
import { MyBooksComponent } from './pages/my-books/my-books.component';
import { ManageBookComponent } from './pages/manage-book/manage-book.component';
import { BorrowedBookListComponent } from './pages/borrowed-book-list/borrowed-book-list.component';
import { RatingComponent } from './components/rating/rating.component';
import { ReturnedBooksComponent } from './pages/returned-books/returned-books.component';
import { BookDetailsComponent } from './pages/book-details/book-details.component';
import { ArchivedBooksComponent } from './pages/archived-books/archived-books.component';
import { FavoritesComponent } from './pages/favorites/favorites.component';
import { FormChatComponent } from './components/Chat/form-chat/form-chat.component';
import { FeaturedBooksComponent } from './components/featured-books/featured-books.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';

// Angular Material modules
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@NgModule({
  declarations: [
    MainComponent,
    MenuComponent,
    BookListComponent,
    BookCardComponent,
    MyBooksComponent,
    ManageBookComponent,
    BorrowedBookListComponent,
    RatingComponent,
    ReturnedBooksComponent,
    BookDetailsComponent,
    ArchivedBooksComponent,
    FormChatComponent,
    FeaturedBooksComponent,
    DashboardComponent,
    FavoritesComponent
  ],
  exports: [
    BookCardComponent,
    RatingComponent
  ],
  imports: [
    CommonModule,
    FormsModule, // Required for ngModel
    RouterModule, // Required for router-outlet and routerLink
    HttpClientModule, // ADD THIS - Required for DashboardService HTTP calls
    BookRoutingModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ]
})
export class BookModule { }
