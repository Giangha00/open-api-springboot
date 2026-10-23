import { Injectable, signal, computed } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';

export type UserRole = 'CUSTOMER' | 'ADMIN' | 'PARTNER';

export interface UserProfile {
  role: UserRole;
  name: string;
  email: string;
  customerId: number | null;
  token: string;
  partnerKey?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly defaultProfiles: Record<string, UserProfile> = {
    customer1: {
      role: 'CUSTOMER',
      name: 'Nguyễn Văn Tuấn (Khách hàng 1)',
      email: 'customer@techshop.vn',
      customerId: 1,
      token: 'dev-customer-token'
    },
    customer2: {
      role: 'CUSTOMER',
      name: 'Trần Thị Mai (Khách hàng 2)',
      email: 'mai.tran@techshop.vn',
      customerId: 2,
      token: 'dev-customer-token'
    },
    admin: {
      role: 'ADMIN',
      name: 'Quản trị viên (Admin)',
      email: 'admin@techshop.vn',
      customerId: null,
      token: 'dev-admin-token'
    },
    partner: {
      role: 'PARTNER',
      name: 'Giao Hàng Tiết Kiệm (Đối tác)',
      email: 'partner@shipping.vn',
      customerId: null,
      token: '',
      partnerKey: 'partner-secret-key-2026'
    }
  };

  private readonly activeProfileKeySignal = signal<string>('customer1');
  readonly activeProfile = computed(() => this.defaultProfiles[this.activeProfileKeySignal()]);

  selectProfile(key: 'customer1' | 'customer2' | 'admin' | 'partner'): void {
    this.activeProfileKeySignal.set(key);
  }

  getAuthHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    const profile = this.activeProfile();

    if (profile.role === 'PARTNER' && profile.partnerKey) {
      headers = headers.set('X-Partner-Key', profile.partnerKey);
    } else if (profile.token) {
      headers = headers.set('Authorization', `Bearer ${profile.token}`);
    }

    return headers;
  }
}
