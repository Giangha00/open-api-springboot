export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  timestamp?: string;
  errors?: Array<{ field: string; message: string }>;
  [key: string]: any;
}
