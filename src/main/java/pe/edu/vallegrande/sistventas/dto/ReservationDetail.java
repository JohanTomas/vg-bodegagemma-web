package pe.edu.vallegrande.sistventas.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import pe.edu.vallegrande.sistventas.model.Product;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservation_detail")
public class ReservationDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    @JsonBackReference
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "subtotal_reservation", nullable = true)
    private Double subtotalReservation;
}