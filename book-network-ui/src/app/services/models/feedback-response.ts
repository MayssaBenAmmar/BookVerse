/* tslint:disable */
/* eslint-disable */
export interface FeedbackResponse {
  comment?: string;
  note?: number;
  ownFeedback?: boolean;
  userId?: number;     // Added for user identification
  user?: {             // Added user object for profile information
    id?: number;
    username?: string;
    firstName?: string;
    lastName?: string;
    profileImageUrl?: string;
  };
}
