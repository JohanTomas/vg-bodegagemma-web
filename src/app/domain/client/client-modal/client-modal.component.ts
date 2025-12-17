import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ClientService } from '../../../core/services/client.service';
import Swal from 'sweetalert2';
import { MatPaginator } from '@angular/material/paginator';
import { ErrorStateMatcher } from '@angular/material/core';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-client-modal',
  templateUrl: './client-modal.component.html',
  styleUrls: ['./client-modal.component.scss']
})
export class ClientModalComponent implements OnInit {
  clientForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<any>,
    private fb: FormBuilder,
    private clientService: ClientService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initClientForm();
    console.log('DATA:', this.data);
  }

  saveClient() {
    if (this.clientForm.invalid) {
      this.clientForm.markAllAsTouched();
      return;
    }
    if (this.data) {
      this.updateClient();
    } else {
      this.registerClient();
    }
  }

  registerClient() {
    this.clientService.newClient(this.clientForm.value).subscribe((res) => {
      console.log('Respuesta registrar cliente:', res);
      this.showInsertNotification();
      this.closeDialog(true);
    });
  }

  showInsertNotification() {
    Swal.fire({
      title: "Cliente Insertado!",
      text: "El cliente se ha insertado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.clientService.clientActualizar.next([]);
    });
  }

  updateClient() {
    this.clientService
      .updateClientById(this.data.id, this.clientForm.value)
      .subscribe((res) => {
        console.log('Respuesta actualizar cliente:', res);
        this.showEditNotification();
        this.closeDialog(true);
      });
  }

  showEditNotification() {
    Swal.fire({
      title: "Cliente Editado!",
      text: "El cliente se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.clientService.clientActualizar.next([]);
    });
  }

  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }

  // Inicialización del formulario y definición de validaciones
  initClientForm() {
    this.clientForm = this.fb.group({
      typeDocument: ['', [Validators.required]],
      numberDocument: ['', {
        validators: [Validators.required],
        asyncValidators: [this.validateNumberDocument.bind(this)],
      }],
      names: ['', [Validators.required, this.validateName.bind(this)]],
      lastName: ['', [Validators.required, this.validateLastName.bind(this)]],
      cellPhone: [null, [this.validateCellPhone.bind(this)]],
      email: [null, [this.validateEmail]],
      birthdate: [null, [this.validateAge]],
    });

    if (this.data) {
      this.clientForm.patchValue(this.data);
      // Quitamos la validación de documento existente cuando se está editando
      this.clientForm.get('numberDocument')?.setAsyncValidators([]);
    }
  }

  // Validación para nombres
  validateName = (control: FormControl) => {
    const namePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
    if (!namePattern.test(control.value)) {
      return { invalidName: true };
    }

    const namesParts = control.value.trim().split(/\s+/);
    for (const namePart of namesParts) {
      if (namePart.length > 0 && namePart[0] !== namePart[0].toUpperCase()) {
        return { firstLetterUpperCase: true };
      }
    }

    return null;
  };

  // Validación para apellidos
  validateLastName = (control: FormControl) => {
    const lastNamePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
    if (!lastNamePattern.test(control.value)) {
      return { invalidLastName: true };
    }

    const lastNameParts = control.value.trim().split(/\s+/);
    for (const lastNamePart of lastNameParts) {
      if (lastNamePart.length > 0 && lastNamePart[0] !== lastNamePart[0].toUpperCase()) {
        return { firstLetterUpperCase: true };
      }
    }

    return null;
  };

  // Validación para número de documento
  async validateNumberDocument(control: AbstractControl) {
    const typeDocument = this.clientForm.get('typeDocument')?.value;
    const numberDocument = control.value;
    if (!typeDocument) {
      return { typeDocumentRequired: true };
    }
    if (typeDocument === 'DNI' && (numberDocument.toString().length !== 8 || isNaN(numberDocument))) {
      return { invalidDNI: true };
    }
    if (typeDocument === 'CNE' && (numberDocument.toString().length !== 20 || isNaN(numberDocument))) {
      return { invalidCNE: true };
    }
    const isDocumentExists = await this.clientService.checkDocumentExists(numberDocument).toPromise();
    if (isDocumentExists) {
      return { documentExists: true };
    }
    return null;
  }

  // Validación para número de celular
  validateCellPhone = (control: FormControl) => {
    if (!control.value) {
      return null; // Permite valor nulo
    }

    const cellPhoneStartsWithNine = control.value.startsWith('9');
    const cellPhonePattern = /^[0-9]{9}$/;
    if (!cellPhoneStartsWithNine) {
      return { cellPhoneStartsWithNine: true };
    }
    if (!cellPhonePattern.test(control.value)) {
      return { invalidCellPhone: true };
    }

    return null;
  };

  // Validación para correo electrónico
  validateEmail = (control: FormControl) => {
    if (!control.value) {
      return null; // Permite valor nulo
    }

    const emailPattern = /^[a-zA-Z0-9._,:-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailPattern.test(control.value)) {
      return { invalidEmail: true };
    }
    return null;
  };

  // Validación para fecha de nacimiento
  validateAge = (control: FormControl) => {
    if (!control.value) {
      return null; // Permite valor nulo
    }

    const birthdate = new Date(control.value);
    const today = new Date();
    let age = today.getFullYear() - birthdate.getFullYear();

    if (today.getMonth() < birthdate.getMonth() || (today.getMonth() === birthdate.getMonth() && today.getDate() < birthdate.getDate())) {
      age--;
    }

    if (age < 18) {
      return { invalidAge: true };
    }

    return null;
  };

  // Getter para obtener los controles del formulario
  get form() {
    return this.clientForm.controls;
  }
}
