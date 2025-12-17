import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SellerService {
  sellerActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }

  checkDocumentExists(numberDocument: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/sellers/exists?numberDocument=${numberDocument}`);
  }

  // Eliminar vendedor lógicamente por ID
  disableSellerById(sellerId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/sellers/disable/${sellerId}`, {});
  }

  // Activar vendedor por ID
  activateSellerById(sellerId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/sellers/activate/${sellerId}`, {});
  }

  // Editar vendedor por ID
  updateSellerById(sellerId: number, updatedSeller: any) {
    return this.http.put(`${environment.apiUrl}/api/v1/sellers/${sellerId}`, updatedSeller);
  }

  // Insertar vendedor
  newSeller(newSeller: any) {
    return this.http.post(`${environment.apiUrl}/api/v1/sellers`, newSeller);
  }

  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/active/pageable?page=${pag}&size=${tam}`);
  }

  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/inactive/pageable?page=${pag}&size=${tam}`);
  }

  // Listado de vendedores activos
  listActiveSellers(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/api/v1/sellers/active`);
  }

  // Listado de vendedores inactivos
  listInactiveSellers() {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/inactive`);
  }

  // Listado de vendedores activos por id
  getSellerById(sellerId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/${sellerId}`);
  }

  // REPORTES
  reportActivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/report`, { responseType: 'blob' });
  }

  reportInactivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/inactive/report`, { responseType: 'blob' });
  }

  reportActiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/report/excel`, { responseType: 'blob' });
  }

  reportInactiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/sellers/inactive/report/excel`, { responseType: 'blob' });
  }

  // Cambio de contraseña
  // En SellerService
  changePassword(sellerId: number, passwords: { oldPassword: string, newPassword: string }) {
    return this.http.put(`${environment.apiUrl}/api/v1/sellers/${sellerId}/change-password`, passwords);
  }

}
