import { PurchaseModalComponent } from './purchase-modal/purchase-modal.component';
import { PurchaseService } from '../../core/services/purchase.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';


@Component({
  selector: 'app-purchase',
  templateUrl: './purchase.component.html',
  styleUrl: './purchase.component.scss'
})
export class PurchaseComponent implements OnInit {
  public displayedColumns: string[] = ['id', 'supplier', 'seller', 'paymentMethod', 'date', 'total', 'editar-eliminar'];
  public dataSourceP: MatTableDataSource<any>;
  public purchases: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactivePurchases: boolean = false;

  constructor(private purchaseService: PurchaseService, private dialog: MatDialog) {
    this.dataSourceP = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.purchaseService.purchaseActualizar.subscribe(() => {
      this.getPurchases();
      this.dataSourceP.sort = this.sort;
    });
    this.getPurchases();
  }

  // Lista de compras activas con paginador
  getPurchases() {
    this.purchaseService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.purchases = res.content;
        this.dataSourceP.data = this.purchases;
        this.dataSourceP.sort = this.sort;
      }
    );
  }

  // Lista de compras inactivas con paginador
  getPurchasesInactive() {
    this.purchaseService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.purchases = res.content;
        this.dataSourceP.data = this.purchases;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactivePurchases) {
      this.purchaseService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.purchases = res.content;
          this.dataSourceP.data = this.purchases;
        }
      );
    } else {
      this.purchaseService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.purchases = res.content;
          this.dataSourceP.data = this.purchases;
        }
      );
    }
  }

  // Cambiar botones y listar compras activas e inactivas
  toggleButtons() {
    this.showingInactivePurchases = !this.showingInactivePurchases;
    if (this.showingInactivePurchases) {
      this.getPurchasesInactive();
    } else {
      this.getPurchases();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de una compra
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar esta compra?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su compra ha sido eliminada.",
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

    this.purchaseService.disablePurchaseById(Id).subscribe(() => {
      this.purchaseService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.purchases = res.content;
        this.dataSourceP.data = this.purchases;

        if (this.purchases.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar compra
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar esta compra?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su compra ha sido restaurada.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar compra
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.purchaseService.activatePurchaseById(Id).subscribe(() => {
      this.purchaseService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.purchases = res.content;
        this.dataSourceP.data = this.purchases;

        if (this.purchases.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(purchase?: any) {
    this.dialog.open(PurchaseModalComponent, {
      disableClose: true,
      width: '1200px',
      height: 'auto',
      data: purchase,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceP.filter = valor.trim().toLowerCase();
  }

  // Método para exportar compras a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["ID", "Proveedor", "Vendedor", "Método de Pago", "Fecha", "Total"];
    const rows = this.dataSourceP.data.map(purchase => [
      purchase.id, purchase.supplierNames, purchase.sellerNames, purchase.paymentMethod.name, purchase.formattedDateTime, purchase.totalPurchase
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_compras.pdf');
  }

  // Método para exportar compras a Excel
  exportExcel() {
    const filteredData = this.dataSourceP.data.map(({ id, supplierNames, sellerNames, paymentMethod, formattedDateTime, totalPurchase }) => ({
      'ID': id,
      'Proveedor': supplierNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalPurchase
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Compras');
    XLSX.writeFile(wb, 'Compras.xlsx');
  }

  // Método para exportar compras a CSV
  exportCSV() {
    const filteredData = this.dataSourceP.data.map(({ id, supplierNames, sellerNames, paymentMethod, formattedDateTime, totalPurchase }) => ({
      'ID': id,
      'Proveedor': supplierNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalPurchase
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Compras.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  verDetalle(Id: number) {
    this.purchaseService.getPurchaseById(Id).subscribe((purchase: any) => {
      // Calcular los subtotales
      purchase.purchaseDetails.forEach((detail: any) => {
        detail.subtotal = this.calculateItemTotal(detail); // Usar la función calculateItemTotal para calcular el subtotal
      });
  
      Swal.fire({
        title: '<strong>Detalle de la Compra</strong>',
        html: `
          <div id="purchase-detail" style="text-align: left; font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #f9f9f9;">
            <h3 style="text-align: center; margin-bottom: 20px;">Factura Electrónica</h3>
            <p><strong>ID:</strong> ${purchase.id}</p>
            <p><strong>Proveedor:</strong> ${purchase.supplierNames}</p>
            <p><strong>Vendedor:</strong> ${purchase.sellerNames}</p>
            <p><strong>Método de Pago:</strong> ${purchase.paymentMethod.name}</p>
            <p><strong>Fecha:</strong> ${purchase.formattedDateTime}</p>
            <p><strong>Total:</strong> S/.${purchase.totalPurchase}</p>
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
                ${purchase.purchaseDetails.map((detail: { product: { name: any; priceUnit: any; unitSale: any; }; amount: any; subtotal: any; }) => `
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
          this.downloadPDF(purchase);
        } else if (result.isDenied) {
          this.downloadExcel(purchase);
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
downloadPDF(purchase: any) {
  const doc = new jsPDF();
  doc.setFontSize(18);
  doc.setTextColor(40, 40, 40);
  doc.text('Factura Electrónica', 105, 20, { align: 'center' });

  doc.setFontSize(12);
  doc.setTextColor(0, 102, 204);
  doc.text('Nombre del Proveedor:', 20, 40);
  doc.setTextColor(0, 0, 0);
  doc.text(`${purchase.supplierNames}`, 70, 40);

  doc.setTextColor(0, 102, 204);
  doc.text('Nombre del Vendedor:', 20, 50);
  doc.setTextColor(0, 0, 0);
  doc.text(`${purchase.sellerNames}`, 70, 50);

  doc.setTextColor(0, 102, 204);
  doc.text('Método de Pago:', 20, 60);
  doc.setTextColor(0, 0, 0);
  doc.text(`${purchase.paymentMethod.name}`, 70, 60);

  // Mover la fecha a la parte superior derecha
  doc.setTextColor(0, 0, 0);
  doc.text(`Fecha: ${purchase.formattedDateTime}`, 200, 30, { align: 'right' });

  doc.setFontSize(14);
  doc.setTextColor(40, 40, 40);
  doc.text('Detalle de la Compra', 105, 90, { align: 'center' });

  const columns = ["Producto", "Precio", "Cantidad", "Unidad de Venta", "Subtotal"];
  const rows = purchase.purchaseDetails.map((detail: any) => [
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
  doc.text(`Total: S/.${purchase.totalPurchase}`, 170, finalY + 18, { align: 'right' });

  doc.save('detalle_compra.pdf');
}

// Descargar detalle en Excel
downloadExcel(purchase: any) {
  const data = purchase.purchaseDetails.map((detail: any) => ({
    'Proveedor': purchase.supplierNames,
    'Vendedor': purchase.sellerNames,
    'Método de Pago': purchase.paymentMethod.name,
    'Fecha': purchase.formattedDateTime,
    'Producto': detail.product.name,
    'Precio': detail.product.priceUnit.toFixed(2),
    'Cantidad': detail.amount,
    'Unidad de Venta': detail.product.unitSale,
    'Subtotal': detail.subtotal.toFixed(2),
    'Total': purchase.totalPurchase
  }));

  const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
  const wb: XLSX.WorkBook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Detalle de Compra');

  // Aplicar estilos al archivo Excel
  const wscols = [
    { wch: 20 }, // Proveedor
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

  XLSX.writeFile(wb, 'detalle_compra.xlsx');
}
}