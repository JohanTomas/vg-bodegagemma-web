import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { ProductService } from '../../core/services/product.service';
import { MatDialog } from '@angular/material/dialog';
import { ProductModalComponent } from './product-modal/product-modal.component';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

@Component({
  selector: 'app-product',
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.scss']
})
export class ProductComponent implements OnInit {
  public displayedColumns: string[] = ['code', 'name', 'description', 'categoryProduct', 'priceUnit', 'unitSale', 'dateExpiryFormatted', 'stock', 'editar-eliminar'];
  public dataSourceP: MatTableDataSource<any>;
  public products: any[] = [];
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  @ViewChild(MatSort) sort!: MatSort;
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveProducts: boolean = false;

  constructor(private productService: ProductService, private dialog: MatDialog) {
    this.dataSourceP = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.dataSourceP.filterPredicate = (data, filter: string) => {
      const transformedFilter = filter.trim().toLowerCase();
      const matchesCode = data.code.toLowerCase().includes(transformedFilter);
      const matchesName = data.name.toLowerCase().includes(transformedFilter);
      const matchesPrice = data.priceUnit.toString().toLowerCase().includes(transformedFilter);
      const matchesUnidadSale = data.unitSale.toLowerCase().includes(transformedFilter);
      const matchesStock = data.stock.toString().toLowerCase().includes(transformedFilter);
      const matchesCategory = data.categoryProduct.name.toLowerCase().includes(transformedFilter);
      return matchesCode || matchesName || matchesPrice || matchesUnidadSale || matchesStock || matchesCategory;
    };

    this.productService.productActualizar.subscribe(() => {
      this.getProducts();
      this.dataSourceP.sort = this.sort;
    });
    this.getProducts();
  }

  getProducts() {
    this.productService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.products = res.content;
        this.dataSourceP.data = this.products;
        this.dataSourceP.sort = this.sort;
        this.showingInactiveProducts = false; // Establece showingInactiveProducts a false
      }
    );
  }

  getProductsInactive() {
    this.productService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.products = res.content;
        this.dataSourceP.data = this.products;
        this.showingInactiveProducts = true; // Establece showingInactiveProducts a true
      }
    );
  }

  Paginator(e: any) {
    if (this.showingInactiveProducts) {
      this.productService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.products = res.content;
          this.dataSourceP.data = this.products;
        }
      );
    } else {
      this.productService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.products = res.content;
          this.dataSourceP.data = this.products;
        }
      );
    }
  }

  toggleButtons() {
    this.showingInactiveProducts = !this.showingInactiveProducts;
    if (this.showingInactiveProducts) {
      this.getProductsInactive();
    } else {
      this.getProducts();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar el producto?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su producto ha sido eliminado.",
          icon: "success"
        });
        this.fnEliminar(Id);
      }
    });
  }

  fnEliminar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.productService.disableProductById(Id).subscribe(() => {
      this.productService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.products = res.content;
        this.dataSourceP.data = this.products;

        if (this.products.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar al producto?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su producto ha sido restaurado.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.productService.activateProductById(Id).subscribe(() => {
      this.productService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.products = res.content;
        this.dataSourceP.data = this.products;

        if (this.products.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  openmodal(producto?: any) {
    this.dialog.open(ProductModalComponent, {
      disableClose: true,
      width: '40rem',
      height: 'auto',
      data: producto,
    });
  }

  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceP.filter = valor.trim().toLowerCase();
  }

  // Método para exportar productos a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["Codigo", "Nombre", "Descripcion", "Categoría", "Precio Unitario", "Unidad de Venta", "Fecha de Expiración", "Stock"];
    const rows = this.dataSourceP.data.map(product => [
      product.code, product.name, product.description, product.categoryProduct.name, product.priceUnit, product.unitSale, product.dateExpiryFormatted, product.stock
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_productos.pdf');
  }

  // Método para exportar productos a Excel
  exportExcel() {
    const filteredData = this.dataSourceP.data.map(({ code, name, description, categoryProduct, priceUnit, unitSale, dateExpiryFormatted, stock }) => ({
      'Codigo': code,
      'Nombre': name,
      'Descripcion': description,
      'Categoría': categoryProduct.name,
      'Precio Unitario': priceUnit,
      'Unidad de Venta': unitSale,
      'Fecha de Expiración': dateExpiryFormatted,
      'Stock': stock
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Productos');
    XLSX.writeFile(wb, 'Productos.xlsx');
  }

  // Método para exportar productos a CSV
  exportCSV() {
    const filteredData = this.dataSourceP.data.map(({ code, name, description, categoryProduct, priceUnit, unitSale, dateExpiryFormatted, stock }) => ({
      'Codigo': code,
      'Nombre': name,
      'Descripcion': description,
      'Categoría': categoryProduct.name,
      'Precio Unitario': priceUnit,
      'Unidad de Venta': unitSale,
      'Fecha de Expiración': dateExpiryFormatted,
      'Stock': stock
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Productos.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  listStock() {
    this.productService.listStock().subscribe(
      (res: any) => {
        this.products = res;
        this.dataSourceP.data = this.products;
        this.showingInactiveProducts = false;
      },
      (error: any) => {
        console.error('Error listing products by stock:', error);
      }
    );
  }

  listExpiracion() {
    this.productService.listExpiracion().subscribe(
      (res: any) => {
        this.products = res;
        this.dataSourceP.data = this.products;
        this.showingInactiveProducts = false;
      },
      (error: any) => {
        console.error('Error listing products by expiracion:', error);
      }
    );
  }
}