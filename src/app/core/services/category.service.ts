import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  categoryActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }
  checkNameExists(name: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/categories/exists?name=${name}`);
  }
  // Eliminar categoria lógicamente por ID
  disableCategoryById(categoryId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/categories/disable/${categoryId}`, {});
  }
  //Activar categoria por ID
  activateCategorytById(categoryId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/categories/activate/${categoryId}`, {});
  }
  // Editar categoria por ID
  updateCategoryById(categoryId: number, updatedCategoryt: any) {
    return this.http.put(`${environment.apiUrl}/api/v1/categories/${categoryId}`, updatedCategoryt);
  }
  // Insertar categoria
  newCategory(newCategory: any) {
    return this.http.post(`${environment.apiUrl}/api/v1/categories`, newCategory);
  }
  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/active/pageable?page=${pag}&size=${tam}`);
  }
  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/inactive/pageable?page=${pag}&size=${tam}`);
  }
  //Listado de categoria activos
  listActiveCategories() {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/active`);
  }
  //Listado de categorias inactivos
  listInactiveCategory() {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/inactive`);
  }
  //listar categorias activas
  activateCategory() {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/active`);
  }
  //REPORTES
  reportActivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/categories/report`, { responseType: 'blob' });
  }
// Método para exportar categorías inactivas a PDF
reportInactivePDF() {
  return this.http.get(`${environment.apiUrl}/api/v1/categories/inactive/report`, { responseType: 'blob' });
}

// Método para exportar categorías activas a Excel
reportActiveExcel() {
  return this.http.get(`${environment.apiUrl}/api/v1/categories/report/excel`, { responseType: 'blob' });
}

// Método para exportar categorías inactivas a Excel
reportInactiveExcel() {
  return this.http.get(`${environment.apiUrl}/api/v1/categories/inactive/report/excel`, { responseType: 'blob' });
}

}