// Create this file at src/app/models/user-profile-dto.ts
export interface UserProfileDto {
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  id?: string;
  profilePictureUrl?: string;
  booksCount?: number;
  sharedBooksCount?: number;
}
