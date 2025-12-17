import { SellerService } from './../../core/services/seller.service';
import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, FormGroupDirective, NgForm, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import Swal from 'sweetalert2';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatPaginator } from '@angular/material/paginator';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-account-password',
  templateUrl: './account-password.component.html',
  styleUrls: ['./account-password.component.scss']
})
export class AccountPasswordComponent implements OnInit {
  sellerForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<any>,
    private fb: FormBuilder,
    private sellerService: SellerService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initSellerForm();
    console.log('DATA:', this.data);
  }

  saveSeller() {
    if (this.sellerForm.invalid) {
      this.sellerForm.markAllAsTouched();
      return;
    }

    const { oldPassword, newPassword } = this.sellerForm.value;

    this.sellerService.changePassword(this.data.id, { oldPassword, newPassword })
      .subscribe(
        (response) => {
          this.showEditNotification();
          this.closeDialog(true);
        },
        (error) => {
          console.error('Error al actualizar la contraseña:', error);
          this.showEditNotification(); // Aquí se llama a la función para mostrar la notificación
          this.closeDialog(true); // Cerramos el diálogo incluso si hay un error
        }
      );
  }

  showEditNotification() {
    Swal.fire({
      title: "Contraseña Actualizada!",
      text: "La contraseña se ha actualizado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.sellerService.sellerActualizar.next([]);
    });
  }

  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }

  initSellerForm() {
    this.sellerForm = this.fb.group({
      oldPassword: ['', [Validators.required, this.validateOldPassword.bind(this)]],
      newPassword: ['', [Validators.required]],
      confirmPassword: ['', [Validators.required, this.validateNewPassword.bind(this)]]
    });

    if (this.data) {
      this.sellerForm.patchValue(this.data);
    }
  }

  validateOldPassword(control: FormControl): ValidationErrors | null {
    if (this.data && this.data.sellerPassword && control.value !== this.data.sellerPassword) {
      return { incorrectPassword: true };
    }
    return null;
  }

  validateNewPassword(control: FormControl): ValidationErrors | null {
    const newPassword = this.sellerForm?.get('newPassword')?.value;
    if (newPassword !== control.value) {
      return { passwordsNotMatch: true };
    }
    return null;
  }

  get form() {
    return this.sellerForm.controls;
  }
}
