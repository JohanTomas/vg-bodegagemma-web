import { SaleModalComponent } from './sale-modal/sale-modal.component';
import { SaleService } from './../../core/services/sale.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';


@Component({
  selector: 'app-sale',
  templateUrl: './sale.component.html',
  styleUrl: './sale.component.scss'
})
export class SaleComponent implements OnInit {
  public displayedColumns: string[] = ['id', 'client', 'seller', 'paymentMethod', 'date', 'total', 'editar-eliminar'];
  public dataSourceS: MatTableDataSource<any>;
  public sales: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveSales: boolean = false;

  constructor(private saleService: SaleService, private dialog: MatDialog) {
    this.dataSourceS = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.saleService.saleActualizar.subscribe(() => {
      this.getSales();
      this.dataSourceS.sort = this.sort;
    });
    this.getSales();
  }

  // Lista de ventas activas con paginador
  getSales() {
    this.saleService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.sales = res.content;
        this.dataSourceS.data = this.sales;
        this.dataSourceS.sort = this.sort;
      }
    );
  }

  // Lista de ventas inactivas con paginador
  getSalesInactive() {
    this.saleService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.sales = res.content;
        this.dataSourceS.data = this.sales;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactiveSales) {
      this.saleService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.sales = res.content;
          this.dataSourceS.data = this.sales;
        }
      );
    } else {
      this.saleService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.sales = res.content;
          this.dataSourceS.data = this.sales;
        }
      );
    }
  }

  // Cambiar botones y listar ventas activas e inactivas
  toggleButtons() {
    this.showingInactiveSales = !this.showingInactiveSales;
    if (this.showingInactiveSales) {
      this.getSalesInactive();
    } else {
      this.getSales();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de una venta
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar esta venta?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su venta ha sido eliminada.",
          icon: "success"
        });
        this.fnEliminar(Id);
      }
    });
  }

  // Función Eliminar
  fnEliminar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.saleService.disableSaleById(Id).subscribe(() => {
      this.saleService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.sales = res.content;
        this.dataSourceS.data = this.sales;

        if (this.sales.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar venta
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar esta venta?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su venta ha sido restaurada.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar venta
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.saleService.activateSaleById(Id).subscribe(() => {
      this.saleService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.sales = res.content;
        this.dataSourceS.data = this.sales;

        if (this.sales.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(sale?: any) {
    this.dialog.open(SaleModalComponent, {
      disableClose: true,
      width: '1200px',
      height: 'auto',
      data: sale,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceS.filter = valor.trim().toLowerCase();
  }

  // Método para exportar ventas a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["ID", "Cliente", "Vendedor", "Método de Pago", "Fecha", "Total"];
    const rows = this.dataSourceS.data.map(sale => [
      sale.id, sale.clientNames, sale.sellerNames, sale.paymentMethod.name, sale.formattedDateTime, sale.totalSale
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_ventas.pdf');
  }

  // Método para exportar ventas a Excel
  exportExcel() {
    const filteredData = this.dataSourceS.data.map(({ id, clientNames, sellerNames, paymentMethod, formattedDateTime, totalSale }) => ({
      'ID': id,
      'Cliente': clientNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalSale
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Ventas');
    XLSX.writeFile(wb, 'Ventas.xlsx');
  }

  // Método para exportar ventas a CSV
  exportCSV() {
    const filteredData = this.dataSourceS.data.map(({ id, clientNames, sellerNames, paymentMethod, formattedDateTime, totalSale }) => ({
      'ID': id,
      'Cliente': clientNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalSale
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Ventas.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  verDetalle(Id: number) {
    this.saleService.getSaleById(Id).subscribe((sale: any) => {
      // Calcular los subtotales
      sale.saleDetails.forEach((detail: any) => {
        detail.subtotal = this.calculateItemTotal(detail); // Usar la función calculateItemTotal para calcular el subtotal
      });
  
      Swal.fire({
        title: '<strong>Detalle de la Venta</strong>',
        html: `
          <div id="sale-detail" style="text-align: left; font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #f9f9f9;">
            <h3 style="text-align: center; margin-bottom: 20px;">Boleta Electrónica</h3>
            <p><strong>ID:</strong> ${sale.id}</p>
            <p><strong>Cliente:</strong> ${sale.clientNames}</p>
            <p><strong>Vendedor:</strong> ${sale.sellerNames}</p>
            <p><strong>Método de Pago:</strong> ${sale.paymentMethod.name}</p>
            <p><strong>Fecha:</strong> ${sale.formattedDateTime}</p>
            <p><strong>Total:</strong> S/.${sale.totalSale}</p>
            <hr style="margin: 20px 0;">
            <h4>Productos</h4>
            <table style="width: 100%; border-collapse: collapse;">
              <thead>
                <tr>
                  <th style="border: 1px solid #ddd; padding: 8px;">Producto</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Precio</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Cantidad</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Unidad de Venta</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                ${sale.saleDetails.map((detail: { product: { name: any; priceUnit: any; unitSale: any; }; amount: any; subtotal: any; }) => `
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.product.name}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">S/.${detail.product.priceUnit.toFixed(2)}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.amount}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.product.unitSale}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">S/.${detail.subtotal.toFixed(2)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
            <hr style="margin: 20px 0;">
            <p style="text-align: center; font-size: 12px; color: #888;">Gracias por su compra</p>
          </div>
        `,
        icon: 'info',
        showCancelButton: true,
        confirmButtonText: 'Cerrar',
        cancelButtonText: 'Descargar PDF',
        showDenyButton: true,
        denyButtonText: 'Descargar Excel',
        customClass: {
          popup: 'swal-wide',
          title: 'swal-title',
          confirmButton: 'swal-button',
          cancelButton: 'swal-button',
          denyButton: 'swal-button'
        },
        width: '800px' // Aumentar el ancho del modal
      }).then((result) => {
        if (result.isDismissed && result.dismiss === Swal.DismissReason.cancel) {
          this.downloadPDF(sale);
        } else if (result.isDenied) {
          this.downloadExcel(sale);
        }
      });
    });
  }

// Función para calcular el subtotal de un producto
calculateItemTotal(detail: any): number {
  const product = detail.product;
  const amount = detail.amount;
  const total = (product?.priceUnit || 0) * (amount || 0);
  return Number(total.toFixed(2)); // Redondear a dos decimales
}

// Descargar detalle en PDF
downloadPDF(sale: any) {
  const doc = new jsPDF();
  doc.setFontSize(18);
  doc.setTextColor(40, 40, 40);
  doc.text('Boleta Electrónica', 105, 20, { align: 'center' });

  doc.setFontSize(12);
  doc.setTextColor(0, 102, 204);
  doc.text('Nombre del Cliente:', 20, 40);
  doc.setTextColor(0, 0, 0);
  doc.text(`${sale.clientNames}`, 70, 40);

  doc.setTextColor(0, 102, 204);
  doc.text('Nombre del Vendedor:', 20, 50);
  doc.setTextColor(0, 0, 0);
  doc.text(`${sale.sellerNames}`, 70, 50);

  doc.setTextColor(0, 102, 204);
  doc.text('Método de Pago:', 20, 60);
  doc.setTextColor(0, 0, 0);
  doc.text(`${sale.paymentMethod.name}`, 70, 60);

  // Mover la fecha a la parte superior derecha
  doc.setTextColor(0, 0, 0);
  doc.text(`Fecha: ${sale.formattedDateTime}`, 200, 30, { align: 'right' });

  doc.setFontSize(14);
  doc.setTextColor(40, 40, 40);
  doc.text('Detalle de la Venta', 105, 90, { align: 'center' });

  const columns = ["Producto", "Precio", "Cantidad", "Unidad de Venta", "Subtotal"];
  const rows = sale.saleDetails.map((detail: any) => [
    detail.product.name,
    `S/.${detail.product.priceUnit.toFixed(2)}`,
    detail.amount,
    detail.product.unitSale,
    `S/.${detail.subtotal.toFixed(2)}`
  ]);

  (doc as any).autoTable({
    startY: 100,
    head: [columns],
    body: rows,
    theme: 'grid',
    headStyles: { fillColor: [22, 160, 133] },
    styles: { halign: 'center' }
  });

  const finalY = (doc as any).lastAutoTable.finalY || 10;
  doc.setFontSize(12);
  doc.setTextColor(0, 0, 0);
  doc.text('Gracias por su compra', 105, finalY + 20, { align: 'center' });

  // Cuadro para el total
  doc.setFillColor(255, 255, 255);
  doc.setDrawColor(0, 0, 0);
  doc.rect(140, finalY + 10, 60, 10, 'FD');
  doc.setTextColor(0, 0, 0);
  doc.text(`Total: S/.${sale.totalSale}`, 170, finalY + 18, { align: 'right' });

  doc.save('detalle_venta.pdf');
}

// Descargar detalle en Excel
downloadExcel(sale: any) {
  const data = sale.saleDetails.map((detail: any) => ({
    'Cliente': sale.clientNames,
    'Vendedor': sale.sellerNames,
    'Método de Pago': sale.paymentMethod.name,
    'Fecha': sale.formattedDateTime,
    'Producto': detail.product.name,
    'Precio': detail.product.priceUnit.toFixed(2),
    'Cantidad': detail.amount,
    'Unidad de Venta': detail.product.unitSale,
    'Subtotal': detail.subtotal.toFixed(2),
    'Total': sale.totalSale
  }));

  const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
  const wb: XLSX.WorkBook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Detalle de Venta');

  // Aplicar estilos al archivo Excel
  const wscols = [
    { wch: 20 }, // Cliente
    { wch: 20 }, // Vendedor
    { wch: 20 }, // Método de Pago
    { wch: 15 }, // Fecha
    { wch: 20 }, // Producto
    { wch: 10 }, // Precio
    { wch: 10 }, // Cantidad
    { wch: 15 }, // Unidad de Venta
    { wch: 15 }, // Subtotal
    { wch: 10 }  // Total
  ];
  ws['!cols'] = wscols;

  // Centrar los títulos
  const merge = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: 9 } }, // Título
    { s: { r: 1, c: 0 }, e: { r: 1, c: 9 } }, // Subtítulo
  ];
  ws['!merges'] = merge;

  XLSX.writeFile(wb, 'detalle_venta.xlsx');
}
}