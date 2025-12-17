import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentMethodService {
  paymentMethodActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }
  //Listado de clientes activos
  listActivePaymentMethod() {
    return this.http.get(`${environment.apiUrl}/api/v1/paymentMethod/active`);
  }
  //Listado de cliebtes inactivos
  listInactivePaymentMethod() {
    return this.http.get(`${environment.apiUrl}/api/v1/paymentMethod/inactive`);
  }
}
