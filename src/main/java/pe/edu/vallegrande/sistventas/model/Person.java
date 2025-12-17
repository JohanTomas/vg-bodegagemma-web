package pe.edu.vallegrande.sistventas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rol_person")
    private String rolPerson;

    @Column(name = "type_document")
    private String typeDocument;

    @Column(name = "number_document", unique = true)
    private String numberDocument;

    @Column
    private String names;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "cell_phone")
    private String cellPhone;

    @Column
    private String email;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column
    private Double salary;

    @Column(name = "seller_rol")
    private String sellerRol;

    @Column(name = "seller_user")
    private String sellerUser;

    @Column(name = "seller_password")
    private String sellerPassword;

    @Column(columnDefinition = "CHAR(1) DEFAULT 'A'")
    private String active;

    // Método setter para el campo birthdateFormatted
    // Campo adicional para almacenar la fecha de nacimiento formateada
    @Setter
    @Transient
    private String birthdateFormatted;

}
