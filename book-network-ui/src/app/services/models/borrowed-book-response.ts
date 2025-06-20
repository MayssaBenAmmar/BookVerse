/* tslint:disable */
/* eslint-disable */
export interface BorrowedBookResponse {
  authorName?: string;
  id?: number;
  isbn?: string;
  rate?: number;
  returnApproved?: boolean;
  returned?: boolean;
  title?: string;
  // NEW: Added date fields for tracking
  borrowedDate?: string;
  returnedDate?: string;
}
