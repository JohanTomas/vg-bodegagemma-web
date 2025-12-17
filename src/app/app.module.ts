import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { LayoutComponent } from './layout/layout.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { DashboardComponent } from './domain/dashboard/dashboard.component';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule } from '@angular/material/menu';
import { ProductComponent } from './domain/product/product.component';
import { CategoryComponent } from './domain/category/category.component';
import { ClientComponent } from './domain/client/client.component';
import { SellerComponent } from './domain/seller/seller.component';
import { MatTableModule } from '@angular/material/table';
import {MatInputModule} from '@angular/material/input';
import {MatFormFieldModule} from '@angular/material/form-field';
import { HttpClientModule } from '@angular/common/http';
import {MatSelectModule} from '@angular/material/select';
import { ClientModalComponent } from './domain/client/client-modal/client-modal.component';
import {MatPaginatorIntl, MatPaginatorModule} from '@angular/material/paginator';
import { MatPaginatorEsp } from './domain/resource/mat-paginator';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductModalComponent } from './domain/product/product-modal/product-modal.component'; 
import { MatSortModule } from '@angular/material/sort';
import {MatDatepickerModule} from '@angular/material/datepicker';
import { MatDialogActions, MatDialogContent } from '@angular/material/dialog';
import { MatNativeDateModule } from '@angular/material/core';
import { SupplierComponent } from './domain/supplier/supplier.component';
import { CategoryModalComponent } from './domain/category/category-modal/category-modal.component';
import { SellerModalComponent } from './domain/seller/seller-modal/seller-modal.component';
import { SupplierModalComponent } from './domain/supplier/supplier-modal/supplier-modal.component';
import { SaleComponent } from './domain/sale/sale.component';
import { PurchaseComponent } from './domain/purchase/purchase.component';
import { ReservationComponent } from './domain/reservation/reservation.component';
import { ReservationModalComponent } from './domain/reservation/reservation-modal/reservation-modal.component';
import { SaleModalComponent } from './domain/sale/sale-modal/sale-modal.component';
import { PurchaseModalComponent } from './domain/purchase/purchase-modal/purchase-modal.component';
import { LoginComponent } from './login/login.component';
import { AccountComponent } from './account/account.component';
import { AccountModalComponent } from './account/account-modal/account-modal.component';
import { AccountPasswordComponent } from './account/account-password/account-password.component';
import { MatAutocompleteModule } from '@angular/material/autocomplete';

@NgModule({
  declarations: [
    AppComponent,
    LayoutComponent,
    DashboardComponent,
    ProductComponent,
    ClientComponent,
    SellerComponent,
    CategoryComponent,
    ClientModalComponent,
    ProductModalComponent,
    SupplierComponent,
    CategoryModalComponent,
    SellerModalComponent,
    SupplierModalComponent,
    SaleComponent,
    PurchaseComponent,
    ReservationComponent,
    SaleModalComponent,
    PurchaseModalComponent,
    ReservationModalComponent,
    LoginComponent,
    AccountComponent,
    AccountModalComponent,
    AccountPasswordComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    MatListModule,
    MatGridListModule,
    MatCardModule,
    MatMenuModule,
    MatTableModule,
    MatInputModule,
    MatFormFieldModule,
    FormsModule,
    MatSelectModule,
    HttpClientModule,
    MatPaginatorModule,
    ReactiveFormsModule,
    MatSortModule,
    MatDatepickerModule,
    CommonModule,
    ReactiveFormsModule,
    MatDialogActions,
    MatDatepickerModule,
    MatNativeDateModule,
    MatAutocompleteModule
  ],
  providers: [
    provideAnimationsAsync(),
    {provide: MatPaginatorIntl, useClass: MatPaginatorEsp}
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
