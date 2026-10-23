export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
}
