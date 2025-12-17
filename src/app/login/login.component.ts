import { SellerService } from './../core/services/seller.service';
import { Component, OnInit } from '@angular/core';
import Swal from 'sweetalert2';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  username: string = '';
  password: string = '';
  sellers: any[] = [];
  hidePassword: boolean = true;
  constructor(private router: Router, private sellerService: SellerService) { }

  ngOnInit() {
    // Obtener la lista de vendedores activos cuando el componente se inicializa
    this.sellerService.listActiveSellers().subscribe(
      (data) => {
        this.sellers = data;
      },
      (error) => {
        console.error('Error al obtener la lista de vendedores activos', error);
      }
    );
  }

  onLogin() {
    const seller = this.sellers.find(s => s.sellerUser === this.username && s.sellerPassword === this.password);
    if (seller) {
      const userId = seller.id; // Obtenemos el ID del vendedor
      console.log('ID del usuario: ' + userId); // Mostramos el ID del usuario en una alerta
      localStorage.setItem('user', this.username);
      localStorage.setItem('role', seller.sellerRol);
      localStorage.setItem('userId', userId); // Guardamos el ID del usuario en el almacenamiento local
      localStorage.setItem('names', seller.names);  // Guardar el nombre del usuario
      localStorage.setItem('lastName', seller.lastName);
      localStorage.setItem('password', `${seller.sellerPassword}`); // Guardar el nombre completo del usuario
      this.router.navigate(['/dashboard']);
    } else {
      Swal.fire({
        title: "Error",
        text: "Usuario o contraseña inválidos",
        icon: "error",
        confirmButtonText: "Aceptar"
      });
    }
  }
  togglePasswordVisibility() {
    this.hidePassword = !this.hidePassword;
  }

}
