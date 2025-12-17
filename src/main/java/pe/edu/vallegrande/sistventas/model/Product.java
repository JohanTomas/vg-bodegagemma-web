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
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_product_id", nullable = false)
    private CategoryProduct categoryProduct;

    @Column(name = "price_unit", nullable = false)
    private Double priceUnit;

    @Column(name = "unit_sale", nullable = false)
    private String unitSale;

    @Column(name = "date_expiry")
    private LocalDate dateExpiry;

    @Column(nullable = false)
    private Double stock;

    @Column(columnDefinition = "CHAR(1) DEFAULT 'A'")
    private String active;

    // Campo adicional para almacenar la fecha de expiración formateada
    @Setter
    @Transient
    private String dateExpiryFormatted;




}
