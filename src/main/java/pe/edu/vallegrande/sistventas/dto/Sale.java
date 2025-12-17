package pe.edu.vallegrande.sistventas.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.model.PaymentMethod;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sale")
public class Sale {
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

    @Column(name = "date_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateTime;

    @Column(name = "active", length = 1)
    private String active = "A";

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDetail> saleDetails;

    @Transient
    private String clientNames;

    @Transient
    private String sellerNames;

    @Transient
    private String formattedDateTime;

    @Column(name = "total_sale")
    private Double totalSale;

    @PrePersist
    protected void onCreate() {
        if (dateTime == null) {
            dateTime = new Date();
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
        if (dateTime != null) {
            this.formattedDateTime = formatDate(dateTime);
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        return formatter.format(date);
    }
}
