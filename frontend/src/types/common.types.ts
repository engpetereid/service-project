export interface ApiError {
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp?: string;
  fieldErrors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
