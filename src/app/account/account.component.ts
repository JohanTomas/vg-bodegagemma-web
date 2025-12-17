import { Component, OnInit } from '@angular/core';
import { SellerService } from '../core/services/seller.service';
import { MatDialog } from '@angular/material/dialog';
import { AccountModalComponent } from './account-modal/account-modal.component';
import { AccountPasswordComponent } from './account-password/account-password.component';

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  userId: number = 0;
  seller: any;
  rolPerson: string = '';
  typeDocument: string = '';
  numberDocument: string = '';
  names: string = '';
  lastName: string = '';
  cellPhone: string = '';
  email: string = '';
  birthdate: string = '';
  salary: number | null = null;
  sellerRol: string | null = null;
  sellerUser: string | null = null;
  sellerPassword: string | null = null;
  active: string = '';
  birthdateFormatted: string | null = null;

  constructor(private dialog: MatDialog, private sellerService: SellerService) { }

  ngOnInit() {
    console.log('OnInit');
    this.sellerService.sellerActualizar.subscribe(() => {
      this.getSeller();
      this.getUserIdFromStorage();
    });
    this.getUserIdFromStorage();
    this.getSeller();
  }

  getUserIdFromStorage() {
    const userIdFromStorage = localStorage.getItem('userId');
    if (userIdFromStorage) {
      this.userId = parseInt(userIdFromStorage);
    } else {
      console.error('No se encontró el ID del usuario en el almacenamiento local.');
    }
  }

  getSeller() {
    this.sellerService.getSellerById(this.userId).subscribe(
      (data) => {
        this.seller = data.data; // Acceder a la propiedad 'data' de la respuesta
        // Asigna los valores a las propiedades del componente
        this.rolPerson = this.seller.rolPerson;
        this.typeDocument = this.seller.typeDocument;
        this.numberDocument = this.seller.numberDocument;
        this.names = this.seller.names;
        this.lastName = this.seller.lastName;
        this.cellPhone = this.seller.cellPhone;
        this.email = this.seller.email;
        this.birthdate = this.seller.birthdate;
        this.salary = this.seller.salary;
        this.sellerRol = this.seller.sellerRol;
        this.sellerUser = this.seller.sellerUser;
        this.sellerPassword = this.seller.sellerPassword;
        this.active = this.seller.active;
        this.birthdateFormatted = this.seller.birthdateFormatted;
      },
      (error) => {
        console.error('Error al obtener los datos del vendedor', error);
      }
    );
  }

  openEditModal() {
    this.dialog.open(AccountModalComponent, {
      disableClose: true,
      width: '50rem',
      height: 'auto',
      data: this.seller
    });
  }
  openPasswordDialog() {
    this.dialog.open(AccountPasswordComponent, {
      disableClose: true,
      width: '50rem',
      height: 'auto',
      data: this.seller
    });
  }
  
}


