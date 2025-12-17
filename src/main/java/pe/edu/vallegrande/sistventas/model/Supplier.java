package pe.edu.vallegrande.sistventas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "supplier")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String ruc;

    @Column(name = "name_company")
    private String nameCompany;

    @Column(name = "type_document")
    private String typeDocument;

    @Column(name = "number_document", unique = true)
    private String numberDocument;

    @Column
    private String names;

    @Column(name = "last_name")
    private String lastName;

    @Column
    private String email;

    @Column(name = "cell_phone")
    private String cellPhone;

    @Column
    private String active;
}
