import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {MainComponent} from './pages/main/main.component';
import {BookListComponent} from './pages/book-list/book-list.component';
import {MyBooksComponent} from './pages/my-books/my-books.component';
import {ManageBookComponent} from './pages/manage-book/manage-book.component';
import {BorrowedBookListComponent} from './pages/borrowed-book-list/borrowed-book-list.component';
import {ReturnedBooksComponent} from './pages/returned-books/returned-books.component';
import {authGuard} from '../../services/guard/auth.guard';
import {BookDetailsComponent} from './pages/book-details/book-details.component';
import {ArchivedBooksComponent} from "./pages/archived-books/archived-books.component";
import {FormChatComponent} from "./components/Chat/form-chat/form-chat.component";
import {FavoritesComponent} from "./pages/favorites/favorites.component";
import {FeaturedBooksComponent} from "src/app/modules/book/components/featured-books/featured-books.component";
import {DashboardComponent} from "./pages/dashboard/dashboard.component"; // ADD THIS

const routes: Routes = [
  {
    path: '',
    component: MainComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        component: BookListComponent,
        canActivate: [authGuard]
      },
      {
        path: 'featured',
        component: FeaturedBooksComponent
      },
      {
        path: 'dashboard', // ADD THIS ROUTE
        component: DashboardComponent,
        canActivate: [authGuard]
      },
      {
        path: 'my-books',
        component: MyBooksComponent,
        canActivate: [authGuard]
      },
      {
        path: 'favorites',
        component: FavoritesComponent
      },
      {
        path: 'my-borrowed-books',
        component: BorrowedBookListComponent,
        canActivate: [authGuard]
      },
      {
        path: 'my-returned-books',
        component: ReturnedBooksComponent,
        canActivate: [authGuard]
      },
      {
        path: 'archived-books',
        component: ArchivedBooksComponent,
        canActivate: [authGuard]
      },
      {
        path: 'details/:bookId',
        component: BookDetailsComponent,
        canActivate: [authGuard]
      },
      {
        path: 'manage',
        component: ManageBookComponent,
        canActivate: [authGuard]
      },
      {
        path: 'chat',
        component: FormChatComponent,
        canActivate: [authGuard]
      },
      {
        path: 'manage/:bookId',
        component: ManageBookComponent,
        canActivate: [authGuard]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BookRoutingModule {
}
