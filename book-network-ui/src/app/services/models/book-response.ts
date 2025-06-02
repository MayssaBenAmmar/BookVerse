/* tslint:disable */
/* eslint-disable */
export interface BookResponse {
  archived?: boolean;
  authorName?: string;
  cover?: Array<string>;
  id?: number;
  isbn?: string;
  owner?: string;
  rate?: number;
  shareable?: boolean;
  synopsis?: string;
  title?: string;
  genre?: string; // Add this line
}

export interface PageResponse<T> {
  content?: T[];
  pageable?: Pageable;
  totalPages?: number;
  totalElements?: number;
  last?: boolean;
  size?: number;
  number?: number;
  sort?: Sort;
  numberOfElements?: number;
  first?: boolean;
  empty?: boolean;
}

interface Pageable {
  sort?: Sort;
  offset?: number;
  pageNumber?: number;
  pageSize?: number;
  paged?: boolean;
  unpaged?: boolean;
}

interface Sort {
  empty?: boolean;
  sorted?: boolean;
  unsorted?: boolean;
}
