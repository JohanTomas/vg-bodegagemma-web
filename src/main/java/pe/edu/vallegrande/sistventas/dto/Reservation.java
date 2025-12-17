package pe.edu.vallegrande.sistventas.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.model.PaymentMethod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Person client;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Person seller;

    @ManyToOne
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reservation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date reservationDate;

    @Column(name = "active", length = 1)
    private String active = "A";

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationDetail> reservationDetails;

    @Transient
    private String clientNames;

    @Transient
    private String sellerNames;

    @Transient
    private String formattedDateTime;

    @Column(name = "total_reservation")
    private Double totalReservation;

    @PrePersist
    protected void onCreate() {
        if (reservationDate == null) {
            reservationDate = new Date();
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void setTransientFields() {
        if (client != null) {
            this.clientNames = client.getNames() + " " + client.getLastName();
        }
        if (seller != null) {
            this.sellerNames = seller.getNames() + " " + seller.getLastName();
        }
        if (reservationDate != null) {
            this.formattedDateTime = formatDate(reservationDate);
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        return formatter.format(date);
    }
}