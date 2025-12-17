import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DataServiceService {

  constructor(private http: HttpClient) { }

  getDashboardData() {
    return forkJoin({
      sales: this.http.get(`${environment.apiUrl}/api/sales/status/A/page`),
      purchases: this.http.get(`${environment.apiUrl}/api/purchases/status/A/page`),
      clients: this.http.get(`${environment.apiUrl}/api/v1/clients/active/pageable`),
      products: this.http.get(`${environment.apiUrl}/api/v1/products/active/pageable`),
      categories: this.http.get(`${environment.apiUrl}/api/v1/categories/active/pageable`),
      suppliers: this.http.get(`${environment.apiUrl}/api/v1/suppliers/active/pageable`),
      sellers: this.http.get(`${environment.apiUrl}/api/v1/sellers/active/pageable`),
      reservations: this.http.get(`${environment.apiUrl}/api/reservations/status/A/page`) // Añade las reservas
    });
  }
}