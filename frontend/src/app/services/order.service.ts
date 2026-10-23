import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateOrderRequest,
  DailyRevenueReportResponse,
  OrderEventResponse,
  OrderResponseV1,
  OrderResponseV2,
  OrderStatus,
  OrderTransitionRequest,
  OrderTransitionResponse,
  ShippingWebhookRequest
} from '../models/order.model';
import { PageResponse } from '../models/page-response.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseApiUrl = 'http://localhost:8080/api';

  createOrder(request: CreateOrderRequest, idempotencyKey: string): Observable<OrderResponseV2> {
    let headers = this.authService.getAuthHeaders();
    if (idempotencyKey) {
      headers = headers.set('Idempotency-Key', idempotencyKey);
    }
    return this.http.post<OrderResponseV2>(`${this.baseApiUrl}/orders`, request, { headers });
  }

  getOrders(
    status?: OrderStatus,
    from?: string,
    to?: string,
    minTotal?: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc'
  ): Observable<PageResponse<OrderResponseV2>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (status) params = params.set('status', status);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    if (minTotal != null) params = params.set('minTotal', minTotal.toString());

    return this.http.get<PageResponse<OrderResponseV2>>(`${this.baseApiUrl}/orders`, {
      headers: this.authService.getAuthHeaders(),
      params
    });
  }

  getOrderByCodeV1(code: string): Observable<OrderResponseV1> {
    return this.http.get<OrderResponseV1>(`${this.baseApiUrl}/orders/${code}`, {
      headers: this.authService.getAuthHeaders()
    });
  }

  getOrderByCodeV2(code: string): Observable<OrderResponseV2> {
    return this.http.get<OrderResponseV2>(`${this.baseApiUrl}/v2/orders/${code}`, {
      headers: this.authService.getAuthHeaders()
    });
  }

  transitionOrder(code: string, request: OrderTransitionRequest, ifMatch: string | number): Observable<OrderTransitionResponse> {
    const cleanETag = `"${ifMatch}"`;
    const headers = this.authService.getAuthHeaders().set('If-Match', cleanETag);
    return this.http.post<OrderTransitionResponse>(`${this.baseApiUrl}/orders/${code}/transitions`, request, { headers });
  }

  adminTransitionOrder(code: string, request: OrderTransitionRequest, ifMatch: string | number): Observable<OrderTransitionResponse> {
    const cleanETag = `"${ifMatch}"`;
    const headers = this.authService.getAuthHeaders().set('If-Match', cleanETag);
    return this.http.post<OrderTransitionResponse>(`${this.baseApiUrl}/admin/orders/${code}/transitions`, request, { headers });
  }

  getOrderEvents(code: string): Observable<OrderEventResponse[]> {
    return this.http.get<OrderEventResponse[]>(`${this.baseApiUrl}/orders/${code}/events`, {
      headers: this.authService.getAuthHeaders()
    });
  }

  uploadInvoice(code: string, file: File): Observable<{ code: string; message: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ code: string; message: string }>(`${this.baseApiUrl}/orders/${code}/invoice`, formData, {
      headers: this.authService.getAuthHeaders()
    });
  }

  downloadInvoice(code: string): Observable<Blob> {
    return this.http.get(`${this.baseApiUrl}/orders/${code}/invoice`, {
      headers: this.authService.getAuthHeaders(),
      responseType: 'blob'
    });
  }

  getDailyRevenue(from?: string, to?: string): Observable<DailyRevenueReportResponse[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.get<DailyRevenueReportResponse[]>(`${this.baseApiUrl}/admin/reports/daily-revenue`, {
      headers: this.authService.getAuthHeaders(),
      params
    });
  }

  sendShippingWebhook(request: ShippingWebhookRequest): Observable<OrderTransitionResponse> {
    return this.http.post<OrderTransitionResponse>(`${this.baseApiUrl}/webhooks/shipping`, request, {
      headers: this.authService.getAuthHeaders()
    });
  }
}
