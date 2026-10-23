import { Routes } from '@angular/router';
import { ProductListComponent } from './pages/products/product-list.component';
import { OrderCreateComponent } from './pages/orders/order-create.component';
import { OrderListComponent } from './pages/orders/order-list.component';
import { OrderDetailComponent } from './pages/orders/order-detail.component';
import { AdminOrdersComponent } from './pages/admin/admin-orders.component';
import { AdminReportsComponent } from './pages/admin/admin-reports.component';
import { ShippingSimulatorComponent } from './pages/partner/shipping-simulator.component';
import { SwaggerViewerComponent } from './pages/swagger/swagger-viewer.component';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'products', component: ProductListComponent, title: 'TechShop - Danh Mục Sản Phẩm' },
  { path: 'orders/create', component: OrderCreateComponent, title: 'TechShop - Đặt Hàng Mới' },
  { path: 'orders', component: OrderListComponent, title: 'TechShop - Danh Sách Đơn Hàng' },
  { path: 'orders/:code', component: OrderDetailComponent, title: 'TechShop - Chi Tiết Đơn Hàng' },
  { path: 'admin/orders', component: AdminOrdersComponent, title: 'TechShop - Quản Lý Đơn Hàng Admin' },
  { path: 'admin/reports', component: AdminReportsComponent, title: 'TechShop - Báo Cáo Doanh Thu' },
  { path: 'partner/shipping', component: ShippingSimulatorComponent, title: 'TechShop - Mô Phỏng Giao Vận' },
  { path: 'swagger', component: SwaggerViewerComponent, title: 'TechShop - Tài Liệu Swagger UI' },
  { path: '**', redirectTo: 'products' }
];
