// src/app/layout/layout.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit {
  private breakpointObserver = inject(BreakpointObserver);
  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(
      map(result => result.matches),
      shareReplay()
    );

  icon: string = 'menu';
  userRole: string = '';
  names: string = '';
  lastName: string = '';

  constructor(private router: Router) {}

  ngOnInit() {
    this.userRole = localStorage.getItem('role') || '';
    this.names = localStorage.getItem('names') || ''; 
    this.lastName = localStorage.getItem('lastName') || '';
  }

  toggleIcon() {
    this.icon = this.icon === 'menu' ? 'arrow_back' : 'menu';
  }

  logout() {
    localStorage.removeItem('user');
    localStorage.removeItem('role'); // Eliminar también el rol del usuario
    this.router.navigate(['/login']);
  }

  navigateTo(route: string) {
    this.router.navigate([route]);
  }
}
