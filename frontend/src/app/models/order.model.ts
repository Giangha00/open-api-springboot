export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURN_REQUESTED'
  | 'RETURNED';

export type OrderAction =
  | 'CONFIRM'
  | 'SHIP'
  | 'DELIVER'
  | 'CANCEL'
  | 'FAIL_DELIVERY'
  | 'REQUEST_RETURN'
  | 'ACCEPT_RETURN';

export type PaymentMethod = 'COD' | 'CARD' | 'BANK_TRANSFER';

export interface CodPaymentRequest {
  method: 'COD';
}

export interface CardPaymentRequest {
  method: 'CARD';
  provider: string;
  last4: string;
}

export interface BankTransferPaymentRequest {
  method: 'BANK_TRANSFER';
  bankCode: string;
  transferRef: string;
}

export type PaymentRequest =
  | CodPaymentRequest
  | CardPaymentRequest
  | BankTransferPaymentRequest;

export interface PaymentResponse {
  method: PaymentMethod;
  amount: number;
  provider?: string;
  last4?: string;
  bankCode?: string;
  transferRef?: string;
  paidAt?: string;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface CreateOrderRequest {
  shippingAddress: string;
  items: OrderItemRequest[];
  payment: PaymentRequest;
}

export interface TotalAmountDto {
  value: number;
  currency: string;
}

export interface OrderResponseV1 {
  code: string;
  customerId: number;
  status: OrderStatus;
  shippingAddress: string;
  totalAmount: number;
  currency: string;
  version: number;
  createdAt: string;
  deliveredAt?: string;
  items: OrderItemResponse[];
  payment: PaymentResponse;
  hasInvoice: boolean;
}

export interface OrderResponseV2 {
  code: string;
  customerId: number;
  status: OrderStatus;
  shippingAddress: string;
  total: TotalAmountDto;
  version: number;
  createdAt: string;
  deliveredAt?: string;
  items: OrderItemResponse[];
  payment: PaymentResponse;
  hasInvoice: boolean;
}

export interface OrderTransitionRequest {
  action: OrderAction;
  reason?: string;
}

export interface OrderTransitionResponse {
  code: string;
  fromStatus: OrderStatus;
  toStatus: OrderStatus;
  action: OrderAction;
  version: number;
  message: string;
}

export interface OrderEventResponse {
  id: number;
  orderCode: string;
  fromStatus?: OrderStatus;
  toStatus: OrderStatus;
  action: OrderAction;
  actor: string;
  reason?: string;
  at: string;
}

export interface DailyRevenueReportResponse {
  date: string;
  totalOrders: number;
  totalRevenue: number;
  currency: string;
}

export interface ShippingWebhookRequest {
  code: string;
  action: OrderAction;
  notes?: string;
}
