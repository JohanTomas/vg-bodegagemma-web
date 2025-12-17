package pe.edu.vallegrande.sistventas.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.sistventas.model.PaymentMethod;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.model.Supplier;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "purchase")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Person seller;

    @ManyToOne
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "date_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateTime;

    @Column(name = "active", length = 1)
    private String active = "A";

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseDetail> purchaseDetails;

    @Transient
    private String supplierNames;

    @Transient
    private String sellerNames;

    @Transient
    private String formattedDateTime;

    @Column(name = "total_purchase")
    private Double totalPurchase;

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
        if (supplier != null) {
            this.supplierNames = supplier.getNames() + " " + supplier.getLastName();
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
