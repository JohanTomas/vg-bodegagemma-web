import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  productActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }

  CheckCodeExists(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/products/byCode/${code}`);
  }

  CheckNameExists(name: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/products/exists?name=${name}`);
  }
  // Eliminar producto lógicamente por ID
  disableProductById(productId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/products/disable/${productId}`, {});
  }
  //Activar producto por ID
  activateProductById(productId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/products/activate/${productId}`, {});
  }
  // Editar producto por ID
  updateProductById(productId: number, updatedProductt: any) {
    return this.http.put(`${environment.apiUrl}/api/v1/products/${productId}`, updatedProductt);
  }
  // Insertar producto
  newProduct(newProduct: any) {
    return this.http.post(`${environment.apiUrl}/api/v1/products`, newProduct);
  }
  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/products/active/pageable?page=${pag}&size=${tam}`);
  }
  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/products/inactive/pageable?page=${pag}&size=${tam}`);
  }
  //Listado de clientes activos
  listActiveProducts() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/active`);
  }
  //Listado de cliebtes inactivos
  listInactiveProducts() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/inactive`);
  }
  //Listado de stock
  listStock() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/lowstock`);
  }
  //Listado de expiracion
  listExpiracion() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/expiring`);
  }

  //REPORTES
  reportActivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/report`, { responseType: 'blob' });
  }
  reportInactivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/inactive/report`, { responseType: 'blob' });
  }
  reportActiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/report/excel`, { responseType: 'blob' });
  }
  reportInactiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/products/inactive/report/excel`, { responseType: 'blob' });
  }

}