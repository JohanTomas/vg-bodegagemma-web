import { DataServiceService } from './../../core/services/data.service';
import { Component, inject } from '@angular/core';
import { Breakpoints, BreakpointObserver } from '@angular/cdk/layout';
import { map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {

  private breakpointObserver = inject(BreakpointObserver);

  constructor(private dataService: DataServiceService) { }

  cards = this.breakpointObserver.observe(Breakpoints.Handset).pipe(
    switchMap(({ matches }) => {
      return this.dataService.getDashboardData().pipe(
        map((data: any) => {
          return [
            { title: 'Ventas', cols: 1, rows: 1, total: data.sales.totalElements , description: 'Ventas realizadas', icon: 'shopping_cart', color: '#003aa5', content: '#0056f5' },
            { title: 'Compras', cols: 1, rows: 1, total: data.purchases.totalElements , description: 'Compras realizadas', icon: 'shopping_basket', color: '#a70101', content: '#cf0000' },
            { title: 'Clientes', cols: 1, rows: 1, total: data.clients.totalElements , description: 'Clientes registrados', icon: 'group', color: '#015837', content: '#039b61' },
            { title: 'Productos', cols: 1, rows: 1, total: data.products.totalElements , description: 'Productos disponibles', icon: 'category', color: '#4602a0', content: '#6300e4' },
            { title: 'Categorías', cols: 1, rows: 1, total: data.categories.totalElements , description: 'Categorías registradas', icon: 'label', color: '#016674', content: '#02abc2' },
            { title: 'Proveedores', cols: 1, rows: 1, total: data.suppliers.totalElements , description: 'Proveedores registrados', icon: 'local_shipping', color: '#aa9f02', content: '#d6c803' },
            { title: 'Vendedores', cols: 1, rows: 1, total: data.sellers.totalElements , description: 'Vendedores registrados', icon: 'work', color: '#7e3d01', content: '#c76000' },
            { title: 'Reservas', cols: 1, rows: 1, total: data.reservations.totalElements , description: 'Reservas realizadas', icon: 'event', color: '#0288d1', content: '#03a9f4' } // Añade las reservas
          ];
        })
      );
    })
  );
}